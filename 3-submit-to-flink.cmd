@echo off
title Submit to Flink
call environment.cmd

set CLASSNAME=com.madhukaraphatak.flink.streaming.examples.StreamingWordCount
set JAR=phatak-dev\flink-examples\target\scala-2.10\flink-examples_2.10-1.0.jar

REM call "%FLINK_DIR%\bin\flink.bat" run --class %CLASSNAME% --jobmanager %JOBMANAGER% --parallelism 1 %JAR% 
call "%FLINK_DIR%\bin\flink.bat" run --detached %JAR% 

REM call "%FLINK_DIR%\bin\flink.bat" -h
