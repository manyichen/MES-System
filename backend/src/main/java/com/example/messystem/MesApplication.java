/*
 * 答辩定位：MES 应用基础 模块的 MesApplication。
 * 分层职责：运行基础设施：负责应用注册、服务器启动、配置读取或数据库连接，是业务模块共享的外部依赖边界。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * MES 应用基础 的 MesApplication，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
@ApplicationPath("/api")
public class MesApplication extends ResourceConfig {
    /**
     * 公共能力：执行 MesApplication 对应的业务步骤。
     * 由 MesApplication 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public MesApplication() {
        packages("com.example.messystem");
    }
}
