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

public class EquipmentService {

    private final EquipmentDao equipmentDao = new EquipmentDao();
    private final EquipmentRepairReportDao repairReportDao = new EquipmentRepairReportDao();
    private final MaintenanceOrderDao maintenanceOrderDao = new MaintenanceOrderDao();
    private final MaintenancePlanDao maintenancePlanDao = new MaintenancePlanDao();

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

    public Optional<MesEquipment> getEquipmentById(long equipmentId) throws SQLException {
        return equipmentDao.findById(equipmentId);
    }

    public List<MesEquipment> listEquipment() throws SQLException {
        return equipmentDao.findAll();
    }

    public List<MesEquipment> listEquipmentByLine(long lineId) throws SQLException {
        return equipmentDao.findByLineId(lineId);
    }

    public boolean updateEquipmentStatus(long equipmentId, String newStatus) throws SQLException {
        return equipmentDao.updateStatus(equipmentId, newStatus);
    }

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

    public Optional<MesEquipmentRepairReport> getRepairReport(long id) throws SQLException {
        return repairReportDao.findById(id);
    }

    public List<MesEquipmentRepairReport> listRepairReportsForEquipment(long equipmentId) throws SQLException {
        return repairReportDao.findByEquipmentId(equipmentId);
    }

    public List<MesEquipmentRepairReport> listRepairReports() throws SQLException {
        return repairReportDao.findAll();
    }

    public boolean approveRepairReport(long id) throws SQLException {
        if (!repairReportDao.updateStatus(id, "APPROVED", "REPORTED")) {
            throw new BadRequestException("只有待核实的报修单才能审核");
        }
        convertRepairReportToMaintenanceOrder(id);
        return true;
    }

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

    public long createMaintenanceOrder(MesMaintenanceOrder order) throws SQLException {
        return maintenanceOrderDao.insert(order);
    }

    public Optional<MesMaintenanceOrder> getMaintenanceOrder(long id) throws SQLException {
        return maintenanceOrderDao.findById(id);
    }

    public List<MesMaintenanceOrder> listMaintenanceOrdersForReport(long repairReportId) throws SQLException {
        return maintenanceOrderDao.findByRepairReportId(repairReportId);
    }

    public List<MesMaintenanceOrder> listMaintenanceOrders() throws SQLException {
        return maintenanceOrderDao.findAll();
    }

    public List<MesMaintenanceOrder> listMaintenanceOrdersForMaintainer(long userId) throws SQLException {
        return maintenanceOrderDao.findByMaintainer(userId);
    }

    public boolean assignMaintenanceOrder(long id, long maintainerId) throws SQLException {
        UserRoleValidator.requireEnabledRole(maintainerId, "EQUIPMENT_MAINTAINER", "设备维护员");
        if (!maintenanceOrderDao.assign(id, maintainerId)) {
            throw new com.example.messystem.common.BadRequestException("维修工单不存在、已派工或状态不允许派工");
        }
        return true;
    }

    public boolean finishMaintenanceOrder(long id, long maintainerId) throws SQLException {
        return finishMaintenanceOrder(id, maintainerId, "");
    }

    public boolean finishMaintenanceOrder(long id, long maintainerId, String resultDesc) throws SQLException {
        return finishMaintenanceOrder(id, maintainerId, resultDesc, false);
    }

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

    public Optional<MesMaintenancePlan> getMaintenancePlan(long id) throws SQLException {
        return maintenancePlanDao.findById(id);
    }

    public List<MesMaintenancePlan> listMaintenancePlansForEquipment(long equipmentId) throws SQLException {
        return maintenancePlanDao.findByEquipmentId(equipmentId);
    }

    public List<MesMaintenancePlan> listMaintenancePlans() throws SQLException {
        return maintenancePlanDao.findAll();
    }

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

    private static void requireText(String value, String message) {
        if (isBlank(value)) {
            throw new BadRequestException(message);
        }
    }

    private static void requireId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new BadRequestException(message);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
