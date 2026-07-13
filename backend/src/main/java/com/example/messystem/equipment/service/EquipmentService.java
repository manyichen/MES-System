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
        return equipmentDao.insert(equipment);
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
        long id = repairReportDao.insert(report);
        equipmentDao.updateStatus(report.equipmentId(), "FAULT");
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
        if (resultDesc == null || resultDesc.isBlank()) {
            throw new BadRequestException("请填写维修结果、处理措施或故障原因");
        }
        if (!maintenanceOrderDao.finishOwn(id, maintainerId, resultDesc.trim())) {
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
        return maintenancePlanDao.insert(plan);
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
}
