/*
 * 答辩定位：公共基础设施 模块的 NotFoundException。
 * 分层职责：公共支撑代码：提供多个业务模块共享的响应、异常、编码或工具能力。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.common;

/**
 * 公共基础设施 的 NotFoundException，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class NotFoundException extends RuntimeException {
    /**
     * 公共能力：执行 NotFoundException 对应的业务步骤。
     * 由 NotFoundException 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public NotFoundException(String message) {
        super(message);
    }
}
