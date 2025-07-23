@echo off
SET SCRIPT_DIR=%~dp0
SET TOOLS_PATH=%homedrive%%homepath%
REM SET TOOLS_PATH=C:\Tools
REM SET KAFKA_PATH="%TOOLS_PATH%\kafka_2.13-3.4.0"
REM SET MONGO_PATH="%TOOLS_PATH%\Mongo\mongodb-7.0.8"
SET KAFKA_PATH="%TOOLS_PATH%\kafka"
SET MONGO_PATH="%TOOLS_PATH%\mongodb"
SET dataDir=%homedrive%%homepath%\mongodb\data
REM SET dataDir=C:\tmp\mongo-data