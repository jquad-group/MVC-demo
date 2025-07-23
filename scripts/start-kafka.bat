@echo off
cls

call settings.bat

echo ------------------------------------
echo Cleaning TMP KAFKA folders...
RMDIR "C:\tmp\kafka-logs" /S /Q
RMDIR "C:\tmp\zookeeper" /S /Q

echo ------------------------------------
echo Starte Kafka Zookeeper...
start "Kafka - Zookeeper" /MIN  %KAFKA_PATH%/bin/windows/zookeeper-server-start.bat  %SCRIPT_DIR%/zookeeper.properties
timeout /T 2 /NOBREAK > nul

echo ------------------------------------
echo Starte Kafka Server...
start "Kafka - Server" /MIN   %KAFKA_PATH%/bin/windows/kafka-server-start.bat %SCRIPT_DIR%/server.properties
timeout /T 5 /NOBREAK > nul

echo ------------------------------------
echo "create new topic local.private-dev.refsys-aggregation.movementdata.events"
call  %KAFKA_PATH%/bin/windows/kafka-topics.bat --bootstrap-server localhost:9092 -topic local.movementdata.events --partitions 3 -create

echo ------------------------------------
echo "create new topic local.private-dev.refsys-aggregation.movementdata.events-dlt"
call  %KAFKA_PATH%/bin/windows/kafka-topics.bat --bootstrap-server localhost:9092 -topic local.movementdata.events-dlt -create

echo ------------------------------------
echo "create new topic local.private-dev.refsys-aggregation.changedata.events"
call %KAFKA_PATH%/bin/windows/kafka-topics.bat --bootstrap-server localhost:9092 -topic local.changedata.events -create

echo ------------------------------------
echo Topic List:
call  %KAFKA_PATH%/bin/windows/kafka-topics.bat --bootstrap-server=localhost:9092 --list

pause