package christianGroupId;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer010;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer010.FlinkKafkaProducer010Configuration;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;

public class SocketTextStreamWordCount {
	public static void main(String[] args) throws Exception {
                final ParameterTool parameterTool = ParameterTool.fromArgs(args);

                final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

                // --topic test --bootstrap.servers localhost:9092 --zookeeper.connect localhost:2181 --group.id myGroup
                DataStream<String> messageStream = env.addSource(
                    new FlinkKafkaConsumer010<>(
                        parameterTool.getRequired("topic"),
                        new SimpleStringSchema(),
                        parameterTool.getProperties()
                    )
                );

                SingleOutputStreamOperator<String> sum = messageStream
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
                        sum,                            // input stream
                        "results",                      // target topic
                        new SimpleStringSchema(),       // serialization schema
                        parameterTool.getProperties()); // custom configuration for KafkaProducer (including broker list)

                // the following is necessary for at-least-once delivery guarantee
                myProducerConfig.setLogFailuresOnly(false);   // "false" by default
                myProducerConfig.setFlushOnCheckpoint(true);  // "false" by default

                env.execute();
        }
}