# MES JDBC Demo

这个小项目使用 JDBC 连接本机 MySQL，初始化 `MES` 数据库和 `user` 表，并通过 JUnit 验证用户表的新增、修改、删除操作。

## 数据库配置

默认配置在 `src/main/java/com/mes/jdbc/DbConfig.java`：

- host: `localhost`
- port: `3306`
- database: `MES`
- user: `root`
- password: 已按任务要求设置为默认值

也可以通过环境变量覆盖：

- `MES_DB_HOST`
- `MES_DB_PORT`
- `MES_DB_NAME`
- `MES_DB_USER`
- `MES_DB_PASSWORD`

## 运行测试

如果已安装 Maven：

```powershell
mvn test
```

如果使用本目录下载的临时 Maven：

```powershell
.\.tools\apache-maven-3.9.11\bin\mvn.cmd "-Dmaven.repo.local=.m2/repository" test
```

## 测试覆盖

`src/test/java/com/mes/jdbc/UserDaoTest.java` 中包含三个测试用例：

- `addUserShouldInsertOneRecord`: 验证新增用户
- `updatePasswordShouldModifyExistingRecord`: 验证修改密码
- `deleteUserShouldRemoveExistingRecord`: 验证删除用户
