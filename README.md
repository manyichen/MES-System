# 双星轮胎 MES 系统

面向轮胎制造现场的前后端分离式 MES。后端使用 Java 21、JAX-RS、JDBC 和 PostgreSQL；前端使用 Vue 3、Vite、Pinia、Vue Router 与 Lucide。

## 目录

```text
backend/              Java 后端和测试
frontend/src/         Vue 业务组件、路由、状态和 API 客户端
frontend/dist/        前端构建产物（不提交 Git）
database/             建表、迁移和验收数据
docs/                 架构、接口、权限与设计文档
deploy/               Nginx、Supervisor 配置
```

后端业务模块统一采用：

```text
controller/ -> service/ -> dao/ -> PostgreSQL
                         -> entity/
```

Controller 只处理 HTTP 参数和响应；Service 负责校验、角色边界和状态流转；DAO 保存 SQL、行映射和数据库事务。

## 本地开发

环境：JDK 21、Node.js 20+、PostgreSQL。

```powershell
Copy-Item .env.example .env
cd frontend
npm.cmd install
npm.cmd run dev
```

前端开发地址：`http://127.0.0.1:5173`，Vite 会把 `/api` 代理到 `http://127.0.0.1:8080`。

另一个终端启动后端：

```powershell
cd backend
..\.tools\apache-maven-3.9.11\bin\mvn.cmd -DskipTests compile exec:java
```

## 生产构建

```powershell
cd frontend
npm.cmd run build
cd ..
.\.tools\apache-maven-3.9.11\bin\mvn.cmd -pl backend test package
```

后端独立运行时托管 `frontend/dist`，并为 Vue Router History 路由回退到 `index.html`。

## 验证

```powershell
# 后端编译、单元测试和分层约束测试
.\.tools\apache-maven-3.9.11\bin\mvn.cmd -pl backend test

# 前端生产构建
cd frontend
npm.cmd run build
```

角色、接口清理和分层说明见 `docs/后端重构与接口审计.md`。
