package com.example.messystem.planning.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.planning.dao.ReworkPlanningDao;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.entity.ReworkPlanningDemand;
import com.example.messystem.planning.entity.ReworkPlanningRequest;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class ReworkPlanningService {
    private final ReworkPlanningDao dao = new ReworkPlanningDao();

    public List<ReworkPlanningDemand> listReworkDemands() {
        try {
            return dao.listDemands();
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

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

    private static IllegalStateException database(SQLException ex) {
        return new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
    }
}
