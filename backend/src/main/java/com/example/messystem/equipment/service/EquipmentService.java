package com.example.messystem.equipment.service;

import com.example.messystem.equipment.dao.EquipmentDao;
import com.example.messystem.equipment.dao.EquipmentRepairReportDao;
import com.example.messystem.equipment.dao.MaintenanceOrderDao;
import com.example.messystem.equipment.dao.MaintenancePlanDao;
import com.example.messystem.equipment.entity.MesEquipment;
import com.example.messystem.equipment.entity.MesEquipmentRepairReport;
import com.example.messystem.equipment.entity.MesMaintenanceOrder;
import com.example.messystem.equipment.entity.MesMaintenancePlan;

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
        return repairReportDao.insert(report);
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
        return repairReportDao.updateStatus(id, "APPROVED");
    }

    public long convertRepairReportToMaintenanceOrder(long repairReportId) throws SQLException {
        MesEquipmentRepairReport report = repairReportDao.findById(repairReportId)
                .orElseThrow(() -> new SQLException("Repair report not found"));
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
        repairReportDao.updateStatus(repairReportId, "CONVERTED");
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

    public boolean updateMaintenanceOrderStatus(long id, String status) throws SQLException {
        boolean updated = maintenanceOrderDao.updateStatus(id, status);
        if (updated && "ACCEPTED".equals(status)) {
            getMaintenanceOrder(id)
                    .map(MesMaintenanceOrder::equipmentId)
                    .filter(equipmentId -> equipmentId != null)
                    .ifPresent(equipmentId -> {
                        try {
                            equipmentDao.updateStatus(equipmentId, "RUNNING");
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        return updated;
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
