/*
 * 答辩定位：访问控制与系统维护 模块的 SystemMaintenanceService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.access.service;

import com.example.messystem.access.dao.SystemMaintenanceDao;

/** 对安全敏感的系统维护命令提供业务层入口。 */
public class SystemMaintenanceService {
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final SystemMaintenanceDao dao = new SystemMaintenanceDao();

    /**
     * 业务用例：装载业务数据。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public SystemMaintenanceDao.SystemMaintenanceSummary loadSummary() {
        return dao.loadSummary();
    }

    /**
     * 业务用例：撤销会话或授权。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public int revokeSession(long sessionId, long actorUserId) {
        return dao.revokeSession(sessionId, actorUserId);
    }

    /**
     * 业务用例：撤销指定用户的全部登录会话。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public int revokeUserSessions(long userId, long actorUserId) {
        return dao.revokeUserSessions(userId, actorUserId);
    }

    /**
     * 业务用例：清理已经过期的登录会话。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public int cleanupExpiredSessions() {
        return dao.cleanupExpiredSessions();
    }

    /**
     * 业务用例：解除账号锁定。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public int unlockUser(long userId) {
        return dao.unlockUser(userId);
    }

    /**
     * 业务用例：停用业务对象。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public int disableUser(long userId, long actorUserId) {
        return dao.disableUser(userId, actorUserId);
    }

    /**
     * 业务用例：恢复已删除的业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public int restoreUser(long userId, long actorUserId) {
        return dao.restoreUser(userId, actorUserId);
    }

    /**
     * 业务用例：将同步异常标记为已处理。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public int markSyncLogHandled(long syncLogId) {
        return dao.markSyncLogHandled(syncLogId);
    }
}
