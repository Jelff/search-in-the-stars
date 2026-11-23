#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
if [[ ! -f labirinto.jar ]]; then bash compilar.sh; fi
java -Djava.net.preferIPv4Stack=true -jar labirinto.jar "$@"
