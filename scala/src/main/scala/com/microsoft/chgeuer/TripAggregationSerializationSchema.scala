package com.microsoft.chgeuer

import org.apache.flink.streaming.util.serialization.SerializationSchema
import com.microsoft.chgeuer.proto.messages.TripAggregation

class TripAggregationSerializationSchema extends SerializationSchema[TripAggregation] {
  override def serialize(element: TripAggregation): Array[Byte] = element.toByteArray
}

/*
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