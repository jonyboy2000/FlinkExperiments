namespace KafkaSender
{
    using System;
    using System.Threading.Tasks;
    using KafkaNet;
    using KafkaNet.Model;
    using KafkaNet.Protocol;
    using System.Linq;
    using ProtoBuf;
    using System.IO;

    class KafkaSenderProgram
    {
        static void Main(string[] args) { Console.Title = "Sender";  MainAync(args).Wait(); Console.WriteLine("Done3"); }

        static async Task MainAync(string[] args)
        {
            var router = new BrokerRouter(
                kafkaOptions: new KafkaOptions(
                    kafkaServerUri: new Uri("http://127.0.0.1:9092")));

            Func<TrackingPacket, byte[]> serialize = p =>
            {
                var ms = new MemoryStream();
                Serializer.Serialize<TrackingPacket>(ms, p);
                return ms.ToArray();
            };

            using (var client = new Producer(router))
            {
                Func<TrackingPacket, Task> send = async (msg) =>
                {
                    await Task.Delay(TimeSpan.FromMilliseconds(500));
                    await client.SendMessageAsync(
                      topic: "test",
                      messages: new[]
                      {
                        new Message { Value = serialize(msg) }
                      });
                };
                /*
                await Task.WhenAll(
                    Enumerable
                        .Range(0, 100)
                        .Select(i => new TrackingPacket { CCN = "Tracker", Id = i } )
                        .Select(send)
                        .ToArray());
                */

                for (var i = 0; i < 100; i++)
                {
                    await send(new TrackingPacket { CCN = "Tracker", Id = i });
                }


                Console.WriteLine("Done");
            }
            Console.WriteLine("Done2");
        }
    }

    [ProtoContract]
    class TrackingPacket
    {
        [ProtoMember(1)]
        public string CCN { get; set; }
        [ProtoMember(2)]
        public Int32 Id { get; set; }
        [ProtoMember(3)]
        public double Lat { get; set; }
        [ProtoMember(4)]
        public double Lon { get; set; }
        [ProtoMember(5)]
        public Timestamp Date { get; set; }
    }

    [ProtoContract]
    class Timestamp
    {
        [ProtoMember(1)]
        public Int64 Seconds { get; set; }
        [ProtoMember(2)]
        public Int32 Nanos { get; set; }
    }
}