/*
 * 答辩定位：MES 应用基础 模块的 RemoteDatabaseTestGuard。
 * 分层职责：自动化回归测试：固定关键业务规则、接口契约和架构边界，防止重构时出现静默回归。
 * 典型调用链：Maven Surefire -> JUnit 5 -> 被测类；测试替身用于隔离远程数据库或文件系统。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem;

import com.example.messystem.common.DbConfig;
import org.junit.jupiter.api.Assumptions;

/** 防止会写数据库的集成测试在未明确授权时污染远程或云端数据库。 */
final class RemoteDatabaseTestGuard {
    /**
     * 回归场景：验证 RemoteDatabaseTestGuard 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    private RemoteDatabaseTestGuard() {
    }

    /**
     * 回归场景：验证 requireExplicitOptInForRemoteDatabase 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    static void requireExplicitOptInForRemoteDatabase() {
        String host = DbConfig.HOST == null ? "" : DbConfig.HOST.trim().toLowerCase();
        boolean local = host.isBlank() || "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
        Assumptions.assumeTrue(local,
                "检测到远程数据库 " + DbConfig.HOST + "，为防止测试数据污染，写库集成测试只允许连接本机隔离数据库");
    }
}
