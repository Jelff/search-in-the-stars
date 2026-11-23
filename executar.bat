@echo off
setlocal
cd /d "%~dp0"
if not exist labirinto.jar call compilar.bat
if errorlevel 1 exit /b 1
java -Djava.net.preferIPv4Stack=true -jar labirinto.jar %*
if errorlevel 1 pause
