package com.microsoft.chgeuer

// com.microsoft.chgeuer.ScalaJob
// --topic.input test --topic.target results --group.id myGroup --bootstrap.servers localhost:9092 --zookeeper.connect localhost:2181

import scala.collection.mutable.ListBuffer
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.util.Collector
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.Window
import org.apache.flink.streaming.api.windowing.assigners.{EventTimeSessionWindows, TumblingEventTimeWindows}
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer010, FlinkKafkaProducer010}

import com.microsoft.chgeuer.proto.messages.{Point, TrackingPacket, TripAggregation}

case class MutableTripAggregation(ccn:Long, tripid:Int, data:ListBuffer[Point])

/*
class WindowReduceFunction extends ReduceFunction[MutableTripAggregation] {
  override def reduce(aggregate: MutableTripAggregation, current: MutableTripAggregation) : MutableTripAggregation = {
    MutableTripAggregation(ccn = aggregate.ccn,
      tripid = aggregate.tripid,
      data = ListBuffer.concat(aggregate.data, current.data))
  }
}

class WindowApplyFunction extends WindowFunction[MutableTripAggregation, MutableTripAggregation, (Long, Int), Window] {
  override def apply(key: (Long, Int), window: Window, input: Iterable[MutableTripAggregation], out: Collector[MutableTripAggregation]): Unit = {
    val ccn = key._1
    val tripid = key._2
  }
}
*/

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

  // http://blog.madhukaraphatak.com/introduction-to-flink-streaming-part-9/
  val rawStreamWithTimestamps: DataStream[TrackingPacket] = rawStream
      .assignTimestampsAndWatermarks(
        new BoundedOutOfOrdernessTimestampExtractor[TrackingPacket](Time.seconds(5)) {
          override def extractTimestamp(element: TrackingPacket): Long = element.ticks
        }
      )

  val converted: DataStream[MutableTripAggregation] = rawStreamWithTimestamps
    .map(x => MutableTripAggregation(
      ccn = x.ccn, tripid = x.tripid,
      data = ListBuffer(Point(ticks = x.ticks, lat = x.lat, lon = x.lon))))

  val keyed: KeyedStream[MutableTripAggregation, (Long, Int)] = converted
    .keyBy(x => (x.ccn, x.tripid))

  // a trigger makes sure apply is called, and then just use imperative code to aggregate (instead of reduce/fold)
  val reduced_new: DataStream[MutableTripAggregation] = keyed
    .window(EventTimeSessionWindows.withGap(Time.seconds(2)))
    .allowedLateness(Time.seconds(5))
    .apply((key: (Long, Int), window: Window, input: Iterable[MutableTripAggregation], out: Collector[MutableTripAggregation]) => {
        val ccn = key._1
        val tripid = key._2

        if (input.nonEmpty) {
          Console.println(s"Input contains ${input.size} elements......")
          input.foreach(i => {
            Console.println(s"Adding  ${i.data(0).ticks} to output")
            out.collect(i)
          })
        }
      })

  val reduced_old: DataStream[MutableTripAggregation]  = keyed
    .window(TumblingEventTimeWindows.of(Time.seconds(10)))
    .allowedLateness(Time.seconds(30))
    .reduce((aggregate: MutableTripAggregation, current: MutableTripAggregation) => MutableTripAggregation(
      ccn = aggregate.ccn,
      tripid = aggregate.tripid,
      data = ListBuffer.concat(aggregate.data, current.data)))

  val targetSchema: DataStream[TripAggregation] = reduced_new
    .map(a => TripAggregation(ccn = a.ccn, tripid = a.tripid, data = a.data))

  // Enrichment:
  // - could do async I/O to join on location DB
  // - send stuff to Kafka and have somebody else deal with it
  // - radical: Everything is a stream. Push all possible locations into cluster memory and join on the streams

  val kafkaSink = FlinkKafkaProducer010.writeToKafkaWithTimestamps[TripAggregation](
    targetSchema.javaStream,
    params.getRequired("topic.target"),
    new TripAggregationSerializationSchema,
    params.getProperties
  )
  kafkaSink.setLogFailuresOnly(false)
  kafkaSink.setFlushOnCheckpoint(true)

  env.execute("Christian's ScalaJob")
}