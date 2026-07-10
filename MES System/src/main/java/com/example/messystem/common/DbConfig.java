package com.example.messystem.common;

public final class DbConfig {

    public static final String HOST = value("MES_DB_HOST", "localhost");
    public static final String PORT = value("MES_DB_PORT", "5432");
    public static final String DATABASE = value("MES_DB_NAME", "MESSystem");
    public static final String USER = value("MES_DB_USER", "MESSystem");
    public static final String PASSWORD = value("MES_DB_PASSWORD", "");

    private DbConfig() {
    }

    public static String getJdbcUrl() {
        String explicitUrl = System.getenv("MES_DB_URL");
        if (explicitUrl != null && !explicitUrl.isBlank()) {
            return explicitUrl;
        }
        return String.format("jdbc:postgresql://%s:%s/%s", HOST, PORT, DATABASE);
    }

    public static String getUser() {
        return USER;
    }

    public static String getPassword() {
        return PASSWORD;
    }

    private static String value(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
