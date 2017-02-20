@echo off
TITLE Kafka
call environment.cmd
call "%~dp0%KAFKA%\bin\windows\kafka-server-start.bat" "%~dp0%KAFKA%\config\server.properties"
