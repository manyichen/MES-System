# 双星轮胎制造 MES 系统

本仓库后续开发统一使用 `MES System` 作为主工程。

## 开发约定

- JDK：21
- 构建：Maven
- Web 工程：`MES System`
- 数据库：阿里云 RDS PostgreSQL
- 数据库配置：项目根目录 `.env`

## 数据库配置

复制模板：

```powershell
Copy-Item .env.example .env
```

填写真实连接信息：

```env
MES_DB_HOST=你的RDS PostgreSQL外网地址
MES_DB_PORT=5432
MES_DB_NAME=MESSystem
MES_DB_USER=MESSystem
MES_DB_PASSWORD=你的密码
```

`.env` 已被 `.gitignore` 忽略，不要提交真实密码。

## 主工程

进入主工程目录：

```powershell
cd "MES System"
```

使用 Maven Wrapper 构建：

```powershell
.\mvnw.cmd test
```

## 数据库脚本

PostgreSQL 建表脚本：

```text
database_design_output/mes_schema_postgresql.sql
```

该脚本已根据 `database_design_output/mes_table_metadata.json` 生成。
