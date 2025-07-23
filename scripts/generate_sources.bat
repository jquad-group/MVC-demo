@echo off

rem Set folder path
cd ..\
:: When this path changes it also needs to be changed in the pom.xml for all manually generated files
set "GENERATED_PATH=src\main\java\de\datev\refsys\generated"

rem Check if folders exist
if exist "%cd%\%GENERATED_PATH%" (
    echo Generated folder exists. Deleting...
    rmdir /s /q "%cd%\%GENERATED_PATH%"
    echo Generated folder deleted.
) else (
    echo Generated folder does not exist. Skipping deletion...
)

rem Run Maven clean
echo Running Maven clean...
call mvn clean
if errorlevel 1 (
    echo Maven clean failed. Exiting.
    pause
    exit /b 1
)
echo Maven clean complete

rem Run Maven unpack
echo Running maven unpack...
call mvn org.apache.maven.plugins:maven-dependency-plugin:unpack@acds-unpack
if errorlevel 1 (
    echo Maven unpack failed. Exiting.
    pause
    exit /b 1
)
echo Maven unpack complete

rem Run Maven generate ACDS client
echo Running maven generate ACDS client...
call mvn openapi-generator:generate@acds-generate
if errorlevel 1 (
    echo Maven generate ACDS client failed. Exiting.
    pause
    exit /b 1
)
echo Maven generate ACDS client complete

REM Run Maven compile
echo Running Maven compile...
call mvn compile
if ERRORLEVEL 1 (
    echo Maven compile failed. Exiting.
    pause
    exit /b 1
)
echo Maven compile complete
echo Generate sources was successful

pause