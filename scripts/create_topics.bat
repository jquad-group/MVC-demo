call settings.bat

echo ------------------------------------
echo "create new topic local.movementdata.events"
call %KAFKA_PATH%/bin/windows/kafka-topics.bat --bootstrap-server localhost:9092 -topic local.movementdata.events --partitions 3 -create

echo ------------------------------------
echo "create new topic local.movementdata.events-dlt"
call %KAFKA_PATH%/bin/windows/kafka-topics.bat --bootstrap-server localhost:9092 -topic local.movementdata.events-dlt -create

echo ------------------------------------
echo "create new topic local.changedata.events"
call %KAFKA_PATH%/bin/windows/kafka-topics.bat --bootstrap-server localhost:9092 -topic local.changedata.events -create

echo ------------------------------------
echo Topic List:
call %KAFKA_PATH%/bin/windows/kafka-topics.bat --bootstrap-server=localhost:9092 --list