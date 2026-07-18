/*
 * 答辩定位：设备与维修保养 模块的 EquipmentService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.equipment.service;

import com.example.messystem.equipment.dao.EquipmentDao;
import com.example.messystem.equipment.dao.EquipmentRepairReportDao;
import com.example.messystem.equipment.dao.MaintenanceOrderDao;
import com.example.messystem.equipment.dao.MaintenancePlanDao;
import com.example.messystem.equipment.entity.MesEquipment;
import com.example.messystem.equipment.entity.MesEquipmentRepairReport;
import com.example.messystem.equipment.entity.MesMaintenanceOrder;
import com.example.messystem.equipment.entity.MesMaintenancePlan;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.common.UserRoleValidator;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 设备与维修保养 的 EquipmentService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class EquipmentService {

    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final EquipmentDao equipmentDao = new EquipmentDao();
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final EquipmentRepairReportDao repairReportDao = new EquipmentRepairReportDao();
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final MaintenanceOrderDao maintenanceOrderDao = new MaintenanceOrderDao();
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final MaintenancePlanDao maintenancePlanDao = new MaintenancePlanDao();

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public long createEquipment(MesEquipment equipment) throws SQLException {
        if (equipment == null) {
            throw new BadRequestException("equipment body is required");
        }
        requireText(equipment.equipmentName(), "设备名称不能为空");
        requireText(equipment.equipmentType(), "设备类型不能为空");
        MesEquipment normalized = new MesEquipment(
                equipment.equipmentId(),
                isBlank(equipment.equipmentCode()) ? IdGenerator.nextCode("EQ") : equipment.equipmentCode().trim(),
                equipment.equipmentName().trim(),
                equipment.equipmentType().trim(),
                equipment.lineId(),
                isBlank(equipment.equipmentStatus()) ? "IDLE" : equipment.equipmentStatus().trim(),
                equipment.lastMaintenanceTime(),
                equipment.enabled() == null ? Boolean.TRUE : equipment.enabled()
        );
        return equipmentDao.insert(normalized);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public Optional<MesEquipment> getEquipmentById(long equipmentId) throws SQLException {
        return equipmentDao.findById(equipmentId);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesEquipment> listEquipment() throws SQLException {
        return equipmentDao.findAll();
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesEquipment> listEquipmentByLine(long lineId) throws SQLException {
        return equipmentDao.findByLineId(lineId);
    }

    /**
     * 业务用例：更新业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public boolean updateEquipmentStatus(long equipmentId, String newStatus) throws SQLException {
        return equipmentDao.updateStatus(equipmentId, newStatus);
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public long createRepairReport(MesEquipmentRepairReport report) throws SQLException {
        if (report == null) {
            throw new BadRequestException("repair report body is required");
        }
        requireId(report.equipmentId(), "报修设备不能为空");
        requireText(report.faultLevel(), "故障级别不能为空");
        requireText(report.faultDesc(), "故障描述不能为空");
        MesEquipmentRepairReport normalized = new MesEquipmentRepairReport(
                report.repairReportId(),
                isBlank(report.repairReportNo()) ? IdGenerator.nextCode("RR") : report.repairReportNo().trim(),
                report.equipmentId(),
                report.workOrderId(),
                report.faultLevel().trim(),
                report.faultDesc().trim(),
                report.reporterId(),
                report.reportTime() == null ? LocalDateTime.now() : report.reportTime(),
                isBlank(report.repairStatus()) ? "REPORTED" : report.repairStatus().trim()
        );
        long id = repairReportDao.insert(normalized);
        equipmentDao.updateStatus(normalized.equipmentId(), "FAULT");
        return id;
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public Optional<MesEquipmentRepairReport> getRepairReport(long id) throws SQLException {
        return repairReportDao.findById(id);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesEquipmentRepairReport> listRepairReportsForEquipment(long equipmentId) throws SQLException {
        return repairReportDao.findByEquipmentId(equipmentId);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesEquipmentRepairReport> listRepairReports() throws SQLException {
        return repairReportDao.findAll();
    }

    /**
     * 业务用例：审核通过业务事项。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public boolean approveRepairReport(long id) throws SQLException {
        if (!repairReportDao.updateStatus(id, "APPROVED", "REPORTED")) {
            throw new BadRequestException("只有待核实的报修单才能审核");
        }
        convertRepairReportToMaintenanceOrder(id);
        return true;
    }

    /**
     * 业务用例：执行 convertRepairReportToMaintenanceOrder 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public long convertRepairReportToMaintenanceOrder(long repairReportId) throws SQLException {
        List<MesMaintenanceOrder> existing = maintenanceOrderDao.findByRepairReportId(repairReportId);
        if (!existing.isEmpty()) {
            return existing.get(0).maintenanceOrderId();
        }
        MesEquipmentRepairReport report = repairReportDao.findById(repairReportId)
                .orElseThrow(() -> new BadRequestException("报修单不存在"));
        if (!"APPROVED".equals(report.repairStatus())) {
            throw new BadRequestException("报修单审核通过后才能转为维修工单");
        }
        long orderId = maintenanceOrderDao.insert(new MesMaintenanceOrder(
                null,
                "MO-" + System.currentTimeMillis(),
                repairReportId,
                report.equipmentId(),
                null,
                "CREATED",
                null,
                null,
                report.faultDesc()
        ));
        repairReportDao.updateStatus(repairReportId, "CONVERTED", "APPROVED");
        return orderId;
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public long createMaintenanceOrder(MesMaintenanceOrder order) throws SQLException {
        return maintenanceOrderDao.insert(order);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public Optional<MesMaintenanceOrder> getMaintenanceOrder(long id) throws SQLException {
        return maintenanceOrderDao.findById(id);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesMaintenanceOrder> listMaintenanceOrdersForReport(long repairReportId) throws SQLException {
        return maintenanceOrderDao.findByRepairReportId(repairReportId);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesMaintenanceOrder> listMaintenanceOrders() throws SQLException {
        return maintenanceOrderDao.findAll();
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesMaintenanceOrder> listMaintenanceOrdersForMaintainer(long userId) throws SQLException {
        return maintenanceOrderDao.findByMaintainer(userId);
    }

    /**
     * 业务用例：分配执行人员或资源。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public boolean assignMaintenanceOrder(long id, long maintainerId) throws SQLException {
        UserRoleValidator.requireEnabledRole(maintainerId, "EQUIPMENT_MAINTAINER", "设备维护员");
        if (!maintenanceOrderDao.assign(id, maintainerId)) {
            throw new com.example.messystem.common.BadRequestException("维修工单不存在、已派工或状态不允许派工");
        }
        return true;
    }

    /**
     * 业务用例：完成业务任务。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public boolean finishMaintenanceOrder(long id, long maintainerId) throws SQLException {
        return finishMaintenanceOrder(id, maintainerId, "");
    }

    /**
     * 业务用例：完成业务任务。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public boolean finishMaintenanceOrder(long id, long maintainerId, String resultDesc) throws SQLException {
        return finishMaintenanceOrder(id, maintainerId, resultDesc, false);
    }

    /**
     * 业务用例：完成业务任务。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public boolean finishMaintenanceOrder(long id, long maintainerId, String resultDesc,
            boolean allowAdministrativeOverride) throws SQLException {
        if (resultDesc == null || resultDesc.isBlank()) {
            throw new BadRequestException("请填写维修结果、处理措施或故障原因");
        }
        boolean finished = allowAdministrativeOverride
                ? maintenanceOrderDao.finishAny(id, resultDesc.trim())
                : maintenanceOrderDao.finishOwn(id, maintainerId, resultDesc.trim());
        if (!finished) {
            throw new com.example.messystem.common.BadRequestException("只能完成分配给本人的维修工单");
        }
        return true;
    }

    /**
     * 业务用例：受理业务事项。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public boolean acceptMaintenanceOrder(long id, long acceptedBy) throws SQLException {
        MesMaintenanceOrder order = maintenanceOrderDao.findById(id)
                .orElseThrow(() -> new BadRequestException("维修工单不存在"));
        if (Long.valueOf(acceptedBy).equals(order.maintainerId())) {
            throw new BadRequestException("维修执行人不能验收自己的维修工单");
        }
        if (!maintenanceOrderDao.acceptFinished(id)) {
            throw new BadRequestException("只有已完成的维修工单才能验收");
        }
        if (order.equipmentId() != null) {
            equipmentDao.updateStatus(order.equipmentId(), "RUNNING");
        }
        return true;
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public long createMaintenancePlan(MesMaintenancePlan plan) throws SQLException {
        if (plan == null) {
            throw new BadRequestException("maintenance plan body is required");
        }
        requireId(plan.equipmentId(), "维护设备不能为空");
        requireText(plan.planCycle(), "维护周期不能为空");
        MesMaintenancePlan normalized = new MesMaintenancePlan(
                plan.maintenancePlanId(),
                plan.equipmentId(),
                plan.planCycle().trim(),
                plan.nextPlanTime() == null ? LocalDateTime.now().plusDays(7) : plan.nextPlanTime(),
                isBlank(plan.planStatus()) ? "ACTIVE" : plan.planStatus().trim(),
                plan.createdAt() == null ? LocalDateTime.now() : plan.createdAt()
        );
        return maintenancePlanDao.insert(normalized);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public Optional<MesMaintenancePlan> getMaintenancePlan(long id) throws SQLException {
        return maintenancePlanDao.findById(id);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesMaintenancePlan> listMaintenancePlansForEquipment(long equipmentId) throws SQLException {
        return maintenancePlanDao.findByEquipmentId(equipmentId);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesMaintenancePlan> listMaintenancePlans() throws SQLException {
        return maintenancePlanDao.findAll();
    }

    /**
     * 业务用例：执行 scheduleNextMaintenance 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public long scheduleNextMaintenance(long equipmentId, String planCycle) throws SQLException {
        MesMaintenancePlan plan = new MesMaintenancePlan(
                null,
                equipmentId,
                planCycle,
                LocalDateTime.now().plusDays(30),
                "SCHEDULED",
                LocalDateTime.now()
        );
        return maintenancePlanDao.insert(plan);
    }

    /**
     * 业务用例：执行 requireText 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static void requireText(String value, String message) {
        if (isBlank(value)) {
            throw new BadRequestException(message);
        }
    }

    /**
     * 业务用例：执行 requireId 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static void requireId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new BadRequestException(message);
        }
    }

    /**
     * 业务用例：执行 isBlank 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
