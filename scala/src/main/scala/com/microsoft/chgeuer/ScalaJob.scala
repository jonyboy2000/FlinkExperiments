package com.microsoft.chgeuer

// com.microsoft.chgeuer.ScalaJob
// --topic.input test --topic.target results --group.id myGroup --bootstrap.servers localhost:9092 --zookeeper.connect localhost:2181

import scala.collection.mutable.ListBuffer
import org.apache.flink.api.java.tuple.Tuple
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.api.windowing.assigners.{EventTimeSessionWindows, TumblingEventTimeWindows}
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer010, FlinkKafkaProducer010}
import com.microsoft.chgeuer.proto.messages.{Point, TrackingPacket, TripAggregation}

case class MutableTripAggregation(ccn:Long, tripid:Int, data:ListBuffer[Point])

object ScalaJob extends App {
  val params = ParameterTool.fromArgs(args)

  val env = StreamExecutionEnvironment.getExecutionEnvironment
  env.getConfig.setGlobalJobParameters(params)
  env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

  val rawStream: DataStream[TrackingPacket] = env.addSource(
    new FlinkKafkaConsumer010[TrackingPacket](
      params.getRequired("topic.input"),
      new TrackingPacketDeserializer, // new ProtobufDeserializationSchema[TrackingPacket](TrackingPacket.parseFrom),
      params.getProperties
    )
  )

  // http://blog.madhukaraphatak.com/introduction-to-flink-streaming-part-9/
  val rawStreamWithTimestamps: DataStream[TrackingPacket] = rawStream
    .assignAscendingTimestamps(_.ticks)

  val converted: DataStream[MutableTripAggregation] = rawStreamWithTimestamps
    .map(x => MutableTripAggregation(
      ccn = x.ccn, tripid = x.tripid,
      data = ListBuffer(Point(ticks = x.ticks, lat = x.lat, lon = x.lon))))

  val keyed: KeyedStream[MutableTripAggregation, (Long, Int)] = converted
    .keyBy(x => (x.ccn, x.tripid))

  val windowed: WindowedStream[MutableTripAggregation, (Long, Int), TimeWindow] = keyed
    .window(TumblingEventTimeWindows.of(Time.seconds(10)))
    .allowedLateness(Time.seconds(30))

  // EventTimeSessionWindows.withGap()


  val reduced: DataStream[MutableTripAggregation] = windowed
    .reduce((aggregate, current) => MutableTripAggregation(
      ccn = aggregate.ccn, tripid = aggregate.tripid,
      data = ListBuffer.concat(aggregate.data, current.data)))

  val targetSchema: DataStream[TripAggregation] = reduced
    .map(a => TripAggregation(ccn = a.ccn, tripid = a.tripid, data = a.data))

  /*
  val messageStream: DataStream[TripAggregation] = windowed
    // val result: DataStream[String] = ... .fold("start")((str, i) => { str + "-" + i })
    // .fold(Vector())((v, tripagg) => v +: tripagg.data)
    // .window(GlobalWindows.create).evictor(TimeEvictor.of(Time.of(10, TimeUnit.SECONDS)))
    .sum("data.length")
    .map(a => TripAggregation(ccn = a.ccn, tripid = a.tripid, data = a.data ))
  */

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