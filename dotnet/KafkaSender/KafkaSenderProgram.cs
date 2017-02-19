namespace KafkaSender
{
    using System;
    using System.Threading.Tasks;
    using KafkaNet;
    using KafkaNet.Model;
    using KafkaNet.Protocol;
    using System.Linq;

    class KafkaSenderProgram
    {
        static void Main(string[] args) { Console.Title = "Sender";  MainAync(args).Wait(); Console.WriteLine("Done3"); }

        static async Task MainAync(string[] args)
        {
            var router = new BrokerRouter(
                kafkaOptions: new KafkaOptions(
                    kafkaServerUri: new Uri("http://127.0.0.1:9092")));

            using (var client = new Producer(router))
            {
                Func<string, Task> send = msg => client.SendMessageAsync(
                    topic: "test", messages: new[] { new Message(value: msg) });

                await Task.WhenAll(
                    Enumerable
                        .Range(0, 10)
                        .Select(i => $"Message {i}")
                        .Select(send)
                        .ToArray());

                Console.WriteLine("Done");
            }
            Console.WriteLine("Done2");
        }
    }
}