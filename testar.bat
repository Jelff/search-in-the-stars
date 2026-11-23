@echo off
setlocal
cd /d "%~dp0"
call compilar.bat
if errorlevel 1 exit /b 1
if not exist build\testes mkdir build\testes
(for %%F in (test\labirinto\*.java) do @echo "%%F") > build\testes.txt
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -Xlint:all -Werror -cp build\classes -d build\testes @build\testes.txt
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp "build\classes;build\testes" labirinto.Testes
if errorlevel 1 exit /b 1
