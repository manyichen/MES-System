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

如果云数据库中已经建过旧表，还需要执行一次共享字段迁移脚本：

```text
database_design_output/mes_shared_field_migration.sql
```

## ABC 联调约定

本项目采用“统一云数据库 + 约定表字段”的方式衔接三人模块：

- A 负责创建和维护订单、生产任务、生产工单等计划数据。
- B 负责仓储物流、领料、配送、报工和计件工资。
- C 负责质检、设备、追溯和看板。
- 模块之间不强制互相调用业务接口，统一通过云数据库业务表读取必要数据。
- 各模块默认只写自己负责的表；确需跨模块回写时，只写约定字段。

B 与 A 的约定字段：

- B 创建领料、提交报工前读取 `mes_work_order.work_order_status`、`planned_qty`、`actual_qty`、`batch_no`。
- B 只允许对 `DISPATCHED`、`RECEIVED`、`RUNNING` 状态的工单执行领料和报工。
- B 审核报工后回写 `mes_work_order.actual_qty`，并按产量推进 `work_order_status` 为 `RUNNING` 或 `FINISHED`。

B 与 C 的约定字段：

- C 通过 `mes_work_report.report_id` 或 `work_order_id` 读取 B 已审核报工。
- C 使用 `mes_work_report.batch_no` 作为质检和追溯批次来源。

本轮已补充的关键字段：

- `mes_work_order.product_id`
- `mes_work_order.batch_no`
- `mes_work_report.batch_no`
