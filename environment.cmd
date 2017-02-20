@echo off


set KAFKA=..\kafka_2.11-0.10.1.1
set FLINK=..\flink-1.2.0-bin-hadoop27-scala_2.10

set FLINK_DIR=%~dp0%FLINK%
set FLINK_CONF_DIR=%FLINK_DIR%\conf

set ZKURL=localhost:2181
set JOBMANAGER=127.0.0.1:8081
