@echo off
TITLE Send input
call environment.cmd
call "%~dp0%KAFKA%\bin\windows\kafka-console-producer.bat" --broker-list localhost:9092 --topic test
