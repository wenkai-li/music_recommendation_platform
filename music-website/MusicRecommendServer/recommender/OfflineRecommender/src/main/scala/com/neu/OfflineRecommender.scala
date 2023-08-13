package com.neu

import org.apache.spark.SparkConf
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.sql.SparkSession
import org.jblas.DoubleMatrix


//case class MusicRating(uid: Int, mid: Int, score: Double, timestamp: Int )
case class MongoConfig(uri:String, db:String)
//基准推荐对象
case class Recommendation( mid: Int, score: Double)
//基于预测评分的用户推荐列表
case class UserRecs(uid: Int, recs: Seq[Recommendation])
//基于LFM的艺术家特征向量的艺术家推荐列表
case class MusicRecs(mid: Int, recs: Seq[Recommendation])

object OfflineRecommender {
  //定义表名和常量
  val MONGODB_MUSIC_COLLECTION = "Music"
  val MONGODB_RATING_COLLECTION = "Rating"

  val USER_RECS = "UserRecs"
  val MUSIC_RECS = "MusicRecs"

  val USER_MAX_RECOMMENDATION = 20
  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommender",
      "mongo.db" -> "recommender"
    )

    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("OfflineRecommender")

    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))
  //加载数据
  val ratingRDD = spark.read
    .option("uri", mongoConfig.uri)
    .option("collection", MONGODB_RATING_COLLECTION)
    .format("com.mongodb.spark.sql")
    .load()
    .as[MusicRating]
    .rdd
    .map(rating => (rating.uid, rating.mid, rating.score))  //转换成rdd，只保留需要的数据即删除时间戳
    .cache()
  //从rating数据中提取出所有的uid和mid，并去重
  val userRDD = ratingRDD.map(_._1).distinct()
  val musicRDD = ratingRDD.map(_._2).distinct()

  //训练隐语义模型
  val trainData = ratingRDD.map( x=> Rating(x._1,x._2,x._3))
  val (rank, iteration, lambda) = (20, 20, 1) //100, 20, 0.1 another
  val model = ALS.train(trainData, rank, iteration, lambda)
  //基于用户和艺术家的隐特征，计算预测评分，得到用户的推荐列表
  //计算user和music的笛卡尔积，得到空评分矩阵
  val userMusics = userRDD.cartesian(musicRDD)
  //调用model的predict方法
  val preRatings = model.predict(userMusics)
  val userRecs = preRatings
    .filter(_.rating > 0) //过滤出评分大于0的项
    .map(rating => (rating.user, (rating.product, rating.rating)))
    .groupByKey()
    .map{
      case( uid, recs) => UserRecs(uid, recs.toList.sortWith(_._2 > _._2).take(USER_MAX_RECOMMENDATION).map(x=>Recommendation(x._1,x._2)))
    }
    .toDF()

  userRecs.write
    .option("uri", mongoConfig.uri)
    .option("collection", USER_RECS)
    .mode("overwrite")
    .format("com.mongodb.spark.sql")
    .save()

  //基于艺术家隐特征计算相似度矩阵，得到艺术家相似度列表
  val musicFeatures = model.productFeatures.map{
    case (mid, features) => (mid, new DoubleMatrix(features))
  }
  // 对所有艺术家两两计算相似度，做笛卡尔积
  val musicRecs = musicFeatures.cartesian(musicFeatures)
    .filter{
      //把自己跟自己的配对过滤
      case (a,b) => a._1 != b._1
    }
    .map{
      case (a,b) => {
        val simScore = this.consinSim(a._2, b._2)
        ( a._1, (b._1, simScore))
      }
    }
    .filter(_._2._2 > 0.5)
    .groupByKey()
    .map{
      case (mid, items) => MusicRecs(mid, items.toList.sortWith(_._2 > _._2).map(x => Recommendation(x._1, x._2)))
    }
    .toDF()

    musicRecs.write
      .option("uri", mongoConfig.uri)
      .option("collection", MUSIC_RECS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()
    spark.close()
  }
  //求向量余弦相似度
  def consinSim(music1: DoubleMatrix, music2: DoubleMatrix): Double = {
    music1.dot(music2) / music1.norm2() * music2.norm2()
  }

}
