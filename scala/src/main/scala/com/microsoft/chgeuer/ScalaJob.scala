package com.microsoft.chgeuer

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.util.serialization.SimpleStringSchema
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer010, FlinkKafkaProducer010}

// --topic.input test --topic.target results --bootstrap.servers localhost:9092 --zookeeper.connect localhost:2181 --group.id myGroup

object ScalaJob {
  // case class Stock(time:Long, symbol:String, value:Double)

  def main(args: Array[String]) {
    val parameterTool = ParameterTool.fromArgs(args)
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val messageStream = env.addSource(
        new FlinkKafkaConsumer010[String](
          parameterTool.getRequired("topic.input"),
          new SimpleStringSchema,
          parameterTool.getProperties
        )
      )
      .map(w => (w.split(' ')(0), w.split(' ')(1).toInt))
      .keyBy(0)
      .sum(1)
      .map(t => s"${t._1} ${t._2}")

    val myProducerConfig = FlinkKafkaProducer010.writeToKafkaWithTimestamps[String](
      messageStream.javaStream,
      parameterTool.getRequired("topic.target"),
      new SimpleStringSchema,
      parameterTool.getProperties
    )
    myProducerConfig.setLogFailuresOnly(false)
    myProducerConfig.setFlushOnCheckpoint(true)

    env.execute("Christian's ScalaJob")
  }
}