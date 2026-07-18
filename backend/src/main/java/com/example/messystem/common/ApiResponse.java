/*
 * 答辩定位：公共基础设施 模块的 ApiResponse。
 * 分层职责：公共支撑代码：提供多个业务模块共享的响应、异常、编码或工具能力。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.common;

/**
 * 公共基础设施 的 ApiResponse，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class ApiResponse<T> {
    /** success 业务字段；具体取值由创建/更新用例校验后写入。 */
    public boolean success;
    /** message 业务字段；具体取值由创建/更新用例校验后写入。 */
    public String message;
    /** data 业务字段；具体取值由创建/更新用例校验后写入。 */
    public T data;

    /**
     * 公共能力：执行 ApiResponse 对应的业务步骤。
     * 由 ApiResponse 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public ApiResponse() {
    }

    /**
     * 内部实现步骤：执行 ApiResponse 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /**
     * 公共能力：执行 ok 对应的业务步骤。
     * 由 ApiResponse 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "操作成功", data);
    }

    /**
     * 公共能力：执行 ok 对应的业务步骤。
     * 由 ApiResponse 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * 公共能力：执行 fail 对应的业务步骤。
     * 由 ApiResponse 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
