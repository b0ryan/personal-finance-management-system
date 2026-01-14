@echo off
echo Compiling Java project...
javac -d out -sourcepath src/main src/main/*.java src/main/model/*.java src/main/service/*.java src/main/ui/*.java
if %errorlevel% == 0 (
    echo Compilation successful!
) else (
    echo Compilation failed!
    exit /b %errorlevel%
)
