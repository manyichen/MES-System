# 双星轮胎制造 MES 系统

本仓库是一个 Web 应用，主工程目录为 `MES System`。项目保留两类入口：

- 本地开发/答辩演示入口：运行 `com.example.messystem.MesBackendApplication`。
- 服务器部署入口：Supervisor 调用 `MES System/run-backend.sh`，脚本内部同样启动 `MesBackendApplication`。

除此之外不再保留 `run-demo`、手写 demo server 等临时入口，避免本地、部署、答辩演示时跑到不同代码路径。

## 环境要求

- JDK 21
- Maven
- PostgreSQL 数据库

数据库连接从项目根目录 `.env` 或系统环境变量读取：

```env
MES_DB_HOST=你的 PostgreSQL 地址
MES_DB_PORT=5432
MES_DB_NAME=MESSystem
MES_DB_USER=MESSystem
MES_DB_PASSWORD=你的密码
```

可以复制模板后填写真实配置：

```powershell
Copy-Item .env.example .env
```

`.env` 已加入忽略规则，不要提交真实密码。

## 本地运行

在 IDE 中打开 `MES System`，直接运行：

```text
src/main/java/com/example/messystem/MesBackendApplication.java
```

默认访问地址：

```text
http://127.0.0.1:8080/
```

也可以在终端运行：

```powershell
cd "MES System"
..\.tools\apache-maven-3.9.11\bin\mvn.cmd -DskipTests compile exec:java
```

如果使用 PowerShell 手动指定端口，`-D` 参数需要加引号：

```powershell
..\.tools\apache-maven-3.9.11\bin\mvn.cmd -DskipTests compile exec:java "-Dexec.args=18080 127.0.0.1"
```

前端入口是静态页面：

```text
MES System/src/main/webapp/index.html
```

本地启动后端后，浏览器访问 `http://127.0.0.1:8080/` 即可打开前端页面，前端会通过同源 `/api/*` 调用后端。

## 构建与测试

```powershell
cd "MES System"
.\mvnw.cmd test
```

## 服务器部署

服务器上的后端进程由 Supervisor 启动：

```text
MES System/run-backend.sh
```

Nginx 负责公开 Web 入口和反向代理：

- `/` 指向 `MES System/src/main/webapp`
- `/api/*` 代理到 `127.0.0.1:8080`

部署模板在：

```text
deploy/nginx/mes.conf
deploy/supervisor/mes-backend.conf
deploy/env.production.example
```

详细步骤见：

```text
腾讯云轻量应用服务器部署步骤.md
```

## 数据库脚本

PostgreSQL 建表脚本：

```text
database_design_output/mes_schema_postgresql.sql
```
