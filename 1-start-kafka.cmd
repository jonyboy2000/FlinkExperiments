title Kafka
set KAFKA=..\kafka_2.11-0.10.1.1
call "%~dp0%KAFKA%\bin\windows\kafka-server-start.bat" "%~dp0%KAFKA%\config\server.properties"
pause

REM https://kafka.apache.org/documentation.html#quickstart_download
REM call "%~dp0%KAFKA%\bin\windows\kafka-topics.bat" --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test
REM call "%~dp0%KAFKA%\bin\windows\kafka-topics.bat" --list   --zookeeper localhost:2181
