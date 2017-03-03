package com.microsoft.chgeuer

import scala.collection.mutable.ListBuffer
import scala.math._
import com.microsoft.chgeuer.proto.messages.{Calculated, Point}

object helper {
  def haversineInMeters (lat1:Double, lon1:Double, lat2:Double, lon2:Double) :Double = {
    val R = 6372.8e3
    val dLat=(lat2 - lat1).toRadians
    val dLon=(lon2 - lon1).toRadians
    val a = pow(sin(dLat/2),2) + pow(sin(dLon/2), 2) * cos(lat1.toRadians) * cos(lat2.toRadians)
    val c = 2 * asin( min(1.0, sqrt(a))   )
    R * c
  }

  def combinePoints (points: Iterable[Point]) : ListBuffer[Point] = {
    val pointList = points.toList
      .sortBy(_.millisecondsSinceEpoch)

    var data : ListBuffer[Point] = ListBuffer[Point]()
    for (i <- pointList.indices) {
      val curr = pointList(i)
      var calculatedProperties : Option[Calculated] = if (i == 0) {
        Console.out.println(s"No speed available, start of window")
        None
      } else {
        val prev = pointList(i - 1)

        val seconds: Double = (curr.millisecondsSinceEpoch - prev.millisecondsSinceEpoch) / 1000.0
        Console.out.println(s"Seconds $seconds curr=${curr.millisecondsSinceEpoch}ms prev=${prev.millisecondsSinceEpoch}ms")
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
      data = data :+ Point(millisecondsSinceEpoch = curr.millisecondsSinceEpoch, lat = curr.lat, lon = curr.lon, properties = calculatedProperties)
    }

    data
  }
}