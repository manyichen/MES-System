package com.example.messystem;

import com.example.messystem.common.DbConfig;
import org.junit.jupiter.api.Assumptions;

/** 防止会写数据库的集成测试在未明确授权时污染远程或云端数据库。 */
final class RemoteDatabaseTestGuard {
    private RemoteDatabaseTestGuard() {
    }

    static void requireExplicitOptInForRemoteDatabase() {
        String host = DbConfig.HOST == null ? "" : DbConfig.HOST.trim().toLowerCase();
        boolean local = host.isBlank() || "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
        Assumptions.assumeTrue(local,
                "检测到远程数据库 " + DbConfig.HOST + "，为防止测试数据污染，写库集成测试只允许连接本机隔离数据库");
    }
}
