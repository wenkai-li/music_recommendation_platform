package com.neu

import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.text.SimpleDateFormat
import java.util.Date

case class Music(mid: Int, name: String, intro: String, tag: String, pic: String)
case class Rating(uid: Int, mid: Int ,score: Double, timestamp: Int)
case class MongoConfig(uri:String, db:String)

//基准推荐对象
case class Recommendation( mid: Int, score: Double)

//定义艺术家推荐top10
case class GenresRecommendation(uri: String, recs: Seq[Recommendation])


object StatisticsRecommender {
  //定义mongodb的表名
  val MONGODB_MUSIC_COLLECTION = "Music"
  val MONGODB_RATING_COLLECTION = "Rating"

  //统计表的名称
  //打分数量统计
  val RATE_MORE_MUSIC = "RateMoreMusic"
  //近期热门统计
  val RATE_RECENTLY_MUSIC = "RateRecentlyMusic"
  //平均评分统计
  val AVERAGE_MUSIC = "AverageMusic"
  //类别统计
  val GENRES_TOP_MUSIC = "GenresTopMusic"

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommender",
      "mongo.db" -> "recommender"
    )

    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("StatisticsRecommender")

    // 创建一个SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    //从mongodb加载数据
    val ratingDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Rating]
      .toDF()

    val musicDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_MUSIC_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Music]
      .toDF()

//    val tagDF = spark.read
//      .option("uri", mongoConfig.uri)
//      .option("collection", MONGODB_TAG_COLLECTION)
//      .format("com.mongodb.spark.sql")
//      .load()
//      .as[Tag]
//      .toDF()

    //创建ratings的临时表
    ratingDF.createOrReplaceTempView("ratings")

    //1. 历史评分数据最多
    val rateMoreMusicDF = spark.sql("select mid, count(mid) as count from ratings group by mid")
    //将结果写入对应的mongodb表中
    storeDFInMongoDB(rateMoreMusicDF, RATE_MORE_MUSIC)

    //2. 近期热门统计，按照"yyyyMM"格式选取最近的评分数据
    val simpleDateFormat = new SimpleDateFormat("yyyyMM") //创建日期格式化工具
    //注册udf，把时间戳转换为年月格式
    spark.udf.register("changeDate", (x:Int)=> simpleDateFormat.format(new Date(x*1000L)).toInt)
    //对原始数据做预处理，去掉uid
    val ratingOfYearMonth = spark.sql("select mid, score, changeDate(timestamp) as yearmonth from ratings")
    ratingOfYearMonth.createOrReplaceTempView("ratingOfMonth")

    //从ratingOfMonth中查找电影在各个月份的评分统计
    val rateRecentlyMusicDF = spark.sql("select mid, count(mid) as count, yearmonth from ratingOfMonth group by yearmonth, mid order by yearmonth desc, count desc")
    //存入mongodb
    storeDFInMongoDB(rateRecentlyMusicDF, RATE_RECENTLY_MUSIC)

    //3. 优质艺术家统计，统计艺术家的平均评分, mid, avg
    val averageMusicDF = spark.sql("select mid, avg(score) as avg from ratings group by mid")
    storeDFInMongoDB(averageMusicDF, AVERAGE_MUSIC)

    //4. 各类别音乐TOP统计
    //定义出来类别，取前20个
    val tag = List("Pop_Rock", "Electronic", "Country", "RnB", "Latin", "Jazz", "International", "Vocal", "Rap",
                  "New-Age", "Folk", "Reggae", "Blues")

    // 把电影评分拿到加入music表中,inner join
    val musicWithScore = musicDF.join(averageMusicDF, "mid")

    //为做笛卡尔积，把tag转成rdd
    val tagRDD = spark.sparkContext.makeRDD(tag)

    //计算类别top10,对类别和艺术家做笛卡尔积
    val tagTopMusicDF = tagRDD.cartesian(musicWithScore.rdd)
      .filter{
        //条件过滤，找出music的字段tag的值如果包含当前类别
        case (tag, musicRow) => musicRow.getAs[String]("tag").toLowerCase.contains(tag.toLowerCase)
      }
      .map{
        case (tag, musicRow) => ( tag, (musicRow.getAs[Int]("mid"), musicRow.getAs[Double]("avg")))
      }
      .groupByKey()
      .map{
        case(tag, items) => GenresRecommendation(tag, items.toList.sortWith(_._2 > _._2).take(10).map( item => Recommendation(item._1, item._2)))
      }
      .toDF()

    storeDFInMongoDB(tagTopMusicDF, GENRES_TOP_MUSIC)
  }

  def storeDFInMongoDB(df: DataFrame, collection_name: String)(implicit mongoConfig: MongoConfig): Unit ={
    df.write
      .option("uri", mongoConfig.uri)
      .option("collection", collection_name)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()
  }
}
