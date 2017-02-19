title Kafka Setup
set KAFKA=..\kafka_2.11-0.10.1.1
set ZKURL=localhost:2181
REM call "%~dp0%KAFKA%\bin\windows\kafka-topics.bat" --create --topic test --zookeeper %ZKURL% --partitions 1 --replication-factor 1
call "%~dp0%KAFKA%\bin\windows\kafka-topics.bat" --create --topic results --zookeeper %ZKURL% --partitions 1 --replication-factor 1

REM call "%~dp0%KAFKA%\bin\windows\kafka-topics.bat" --delete --topic test --zookeeper %ZKURL%
pause
