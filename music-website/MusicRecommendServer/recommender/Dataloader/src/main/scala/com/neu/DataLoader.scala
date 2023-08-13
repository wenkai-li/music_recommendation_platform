package com.neu

import java.net.InetAddress
import com.mongodb.casbah.Imports.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient


case class Music(mid: Int, name: String, intro: String, tag: String, pic: String)
case class Rating(uid: Int, mid: Int ,score: Double, timestamp: Int)
case class Tag(uid: Int, aid: Int, tag: String, timestamp: Int)

//把mongo和es封装成样例类
case class MongoConfig(uri:String, db:String)

/**
 * @param httpHost        http主机列表
 * @param transportHosts  transport主机列表
 * @param index           需要操作的索引
 * @param clusternames    集群名称
 */
case class ESConfig(httpHost: String, transportHosts: String, index: String, clusternames: String)

object DataLoader {

  // 常量定义

  val MUSIC_DATA_PATH = this.getClass.getClassLoader.getResource("music.csv").getPath
  val RATING_DATA_PATH = this.getClass.getClassLoader.getResource("rating.csv").getPath
//  val TAG_DATA_PATH = "/Users/leo/JavaProgram/final/MusicRecommendation/MusicRecommendServer/recommender/Dataloader/src/main/resources/music.csv"
  val MONGODB_MUSIC_COLLECTION = "Music"
  val MONGODB_RATING_COLLECTION = "Rating"
  val MONGODB_TAG_COLLECTION = "Tag"
  val ES_MUSIC_INDEX = "Music"

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommender",
      "mongo.db" -> "recommender",
      "es.httpHosts" -> "localhost:9200",
      "es.transportHosts" -> "localhost:9300",
      "es.index" -> "recommender",
      "es.cluster.name" -> "elasticsearch_brew"
    )
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("DataLoader")

    // 创建一个SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._
    //加载数据
    val musicRDD = spark.sparkContext.textFile(MUSIC_DATA_PATH)
    val ratingRDD = spark.sparkContext.textFile(RATING_DATA_PATH)
//    val tagRDD = spark.sparkContext.textFile(TAG_DATA_PATH)
    // 最后一列当不存在值的时候，已替换为" "
    val musicDF = musicRDD.map(
      item => {
        val attr = item.split(",")
        Music(attr(0).toInt, attr(1), attr(2), attr(3), attr(4))
      }
    ).toDF()

    val ratingDF = ratingRDD.map(
      item => {
        val attr = item.split(",")
        Rating(attr(0).toInt, attr(1).toInt, attr(2).toDouble, attr(3).toInt)
      }
    ).toDF()


//    val tagDF = tagRDD.map(
//      item => {
//        val attr = item.split(",")
//        Tag(attr(0).toInt, attr(1).toInt, attr(3).trim, attr(2).toInt)
//      }
//    ).toDF()

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    //将数据保存到mongoDB
    storeDataInMongoDB(musicDF, ratingDF)

    //数据预处理(把music对应的tag信息添加)
    import org.apache.spark.sql.functions._
    /**
     * tid
     *  tags: tag1|tag2|tag3
     */
//    val newTag = tagDF.groupBy($"aid").agg(concat_ws("|",collect_set($"tag")).as("tags"))
//      .select("aid","tags")
//
//    // newTag join with Music, use left join
//    val musicWithTagsDF = musicDF.join(newTag, Seq("aid"), "left")

    implicit val esConfig = ESConfig(config("es.httpHosts"), config("es.transportHosts"), config("es.index"), config("es.cluster.name"))

    //将数据保存到es
    storeDataInES(musicDF)

    spark.stop()
  }

  def storeDataInMongoDB(musicDF:DataFrame, ratingDF: DataFrame)(implicit mongoConfig: MongoConfig): Unit = {
    // 创建一个mongoDB的连接
    val mongoClient = MongoClient(MongoClientURI(mongoConfig.uri))

    //如果mongodb中已经有相应的数据库，先删除
    mongoClient(mongoConfig.db)(MONGODB_MUSIC_COLLECTION).dropCollection()
    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).dropCollection()
//    mongoClient(mongoConfig.db)(MONGODB_TAG_COLLECTION).dropCollection()

    //将对应的数据写进Mongodb表中
    musicDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_MUSIC_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    ratingDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

//    tagDF.write
//      .option("uri", mongoConfig.uri)
//      .option("collection", MONGODB_TAG_COLLECTION)
//      .mode("overwrite")
//      .format("com.mongodb.spark.sql")
//      .save()

    mongoClient(mongoConfig.db)(MONGODB_MUSIC_COLLECTION).createIndex(MongoDBObject("mid" -> 1))
    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).createIndex(MongoDBObject("uid" -> 1))
    mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).createIndex(MongoDBObject("mid" -> 1))
//    mongoClient(mongoConfig.db)(MONGODB_TAG_COLLECTION).createIndex(MongoDBObject("uid" -> 1))
//    mongoClient(mongoConfig.db)(MONGODB_TAG_COLLECTION).createIndex(MongoDBObject("mid" -> 1))

    mongoClient.close()
  }


  def storeDataInES(musicDF: DataFrame)(implicit esConfig: ESConfig): Unit = {

//    musicDF.drop("intro");
    //新建es配置
    val settings: Settings = Settings.builder().put("cluster.name", esConfig.clusternames).build()

    //新建es客户端
    val esClient = new PreBuiltTransportClient(settings)

    val REGEX_HOST_PORT = "(.+):(\\d+)".r
    esConfig.transportHosts.split(",").foreach{
      case REGEX_HOST_PORT(host: String, port: String) =>{
        esClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port.toInt))
      }
    }

    //清理遗留的数据
    if( esClient.admin().indices().exists( new IndicesExistsRequest(esConfig.index))
      .actionGet()
      .isExists
    ){
      esClient.admin().indices().delete(new DeleteIndexRequest(esConfig.index))
    }

    esClient.admin().indices().create( new CreateIndexRequest(esConfig.index))

    musicDF.write
      .option("es.nodes", esConfig.httpHost)
      .option("es.http.timeout", "100m")
      .option("es.mapping.id", "mid")
      .mode("overwrite")
      .format("org.elasticsearch.spark.sql")
      .save(esConfig.index + "/" + ES_MUSIC_INDEX)

  }
}
