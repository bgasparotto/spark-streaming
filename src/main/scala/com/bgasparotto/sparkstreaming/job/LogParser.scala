package com.bgasparotto.sparkstreaming.job

import java.util.regex.Matcher

import com.bgasparotto.sparkstreaming.infrastructure.LogPattern.apacheLogPattern
import com.bgasparotto.sparkstreaming.infrastructure.MinimalLogger.setupLogging
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Seconds, StreamingContext}

/** Maintains top URL's visited over a 5 minute window, from a stream
  * of Apache access logs on port 9999.
  * Before running this class, run `nc -kl 9999 -i 1 < dataset/apache/access_log.txt` on the
  * terminal from this project's folder.
  */
object LogParser {

  def main(args: Array[String]) {

    // Create the context with a 1 second batch size
    val ssc = new StreamingContext("local[*]", "LogParser", Seconds(1))

    setupLogging()

    // Construct a regular expression (regex) to extract fields from raw Apache log lines
    val pattern = apacheLogPattern()

    // Create a socket stream to read log data published via netcat on port 9999 locally
    val lines =
      ssc.socketTextStream("127.0.0.1", 9999, StorageLevel.MEMORY_AND_DISK_SER)

    // Extract the request field from each log line
    val requests = lines.map(x => {
      val matcher: Matcher = pattern.matcher(x)
      if (matcher.matches()) matcher.group(5)
    })

    // Extract the URL from the request
    val urls = requests.map(x => {
      val arr = x.toString.split(" ")
      if (arr.size == 3) arr(1) else "[error]"
    })

    // Reduce by URL over a 5-minute window sliding every second
    val urlCounts = urls
      .map(x => (x, 1))
      .reduceByKeyAndWindow(_ + _, _ - _, Seconds(300), Seconds(1))

    // Sort and print the results
    val sortedResults =
      urlCounts.transform(rdd => rdd.sortBy(x => x._2, ascending = false))
    sortedResults.print()

    // Kick it off
    ssc.checkpoint("spark_tmp/checkpoint")
    ssc.start()
    ssc.awaitTermination()
  }
}
