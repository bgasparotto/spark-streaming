package com.bgasparotto.sparkstreaming.job.integration.input.flume

import java.util.regex.Matcher

import com.bgasparotto.sparkstreaming.infrastructure.LogPattern.apacheLogPattern
import com.bgasparotto.sparkstreaming.infrastructure.MinimalLogger.setupLogging
import org.apache.spark.streaming.dstream.DStream.toPairDStreamFunctions
import org.apache.spark.streaming.flume._
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * Example of connecting to Flume in a "push" configuration.
  *
  * Flume setup steps (and more) at
  * https://spark.apache.org/docs/latest/streaming-flume-integration.html
  */
object FlumePushExample {

  def main(args: Array[String]) {

    // Create the context with a 1 second batch size
    val ssc = new StreamingContext("local[*]", "FlumePushExample", Seconds(1))

    setupLogging()

    // Construct a regular expression (regex) to extract fields from raw Apache log lines
    val pattern = apacheLogPattern()

    // Create a Flume stream receiving from a given host & port. It's that easy.
    val flumeStream = FlumeUtils.createStream(ssc, "localhost", 9092)

    // Except this creates a DStream of SparkFlumeEvent objects. We need to extract the actual messages.
    // This assumes they are just strings, like lines in a log file.
    // In addition to the body, a SparkFlumeEvent has a schema and header you can get as well. So you
    // could handle structured data if you want.
    val lines = flumeStream.map(x => new String(x.event.getBody.array()))

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
