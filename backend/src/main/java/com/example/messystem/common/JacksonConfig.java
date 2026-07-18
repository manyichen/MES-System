/*
 * 答辩定位：公共基础设施 模块的 JacksonConfig。
 * 分层职责：运行基础设施：负责应用注册、服务器启动、配置读取或数据库连接，是业务模块共享的外部依赖边界。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * 公共基础设施 的 JacksonConfig，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
@Provider
public class JacksonConfig implements ContextResolver<ObjectMapper> {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 公共能力：查询单条记录或详情。
     * 由 JacksonConfig 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}
