# Singleton-Install on Linux

## Fetch software

```
apt-get -y update
apt-get -y install jq default-jdk

curl --get --url http://mirror.netcologne.de/apache.org/flink/flink-1.2.0/flink-1.2.0-bin-hadoop27-scala_2.11.tgz --output flink-1.2.0-bin-hadoop27-scala_2.11.tgz
curl --get --url http://mirror.netcologne.de/apache.org/kafka/0.10.2.0/kafka_2.11-0.10.2.0.tgz --output kafka_2.11-0.10.2.0.tgz

tar xvfz flink-1.2.0-bin-hadoop27-scala_2.11.tgz
tar xvfz kafka_2.11-0.10.2.0.tgz

sudo mkdir /flinksave
sudo mount -t cifs //flink1.file.core.windows.net/savepoints /flinksave -o vers=3.0,username=flink1,password=HKJHKJHJHHJKHJKHKJK7Q==,dir_mode=0777,file_mode=0777

echo "state.backend: filesystem" >> flink-1.2.0/conf/flink-conf.yaml
echo "state.backend.fs.checkpointdir: file:///flinksave" >> flink-1.2.0/conf/flink-conf.yaml

externalPublicIP=`curl -s -H Metadata:true http://169.254.169.254/metadata/latest/instance/network?format=json | jq -r .interface[0].ipv4.ipaddress[0].publicip`
echo "advertised.listeners=PLAINTEXT://$externalPublicIP:9092" >> kafka_2.11-0.10.2.0/config/server.properties
echo "listeners=PLAINTEXT://:9092" >> kafka_2.11-0.10.2.0/config/server.properties
```

## Start ZK, Kafka, Flink

```
./kafka_2.11-0.10.2.0/bin/zookeeper-server-start.sh ./kafka_2.11-0.10.2.0/config/zookeeper.properties > zk.out 2>&1 &
./kafka_2.11-0.10.2.0/bin/kafka-server-start.sh ./kafka_2.11-0.10.2.0/config/server.properties > kafka.out 2>&1 &
./flink-1.2.0/bin/start-local.sh


./kafka_2.11-0.10.2.0/bin/kafka-topics.sh --create --topic test --zookeeper localhost:2181 --partitions 1 --replication-factor 1
./kafka_2.11-0.10.2.0/bin/kafka-topics.sh --create --topic results --zookeeper localhost:2181 --partitions 1 --replication-factor 1
```

## Clean up the mess

```
rm -rf kafka_2.11-0.10.2.0 flink-1.2.0
rm -rf /tmp/blobStore-* /tmp/flink-* /tmp/kafka-logs /tmp/zookeeper
```

## Talk to it

    ```
./kafka_2.11-0.10.2.0/bin/kafka-console-producer.sh --broker-list 13.73.154.72:9092 --topic test
"C:\Users\chgeuer\Java\kafka_2.11-0.10.1.1\bin\windows\kafka-console-consumer.bat" --bootstrap-server 13.73.154.72:9092 --topic results --from-beginning
```
