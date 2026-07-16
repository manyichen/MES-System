package com.example.messystem;

import com.example.messystem.common.DbConfig;
import org.junit.jupiter.api.Assumptions;

/** 防止会写数据库的集成测试在未明确授权时污染远程或云端数据库。 */
final class RemoteDatabaseTestGuard {
    private static final String OPT_IN_PROPERTY = "mes.allowDestructiveRemoteDbTests";
    private static final String OPT_IN_ENV = "MES_ALLOW_DESTRUCTIVE_REMOTE_DB_TESTS";

    private RemoteDatabaseTestGuard() {
    }

    static void requireExplicitOptInForRemoteDatabase() {
        String host = DbConfig.HOST == null ? "" : DbConfig.HOST.trim().toLowerCase();
        boolean local = host.isBlank() || "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
        boolean allowed = Boolean.parseBoolean(System.getProperty(OPT_IN_PROPERTY, "false"))
                || Boolean.parseBoolean(System.getenv(OPT_IN_ENV));
        Assumptions.assumeTrue(local || allowed,
                "检测到远程数据库 " + DbConfig.HOST + "，已跳过写库集成测试；如确需执行，请显式设置 -D"
                        + OPT_IN_PROPERTY + "=true");
    }
}
