/*
 * 答辩定位：公共基础设施 模块的 ResourceSupport。
 * 分层职责：公共支撑代码：提供多个业务模块共享的响应、异常、编码或工具能力。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.common;

import jakarta.ws.rs.core.Response;

/**
 * 公共基础设施 的 ResourceSupport，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public final class ResourceSupport {
    /**
     * 内部实现步骤：执行 ResourceSupport 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private ResourceSupport() {
    }

    /**
     * 公共能力：执行 ok 对应的业务步骤。
     * 由 ResourceSupport 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static Response ok(Object data) {
        return Response.ok(ApiResponse.ok(data)).build();
    }

    /**
     * 公共能力：创建业务记录。
     * 由 ResourceSupport 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static Response created(String message, Object data) {
        return Response.status(Response.Status.CREATED).entity(ApiResponse.ok(message, data)).build();
    }

    /**
     * 公共能力：执行 action 对应的业务步骤。
     * 由 ResourceSupport 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static Response action(String message, Object data) {
        return Response.ok(ApiResponse.ok(message, data)).build();
    }

    /**
     * 公共能力：执行 handle 对应的业务步骤。
     * 由 ResourceSupport 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static Response handle(RuntimeException ex) {
        String message = ChineseMessageSupport.translate(ex.getMessage());
        if (ex instanceof NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND).entity(ApiResponse.fail(message)).build();
        }
        if (ex instanceof BadRequestException) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.fail(message)).build();
        }
        return Response.serverError().entity(ApiResponse.fail(message)).build();
    }
}
