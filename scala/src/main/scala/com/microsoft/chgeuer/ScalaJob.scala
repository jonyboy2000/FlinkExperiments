package com.microsoft.chgeuer

// com.microsoft.chgeuer.ScalaJob
// --topic.input test --topic.target results --group.id myGroup --bootstrap.servers localhost:9092 --zookeeper.connect localhost:2181

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer010, FlinkKafkaProducer010}
import com.microsoft.chgeuer.proto.messages.{Point, TrackingPacket, TripAggregation}
import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.java.tuple.Tuple
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import scala.collection.mutable.ListBuffer

case class MutableTripAggregation(ccn:Long, tripid:Int, data:ListBuffer[Point])

object ScalaJob extends App {
    val params = ParameterTool.fromArgs(args)

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.getConfig.setGlobalJobParameters(params)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val rawStream: DataStream[TrackingPacket] = env.addSource(
        new FlinkKafkaConsumer010[TrackingPacket](
          params.getRequired("topic.input"),
          // new TrackingPacketSerializer,
          new ProtobufDeserializationSchema[TrackingPacket](TrackingPacket.parseFrom),
          params.getProperties
        )
      )

    // http://blog.madhukaraphatak.com/introduction-to-flink-streaming-part-9/
    val rawStreamWithTimestamps: DataStream[TrackingPacket] = rawStream
      .assignAscendingTimestamps(_.ticks)

    val converted: DataStream[MutableTripAggregation] = rawStreamWithTimestamps
      .map(x => MutableTripAggregation(ccn = x.ccn, tripid = x.tripid, data = ListBuffer(Point(ticks = x.ticks, lat = x.lat, lon = x.lon))))

    val keyed: KeyedStream[MutableTripAggregation, Tuple] = converted
      .keyBy("ccn", "tripid")

    val windowed: WindowedStream[MutableTripAggregation, Tuple, TimeWindow] = keyed
      .window(TumblingEventTimeWindows.of(Time.seconds(5)))

  /*
    // scala.collection.mutable.ListBuffer(1, 2) :+ 3
    val x: DataStream[TripAggregation] = windowed
          .reduce(function = (a: TripAggregation, b: TripAggregation) => {
            val add: Seq[Equals] = a.data :+ b.data

            TripAggregation(ccn = a.ccn, tripid = b.tripid, data = add)
          })
*/

    val messageStream: DataStream[TripAggregation] = windowed
      // val result: DataStream[String] = ... .fold("start")((str, i) => { str + "-" + i })
      // .fold(Vector())((v, tripagg) => v +: tripagg.data)
      // .window(GlobalWindows.create).evictor(TimeEvictor.of(Time.of(10, TimeUnit.SECONDS)))
      .sum("data.length")
      .map(a => TripAggregation(ccn = a.ccn, tripid = a.tripid, data = a.data ))

    // http://stackoverflow.com/questions/8295597/adding-an-item-to-an-immutable-seq

    val myProducerConfig = FlinkKafkaProducer010.writeToKafkaWithTimestamps[TripAggregation](
      messageStream.javaStream,
      params.getRequired("topic.target"),
      new ProtobufSerializationSchema[TripAggregation](),
      params.getProperties
    )
    myProducerConfig.setLogFailuresOnly(false)
    myProducerConfig.setFlushOnCheckpoint(true)

    env.execute("Christian's ScalaJob")
}