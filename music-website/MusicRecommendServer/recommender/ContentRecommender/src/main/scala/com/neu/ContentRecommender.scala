package com.neu

import org.apache.spark.SparkConf
import org.apache.spark.ml.feature.{HashingTF, IDF, Tokenizer}
import org.apache.spark.ml.linalg.SparseVector
import org.apache.spark.sql.SparkSession
import org.jblas.DoubleMatrix


// 需要的数据源：音乐内容信息
case class Music(mid: Int, name: String, intro: String, tag: String)
case class MongoConfig(uri:String, db:String)
//基准推荐对象
case class Recommendation( mid: Int, score: Double)

//基于音乐内容信息提取出的的音乐特征向量的音乐相似度列表
case class MusicRecs(mid: Int, recs: Seq[Recommendation])


object ContentRecommender {

  val MONGODB_MUSIC_COLLECTION = "Music"
  val CONTENT_MUSIC_RECS = "ContentMusicRecs"

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommender",
      "mongo.db" -> "recommender"
    )

    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("ContentRecommender")

    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

//    加载数据并做预处理
    val musicTagsDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_MUSIC_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Music]
      .map(
        x=> (x.mid, x.name, x.tag )
      )
      // 提取mid, name, tag三项作为原始内容特征
      .toDF("mid", "name", "tag")
      .cache()

      //使用TF-IDF从内容信息中提取音乐特征向量

      // 创建一个分词器，按照空格分词
      val tokenizer = new Tokenizer().setInputCol("tag").setOutputCol("words")

      // 用分词器对原始数据做转换，生成新的一列words
      val wordsData = tokenizer.transform(musicTagsDF)
      // 引入HashingTF工具，可以把一个词语序列转化成对应的词频
      val hashingTF = new HashingTF().setInputCol("words").setOutputCol("rawFeatures").setNumFeatures(13)
      val featurizedData = hashingTF.transform(wordsData)
      // 引入IDF工具，可以得到idf模型

      val idf = new IDF().setInputCol("rawFeatures").setOutputCol("features")
      // 训练idf模型，得到每个词的逆文档频率
      val idfModel = idf.fit(featurizedData)
      // 用模型对原数据进行处理，得到文档中每个词的tf-idf，作为新的特征向量
      val rescaledData = idfModel.transform(featurizedData)

      // 从内容信息中提取音乐特征向量
      val musicFeatures = rescaledData.map(
        row=> (row.getAs[Int]("mid"), row.getAs[SparseVector]("features").toArray)
      )
        .rdd
        .map(
          x=> (x._1, new DoubleMatrix(x._2))
        )

      val musicRecs = musicFeatures.cartesian(musicFeatures)
        .filter {
          //把自己跟自己的配对过滤
          case (a, b) => a._1 != b._1
        }
        .map {
          case (a, b) => {
            val simScore = this.consinSim(a._2, b._2)
            (a._1, (b._1, simScore))
          }
        }
        .filter(_._2._2 > 0.6)
        .groupByKey()
        .map {
          case (mid, items) => MusicRecs(mid, items.toList.sortWith(_._2 > _._2).map(x => Recommendation(x._1, x._2)))
        }
        .toDF()

      musicRecs.write
        .option("uri", mongoConfig.uri)
        .option("collection", CONTENT_MUSIC_RECS)
        .mode("overwrite")
        .format("com.mongodb.spark.sql")
        .save()
      spark.stop()
    }

      //求向量余弦相似度
      def consinSim(music1: DoubleMatrix, music2: DoubleMatrix): Double = {
        music1.dot(music2) / music1.norm2() * music2.norm2()
      }
}
