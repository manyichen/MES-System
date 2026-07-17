# MES Postman 全量接口测试

本目录包含可导入 Postman 客户端的集合、环境，以及用于生成答辩报告的命令行运行脚本。

## 测试范围

- 自动扫描后端全部 JAX-RS `Resource` 接口，不维护容易过期的手工接口清单。
- 全部受保护接口验证未登录时必须返回 `401`，覆盖 GET、POST、PUT、DELETE。
- 使用 `superadmin` 登录后对全部 GET 接口验证认证、权限策略、无 `5xx`、响应时间和 JSON 格式。
- 使用 `admin` 验证系统运维角色不能读取生产工单，展示最小权限边界。
- 验证登录、当前用户、错误密码、公开追溯和退出登录。
- 现有 `.env` 使用远程数据库，因此不会执行会写入业务数据的成功 CRUD 流程。此类测试必须切换到独立测试数据库后再扩展，禁止在云端正式/演示库批量造数。

## Postman 客户端运行

1. 双击 `start-postman-backend.cmd`，等待窗口显示 `MES backend started: http://127.0.0.1:18084/`。测试结束前不要关闭这个窗口。
2. 打开 Postman，点击左上角 `Import`。
3. 导入 `MES-Full-API.postman_collection.json` 和 `MES-Local.postman_environment.json`。
4. 右上角环境选择“MES 本地答辩环境”。
5. 选中集合，点击 `Run`，保持默认顺序并点击 `Run 双星轮胎 MES - 全量接口测试`。
6. 运行结束后查看 `Passed`、`Failed`、`Test Results` 和平均响应时间；答辩时截取总览页。
7. 测试结束后回到后端窗口，按 `Ctrl+C` 停止服务。

如果 Runner 在第一个请求就显示 `No response`、`Errors 1`、`All tests 0`，说明 `18084` 后端没有启动，或启动窗口已经被关闭。先执行第 1 步，再重新运行集合。

## 重新生成和命令行报告

```powershell
cd postman
npm.cmd run generate
npm.cmd test
```

答辩用脱敏 HTML 输出到 `reports/defense-test-report.html`，汇总 JSON 输出到 `reports/defense-test-summary.json`，JUnit XML 输出到 `reports/postman-junit.xml`。报告不会保存密码、Bearer Token、请求体或响应体。
