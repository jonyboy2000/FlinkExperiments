package com.microsoft.chgeuer

import scala.util.control.Breaks._
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import com.microsoft.chgeuer.proto.messages.TrackingPacket

class TrackingPacketSource extends SourceFunction[TrackingPacket] {
  @volatile var cancelled = false

  override def cancel(): Unit = { cancelled = true }

  override def run(ctx: SourceContext[TrackingPacket]): Unit = {
    val ccn : Long = 1
    val tripid : Int = 1

    breakable {
      for ((milliseconds, lat, lon) <- TrackingSample.data)
      {
        if (cancelled) { break }
        Console.println(s"Sleeping $milliseconds")

        Thread.sleep(milliseconds)

        ctx.collect(
          TrackingPacket(
            ccn = ccn, tripid = tripid,
            millisecondsSinceEpoch = System.currentTimeMillis,
            lat = lat, lon = lon)
        )
      }
    }
  }
}