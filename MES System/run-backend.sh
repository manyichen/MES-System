#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

if [ -f "$APP_DIR/../.env" ]; then
    set -a
    . "$APP_DIR/../.env"
    set +a
fi

cd "$APP_DIR"

MAVEN_CMD="${MAVEN_CMD:-mvn}"
HOST="${MES_HOST:-127.0.0.1}"
PORT="${MES_PORT:-8080}"

echo
echo "Starting MES backend on ${HOST}:${PORT}"
exec "$MAVEN_CMD" -DskipTests compile exec:java \
    -Dexec.mainClass=com.example.messystem.MesBackendApplication \
    -Dexec.args="${PORT} ${HOST}"
