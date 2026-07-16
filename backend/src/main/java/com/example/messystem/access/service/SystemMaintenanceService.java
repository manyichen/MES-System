package com.example.messystem.access.service;

import com.example.messystem.access.dao.SystemMaintenanceDao;

/** 对安全敏感的系统维护命令提供业务层入口。 */
public class SystemMaintenanceService {
    private final SystemMaintenanceDao dao = new SystemMaintenanceDao();

    public SystemMaintenanceDao.SystemMaintenanceSummary loadSummary() {
        return dao.loadSummary();
    }

    public int revokeSession(long sessionId, long actorUserId) {
        return dao.revokeSession(sessionId, actorUserId);
    }

    public int revokeUserSessions(long userId, long actorUserId) {
        return dao.revokeUserSessions(userId, actorUserId);
    }

    public int cleanupExpiredSessions() {
        return dao.cleanupExpiredSessions();
    }

    public int unlockUser(long userId) {
        return dao.unlockUser(userId);
    }

    public int disableUser(long userId, long actorUserId) {
        return dao.disableUser(userId, actorUserId);
    }

    public int restoreUser(long userId, long actorUserId) {
        return dao.restoreUser(userId, actorUserId);
    }

    public int markSyncLogHandled(long syncLogId) {
        return dao.markSyncLogHandled(syncLogId);
    }
}
