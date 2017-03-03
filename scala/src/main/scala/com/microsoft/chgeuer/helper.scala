package com.microsoft.chgeuer

import scala.math._
import java.util.Calendar

object helper {
  def haversineInMeters (lat1:Double, lon1:Double, lat2:Double, lon2:Double) :Double = {
    val R = 6372.8e3
    val dLat=(lat2 - lat1).toRadians
    val dLon=(lon2 - lon1).toRadians
    val a = pow(sin(dLat/2),2) + pow(sin(dLon/2), 2) * cos(lat1.toRadians) * cos(lat2.toRadians)
    val c = 2 * asin( min(1.0, sqrt(a))   )
    R * c
  }

  val TICKS_PER_SECOND = 10000000.0
  val TICKS_PER_MILLISECOND : Long = 10000
  val TICKS_AT_EPOCH : Long = 621355968000000000L

  def ticksToSeconds(ticks: Long): Double = {
    ticks / TICKS_PER_SECOND
  }
  def javaMillisToTicks (millis: Long): Long = {
    val ticks = millis * TICKS_PER_MILLISECOND + TICKS_AT_EPOCH
    ticks
  }

  def tickToDate (ticks:Long): String = {
    val javaMillis : Long = ticks  / TICKS_PER_MILLISECOND
    val calendar = Calendar.getInstance()
    calendar.setTimeInMillis(javaMillis)
    val str = s"${calendar.get(Calendar.HOUR)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}"
    str
  }
}