@echo off
title Kafka Setup
call environment.cmd
call "%~dp0%KAFKA%\bin\windows\kafka-topics.bat" --create --topic results --zookeeper %ZKURL% --partitions 1 --replication-factor 1
