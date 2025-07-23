@echo off
cls

call settings.bat

echo ------------------------------------
echo Starte Mongodb...

IF NOT EXIST %dataDir% (mkdir %dataDir%)
%MONGO_PATH%\bin\mongod --port=27017 --dbpath=%dataDir% --replSet rs0
pause