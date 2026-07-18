#!/usr/bin/env bash
# 生产发布安装器：以“暂存、验证、切换、探针、失败回滚”方式部署 release 压缩包。
# 需要 root，因为会写 /www、/etc/nginx、/etc/supervisor 并重载系统服务。
set -Eeuo pipefail

# 发布包参数和所有目录都集中定义；时间戳保证暂存、备份、失败现场互不覆盖。
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

# 统一输出到 stderr，并通过非零返回值触发 ERR trap。
fail() {
    echo "ERROR: $*" >&2
    return 1
}

# ERR trap 回滚函数：只有完成线上目录切换后才恢复应用和服务配置，否则保留暂存目录供排查。
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

# 任意未处理错误进入回滚流程，避免前端、后端或服务配置只更新一半。
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

# 提前检查部署依赖，避免切换线上目录后才发现命令缺失。
for command_name in tar java mvn curl nginx supervisorctl systemctl; do
    if ! command -v "$command_name" >/dev/null; then
        fail "required command not found: $command_name"
    fi
done

# 发布包先解压到不对外服务的暂存目录，并验证关键文件齐全。
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

# .env 和 storage 是运行状态，不打进发布包；优先沿用当前目录，兼容旧部署目录迁移。
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

# 在切换前完成后端编译，语法或依赖失败不会影响当前线上版本。
echo "Compiling the staged backend before switching..."
(
    cd "$STAGE_ROOT/backend"
    mvn -DskipTests compile
)

# 备份当前应用和服务配置，再把暂存目录移动为正式目录；同文件系统 mv 接近原子切换。
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

# 安装 Nginx/Supervisor 配置，先 nginx -t 校验，再启用与重载服务。
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

# 最多等待 45 秒，先探测 Java 根页面，再通过 Nginx Host 头验证公网入口链路。
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
# 使用不存在的账号调用真实登录接口；预期 400 证明 Nginx之外的 Java/Jersey/数据库链路均可用。
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
