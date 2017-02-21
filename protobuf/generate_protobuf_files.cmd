@echo off

REM https://github.com/google/protobuf/releases
REM Get protoc.exe from https://github.com/google/protobuf/releases/download/v3.2.0/protoc-3.2.0-win32.zip 

protoc.exe -I="%~dp0..\scala\src\main\protobuf" "%~dp0..\scala\src\main\protobuf\messages.proto" --java_out="%~dp0java"     
protoc.exe -I="%~dp0..\scala\src\main\protobuf" "%~dp0..\scala\src\main\protobuf\messages.proto" --csharp_out="%~dp0csharp" 
