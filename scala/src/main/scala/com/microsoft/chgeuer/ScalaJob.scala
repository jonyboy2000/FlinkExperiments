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
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer010, FlinkKafkaProducer010}

import scala.collection.mutable.ListBuffer

object ScalaJob extends App {
  val args2 = "--topic.input test --topic.target results --group.id myGroup --bootstrap.servers localhost:9092 --zookeeper.connect localhost:2181".split(" +")
  val env = StreamExecutionEnvironment.createLocalEnvironment(parallelism = 1)

  val params = ParameterTool.fromArgs(args2)
  //  val env = StreamExecutionEnvironment.getExecutionEnvironment

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
        override def extractTimestamp(element: TrackingPacket): Long = helper.ticksToMillis(element.ticks)
      }
    )

  val keyed: KeyedStream[TrackingPacket, (Long, Int)] = rawStreamWithTimestamps
    .keyBy(x => (x.ccn, x.tripid))

  val reduced_new: DataStream[TripAggregation] = keyed
    // .window(GlobalWindows.create()).trigger(CountTrigger.of(2))
    // .countWindow(size = 10)
    .window(EventTimeSessionWindows.withGap(Time.seconds(5)))
    .allowedLateness(Time.seconds(2))
    .apply((key: (Long, Int), window: Window, input: Iterable[TrackingPacket], out: Collector[TripAggregation]) => {
      Console.out.println("Evaluating window")

      val ccn = key._1
      val tripid = key._2

      if (input.nonEmpty) {
        val pointList = input.map(tp => Point(ticks = tp.ticks, lat = tp.lat, lon = tp.lon))
          .toList
          .sortBy(_.ticks)

        var data : ListBuffer[Point] = ListBuffer[Point]()
        for (i <- pointList.indices) {
          val curr = pointList(i)
          var calculatedProperties : Option[Calculated] = if (i == 0) {
            Console.out.println(s"No speed available, start of window")
            None
          } else {
            val prev = pointList(i - 1)

            // Console.println(s"Event  ${helper.tickToDate(curr.ticks)} system ${helper.tickToDate(helper.javaMillisToTicks( System.currentTimeMillis()))}")

            val seconds = helper.ticksToSeconds(curr.ticks - prev.ticks)
            val meters = helper.haversineInMeters(prev.lat, prev.lon, curr.lat, curr.lon)
            val kmh = 3.6 * meters / seconds

            if (kmh > 300.0) {
              Console.out.println(s"Speed too high, discarding data point: ${kmh.formatted("%.1f")} km/h")
              // None
              Some(Calculated(timeDifferenceToPreviousPoint = seconds, distanceInMetersToPreviousPoint = meters))
            }
            else {
              Console.out.println(s"Valid speed: ${kmh.formatted("%.1f")} km/h")
              Some(Calculated(timeDifferenceToPreviousPoint = seconds, distanceInMetersToPreviousPoint = meters))
            }
          }

          Console.out.println(s"Adding point ${calculatedProperties}")
          data = data :+ Point(ticks = curr.ticks, lat = curr.lat, lon = curr.lon, properties = calculatedProperties)
        }


        out.collect(TripAggregation(ccn = ccn, tripid = tripid, data = data))
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