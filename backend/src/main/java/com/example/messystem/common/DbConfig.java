/*
 * 答辩定位：公共基础设施 模块的 DbConfig。
 * 分层职责：运行基础设施：负责应用注册、服务器启动、配置读取或数据库连接，是业务模块共享的外部依赖边界。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.common;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 集中读取数据库和运行环境配置。
 * <p>优先级：JVM {@code -Dname=value} -> 操作系统环境变量 -> 根目录/上级目录的
 * {@code .env} -> 代码默认值。生产环境应使用环境变量或权限为 600 的 .env，真实密码不进 Git。</p>
 */
public final class DbConfig {
    /** 启动时只读取一次 .env 并缓存，后续取值不会反复访问磁盘。 */
    private static final Map<String, String> DOT_ENV = loadDotEnv();

    /** PostgreSQL 主机名，生产通常是云数据库/RDS 内外网地址。 */
    public static final String HOST = value("MES_DB_HOST", "localhost");
    /** PostgreSQL TCP 端口，默认 5432。 */
    public static final String PORT = value("MES_DB_PORT", "5432");
    /** MES 逻辑数据库名称。 */
    public static final String DATABASE = value("MES_DB_NAME", "MESSystem");
    /** 数据库登录用户；应按最小权限原则授予业务库权限。 */
    public static final String USER = value("MES_DB_USER", "postgres");
    /** 数据库连接密码；只存在运行配置和进程内存中，不写日志、不返回前端。 */
    public static final String PASSWORD = value("MES_DB_PASSWORD", "");

    /**
     * 内部实现步骤：执行 DbConfig 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private DbConfig() {
    }

    /**
     * 生成 JDBC URL。若配置 {@code MES_DB_URL} 则整串覆盖，便于追加 SSL 等驱动参数；
     * 否则由 HOST、PORT、DATABASE 组装标准 PostgreSQL URL。
     */
    public static String getJdbcUrl() {
        String explicitUrl = value("MES_DB_URL", "");
        if (explicitUrl != null && !explicitUrl.isBlank()) {
            return explicitUrl;
        }
        return String.format(
                "jdbc:postgresql://%s:%s/%s",
                HOST,
                PORT,
                DATABASE
        );
    }

    /** 根据 JDBC URL 选择驱动；当前正式技术栈使用 org.postgresql.Driver。 */
    public static String getDriverClassName() {
        String explicitDriver = value("MES_DB_DRIVER", "");
        if (explicitDriver != null && !explicitDriver.isBlank()) {
            return explicitDriver;
        }
        String jdbcUrl = getJdbcUrl();
        if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        }
        return "";
    }

    /**
     * 公共能力：查询单条记录或详情。
     * 由 DbConfig 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static String getUser() {
        return USER;
    }

    /**
     * 公共能力：查询单条记录或详情。
     * 由 DbConfig 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static String getPassword() {
        return PASSWORD;
    }

    /**
     * 公共能力：查询单条记录或详情。
     * 由 DbConfig 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static String getValue(String name, String fallback) {
        return value(name, fallback);
    }

    /** 按系统属性、环境变量、.env、默认值的顺序解析一个配置项。 */
    private static String value(String name, String fallback) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            value = System.getenv(name);
        }
        if (value == null || value.isBlank()) {
            value = DOT_ENV.get(name);
        }
        return value == null || value.isBlank() ? fallback : value;
    }

    /** 同时兼容从仓库根目录和 backend 子目录启动，查找最近的根目录 .env。 */
    private static Map<String, String> loadDotEnv() {
        Path current = Path.of("").toAbsolutePath().normalize();
        Path[] candidates = {
                current.resolve(".env"),
                current.resolve("../.env").normalize()
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return readDotEnv(candidate);
            }
        }
        return Map.of();
    }

    /**
     * 以 UTF-8 解析简单 KEY=VALUE；忽略空行/注释，只在第一个等号处分割，密码可含等号。
     * 读取失败立即终止启动，避免悄悄使用错误的默认数据库。
     */
    private static Map<String, String> readDotEnv(Path file) {
        Map<String, String> values = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                String[] parts = trimmed.split("=", 2);
                values.put(parts[0].trim(), stripQuotes(parts[1].trim()));
            }
            return values;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load " + file, ex);
        }
    }

    /**
     * 内部实现步骤：执行 stripQuotes 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
