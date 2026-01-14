@echo off
echo Compiling Java project...

REM Компилируем проект с сохранением структуры пакетов
javac -d out -sourcepath src/main src/main/*.java src/main/model/*.java src/main/service/*.java src/main/ui/*.java

if %errorlevel% == 0 (
    echo Compilation successful!
    echo Output structure: out\Main.class, out\model\, out\service\, out\ui\
) else (
    echo Compilation failed!
    exit /b %errorlevel%
)
