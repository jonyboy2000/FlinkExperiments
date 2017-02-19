title Flink
set FLINK=..\flink-1.2.0-bin-hadoop27-scala_2.10
set FLINK_DIR=%~dp0%FLINK%
set FLINK_CONF_DIR=%FLINK_DIR%\conf
call "%FLINK_DIR%\bin\start-local.bat"
pause
