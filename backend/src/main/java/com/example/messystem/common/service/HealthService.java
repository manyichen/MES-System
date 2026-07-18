/*
 * 答辩定位：公共基础设施 模块的 HealthService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.common.service;

import com.example.messystem.common.DbConfig;
import com.example.messystem.common.dao.HealthDao;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** 构建公开健康视图，避免控制器接触数据库凭据或 SQL。 */
public class HealthService {
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final HealthDao dao = new HealthDao();

    /** 组合可公开的连接配置、服务器状态和数据库结构元数据。 */
    public Map<String, Object> databaseHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("host", DbConfig.HOST);
        health.put("port", DbConfig.PORT);
        health.put("database", DbConfig.DATABASE);
        health.put("user", DbConfig.USER);
        try {
            health.putAll(dao.inspect());
            health.put("connected", true);
            return health;
        } catch (SQLException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }
}
