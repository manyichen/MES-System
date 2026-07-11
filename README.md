# 双星轮胎制造 MES 系统

这是一个 Java Web 管理软件项目。当前目录已按前后端、数据库、部署和文档分层整理，避免继续使用带空格的嵌套目录 `MES System`。

## 目录结构

```text
backend/      Java 后端、Maven 模块、测试代码
frontend/     静态前端页面、CSS、JavaScript
database/     建表脚本、种子数据、数据库设计输出
deploy/       Nginx、Supervisor、生产环境变量模板
docs/         实训报告、ER 图、设计文档、模块 API 说明
scripts/      本地/服务器启动脚本、Maven Wrapper
.github/      GitHub 仓库配置
```

## 本地运行

环境要求：

- JDK 21
- Maven
- PostgreSQL

复制并填写数据库配置：

```powershell
Copy-Item .env.example .env
```

在 IDE 中运行后端入口：

```text
backend/src/main/java/com/example/messystem/MesBackendApplication.java
```

后端会同时托管 `frontend/` 静态页面，默认访问：

```text
http://127.0.0.1:8080/
```

也可以用命令行启动：

```powershell
cd backend
..\.tools\apache-maven-3.9.11\bin\mvn.cmd -DskipTests compile exec:java
```

PowerShell 手动指定端口时，`-D` 参数需要加引号：

```powershell
cd backend
..\.tools\apache-maven-3.9.11\bin\mvn.cmd -DskipTests compile exec:java "-Dmes.port=18080" "-Dmes.host=127.0.0.1"
```

## 构建验证

```powershell
.\.tools\apache-maven-3.9.11\bin\mvn.cmd -pl backend -DskipTests compile
```

如需运行数据库集成测试，先确认 `.env` 指向可写的测试数据库，再执行：

```powershell
.\.tools\apache-maven-3.9.11\bin\mvn.cmd -pl backend test
```

## 部署入口

服务器后端入口：

```text
scripts/run-backend.sh
```

Nginx 静态目录：

```text
frontend/
```

部署配置模板：

```text
deploy/nginx/mes.conf
deploy/supervisor/mes-backend.conf
deploy/env.production.example
```

详细部署步骤：

```text
docs/腾讯云轻量应用服务器部署步骤.md
```

## 数据库脚本

主建表脚本：

```text
database/design_output/mes_schema_postgresql.sql
```

种子数据：

```text
database/design_output/mes_seed_data.sql
```

## 数据库优化设计 v2

权限管理、组织人员、审计日志、登录会话、工艺质量标准、设备状态履历等增量优化设计见：

```text
docs/数据库优化详细设计-v2.md
database/mes_database_optimization_v2.sql
```

该 v2 脚本是增量增强脚本，不删除现有表和字段。建议审核设计文档后，再通过 `DatabaseMigrationRunner` 执行到云数据库。

## 权限管理 v3

正式角色边界、看板待办和数据范围：

```text
docs/权限管理正式实施说明-v3.md
```

权限实施迁移：

```text
database/mes_permission_implementation_v3.sql
database/mes_data_scope_v4.sql
```

`v4` 数据范围迁移为车间管理员建立产线分配、为仓库管理员建立仓库分配，并补齐领料申请和机器人仓库归属。范围由系统管理员在“用户与角色”页面分配；未分配范围时返回空列表并拒绝详情和操作，不回退为模块全量。

角色权限验收账号初始化脚本：

```text
database/mes_role_acceptance_accounts.sql
```

该脚本只保存密码哈希，不在仓库中保存验收账号的明文密码。
