package com.example.messystem.trace.service;

import com.example.messystem.common.DbConfig;
import java.nio.file.Path;
import java.net.URI;

public final class TraceRuntimeConfig {
    private TraceRuntimeConfig() {
    }

    public static String publicBaseUrl() {
        String configured = DbConfig.getValue("MES_PUBLIC_URL", "");
        String value = configured == null || configured.isBlank()
                ? System.getProperty("MES_RUNTIME_PUBLIC_URL", "http://127.0.0.1:8080")
                : configured;
        return value.replaceFirst("/+$", "");
    }

    public static String publicBaseUrl(String requestOrigin) {
        String configured = DbConfig.getValue("MES_PUBLIC_URL", "");
        if (configured != null && !configured.isBlank()) return configured.replaceFirst("/+$", "");
        if (requestOrigin != null && !requestOrigin.isBlank()) {
            try {
                URI uri = URI.create(requestOrigin);
                if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                        && uri.getHost() != null && uri.getUserInfo() == null) {
                    int port = uri.getPort();
                    return uri.getScheme() + "://" + uri.getHost() + (port < 0 ? "" : ":" + port);
                }
            } catch (IllegalArgumentException ignored) { }
        }
        return publicBaseUrl();
    }

    public static Path storageRoot() {
        String configured = DbConfig.getValue("MES_TRACE_STORAGE", "storage/trace");
        Path path = Path.of(configured);
        if (path.isAbsolute()) return path.normalize();
        Path current = Path.of("").toAbsolutePath().normalize();
        Path projectRoot = current.getFileName() != null && "backend".equalsIgnoreCase(current.getFileName().toString())
                ? current.getParent() : current;
        return projectRoot.resolve(path).normalize();
    }
}
