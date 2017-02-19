namespace KafkaReceiver
{
    using System;
    using KafkaNet;
    using KafkaNet.Model;
    using System.Text;

    class KafkaReceiverProgram
    {
        static void Main(string[] args)
        {
            Console.Title = "Receiver";

            var router = new BrokerRouter(
                kafkaOptions: new KafkaOptions(
                    kafkaServerUri: new Uri("http://127.0.0.1:9092")));
                    var consumer = new Consumer(
                        options: new ConsumerOptions(
                            topic: "results",
                            router: router));

            foreach (var message in consumer.Consume())
            {
                var payload = Encoding.UTF8.GetString(message.Value);
                Console.WriteLine(
                    $"Response: Partition {message.Meta.PartitionId}, Offset {message.Meta.Offset} : \"{payload}\"");
            }
        }
    }
}