@echo off
title Kafka Setup
call environment.cmd
call "%~dp0%KAFKA%\bin\windows\kafka-topics.bat" --delete --topic %KAFKA_TOPIC_INGEST% --zookeeper %ZKURL%
call "%~dp0%KAFKA%\bin\windows\kafka-topics.bat" --delete --topic %KAFKA_TOPIC_RESULT% --zookeeper %ZKURL%
call "%~dp0%KAFKA%\bin\windows\kafka-topics.bat" --create --topic %KAFKA_TOPIC_INGEST% --zookeeper %ZKURL% --partitions 1 --replication-factor 1
call "%~dp0%KAFKA%\bin\windows\kafka-topics.bat" --create --topic %KAFKA_TOPIC_RESULT% --zookeeper %ZKURL% --partitions 1 --replication-factor 1
