package com.mes.jdbc;

public final class DbConfig {
    public static final String HOST = value("MES_DB_HOST", "localhost");
    public static final String PORT = value("MES_DB_PORT", "3306");
    public static final String DATABASE = value("MES_DB_NAME", "MES");
    public static final String USER = value("MES_DB_USER", "root");
    public static final String PASSWORD = value("MES_DB_PASSWORD", "Manyichen060325");

    private static final String OPTIONS =
            "useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";

    private DbConfig() {
    }

    public static String serverUrl() {
        return "jdbc:mysql://" + HOST + ":" + PORT + "/?" + OPTIONS;
    }

    public static String databaseUrl() {
        return "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE + "?" + OPTIONS;
    }

    private static String value(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
