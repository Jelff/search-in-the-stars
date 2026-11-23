#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
bash compilar.sh
mkdir -p build/testes
printf '"%s"\n' test/labirinto/*.java > build/testes.txt
java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -Xlint:all -Werror -cp build/classes -d build/testes @build/testes.txt
java -Djava.awt.headless=true -cp build/classes:build/testes labirinto.Testes
