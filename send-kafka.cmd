set KAFKA=..\kafka_2.11-0.10.1.1
call "%~dp0%KAFKA%\bin\windows\kafka-console-producer.bat" --broker-list localhost:9092 --topic test
