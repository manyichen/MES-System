package com.example.messystem.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DbConfig {
    private static final Map<String, String> DOT_ENV = loadDotEnv();

    public static final String HOST = value("MES_DB_HOST", "localhost");
    public static final String PORT = value("MES_DB_PORT", "5432");
    public static final String DATABASE = value("MES_DB_NAME", "MESSystem");
    public static final String USER = value("MES_DB_USER", "MESSystem");
    public static final String PASSWORD = value("MES_DB_PASSWORD", "");

    private static final String OPTIONS = "sslmode=prefer";

    private DbConfig() {
    }

    public static String serverUrl() {
        return "jdbc:postgresql://" + HOST + ":" + PORT + "/postgres?" + OPTIONS;
    }

    public static String databaseUrl() {
        String explicitUrl = value("MES_DB_URL", "");
        if (!explicitUrl.isBlank()) {
            return explicitUrl;
        }
        return "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DATABASE + "?" + OPTIONS;
    }

    public static String getUser() {
        return USER;
    }

    public static String getPassword() {
        return PASSWORD;
    }

    private static String value(String name, String fallback) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = DOT_ENV.get(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> values = new HashMap<>();
        for (Path path : List.of(Path.of(".env"), Path.of("..", ".env"))) {
            if (Files.isRegularFile(path)) {
                readDotEnv(path, values);
                break;
            }
        }
        return values;
    }

    private static void readDotEnv(Path path, Map<String, String> values) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int equalsIndex = trimmed.indexOf('=');
                if (equalsIndex <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, equalsIndex).trim();
                String rawValue = trimmed.substring(equalsIndex + 1).trim();
                values.put(key, unquote(rawValue));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read .env file: " + path, e);
        }
    }

    private static String unquote(String value) {
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
