/*
 * 答辩定位：登录认证与会话 模块的 LoginRequest。
 * 分层职责：公共支撑代码：提供多个业务模块共享的响应、异常、编码或工具能力。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.auth;

/**
 * 登录认证与会话 的 LoginRequest，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class LoginRequest {
    /** 登录账号，认证时作为用户唯一标识之一。 */
    public String username;
    /** 本次请求携带的明文密码；仅在认证边界短暂使用，不应持久化。 */
    public String password;
}
