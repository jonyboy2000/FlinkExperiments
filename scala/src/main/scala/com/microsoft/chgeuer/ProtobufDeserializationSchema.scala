package com.microsoft.chgeuer

import com.google.protobuf.{GeneratedMessage => ProtobufGeneratedMessage}
import org.apache.commons.lang3.SerializationException
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.types.DeserializationException
import org.apache.flink.streaming.util.serialization.{DeserializationSchema, SerializationSchema}
import scala.util.{Failure, Success, Try}

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