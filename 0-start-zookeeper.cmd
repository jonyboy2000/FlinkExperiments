title ZooKeeper
set KAFKA=..\kafka_2.11-0.10.1.1
call "%~dp0%KAFKA%\bin\windows\zookeeper-server-start.bat" "%~dp0%KAFKA%\config\zookeeper.properties"
pause

REM https://kafka.apache.org/documentation.html#quickstart_download
