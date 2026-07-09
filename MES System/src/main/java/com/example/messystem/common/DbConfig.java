package com.example.messystem.common;

public final class DbConfig {

    public static String getHost() {
        return getEnvOrDefault("MES_DB_HOST", "localhost");
    }

    public static String getPort() {
        return getEnvOrDefault("MES_DB_PORT", "3306");
    }

    public static String getDatabase() {
        return getEnvOrDefault("MES_DB_NAME", "MES");
    }

    public static String getUser() {
        return getEnvOrDefault("MES_DB_USER", "root");
    }

    public static String getPassword() {
        return getEnvOrDefault("MES_DB_PASSWORD", "Manyichen060325");
    }

    public static String getJdbcUrl() {
        return String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                getHost(), getPort(), getDatabase());
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private DbConfig() {
    }
}
