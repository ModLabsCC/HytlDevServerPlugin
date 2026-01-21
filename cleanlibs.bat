@echo off
REM Define the target directory
set "TARGET_DIR=.\build\libs"

REM Check if the directory exists
if not exist "%TARGET_DIR%" (
    echo The directory %TARGET_DIR% does not exist.
    exit /b 1
)

REM Delete all .jar files in the directory
del /f /q "%TARGET_DIR%\*.jar"

REM Confirm the operation
echo All .jar files in %TARGET_DIR% have been deleted.