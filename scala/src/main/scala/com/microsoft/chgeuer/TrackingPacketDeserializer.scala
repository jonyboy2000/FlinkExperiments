package com.microsoft.chgeuer

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.streaming.util.serialization.DeserializationSchema
import com.microsoft.chgeuer.proto.messages.TrackingPacket

class TrackingPacketDeserializer extends DeserializationSchema[TrackingPacket]
{
  override def isEndOfStream(nextElement: TrackingPacket): Boolean = false
  override def deserialize(message: Array[Byte]): TrackingPacket = TrackingPacket.parseFrom(message)
  override def getProducedType: TypeInformation[TrackingPacket] = createTypeInformation[TrackingPacket]
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
*/