@echo off
chcp 65001 >nul
echo Running Finance Management Application...
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp out Main
