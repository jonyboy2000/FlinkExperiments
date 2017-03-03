package com.microsoft.chgeuer

import scala.math._

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