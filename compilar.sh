#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
mkdir -p build/classes
printf '"%s"\n' src/labirinto/*.java > build/fontes.txt
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/classes @build/fontes.txt
java -m jdk.jartool/sun.tools.jar.Main --create --file labirinto.jar --main-class labirinto.Main -C build/classes . -C . web
echo "Compilado: labirinto.jar"
