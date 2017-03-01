namespace KafkaReceiver
{
    using System;
    using System.Linq;
    using KafkaNet;
    using KafkaNet.Model;
    using Kafka.Contracts;

    class KafkaReceiverProgram
    {
        static void Main(string[] args)
        {
            Console.Title = "Receiver";

            var consumer = new Consumer(
                options: new ConsumerOptions(
                    topic: "results",
                    router: new BrokerRouter(
                        kafkaOptions: new KafkaOptions(
                            kafkaServerUri: new Uri("http://13.73.154.72:9092")))));

            foreach (var message in consumer.Consume())
            {
                try
                {
                    var payload = message.Value.deserialize<TripAggregation>();

                    var data = string.Join(", ", payload.Data.Select(point =>
                        $"{new DateTime(ticks: point.Ticks, kind: DateTimeKind.Utc).ToLocalTime().ToString("yyyy-MM-dd HH:mm:ss")}")
                        .ToArray());

                    Console.WriteLine(
                        $"Response: Partition {message.Meta.PartitionId}, Offset {message.Meta.Offset} : ccn={payload.CCN} tripid={payload.TripID} {data}");
                }
                catch (Exception e)
                {
                    Console.Error.WriteLine($"Error: {e.Message}");
                }
            }
        }
    }
}