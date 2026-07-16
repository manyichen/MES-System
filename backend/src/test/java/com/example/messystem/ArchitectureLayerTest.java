package com.example.messystem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** 防止后续修改绕过 controller、service、DAO 的分层边界。 */
class ArchitectureLayerTest {
    private static final Path SOURCE_ROOT = Path.of("src/main/java");
    private static final Pattern PATH_ANNOTATION = Pattern.compile("@Path\\(\\\"([^\\\"]*)\\\"\\)");
    private static final Pattern HTTP_ANNOTATION = Pattern.compile("@(GET|POST|PUT|DELETE|PATCH)\\b");

    @Test
    void httpEndpointsMustLiveInControllerPackages() throws IOException {
        for (Path source : javaSources()) {
            String code = Files.readString(source);
            if (code.contains("@Path(")) {
                assertTrue(normalize(source).contains("/controller/"),
                        () -> "JAX-RS endpoint is outside a controller package: " + source);
            }
        }
    }

    @Test
    void controllersMustNotAccessSqlOrDaosDirectly() throws IOException {
        for (Path source : javaSources()) {
            if (!normalize(source).contains("/controller/")) continue;
            String code = Files.readString(source);
            assertFalse(code.contains("com.example.messystem.common.Db"),
                    () -> "Controller accesses Db directly: " + source);
            assertFalse(code.contains(".dao."),
                    () -> "Controller depends on a DAO directly: " + source);
            assertFalse(code.contains("PreparedStatement") || code.contains("ResultSet")
                            || code.contains("Db.getConnection"),
                    () -> "Controller contains JDBC operations: " + source);
        }
    }

    /** 所有以 Service 命名的业务类必须进入模块的 service 包。 */
    @Test
    void serviceClassesMustLiveInServicePackages() throws IOException {
        for (Path source : javaSources()) {
            if (!source.getFileName().toString().endsWith("Service.java")) continue;
            assertTrue(normalize(source).contains("/service/"),
                    () -> "Service 类未放在 service 包中: " + source);
        }
    }

    /** 业务层只编排规则和事务用例，不允许直接创建 JDBC 连接或执行 SQL。 */
    @Test
    void servicesMustNotAccessJdbcDirectly() throws IOException {
        for (Path source : javaSources()) {
            if (!normalize(source).contains("/service/")) continue;
            String code = Files.readString(source);
            assertFalse(code.contains("import com.example.messystem.common.Db;")
                            || code.contains("java.sql.Connection")
                            || code.contains("PreparedStatement")
                            || code.contains("ResultSet")
                            || code.contains("Db.getConnection"),
                    () -> "Service 直接执行 JDBC 操作: " + source);
        }
    }

    @Test
    void daosMustNotDependOnHttpTypes() throws IOException {
        for (Path source : javaSources()) {
            if (!normalize(source).contains("/dao/")) continue;
            assertFalse(Files.readString(source).contains("jakarta.ws.rs"),
                    () -> "DAO depends on HTTP/JAX-RS: " + source);
        }
    }

    /** 将路径参数名称规范化后，检查是否存在相同 HTTP 方法和路径的重复接口。 */
    @Test
    void httpMethodAndNormalizedPathMustBeUnique() throws IOException {
        Map<String, Path> endpoints = new LinkedHashMap<>();
        for (Path source : javaSources()) {
            if (!normalize(source).contains("/controller/")) continue;
            String classPath = "";
            String methodPath = "";
            String httpMethod = null;
            boolean classDeclared = false;
            for (String line : Files.readAllLines(source)) {
                String trimmed = line.trim();
                Matcher pathMatcher = PATH_ANNOTATION.matcher(trimmed);
                if (!classDeclared && pathMatcher.find()) classPath = pathMatcher.group(1);
                if (trimmed.matches(".*\\bclass\\s+\\w+.*")) {
                    classDeclared = true;
                    continue;
                }
                if (!classDeclared) continue;
                if (pathMatcher.find(0)) methodPath = pathMatcher.group(1);
                Matcher httpMatcher = HTTP_ANNOTATION.matcher(trimmed);
                if (httpMatcher.find()) httpMethod = httpMatcher.group(1);
                if (httpMethod != null && trimmed.matches(".*\\b(public|protected|private)\\b.*\\(.*")) {
                    String route = normalizeRoute(classPath, methodPath);
                    String key = httpMethod + " " + route;
                    Path previous = endpoints.putIfAbsent(key, source);
                    assertTrue(previous == null,
                            () -> "重复接口 " + key + ": " + previous + " 与 " + source);
                    methodPath = "";
                    httpMethod = null;
                }
            }
        }
    }

    private static List<Path> javaSources() throws IOException {
        try (var paths = Files.walk(SOURCE_ROOT)) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private static String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String normalizeRoute(String classPath, String methodPath) {
        String route = ("/" + classPath + "/" + methodPath).replaceAll("/+", "/");
        if (route.length() > 1 && route.endsWith("/")) route = route.substring(0, route.length() - 1);
        return route.replaceAll("\\{[^}]+}", "{}");
    }
}
