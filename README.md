# Sample on using Apache Flink and Kafka together

## Compile the Scala example

```shell
cd scala
sbt assembly package
``` 

## Compile the Java example

```shell
cd java
mvn package
``` 

## Submit Jobs

Go to the [Flink Portal](http://localhost:8081/#/overview)

- Scala Class: `com.microsoft.chgeuer.ScalaJob`
- Java Class: `com.microsoft.chgeuer.JavaJob`

```
--topic.input test --topic.target results --bootstrap.servers localhost:9092 --zookeeper.connect localhost:2181 --group.id myGroup
```

