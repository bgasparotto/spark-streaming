# Spark Streaming
Repo for sharing my Spark streaming studies.
The source code also includes example classes and datasets provided as part as some courses and books materials.

## Stack
- Scala 2.11.12
- Spark 2.4.4
- Java 8

## Running the project
Assembly it with SBT
```shell script
cd spark-scala
sbt assembly
```

Run Spark master and worker with docker-compose:
```shell script
docker-compose up -d
```

(Optional) Scale up the the 'spark-worker' containers:
```shell script
docker-compose scale spark-worker=3
```

### Running your application from Intellij IDEA:
Under _Edit Configurations_ of your runnable class, add the following on _VM options:_:
```jvm
-Dspark.master=local[*]
```

### Running your application on the cluster:
Use `spark-submit` of the spark-master node:
```shell script
docker container exec -it spark-master bash ./spark/bin/spark-submit --master spark://spark-master:7077 --class com.bgasparotto.sparkstreaming.job.PrintTweets /app/spark-streaming-assembly-0.1.jar
```

### Accessing Spark UI
Spark UI should be accessible at http://localhost:8080
