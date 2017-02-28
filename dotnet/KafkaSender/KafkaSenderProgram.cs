namespace KafkaSender
{
    using System;
    using System.IO;
    using System.Threading.Tasks;
    using Kafka.Contracts;
    using KafkaNet;
    using KafkaNet.Model;
    using KafkaNet.Protocol;
    using ProtoBuf;

    class KafkaSenderProgram
    {
        static void Main(string[] args) { Console.Title = "Sender";  MainAync(args).Wait(); }

        static async Task MainAync(string[] args)
        {
            var fn = @"..\..\..\..\..\VodafoneTestData\data\proto\8080415317.protobuf";

            var router = new BrokerRouter(
                kafkaOptions: new KafkaOptions(
                    kafkaServerUri: new Uri("http://13.73.154.72:9092")));

            var packets = ReadProtobufFile(fn);
            using (var client = new Producer(router))
            {
                await SendMessages(client, packets);
            }
        }

        static TrackingPacket[] ReadProtobufFile(string filename)
        {
            using (var file = File.OpenRead(filename))
            {
                return Serializer.Deserialize<TrackingPacket[]>(file);
            }
        }

        public static byte[] serialize<T>(T p) 
        {
            var ms = new MemoryStream();
            Serializer.Serialize<T>(ms, p);
            return ms.ToArray();
        }

        public static async Task send(Producer client, TrackingPacket msg)
        {
            await client.SendMessageAsync(
                topic: "test",
                messages: new[] { new Message { Value = serialize(msg) } });
        }

        static async Task SendMessages(Producer client, TrackingPacket[] packets)
        {
            if (packets == null || packets.Length == 0) { return; }

            long startTicks = DateTime.UtcNow.Ticks;
            await send(client, packets[0]);

            if (packets.Length == 1) { return; }
            for (int i=1; i<packets.Length; i++)
            {
                var packet = packets[i];
                long now = DateTime.UtcNow.Ticks;
                var delay = TimeSpan.FromTicks(startTicks + packet.Ticks - now);

                Console.WriteLine($"Waiting {delay} before sending packet {packet.Ticks}");
                await Task.Delay(delay);
                await send(client, packet);
            }
        }
    }

    
}