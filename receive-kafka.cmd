set KAFKA=..\kafka_2.11-0.10.1.1
call "%~dp0%KAFKA%\bin\windows\kafka-console-consumer.bat" --bootstrap-server localhost:9092 --topic results --from-beginning
REM https://kafka.apache.org/documentation.html#quickstart_download
