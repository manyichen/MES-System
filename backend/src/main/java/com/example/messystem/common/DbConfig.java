package com.example.messystem.common;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DbConfig {
    private static final Map<String, String> DOT_ENV = loadDotEnv();

    

    public static final String HOST = value("MES_DB_HOST", "localhost");
    public static final String PORT = value("MES_DB_PORT", "5432");
    public static final String DATABASE = value("MES_DB_NAME", "MESSystem");
    public static final String USER = value("MES_DB_USER", "postgres");
    public static final String PASSWORD = value("MES_DB_PASSWORD", "");

    private DbConfig() {
    }

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

    public static String getUser() {
        return USER;
    }

    public static String getPassword() {
        return PASSWORD;
    }

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
