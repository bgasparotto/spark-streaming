name := "spark-streaming"

version := "0.1"

scalaVersion := "2.11.12"

val sparkVersion = "2.4.4"

/* Jpountz conflicts with Kafka throwing NoSuchMethodError: net.jpountz.lz4.LZ4BlockInputStream */
lazy val excludeJpountz = ExclusionRule(organization = "net.jpountz.lz4", name = "lz4")

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-streaming" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided",
  "org.apache.bahir" %% "spark-streaming-twitter" % "2.4.0",
  "org.apache.spark" %% "spark-streaming-kafka" % "1.6.3" excludeAll excludeJpountz,
  "org.apache.spark" % "spark-streaming-flume_2.11" % sparkVersion,
  "org.apache.spark" % "spark-streaming-kinesis-asl_2.11" % sparkVersion,
  "com.datastax.spark" % "spark-cassandra-connector_2.11" % "2.4.3"
)
