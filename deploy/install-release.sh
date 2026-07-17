#!/usr/bin/env bash
set -Eeuo pipefail

ARCHIVE="${1:-}"
APP_ROOT="${MES_DEPLOY_ROOT:-/www/wwwroot/mes}"
LEGACY_ROOT="${MES_LEGACY_ROOT:-/www/wwwroot/mes-app}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
STAGE_ROOT="${APP_ROOT}.stage-${TIMESTAMP}"
BACKUP_ROOT="${APP_ROOT}.backup-${TIMESTAMP}"
FAILED_ROOT="${APP_ROOT}.failed-${TIMESTAMP}"
NGINX_TARGET="/etc/nginx/sites-available/mes.conf"
SUPERVISOR_TARGET="/etc/supervisor/conf.d/mes-backend.conf"
NGINX_BACKUP="${NGINX_TARGET}.backup-${TIMESTAMP}"
SUPERVISOR_BACKUP="${SUPERVISOR_TARGET}.backup-${TIMESTAMP}"
SWITCHED=false
HAD_APP=false
HAD_NGINX=false
HAD_SUPERVISOR=false

fail() {
    echo "ERROR: $*" >&2
    return 1
}

restore_previous_release() {
    local status=$?
    trap - ERR
    if [ "$SWITCHED" = true ]; then
        echo "Deployment failed; restoring the previous release..." >&2
        supervisorctl stop mes-backend >/dev/null 2>&1 || true
        if [ -d "$APP_ROOT" ]; then
            mv "$APP_ROOT" "$FAILED_ROOT"
        fi
        if [ "$HAD_APP" = true ] && [ -d "$BACKUP_ROOT" ]; then
            mv "$BACKUP_ROOT" "$APP_ROOT"
        fi
        if [ "$HAD_NGINX" = true ]; then
            cp -a "$NGINX_BACKUP" "$NGINX_TARGET"
        else
            rm -f "$NGINX_TARGET"
            rm -f /etc/nginx/sites-enabled/mes.conf
        fi
        if [ "$HAD_SUPERVISOR" = true ]; then
            cp -a "$SUPERVISOR_BACKUP" "$SUPERVISOR_TARGET"
        else
            rm -f "$SUPERVISOR_TARGET"
        fi
        supervisorctl reread >/dev/null 2>&1 || true
        supervisorctl update >/dev/null 2>&1 || true
        supervisorctl restart mes-backend >/dev/null 2>&1 || true
        if nginx -t; then
            systemctl reload nginx || true
        fi
        echo "Previous release restored. Failed files: $FAILED_ROOT" >&2
    else
        echo "Deployment stopped before the live release was changed." >&2
        echo "Staged files: $STAGE_ROOT" >&2
    fi
    exit "$status"
}

trap restore_previous_release ERR

if [ "${EUID}" -ne 0 ]; then
    fail "run this script with sudo or as root"
fi
if [ -z "$ARCHIVE" ]; then
    fail "usage: sudo bash install-release.sh /tmp/mes-web-v1.tar.gz"
fi
if [ ! -f "$ARCHIVE" ]; then
    fail "release archive not found: $ARCHIVE"
fi

for command_name in tar java mvn curl nginx supervisorctl systemctl; do
    if ! command -v "$command_name" >/dev/null; then
        fail "required command not found: $command_name"
    fi
done

mkdir -p "$(dirname "$APP_ROOT")" /www/wwwlogs "$STAGE_ROOT"
tar -xzf "$ARCHIVE" -C "$STAGE_ROOT"

for required_path in \
    pom.xml \
    backend/pom.xml \
    backend/src/main/java/com/example/messystem/MesBackendApplication.java \
    frontend/dist/index.html \
    scripts/run-backend.sh \
    deploy/nginx/mes.conf \
    deploy/supervisor/mes-backend.conf; do
    if [ ! -e "$STAGE_ROOT/$required_path" ]; then
        fail "invalid release archive; missing $required_path"
    fi
done

RUNTIME_SOURCE=""
if [ -f "$APP_ROOT/.env" ]; then
    RUNTIME_SOURCE="$APP_ROOT"
elif [ -f "$LEGACY_ROOT/.env" ]; then
    RUNTIME_SOURCE="$LEGACY_ROOT"
else
    fail "no production .env found in $APP_ROOT or $LEGACY_ROOT"
fi

install -m 600 "$RUNTIME_SOURCE/.env" "$STAGE_ROOT/.env"
if [ -d "$RUNTIME_SOURCE/storage" ]; then
    cp -a "$RUNTIME_SOURCE/storage" "$STAGE_ROOT/storage"
fi
chmod +x "$STAGE_ROOT/scripts/run-backend.sh"
chown -R root:root "$STAGE_ROOT"

echo "Compiling the staged backend before switching..."
(
    cd "$STAGE_ROOT/backend"
    mvn -DskipTests compile
)

if [ -f "$NGINX_TARGET" ]; then
    cp -a "$NGINX_TARGET" "$NGINX_BACKUP"
    HAD_NGINX=true
fi
if [ -f "$SUPERVISOR_TARGET" ]; then
    cp -a "$SUPERVISOR_TARGET" "$SUPERVISOR_BACKUP"
    HAD_SUPERVISOR=true
fi
if [ -d "$APP_ROOT" ]; then
    mv "$APP_ROOT" "$BACKUP_ROOT"
    HAD_APP=true
fi
mv "$STAGE_ROOT" "$APP_ROOT"
SWITCHED=true

install -m 644 "$APP_ROOT/deploy/nginx/mes.conf" "$NGINX_TARGET"
install -m 644 "$APP_ROOT/deploy/supervisor/mes-backend.conf" "$SUPERVISOR_TARGET"
ln -sfn "$NGINX_TARGET" /etc/nginx/sites-enabled/mes.conf

nginx -t
systemctl enable --now supervisor
systemctl enable --now nginx
supervisorctl reread
supervisorctl update
supervisorctl restart mes-backend
systemctl reload nginx

echo "Waiting for the backend..."
for attempt in $(seq 1 45); do
    if curl --fail --silent --show-error --max-time 3 http://127.0.0.1:8080/ >/dev/null; then
        break
    fi
    if [ "$attempt" -eq 45 ]; then
        false
    fi
    sleep 1
done

curl --fail --silent --show-error --max-time 5 \
    --header 'Host: 119.45.196.92' http://127.0.0.1/ >/dev/null
supervisorctl status mes-backend | grep -q RUNNING
LOGIN_PROBE_STATUS="$(curl --silent --show-error --max-time 10 \
    --output /dev/null --write-out '%{http_code}' \
    --header 'Content-Type: application/json' \
    --data '{"username":"__deployment_health_probe__","password":"not-a-real-password"}' \
    http://127.0.0.1:8080/api/auth/login)"
if [ "$LOGIN_PROBE_STATUS" != "400" ]; then
    fail "database-backed login probe returned HTTP $LOGIN_PROBE_STATUS instead of 400"
fi

trap - ERR
echo "Deployment completed successfully."
echo "Live release: $APP_ROOT"
if [ "$HAD_APP" = true ]; then
    echo "Rollback copy: $BACKUP_ROOT"
fi
echo "Public URL: http://119.45.196.92/"
