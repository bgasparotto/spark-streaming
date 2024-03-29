# Spark Streaming
- Old API: DStreams with RDDs.
    - Processes in micro-batches of a specific time frame;
    - Can maintain state between different batches (running totals, counters, session, etc)

- New API: Structured Streaming with DataFrames
    - Introduced in Spark 2, processes in real-time.

- The Twitter API has been removed from in Spark 2 and extracted to a different package, visit 
http://bahir.apache.org/ or add the dependency
`org.apache.bahir" %% "spark-streaming-twitter" % "2.4.0"`

## Streaming intervals
- Batch: how often data is ingested for processing;
    - Set up with the StreamingContext.
- Slide: how often the results will be updated;
    - Set up in the reduce operations.
- Window: how far back we will look at from the slide interval.
    - Also set up in the reduce operations.
    
## Fault tolerance
- All incoming data is replicated to at least 2 worker nodes;
- Specially when using stateful operations, we should use a checkpoint directory `ssc.checkpoint()`
in order to persist and recover the state;
- HDFS is a good choice for using checkpoints;
- Checkpoints should be stored externally, not on the same node as the driver.

### Receiver failures
- If the receiver fails, the data can be lost;
- If Kafka is pushing data to Spark and the driver script fails, the data can be lost (skipped). In
this case, pull-based receivers are preferable;

### Driver-script failures
- Although works replicate data, the driver can be a single point of failure since it orchestrates
the process;
- `StreamingContext.getOrCreate()` is preferred instead of always creating a new context, given it
checks your checkpoint directly and recover the last state if a failure happened. If no data is
present in your checkpoint directory, that means the last execution completed successfully;
- Your driver scripts should be monitored, ideally by a cluster manager such as:
    - Kubernetes
    - Spark itself (use -supervise on spark-submit)
    - Zookeeper

## Streaming with RDDs and DataFrames
- If you intend to perform more than 1 action with your rdd/df, cache it first with `cache()`.
Persisting the RDD with `persist()` has a similar effect.
- The result of a reduceByWindow operation is always a single RDD;

## DStream vs Structured Streaming
- DStreams are created from the StreamingContext: `new StreamingContext(...)`
- Structured Streaming dataframes are created from SparkSession: `SparkSession.builder...`
- Creating a `SparkSession` is similar to opening a database connection, so one should make sure
the session is closed at the end of the code.

## Integrations
### Kafka
- Prior to Spark 1.3, Spark Streaming had to connect to Zookeeper in order to read messages;
- However, Spark Streaming can connect directly to Kafka since Spark 1.3;
- It's needed to add the dependency spark-streaming-kafka (it's not built in Spark);
- When connection to Kafka directly, you have to maintain yourself the list of Kafka brokers, instead 
  of just connection to the Zookeeper. However, the reliability of the messages (including reprocessing) pays off.

### Apache Flume
- Similar to Kafka, but tailored for large amounts of log data;
- Spark can connect to Flume either push or pull based, however, pull is the most recommended:
    * Data can be lost if the push fails;
    * You have to set up your receiver into Flume's configuration.
- To pull messages from Flume, you need to install an extra package Spark Sink on Flume.

### Kafka vs Flume
- Flume integration with the Hadoop ecosystem is smoother;
- Kakfa is more reliable;
- Both can be used together, some projects are doing so.

### Amazon Kinesis
- Similar to Kafka, but as a service hosted in AWS;
- Shards have a similar purpose as a Kafka broker;
- It's needed to add the dependency spark-streaming-kinesis-asl (it's not built in Spark);

### Custom Integration
- In order to create a custom receiver implementation:
  1. Create a subclass of `org.apache.spark.streaming.receiver.Receiver`;
  2. Implement `onStart()`, `onStop()` and `receive()` methods;
  3. Create a DStream using `ssc.receiverStream(new CustomReceiver(params))`

### Cassandra as the output
- Tables should be created with the queries you'll be running later in mind, so they execute really fast;
- To integrate Spark with Cassandra:
  1. Add the spark-cassandra-connector dependency;
  2. Set `spark.cassandra.connection.host` on your spark conf;
  3. Use `rdd.saveToCassandra` to store tuples into named columns in a specific Cassandra table.

## Stateful Spark Streaming
- Maintains state across batches, such as running totals, lists, etc;
- Use `DStream.mapWithState(stateSpec)`, but also:
  1. Define a data type using a case class;
  2. Provide a `StateSpec.function` implementation;
  3. Create a `DStream` as usual;
  4. Invoke `mapWithState(yourStateSpecFunction)` on your `DStream`.

## Spark MLLib
- In MLLib terminology, a Vector format consists of data separated by commas and enclosed by brackets:
`[someid,22,98]` whereas `someid` is a common id, `98` and `xx` could be two features such as income and age.
- A Labeled Point is the data with the result, such as test data.

### K-Means
- K-Means clustering is an unsupervised model which attempts to split data into K groups that are closest to K centroids;
- To create a new model, instantiate it with `new StreamingKMeans()`;
- Use the method `model.trainOn()` to train your model;
- Use `model.predictOnValues()` to run a prediction

### Linear Regression
- Linear regression is a supervised model which fits a line to a data set of observations;
- The simplest way of doing it is by using the technique of Least Squares, which is useful for uni-dimensional data.
- Spark Streaming, however, uses the complex model Stochastic Gradient Descent (SGD), which is friendlier to multi-dimensional data.
- Create a new model with `new StreamingLinearRegressionWithSGD()`;
- SGD doesn't handle feature scaling well. It assumes your data is similar to a normal distribution, such as: -2 -1 0 1 2 etc;
- To make it work, you need to scale your data down and back up again when you're done.
- It also assumes your y-intercept is 0, unless you call `model.algorithm.setIntercept(true)`;

## Running on AWS EMR (Elastic Map Reduce)
- Bear in mind that `--master` is overridden by `SparkConf`;
- The parameter `--executor-memory` is commonly used to make sure Spark doesn't try to use more memory than available. It sets the Java heap memory under the hood.
- The parameter `--total-executor-cores` do a similar job in regards to CPU;
- When using Hadoop Yarn as the cluster manager, the parameter `--num-executors` is mandatory;
- Always understand the hardware you are running it on.
- 9 out of 10 times a Spark job fails is because of not enough resources.
- For integration with S3, on Spark you can specific a S3 url using the S3N protocol from your driver scripts, e.g. `sc.textFile("s3n://your_url/your_file")`

## Tuning and Troubleshooting on a cluster
- Spark UI can be accessed via browser on port 4040;
- For AWS EMR, open a ssh tunnel following the instructions on AWS console next to the "connections" item;

## Performance tips
- Use `cache()` or `persist()` if you perform multiple actions on an RDD/DataFrame;
- Don't make your executor memory too large.
- Think about partitioning vs your cluster size. Use `repartition()` when appropriate to get better parallelism.
  Usually, you'd like to `repartition(n)` where `n` is the number of executors.
- In contrast, when shrinking an RDD/DataFrame, use `coalesce()` instead of `repartition()` to avoid data shuffling.
- Use Kryo serialisation instead of Java serialisation. Kryo is more efficient but doesn't come enable by default because you must manually register your classes:
```
conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
conf.registerKryoClasses(Array(classOf[MyClass1], classOf[MyClass2]))
```

## Stability tips
- Use Mesos or YARN
- Use Zookeeper or Kubernetes
- Chose `--total-executor-cores` and `--executor-memory` wisely. AWS EMR tries to set that up automatically.
- Make sure your receivers are reliable;
- Create checkpoints;
- Make sure your logs are accessible and durable so you can look at them.

## Learning More
- Book Learning Real-time processing with Spark Streaming from Gupta (Packt);
- Learning Spark (O'Reilly)
- Official site spark.apache.org
