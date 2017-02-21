@echo off
title Submit to Flink
call environment.cmd

set CLASSNAME=com.microsoft.chgeuer.ScalaJob
set JAR=scala/target/scala-2.11/chgeuername-assembly-0.1-SNAPSHOT.jar
set ARGS=--topic.input test --topic.target results --bootstrap.servers localhost:9092 --zookeeper.connect localhost:2181 --group.id myGroup

call "%FLINK_DIR%\bin\flink.bat" run --detached --class %CLASSNAME% --jobmanager %JOBMANAGER% --parallelism 1 %JAR% %ARGS%
