package com.example.messystem.common.service;

import com.example.messystem.common.DbConfig;
import com.example.messystem.common.dao.HealthDao;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** 构建公开健康视图，避免控制器接触数据库凭据或 SQL。 */
public class HealthService {
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
