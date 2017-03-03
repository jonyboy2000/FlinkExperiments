namespace KafkaSender
{
    using System;
    using System.Linq;
    using System.IO;
    using System.Threading.Tasks;
    using Kafka.Contracts;
    using KafkaNet;
    using KafkaNet.Model;
    using KafkaNet.Protocol;
    using ProtoBuf;

    class KafkaSenderProgram
    {
        static readonly FiringMechanism firingMechanism = FiringMechanism.KeyPress;

        static void Main(string[] args) { Console.Title = "Sender";  MainAync(args).Wait(); }

        static async Task MainAync(string[] args)
        {
            Console.Write("Press <return> to start"); Console.ReadLine();

            var fn = @"..\..\..\..\..\VodafoneTestData\data\proto\8080415317.protobuf";

            var kafkaHost = "13.73.154.72";
            kafkaHost = "127.0.0.1";

            var router = new BrokerRouter(
                kafkaOptions: new KafkaOptions(
                    kafkaServerUri: new Uri($"http://{kafkaHost}:9092")));

            var packets = ReadProtobufFile(fn);
            using (var client = new Producer(router))
            {
                if (firingMechanism == FiringMechanism.ConcreteTimes)
                {
                    await SendMessagesBasedOnTime(client, packets);
                }
                else
                {
                    await SendMessagesOnKeyPress(client, packets);

                }
            }
        }

        

        static async Task SendMessagesOnKeyPress(Producer client, TrackingPacket[] packets)
        {
            if (packets == null || packets.Length == 0) { return; }

            for (int i = 0; i < packets.Length; i++)
            {
                var packet = packets[i];
                packet.MillisecondsSinceEpoch =  1000 * DateTime.UtcNow.ToUnixTime(); // += startTicks;

                Console.ReadKey(intercept: true);
                await send(client, packet);

                
                Console.WriteLine($"Packet #{i} sent {(packet.MillisecondsSinceEpoch / 1000).FromUnixTime().ToLocalTime().ToString("HH:mm:ss")}");
            }
        }

        static async Task SendMessagesBasedOnTime(Producer client, TrackingPacket[] packets)
        {
            if (packets == null || packets.Length == 0) { return; }

            long startMillis = 1000 * DateTime.UtcNow.ToUnixTime();
            packets[0].MillisecondsSinceEpoch = startMillis;
            await send(client, packets[0]);
            if (packets.Length == 1) { return; }

            for (int i=1; i<packets.Length; i++)
            {
                var packet = packets[i];
                packet.MillisecondsSinceEpoch += startMillis;

                var delay = packet.MillisecondsSinceEpoch.FromUnixTime().Subtract(DateTime.UtcNow);
                Console.WriteLine($"Now {DateTime.Now.ToLocalTime().ToString("HH:mm:ss")}, waiting {delay} before sending packet {packet.MillisecondsSinceEpoch.FromUnixTime().ToLocalTime().ToString("HH:mm:ss")}");
                await Task.Delay(delay);
                await send(client, packet);
            }
        }

        public static async Task send(Producer client, TrackingPacket msg)
        {
            await client.SendMessageAsync(topic: "test",
                messages: new[] { new Message { Value = msg.serialize() } });
        }

        static TrackingPacket[] ReadProtobufFile(string filename)
        {
            using (var file = File.OpenRead(filename))
            {
                return Serializer.Deserialize<TrackingPacket[]>(file)
                    .OrderBy(_ => _.MillisecondsSinceEpoch)
                    .ToArray();
            }
        }
    }

    enum FiringMechanism { KeyPress, ConcreteTimes }
}