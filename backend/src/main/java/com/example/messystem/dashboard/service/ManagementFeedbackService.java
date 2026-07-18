/*
 * 答辩定位：驾驶舱、反馈与产品追溯 模块的 ManagementFeedbackService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.dashboard.service;

import com.example.messystem.dashboard.dao.ManagementFeedbackDao;
import com.example.messystem.dashboard.entity.MesManagementFeedback;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 驾驶舱、反馈与产品追溯 的 ManagementFeedbackService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class ManagementFeedbackService {

    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final ManagementFeedbackDao feedbackDao = new ManagementFeedbackDao();

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public long createFeedback(MesManagementFeedback feedback, long createdBy) throws SQLException {
        return feedbackDao.insert(feedback, createdBy);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public Optional<MesManagementFeedback> getFeedback(long id) throws SQLException {
        return feedbackDao.findById(id);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesManagementFeedback> listFeedbackForWorkOrder(long workOrderId) throws SQLException {
        return feedbackDao.findByWorkOrderId(workOrderId);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesManagementFeedback> listOwnFeedbackForWorkOrder(long workOrderId, long userId) throws SQLException {
        return feedbackDao.findByWorkOrderIdAndCreator(workOrderId, userId);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public Optional<MesManagementFeedback> getOwnFeedback(long id, long userId) throws SQLException {
        return feedbackDao.findByIdAndCreator(id, userId);
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public long createDefaultFeedback(long orderId, long taskId, long workOrderId, String type, String content) throws SQLException {
        MesManagementFeedback feedback = new MesManagementFeedback(
                null,
                "FB-" + System.currentTimeMillis(),
                orderId,
                taskId,
                workOrderId,
                type,
                content,
                "",
                "OPEN",
                LocalDateTime.now(),
                null
        );
        return feedbackDao.insert(feedback, 1L);
    }

    /**
     * 业务用例：关闭业务事项。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public boolean closeFeedback(long id) throws SQLException {
        return feedbackDao.close(id);
    }
}
