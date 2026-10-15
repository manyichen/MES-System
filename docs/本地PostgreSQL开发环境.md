# 本地 PostgreSQL 开发环境

现有根目录 `.env` 继续保留云数据库配置，不需要修改。本地开发使用被 Git 忽略的 `.env.local`，由启动脚本以进程环境变量覆盖云配置；以后切回云数据库时仍可使用原启动方式。

## 1. 安装要求

- PostgreSQL 18 服务端（项目备份由 PostgreSQL 18 生成）
- JDK 21、Maven、Node.js 20+
- Navicat 可用于查看和编辑数据库，但它不是 PostgreSQL 服务端

安装 PostgreSQL 时请记住 `postgres` 超级用户密码，端口保持默认 `5432` 即可。

## 2. 一键创建和恢复

在项目根目录运行：

```powershell
.\scripts\setup-local-db.cmd
```

脚本会：

1. 创建/更新本地应用用户 `mes_app`；
2. 创建数据库 `MES`；
3. 从 `database/backups/mes_before_kitting_integrity_v12_20260716_222140.backup` 恢复云库的本地备份；
4. 应用备份之后的两条 v12 迁移；
5. 验证连接和用户数据；
6. 生成不会提交到 Git 的 `.env.local`。

如需指定参数：

```powershell
.\scripts\setup-local-db.cmd -PostgresPort 5432 -Database MES -AppUser mes_app
```

该脚本会清理并重建目标数据库内与备份同名的对象，所以只应指向本地开发数据库。

## 3. 启动和登录

```powershell
.\scripts\start-local.cmd
```

另开一个终端启动前端：

```powershell
Set-Location frontend
npm.cmd install
npm.cmd run dev
```

打开 `http://127.0.0.1:5173`，可用 `admin / 123456` 登录。项目中的其他验收账号也统一使用密码 `123456`。

## 4. Navicat 连接参数

新建 PostgreSQL 连接：

- 主机：`127.0.0.1`
- 端口：`5432`
- 初始数据库：`MES`
- 用户名：`mes_app`
- 密码：运行建库脚本时设置的密码（也保存在本机 `.env.local`）

## 5. 将来切回云数据库

本地开发没有改动原 `.env`。停止 `start-local.cmd` 后，按原项目启动方式运行后端，就会继续读取 `.env` 中的云数据库配置。正式迁回云端前应先使用 `pg_dump` 备份本地库，再在空白云库中用 `pg_restore` 恢复并验收。

## 6. 修改 PostgreSQL 管理员密码

如果管理员密码曾出现在终端截图、聊天或日志中，运行：

```powershell
.\scripts\change-local-postgres-password.cmd
```

先输入当前密码，再输入两次新密码。该操作不影响应用使用的独立账号 `mes_app`。
