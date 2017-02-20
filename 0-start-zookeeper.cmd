@echo off
TITLE ZooKeeper
call environment.cmd
call "%~dp0%KAFKA%\bin\windows\zookeeper-server-start.bat" "%~dp0%KAFKA%\config\zookeeper.properties"
