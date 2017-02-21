package com.microsoft.chgeuer

// com.microsoft.chgeuer.ScalaJob
// --topic.input test --topic.target results --bootstrap.servers localhost:9092 --zookeeper.connect localhost:2181 --group.id myGroup

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.util.serialization.SimpleStringSchema
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer010, FlinkKafkaProducer010}

object ScalaJob {
  case class Point(word:String, count:Int)

  def main(args: Array[String]) {
    val params = ParameterTool.fromArgs(args)

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.getConfig.setGlobalJobParameters(params)
    env.setStreamTimeCharacteristic(TimeCharacteristic.IngestionTime)

    val messageStream = env.addSource(
        new FlinkKafkaConsumer010[String](
          params.getRequired("topic.input"),
          new SimpleStringSchema,
          params.getProperties
        )
      )
      .map(w => new Point(word = w.split(' ')(0), count = w.split(' ')(1).toInt))
      .keyBy("word")
      .window(TumblingEventTimeWindows.of(Time.seconds(5)))
      // .window(GlobalWindows.create).evictor(TimeEvictor.of(Time.of(10, TimeUnit.SECONDS)))
      .sum("count")
      .map(t => s"${t.word} ${t.count}")
      // .map(w => (w.split(' ')(0), w.split(' ')(1).toInt)).keyBy(0).sum(1).map(t => s"${t._1} ${t._2}")

    val myProducerConfig = FlinkKafkaProducer010.writeToKafkaWithTimestamps[String](
      messageStream.javaStream,
      params.getRequired("topic.target"),
      new SimpleStringSchema,
      params.getProperties
    )
    myProducerConfig.setLogFailuresOnly(false)
    myProducerConfig.setFlushOnCheckpoint(true)

    env.execute("Christian's ScalaJob")
  }
}