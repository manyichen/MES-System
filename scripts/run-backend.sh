#!/usr/bin/env bash
# 本地 Linux 与生产 Supervisor 共用的后端入口。
# 流程：定位项目 -> 加载根目录 .env -> 选择 Maven/监听地址 -> 编译并 exec Java 主类。
set -euo pipefail

# 通过脚本真实位置推导目录，保证从任意工作目录调用都能找到 backend 和 .env。
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
APP_DIR="$(cd "$SCRIPT_DIR/../backend" && pwd -P)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd -P)"

# set -a 使 .env 中的键自动 export，DbConfig 随后可通过 System.getenv 读取。
if [ -f "$ROOT_DIR/.env" ]; then
    set -a
    . "$ROOT_DIR/.env"
    set +a
fi

cd "$APP_DIR"

# 支持用 MAVEN_CMD 指向自带 Maven；未设置时使用服务器 PATH 中的 mvn。
MAVEN_CMD="${MAVEN_CMD:-mvn}"
HOST="${MES_HOST:-127.0.0.1}"
PORT="${MES_PORT:-8080}"

echo
echo "Starting MES backend on ${HOST}:${PORT}"
# exec 用 Java/Maven 进程替换 shell，使 Supervisor 能准确收发停止信号和判断退出码。
exec "$MAVEN_CMD" -DskipTests compile exec:java \
    -Dexec.mainClass=com.example.messystem.MesBackendApplication \
    -Dmes.port="${PORT}" \
    -Dmes.host="${HOST}"
