@echo off
setlocal
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
(for %%F in (src\labirinto\*.java) do @echo "%%F") > build\fontes.txt
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -Xlint:all -Werror -d build\classes @build\fontes.txt
if errorlevel 1 exit /b 1
java -m jdk.jartool/sun.tools.jar.Main --create --file labirinto.jar --main-class labirinto.Main -C build\classes . -C . web
if errorlevel 1 exit /b 1
echo Compilado: labirinto.jar
