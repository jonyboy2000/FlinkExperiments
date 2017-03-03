package com.microsoft.chgeuer

// --topic.input test --topic.target results --group.id myGroup --bootstrap.servers localhost:9092 --zookeeper.connect localhost:2181

import com.microsoft.chgeuer.proto.messages.{Calculated, Point, TrackingPacket, TripAggregation}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.util.Collector
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.Window
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer010, FlinkKafkaProducer010}

object ScalaJob extends App {
  val args2 = "--topic.input test --topic.target results --group.id myGroup --bootstrap.servers localhost:9092 --zookeeper.connect localhost:2181".split(" +")
  // val env = StreamExecutionEnvironment.createLocalEnvironment(parallelism = 1)

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
      new BoundedOutOfOrdernessTimestampExtractor[TrackingPacket](Time.seconds(0)) {
        override def extractTimestamp(element: TrackingPacket): Long = element.millisecondsSinceEpoch
      }
    )

  val keyed: KeyedStream[TrackingPacket, (Long, Int)] = rawStreamWithTimestamps
    .keyBy(x => (x.ccn, x.tripid))

  val reduced_new: DataStream[TripAggregation] = keyed
    .countWindow(size = 10)
    // .window(EventTimeSessionWindows.withGap(Time.seconds(2))) // .allowedLateness(Time.seconds(2))
    .apply((key: (Long, Int), window: Window, input: Iterable[TrackingPacket], out: Collector[TripAggregation]) => {
      val ccn = key._1
      val tripid = key._2

      if (input.nonEmpty) {
        val pointList = input.map(tp => Point(
          millisecondsSinceEpoch = tp.millisecondsSinceEpoch,
          lat = tp.lat, lon = tp.lon))

        val data = helper.combinePoints(pointList)

        out.collect(TripAggregation(ccn = ccn, tripid = tripid, data = data))
      }
    }
  )

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


object ScalaJobProd extends App {
  val params = ParameterTool.fromArgs(args)

  val env = StreamExecutionEnvironment.getExecutionEnvironment
  env.getConfig.setGlobalJobParameters(params)
  env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

  val processedStream = env.addSource(
      new FlinkKafkaConsumer010[TrackingPacket](
        params.getRequired("topic.input"),
        new TrackingPacketDeserializer,
        params.getProperties
      )
    )
    .keyBy(x => (x.ccn, x.tripid))
    .countWindow(size = 10)
    .apply((key: (Long, Int), window: Window, input: Iterable[TrackingPacket], out: Collector[TripAggregation]) => {
        val ccn = key._1
        val tripid = key._2

        if (input.nonEmpty) {
          val pointList = input.map(tp => Point(
            millisecondsSinceEpoch = tp.millisecondsSinceEpoch,
            lat = tp.lat, lon = tp.lon))

          val data = helper.combinePoints(pointList)

          out.collect(TripAggregation(ccn = ccn, tripid = tripid, data = data))
        }
      }
    )
    .javaStream

  val kafkaSink = FlinkKafkaProducer010.writeToKafkaWithTimestamps[TripAggregation](
    processedStream,
    params.getRequired("topic.target"),
    new TripAggregationSerializationSchema,
    params.getProperties
  )
  kafkaSink.setLogFailuresOnly(false)
  kafkaSink.setFlushOnCheckpoint(true)

  env.execute()
}

// Enrichment:
// - could do async I/O to join on location DB
// - send stuff to Kafka and have somebody else deal with it
// - radical: Everything is a stream. Push all possible locations into cluster memory and join on the streams
