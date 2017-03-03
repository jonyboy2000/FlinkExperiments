namespace Kafka.Contracts
{
    using System;
    using System.IO;
    using ProtoBuf;

    public static class Proto
    {
        public static byte[] serialize<T>(this T p)
        {
            var ms = new MemoryStream();
            Serializer.Serialize<T>(ms, p);
            return ms.ToArray();
        }

        public static T deserialize<T>(this byte[] p)
        {
            var ms = new MemoryStream(p, 0, p.Length);
            return Serializer.Deserialize<T>(ms);
        }

        public static DateTime FromUnixTime(this long unixTime)
        {
            var epoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
            return epoch.AddSeconds(unixTime);
        }

        public static long ToUnixTime(this DateTime date)
        {
            var epoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
            return Convert.ToInt64((date - epoch).TotalSeconds);
        }
    }

    [ProtoContract]
    public class TrackingPacket
    {
        [ProtoMember(1)]
        public Int64 CCN { get; set; }
        [ProtoMember(2)]
        public Int32 TripID { get; set; }
        [ProtoMember(3)]
        public Int64 MillisecondsSinceEpoch { get; set; }
        [ProtoMember(4)]
        public Double Latitude { get; set; }
        [ProtoMember(5)]
        public Double Longtitude { get; set; }
    }

    [ProtoContract]
    public class TripAggregation
    {
        [ProtoMember(1)]
        public Int64 CCN { get; set; }
        [ProtoMember(2)]
        public Int32 TripID { get; set; }
        [ProtoMember(3)]
        public Point[] Data { get; set; }
    }

    [ProtoContract]
    public class Point
    {
        [ProtoMember(1)]
        public Int64 MillisecondsSinceEpoch { get; set; }
        [ProtoMember(2)]
        public Double Latitude { get; set; }
        [ProtoMember(3)]
        public Double Longtitude { get; set; }
        [ProtoMember(4)]
        public Calculated Properties { get; set; }
    }

    [ProtoContract]
    public class Calculated
    {
        [ProtoMember(1)]
        public Double TimeDifferenceToPreviousPoint { get; set; }
        [ProtoMember(2)]
        public Double DistanceInMetersToPreviousPoint { get; set; }
    }
}