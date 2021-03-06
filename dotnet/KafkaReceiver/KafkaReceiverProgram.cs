﻿namespace KafkaReceiver
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

            Console.WriteLine($"Using {Kafka.Contracts.Endpoint.KafkaHost}");

            var consumer = new Consumer(
                options: new ConsumerOptions(
                    topic: "results",
                    router: new BrokerRouter(
                        kafkaOptions: new KafkaOptions(
                            kafkaServerUri: new Uri($"http://{Kafka.Contracts.Endpoint.KafkaHost}:9092")))));

            foreach (var message in consumer.Consume())
            {
                try
                {
                    var payload = message.Value.deserialize<TripAggregation>();

                    Func<Calculated, string> getSpeed = _ =>
                    {
                        if (_ == null)
                            return "null";

                        var sec = _.TimeDifferenceToPreviousPoint;
                        var m = _.DistanceInMetersToPreviousPoint;

                        if (sec == 0.0)
                            return "seconds == 0";

                        if (m < 0.1)
                            return "distance too short";

                        var kmh = 3.6 * m / sec;

                        return $"{kmh.ToString("000.0")} km/h";
                    };

                    // "yyyy-MM-dd HH:mm:ss"
                    var data = string.Join(", ", payload.Data.Select(point =>
                        $"{(point.MillisecondsSinceEpoch / 1000).FromUnixTime().ToLocalTime().ToString("HH:mm:ss")} ({ getSpeed(point.Properties)})").ToArray());

                    Console.WriteLine(
                        $"Kafka Offset {message.Meta.Offset} : ccn={payload.CCN} tripid={payload.TripID} {data}");
                }
                catch (Exception e)
                {
                    Console.Error.WriteLine($"Error: {e.Message}");
                }
            }
        }
    }
}