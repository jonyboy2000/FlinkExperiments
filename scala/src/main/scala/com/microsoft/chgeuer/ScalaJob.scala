package com.microsoft.chgeuer

// com.microsoft.chgeuer.ScalaJob
// --topic.input test --topic.target results --group.id myGroup --bootstrap.servers localhost:9092 --zookeeper.connect localhost:2181

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.util.Collector
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.Window
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer010, FlinkKafkaProducer010}
import com.microsoft.chgeuer.proto.messages.{Point, TrackingPacket, TripAggregation}
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows
import org.apache.flink.streaming.api.windowing.triggers.CountTrigger

object ScalaJob extends App {
  val args2 = "--topic.input test --topic.target results --group.id myGroup --bootstrap.servers localhost:9092 --zookeeper.connect localhost:2181".split(" +")

  val params = ParameterTool.fromArgs(args2)

  val env = StreamExecutionEnvironment.getExecutionEnvironment
  env.getConfig.setGlobalJobParameters(params)
  env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

  val rawStream: DataStream[TrackingPacket] = env.addSource(
    new FlinkKafkaConsumer010[TrackingPacket](
      params.getRequired("topic.input"),
      new TrackingPacketDeserializer,
      params.getProperties
    )
  )

  val rawStreamWithTimestamps: DataStream[TrackingPacket] = rawStream
    .assignTimestampsAndWatermarks(
      new BoundedOutOfOrdernessTimestampExtractor[TrackingPacket](Time.seconds(5)) {
        override def extractTimestamp(element: TrackingPacket): Long = element.ticks
      }
    )

  val keyed: KeyedStream[TrackingPacket, (Long, Int)] = rawStreamWithTimestamps
    .keyBy(x => (x.ccn, x.tripid))

  // a trigger makes sure apply is called, and then just use imperative code to aggregate (instead of reduce/fold)
  val reduced_new: DataStream[TripAggregation] = keyed
    // .window(GlobalWindows.create()).trigger(CountTrigger.of(2))
    .countWindow(size = 10)
    // .window(EventTimeSessionWindows.withGap(Time.seconds(2)))
    // .allowedLateness(Time.seconds(5))
    .apply((key: (Long, Int), window: Window, input: Iterable[TrackingPacket], out: Collector[TripAggregation]) => {
      Console.println(s"Input contains ${input.size} elements......")
      if (input.nonEmpty) {
        out.collect(TripAggregation(
          ccn = key._1,
          tripid = key._2,
          data = input.map(tp => Point(ticks = tp.ticks, lat = tp.lat, lon = tp.lon)).toList))

      }
    }
  )

  // Enrichment:
  // - could do async I/O to join on location DB
  // - send stuff to Kafka and have somebody else deal with it
  // - radical: Everything is a stream. Push all possible locations into cluster memory and join on the streams

  val kafkaSink = FlinkKafkaProducer010.writeToKafkaWithTimestamps[TripAggregation](
    reduced_new.javaStream,
    params.getRequired("topic.target"),
    new TripAggregationSerializationSchema,
    params.getProperties
  )
  kafkaSink.setLogFailuresOnly(false)
  kafkaSink.setFlushOnCheckpoint(true)

  env.execute("Christian's ScalaJob")
}