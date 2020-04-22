package com.bgasparotto.sparkstreaming.job.integration.output

import java.util.regex.Matcher

import com.bgasparotto.sparkstreaming.infrastructure.LogPattern.apacheLogPattern
import com.bgasparotto.sparkstreaming.infrastructure.MinimalLogger.setupLogging
import com.datastax.spark.connector._
import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Seconds, StreamingContext}

/** Listens to Apache log data on port 9999 and saves URL, status, and user agent
  *  by IP address in a Cassandra database.
  *
  * Before running the class, publish the messages from the access log with:
  * `nc -kl 7777 -i 1 < dataset/apache/access_log.txt`
  */
object CassandraExample {

  def main(args: Array[String]) {

    // Set up the Cassandra host address
    val conf = new SparkConf()
    conf.set("spark.cassandra.connection.host", "127.0.0.1")
    conf.setMaster("local[*]")
    conf.setAppName("CassandraExample")

    // Create the context with a 10 second batch size
    val ssc = new StreamingContext(conf, Seconds(10))

    setupLogging()

    // Construct a regular expression (regex) to extract fields from raw Apache log lines
    val pattern = apacheLogPattern()

    // Create a socket stream to read log data published via netcat on port 9999 locally
    val lines =
      ssc.socketTextStream("127.0.0.1", 9999, StorageLevel.MEMORY_AND_DISK_SER)

    // Extract the (IP, URL, status, useragent) tuples that match our schema in Cassandra
    val requests = lines.map(x => {
      val matcher: Matcher = pattern.matcher(x)
      if (matcher.matches()) {
        val ip = matcher.group(1) // key, only the latest entry will prevail
        val request = matcher.group(5)
        val requestFields = request.split(" ")
        val url = scala.util.Try(requestFields(1)) getOrElse "[error]"
        (ip, url, matcher.group(6).toInt, matcher.group(9))
      }
      else {
        ("error", "error", 0, "error")
      }
    })

    // Now store it in Cassandra
    requests.foreachRDD((rdd, _) => {
      rdd.cache()
      println("Writing " + rdd.count() + " rows to Cassandra")
      rdd.saveToCassandra(
        "Udemy",
        "LogTest",
        SomeColumns("IP", "URL", "Status", "UserAgent")
      )
    })

    // Kick it off
    ssc.checkpoint("spark_tmp/checkpoint")
    ssc.start()
    ssc.awaitTermination()
  }
}
