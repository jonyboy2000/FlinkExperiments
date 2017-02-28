namespace Kafka.Contracts
{
    using System;
    using ProtoBuf;

    [ProtoContract]
    public class TrackingPacket
    {
        [ProtoMember(1)]
        public Int64 CCN { get; set; }
        [ProtoMember(2)]
        public Int32 TripID { get; set; }
        [ProtoMember(3)]
        public Int64 Ticks { get; set; }
        [ProtoMember(4)]
        public Double Latitude { get; set; }
        [ProtoMember(5)]
        public Double Longtitude { get; set; }
    }


    /*
    message TripAggregation
    {
        int64 ccn = 1;
        int32 tripid = 2;
        repeated Point data = 3;
    }
    */

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
        public Int64 Ticks { get; set; }
        [ProtoMember(2)]
        public Double Latitude { get; set; }
        [ProtoMember(3)]
        public Double Longtitude { get; set; }
    }
}
