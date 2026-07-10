#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
APP_DIR="$(cd "$SCRIPT_DIR/../backend" && pwd -P)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd -P)"

if [ -f "$ROOT_DIR/.env" ]; then
    set -a
    . "$ROOT_DIR/.env"
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
    -Dmes.port="${PORT}" \
    -Dmes.host="${HOST}"
