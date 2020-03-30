package com.bgasparotto.sparkstreaming.job

import com.bgasparotto.sparkstreaming.infrastructure.MinimalLogger.setupLogging
import com.bgasparotto.sparkstreaming.infrastructure.TwitterConfig.setupTwitter
import org.apache.spark.streaming._
import org.apache.spark.streaming.twitter._

object PopularWords {

  def main(args: Array[String]) {
    setupTwitter()
    val ssc = new StreamingContext("local[*]", "PopularWords", Seconds(2))
    setupLogging()

    val tweets = TwitterUtils.createStream(ssc, None)
    val words = tweets
      .map(_.getText)
      .flatMap(_.split(" "))
      .filter(word => !word.startsWith("#"))
      .map(_.toLowerCase)

    val commonWordsInWindow = words
      .map(word => (word, 1))
      .reduceByKeyAndWindow(
        _ + _,
        _ - _,
        Seconds(30),
        Seconds(4)
      )
      .transform(
        _.sortBy(_._2, ascending = false)
      )

    commonWordsInWindow.print

    ssc.checkpoint("spark_tmp/checkpoint")
    ssc.start()
    ssc.awaitTermination()
  }
}
