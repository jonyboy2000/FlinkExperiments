package com.microsoft.chgeuer

// com.microsoft.chgeuer.ScalaJob
// --topic.input test --topic.target results --group.id myGroup --bootstrap.servers localhost:9092 --zookeeper.connect localhost:2181
import math._
import com.microsoft.chgeuer.proto.messages.{Calculated, Point, TrackingPacket, TripAggregation}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.util.Collector
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.Window
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer010, FlinkKafkaProducer010}
import scala.collection.mutable.ListBuffer

object helper {
  def ticksToSeconds(ticks: Long): Double = {
    ticks / 10000000.0
  }

  def haversineInMeters (lat1:Double, lon1:Double, lat2:Double, lon2:Double) :Double = {
    val R = 6372.8e3
    val dLat=(lat2 - lat1).toRadians
    val dLon=(lon2 - lon1).toRadians
    val a = pow(sin(dLat/2),2) + pow(sin(dLon/2), 2) * cos(lat1.toRadians) * cos(lat2.toRadians)
    val c = 2 * asin( min(1.0, sqrt(a))   )
    R * c
  }
}

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

      if (input.nonEmpty) {
        val pointList = input.map(tp => Point(ticks = tp.ticks, lat = tp.lat, lon = tp.lon))
          .toList
          .sortBy(_.ticks)

        var data : ListBuffer[Point] = ListBuffer[Point]()
        for (i <- pointList.indices) {
          val curr = pointList(i)

          if (i == 0) {
            data = data :+ Point(ticks = curr.ticks, lat = curr.lat, lon = curr.lon, properties = None)
          } else {
            val prev = pointList(i-1)

            val timeDifferenceToPreviousPoint  = helper.ticksToSeconds( curr.ticks - prev.ticks)

            val distanceInMetersToPreviousPoint = helper.haversineInMeters( prev.lat, prev.lon, curr.lat, curr.lon )

            val speedInKmh = 3.6 * distanceInMetersToPreviousPoint / timeDifferenceToPreviousPoint

            Console.println(s"${speedInKmh.formatted("%.1f")} km/h")

            val calculatedProperties : Option[Calculated] = if (speedInKmh > 300.0)
              None
            else
              Some(Calculated(
                timeDifferenceToPreviousPoint = timeDifferenceToPreviousPoint,
                distanceInMetersToPreviousPoint = distanceInMetersToPreviousPoint))

            data = data :+ Point(ticks = curr.ticks, lat = curr.lat, lon = curr.lon, properties = calculatedProperties)
          }
        }

        val ta = TripAggregation(ccn = key._1, tripid = key._2, data = data)

        out.collect(ta)
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