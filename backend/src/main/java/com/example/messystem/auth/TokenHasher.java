/*
 * 答辩定位：登录认证与会话 模块的 TokenHasher。
 * 分层职责：公共支撑代码：提供多个业务模块共享的响应、异常、编码或工具能力。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** 生成不可预测的会话令牌，并仅将其哈希值写入数据库。 */
public final class TokenHasher {
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 内部实现步骤：执行 TokenHasher 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private TokenHasher() {
    }

    /**
     * 公共能力：执行 newToken 对应的业务步骤。
     * 由 TokenHasher 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 公共能力：计算不可逆摘要。
     * 由 TokenHasher 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to hash session token", ex);
        }
    }
}
