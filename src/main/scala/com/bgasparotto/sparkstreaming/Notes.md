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
