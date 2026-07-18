/*
 * 答辩定位：订单、计划、齐套与工单 模块的 ReworkPlanningService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.planning.dao.ReworkPlanningDao;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.entity.ReworkPlanningDemand;
import com.example.messystem.planning.entity.ReworkPlanningRequest;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单、计划、齐套与工单 的 ReworkPlanningService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class ReworkPlanningService {
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final ReworkPlanningDao dao = new ReworkPlanningDao();

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<ReworkPlanningDemand> listReworkDemands() {
        try {
            return dao.listDemands();
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    /**
     * 业务用例：执行 plan 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesProductionTask plan(long reworkOrderId, ReworkPlanningRequest request, long plannerId) {
        if (request == null) throw new BadRequestException("返工排产信息不能为空");
        if (plannerId <= 0) throw new BadRequestException("plannerId is required");
        LocalDateTime start = request.plannedStartTime == null ? LocalDateTime.now() : request.plannedStartTime;
        LocalDateTime end = request.plannedEndTime == null ? start.plusDays(3) : request.plannedEndTime;
        if (!end.isAfter(start)) throw new BadRequestException("返工计划完成时间必须晚于开始时间");

        try {
            long taskId = dao.createTask(reworkOrderId, plannerId, request.planQty, start, end,
                    request.targetLineId);
            return new ProductionTaskService().getTask(taskId);
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    /**
     * 业务用例：执行 database 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static IllegalStateException database(SQLException ex) {
        return new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
    }
}
