#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

MAVEN_CMD="${MAVEN_CMD:-mvn}"
HOST="${MES_HOST:-0.0.0.0}"
PORT="${MES_PORT:-8080}"

"$MAVEN_CMD" test

echo
echo "Starting MES demo server on ${HOST}:${PORT}"
exec java -cp target/classes com.example.messystem.demo.DemoServer "$PORT" "$HOST"
