@echo off
TITLE Reveive results 
call environment.cmd
call "%~dp0%KAFKA%\bin\windows\kafka-console-consumer.bat" --bootstrap-server localhost:9092 --topic results --from-beginning
