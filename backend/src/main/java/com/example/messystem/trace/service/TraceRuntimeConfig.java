/*
 * 答辩定位：轮胎标签与公开追溯 模块的 TraceRuntimeConfig。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.trace.service;

import com.example.messystem.common.DbConfig;
import java.nio.file.Path;
import java.net.URI;

/**
 * 轮胎标签与公开追溯 的 TraceRuntimeConfig，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public final class TraceRuntimeConfig {
    /**
     * 业务用例：执行 TraceRuntimeConfig 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private TraceRuntimeConfig() {
    }

    /**
     * 业务用例：执行 publicBaseUrl 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public static String publicBaseUrl() {
        String configured = DbConfig.getValue("MES_PUBLIC_URL", "");
        String value = configured == null || configured.isBlank()
                ? System.getProperty("MES_RUNTIME_PUBLIC_URL", "http://127.0.0.1:8080")
                : configured;
        return value.replaceFirst("/+$", "");
    }

    /**
     * 业务用例：执行 publicBaseUrl 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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

    /**
     * 业务用例：执行 storageRoot 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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
