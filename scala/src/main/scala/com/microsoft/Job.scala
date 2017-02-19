package com.microsoft

import org.apache.flink.streaming.api.scala._
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.util.serialization.SimpleStringSchema
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer010, FlinkKafkaProducer010}

object Job {
  // case class Stock(time:Long, symbol:String, value:Double)

  def main(args: Array[String]) {
    val parameterTool = ParameterTool.fromArgs(args)
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val messageStream: DataStream[String] = env.addSource(
        new FlinkKafkaConsumer010[String](
          parameterTool.getRequired("topic"),
          new SimpleStringSchema,
          parameterTool.getProperties
        )
      )
      .map(w => (w, 1))
      .keyBy(0)
      .sum(1)
      .map(t => s"${t._1} ${t._2}")

    val myProducerConfig = FlinkKafkaProducer010.writeToKafkaWithTimestamps[String](
      messageStream.javaStream,
      "results",
      new SimpleStringSchema,
      parameterTool.getProperties
    )
    myProducerConfig.setLogFailuresOnly(false)
    myProducerConfig.setFlushOnCheckpoint(true)

    env.execute("Scala SocketTextStreamWordCount Example")
  }
}