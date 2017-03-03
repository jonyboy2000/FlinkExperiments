package com.microsoft.chgeuer

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.streaming.util.serialization.{DeserializationSchema, SerializationSchema}
import com.microsoft.chgeuer.proto.messages.{TrackingPacket, TripAggregation}

class TrackingPacketDeserializer extends DeserializationSchema[TrackingPacket]
{
  override def isEndOfStream(nextElement: TrackingPacket): Boolean = false
  override def deserialize(message: Array[Byte]): TrackingPacket = TrackingPacket.parseFrom(message)
  override def getProducedType: TypeInformation[TrackingPacket] = createTypeInformation[TrackingPacket]
}

class TripAggregationSerializationSchema extends SerializationSchema[TripAggregation] {
  override def serialize(element: TripAggregation): Array[Byte] = element.toByteArray
}

/*
class ProtobufDeserializationSchema[T](parser: Array[Byte] => T) extends DeserializationSchema[T] {
  override def deserialize(bytes: Array[Byte]): T = {
    Try(parser.apply(bytes)) match {
      case Success(element) =>
        element
      case Failure(e) =>
        throw new DeserializationException(s"Unable to de-serialize bytes", e)
    }
  }

  override def isEndOfStream(nextElement: T): Boolean = false

  override def getProducedType: TypeInformation[T] = ???
}

class ProtobufSerializationSchema[T] extends SerializationSchema[T] {
  override def serialize(element: T): Array[Byte] = {
    Try(element.asInstanceOf[ProtobufGeneratedMessage].toByteArray) match {
      case Success(bytes) =>
        bytes
      case Failure(e) =>
        throw new SerializationException(s"Unable to serialize bytes for $element", e)
    }
  }
}
*/