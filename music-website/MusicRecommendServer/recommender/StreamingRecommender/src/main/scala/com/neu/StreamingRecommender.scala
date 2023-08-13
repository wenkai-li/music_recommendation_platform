package com.neu

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import kafka.Kafka
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.log4j.PropertyConfigurator
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis



//定义链接助手对象，序列化
object ConnHelper extends Serializable{
  lazy val jedis = new Jedis("localhost")
  lazy val mongoClient = MongoClient( MongoClientURI("mongodb://localhost:27017/recommender") )
}

case class MongoConfig(uri:String, db:String)
//基准推荐对象
case class Recommendation( mid: Int, score: Double)
//基于预测评分的用户推荐列表
case class UserRecs(uid: Int, recs: Seq[Recommendation])
//基于LFM的艺术家特征向量的艺术家推荐列表
case class MusicRecs(mid: Int, recs: Seq[Recommendation])


object StreamingRecommender {

  val MAX_USER_RATINGS_NUM = 20
  val MAX_SIM_MUSIC_NUM = 20
  val MONGODB_STREAM_RECS_COLLECTION = "StreamRecs"
  val MONGODB_RATING_COLLECTION = "Rating"
  val MONGODB_MUSIC_RECS_COLLECTION = "ContentMusicRecs"
  PropertyConfigurator.configure(this.getClass.getClassLoader.getResource("log4.properties").getPath)
  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommender",
      "mongo.db" -> "recommender",
      "kafka.topic" -> "mrd"
    )

    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("StreamingRecommender")

    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    // 拿到streaming context
    val sc = spark.sparkContext
    val ssc = new StreamingContext(sc, Seconds(2))  // batch duration

    import spark.implicits._

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    // 加载艺术家相似度矩阵数据，广播数据
    val simMusicMatrix = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_MUSIC_RECS_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[MusicRecs]
      .rdd
      .map{musicRecs =>
        (musicRecs.mid, musicRecs.recs.map( x=> (x.mid, x.score)).toMap)
      }.collectAsMap()

    val simMusicMatrixBroadCast = sc.broadcast(simMusicMatrix)

    // 定义kafka连接参数
    val kafkaParam = Map(
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "mrd",
      "auto.offset.reset" -> "latest"
    )

    // 通过kafka创建一个DStream
    val kafkaStream = KafkaUtils.createDirectStream[String, String](ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](Array(config("kafka.topic")), kafkaParam)
    )

    // 把原始数据UID|MID|SCORE|TIMESTAMP 转换成评分流
//    测试 1|11|6|1680188861
    val ratingStream = kafkaStream.map {
      msg =>
        val attr = msg.value().split("\\|")
        (attr(0).toInt, attr(1).toInt, attr(2).toInt, attr(3).toInt)
    }

    //继续做流式处理，核心实时算法部分
    ratingStream.foreachRDD{
      rdds => rdds.foreach{
        case(uid, mid, score, timestamp) =>{
          println("rating data is coming! ============")

          //1.从redis中获得当前用户最近的k次评分，保存成Array[(mid, score)]
          val userRecentlyRatings = getUserRecentlyRating(MAX_USER_RATINGS_NUM, uid, ConnHelper.jedis)
          println("Get from redis finished")
          //2.从相似度矩阵中当前艺术家最相似的n个艺术家，作为备选列表，Array[mid]
          val candidateMusic = getTopSimMusic(MAX_SIM_MUSIC_NUM, mid, uid, simMusicMatrixBroadCast.value)

          //3.对每个备选艺术家，计算推荐优先级，得到当前用户的实时推荐列表，Array[]
          val streamRecs = computeMusicScores(candidateMusic, userRecentlyRatings, simMusicMatrixBroadCast.value)
          //4.将推荐数据保存在mongodb
          saveDataToMongoDB(uid, streamRecs)
        }
      }
    }
    //开始接收和处理数据
    ssc.start()

    println(">>>>>>>>>>>>>>> streaming started!")

    ssc.awaitTermination()
  }
  // redis操作返回的是java类，为了用map操作需要引入转换类

  import scala.collection.JavaConversions._

  def getUserRecentlyRating(num: Int, uid: Int, jedis: Jedis): Array[(Int, Double)] = {
    // 从redis读取数据，用户评分数据保存在 uid:UID 为key的队列里，value是 AID:SCORE
    jedis.lrange("uid:" + uid, 0, num - 1)
      .map {
        item => // 具体每个评分又是以冒号分隔的两个值
          val attr = item.split("\\:")
          (attr(0).trim.toInt, attr(1).trim.toDouble)
      }
      .toArray

  }

  def getTopSimMusic(num: Int, mid: Int, uid: Int, simMusic: scala.collection.Map[Int, scala.collection.immutable.Map[Int, Double]])
                     (implicit mongoConfig: MongoConfig): Array[Int] = {
    // 1. 从相似度矩阵中拿到所有相似的艺术家
    val allSimMusics = simMusic(mid).toArray

    // 2. 从mongodb中查询用户已看过的艺术家
//    val ratingExist = ConnHelper.mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION)
//      .find(MongoDBObject("uid" -> uid))
//      .toArray
//      .map {
//        item => item.get("mid").toString.toInt
//      }

    // 3. 进行排序得到输出列表
    allSimMusics.sortWith(_._2 > _._2)
      .take(num)
      .map(x => x._1)
  }

  def computeMusicScores(candidatesMusics: Array[Int],
                         userRecentlyRatings: Array[(Int, Double)],
                         simMusics: scala.collection.Map[Int, scala.collection.immutable.Map[Int, Double]]): Array[(Int, Double)] = {
    // 定义一个ArrayBuffer，用于保存每一个备选艺术家的基础得分
    val scores = scala.collection.mutable.ArrayBuffer[(Int, Double)]()
    // 定义一个HashMap，保存每一个备选艺术家的增强减弱因子
    val increMap = scala.collection.mutable.HashMap[Int, Int]()
    val decreMap = scala.collection.mutable.HashMap[Int, Int]()

    for (candidatesMusics <- candidatesMusics; userRecentlyRating <- userRecentlyRatings) {
      // 拿到备选艺术家和最近评分艺术家的相似度
      val simScore = getMusicsSimScore(candidatesMusics, userRecentlyRating._1, simMusics)

      if (simScore > 0.5) {
        // 计算备选艺术家的基础推荐得分
        scores += ((candidatesMusics, simScore * userRecentlyRating._2))
        if (userRecentlyRating._2 > 3) {
          increMap(candidatesMusics) = increMap.getOrDefault(candidatesMusics, 0) + 1
        } else {
          decreMap(candidatesMusics) = decreMap.getOrDefault(candidatesMusics, 0) + 1
        }
      }
    }
    // 根据备选艺术家的mid做groupby，根据公式去求最后的推荐评分
    scores.groupBy(_._1).map {
      // groupBy之后得到的数据 Map( mid -> ArrayBuffer[(mid, score)] )
      case (mid, scoreList) =>
        (mid, scoreList.map(_._2).sum / scoreList.length + log(increMap.getOrDefault(mid, 1)) - log(decreMap.getOrDefault(mid, 1)))
    }.toArray.sortWith(_._2 > _._2)
  }

  // 获取两个艺术家之间的相似度
  def getMusicsSimScore(mid1: Int, mid2: Int, simMusic: scala.collection.Map[Int,
    scala.collection.immutable.Map[Int, Double]]): Double = {

    simMusic.get(mid1) match {
      case Some(sims) => sims.get(mid2) match {
        case Some(score) => score
        case None => 0.0
      }
      case None => 0.0
    }
  }

  // 求一个数的对数，利用换底公式，底数默认为10
  def log(m: Int): Double = {
    val N = 10
    math.log(m) / math.log(N)
  }

  def saveDataToMongoDB(uid: Int, streamRecs: Array[(Int, Double)])(implicit mongoConfig: MongoConfig): Unit = {
    // 定义到StreamRecs表的连接
    val streamRecsCollection = ConnHelper.mongoClient(mongoConfig.db)(MONGODB_STREAM_RECS_COLLECTION)

    // 如果表中已有uid对应的数据，则删除
    streamRecsCollection.findAndRemove(MongoDBObject("uid" -> uid))
    // 将streamRecs数据存入表中
    streamRecsCollection.insert(MongoDBObject("uid" -> uid,
      "recs" -> streamRecs.map(x => MongoDBObject("mid" -> x._1, "score" -> x._2))))
  }

}
