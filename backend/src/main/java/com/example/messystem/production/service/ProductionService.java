/*
 * 答辩定位：生产报工与计件工资 模块的 ProductionService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.production.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.production.dao.ProductionDao;
import com.example.messystem.production.entity.MesPieceworkWage;
import com.example.messystem.production.entity.MesWorkReport;
import java.sql.SQLException;
import java.util.List;

/**
 * 生产报工与计件工资 的 ProductionService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class ProductionService {
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final ProductionDao dao = new ProductionDao();

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesWorkReport> listWorkReports() {
        return database(dao::listWorkReports);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesWorkReport> listWorkReportsByOperator(long operatorId) {
        return database(() -> dao.listWorkReportsByOperator(operatorId));
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWorkReport createWorkReport(MesWorkReport report) {
        return createWorkReport(report, false);
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWorkReport createWorkReport(MesWorkReport report, boolean allowAdministrativeOverride) {
        if (report.workOrderId == null || report.workOrderId <= 0) {
            throw new BadRequestException("生产工单ID不能为空");
        }
        report.reportQty = report.reportQty == null ? 0 : report.reportQty;
        report.qualifiedQty = report.qualifiedQty == null ? 0 : report.qualifiedQty;
        report.defectQty = report.defectQty == null ? 0 : report.defectQty;
        if (report.reportQty < 0 || report.qualifiedQty < 0 || report.defectQty < 0) {
            throw new BadRequestException("数量不能为负数");
        }
        if (report.qualifiedQty + report.defectQty > report.reportQty) {
            throw new BadRequestException("合格数量与不合格数量之和不能超过报工数量");
        }
        return database(() -> dao.insertWorkReport(report, !allowAdministrativeOverride));
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWorkReport getWorkReport(long reportId) {
        return database(() -> dao.findWorkReport(reportId));
    }

    /**
     * 业务用例：更新业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWorkReport updateWorkReport(long reportId, MesWorkReport report) {
        return updateWorkReport(reportId, report, false);
    }

    /**
     * 业务用例：更新业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWorkReport updateWorkReport(long reportId, MesWorkReport report,
            boolean allowAdministrativeOverride) {
        if (report.reportQty != null && report.reportQty < 0
                || report.qualifiedQty != null && report.qualifiedQty < 0
                || report.defectQty != null && report.defectQty < 0) {
            throw new BadRequestException("数量不能为负数");
        }
        if (report.reportQty != null && report.qualifiedQty != null && report.defectQty != null
                && report.qualifiedQty + report.defectQty > report.reportQty) {
            throw new BadRequestException("合格数量与不合格数量之和不能超过报工数量");
        }
        return database(() -> dao.updateWorkReport(reportId, report, !allowAdministrativeOverride));
    }

    /**
     * 业务用例：删除业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public void deleteWorkReport(long reportId) {
        database(() -> {
            dao.deleteWorkReport(reportId);
            return null;
        });
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesWorkReport> listWorkReportsByWorkOrder(long workOrderId) {
        if (workOrderId <= 0) {
            throw new BadRequestException("生产工单ID不能为空");
        }
        return database(() -> dao.listWorkReportsByWorkOrder(workOrderId));
    }

    /**
     * 业务用例：审核通过业务事项。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWorkReport approveWorkReport(long reportId) {
        return database(() -> dao.approveWorkReport(reportId));
    }

    /**
     * 业务用例：驳回业务事项。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWorkReport rejectWorkReport(long reportId) {
        return rejectWorkReport(reportId, null);
    }

    /**
     * 业务用例：驳回业务事项。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWorkReport rejectWorkReport(long reportId, String reason) {
        return database(() -> dao.rejectWorkReport(reportId, reason));
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesPieceworkWage> listWages() {
        return database(dao::listWages);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesWorkReport> listWorkReportsByWorkOrderAndOperator(long workOrderId, long operatorId) {
        return database(() -> dao.listWorkReportsByWorkOrderAndOperator(workOrderId, operatorId));
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesPieceworkWage> listWagesByOperator(long operatorId) {
        return database(() -> dao.listWagesByOperator(operatorId));
    }

    /**
     * 业务用例：执行 wageSummary 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public java.util.Map<String, Object> wageSummary() {
        return database(dao::wageSummary);
    }

    /**
     * 业务用例：执行 wageSummaryForWorkshop 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public java.util.Map<String, Object> wageSummaryForWorkshop(long userId) {
        return database(() -> dao.wageSummaryForWorkshop(userId));
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesPieceworkWage getWage(long wageId) {
        return database(() -> dao.findWage(wageId));
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesPieceworkWage> listWagesByReport(long reportId) {
        if (reportId <= 0) {
            throw new BadRequestException("报工单ID不能为空");
        }
        return database(() -> dao.listWagesByReport(reportId));
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesPieceworkWage> listWagesByReportAndOperator(long reportId, long operatorId) {
        return database(() -> dao.listWagesByReportAndOperator(reportId, operatorId));
    }

    /**
     * 业务用例：执行 database 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static <T> T database(SqlCall<T> call) {
        try {
            return call.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface SqlCall<T> {
        T execute() throws SQLException;
    }
}
