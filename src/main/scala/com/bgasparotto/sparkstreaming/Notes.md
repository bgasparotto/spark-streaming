# Spark Streaming
- Old API: DStreams with RDDs.
    - Processes in micro-batches of a specific time frame;
    - Can maintain state between different batches (running totals, counters, session, etc)

- New API: Structured Streaming with DataFrames
    - Introduced in Spark 2, processes in real-time.

- The Twitter API has been removed from in Spark 2 and extracted to a different package, visit 
http://bahir.apache.org/ or add the dependency
`org.apache.bahir" %% "spark-streaming-twitter" % "2.4.0"`
