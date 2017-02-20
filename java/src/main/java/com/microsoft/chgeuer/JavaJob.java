package com.microsoft.chgeuer;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer010;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer010.FlinkKafkaProducer010Configuration;
import org.apache.flink.streaming.util.serialization.SerializationSchema;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;

// --topic.input test --topic.target results --bootstrap.servers localhost:9092 --zookeeper.connect localhost:2181 --group.id myGroup

public class JavaJob {
	public static void main(String[] args) throws Exception {
                final ParameterTool parameterTool = ParameterTool.fromArgs(args);
                final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

                DataStream<String> messageStream = env.addSource(
                    new FlinkKafkaConsumer010<>(
                        parameterTool.getRequired("topic.input"),
                        new SimpleStringSchema(),
                        parameterTool.getProperties()
                    )
                );

                DataStream<String> sum = messageStream
                    // .rebalance()
                    // .flatMap(line -> line.split("\\s+"))
                    // .filter(word -> !word.isEmpty())
                    // .map(word -> word.toLowerCase())
                    .map(w -> new Tuple2<>(w, 1))
                    .keyBy(0)
                    // .timeWindow(Time.seconds(5))
                    .sum(1)
                    .map(t -> String.format("%s %d", t.f0, t.f1));

                FlinkKafkaProducer010Configuration<String> myProducerConfig = FlinkKafkaProducer010.writeToKafkaWithTimestamps(
                        sum,
                        parameterTool.getRequired("topic.target"),
                        (SerializationSchema) new SimpleStringSchema(),
                        parameterTool.getProperties()
                );

                // the following is necessary for at-least-once delivery guarantee
                myProducerConfig.setLogFailuresOnly(false);   // "false" by default
                myProducerConfig.setFlushOnCheckpoint(true);  // "false" by default

                env.execute("Christian's JavaJob");
        }
}