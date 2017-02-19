title Submit to Flink

set FLINK=..\flink-1.2.0-bin-hadoop27-scala_2.10
set FLINK_DIR=%~dp0%FLINK%
set FLINK_CONF_DIR=%FLINK_DIR%\conf

set JOBMANAGER=127.0.0.1:8081
set CLASSNAME=com.madhukaraphatak.flink.streaming.examples.StreamingWordCount
set JAR=phatak-dev\flink-examples\target\scala-2.10\flink-examples_2.10-1.0.jar

REM call "%FLINK_DIR%\bin\flink.bat" run --class %CLASSNAME% --jobmanager %JOBMANAGER% --parallelism 1 %JAR% 
call "%FLINK_DIR%\bin\flink.bat" run --detached %JAR% 

REM call "%FLINK_DIR%\bin\flink.bat" -h

pause
