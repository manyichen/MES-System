package com.example.messystem.equipment.dao;

import com.example.messystem.common.Db;
import com.example.messystem.equipment.entity.MesEquipmentRepairReport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EquipmentRepairReportDao {

    public long insert(MesEquipmentRepairReport report) throws SQLException {
        String sql = "INSERT INTO mes_equipment_repair_report (repair_report_no, equipment_id, work_order_id, fault_level, fault_desc, reporter_id, report_time, repair_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, report.repairReportNo());
            ps.setLong(2, report.equipmentId());
            if (report.workOrderId() == null) {
                ps.setNull(3, Types.BIGINT);
            } else {
                ps.setLong(3, report.workOrderId());
            }
            ps.setString(4, report.faultLevel());
            ps.setString(5, report.faultDesc());
            if (report.reporterId() == null) {
                ps.setNull(6, Types.BIGINT);
            } else {
                ps.setLong(6, report.reporterId());
            }
            ps.setObject(7, report.reportTime());
            ps.setString(8, report.repairStatus());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert repair report failed, no ID obtained.");
            }
        }
    }

    public Optional<MesEquipmentRepairReport> findById(long id) throws SQLException {
        String sql = "SELECT repair_report_id, repair_report_no, equipment_id, work_order_id, fault_level, fault_desc, reporter_id, report_time, repair_status FROM mes_equipment_repair_report WHERE repair_report_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<MesEquipmentRepairReport> findByEquipmentId(long equipmentId) throws SQLException {
        String sql = "SELECT repair_report_id, repair_report_no, equipment_id, work_order_id, fault_level, fault_desc, reporter_id, report_time, repair_status FROM mes_equipment_repair_report WHERE equipment_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, equipmentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MesEquipmentRepairReport> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
                return results;
            }
        }
    }

    public List<MesEquipmentRepairReport> findAll() throws SQLException {
        String sql = "SELECT repair_report_id, repair_report_no, equipment_id, work_order_id, fault_level, fault_desc, reporter_id, report_time, repair_status FROM mes_equipment_repair_report";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<MesEquipmentRepairReport> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;
        }
    }

    public boolean updateStatus(long id, String status) throws SQLException {
        String sql = "UPDATE mes_equipment_repair_report SET repair_status = ? WHERE repair_report_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(long id, String status, String expectedStatus) throws SQLException {
        String sql = "UPDATE mes_equipment_repair_report SET repair_status = ? WHERE repair_report_id = ? AND repair_status = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, id);
            ps.setString(3, expectedStatus);
            return ps.executeUpdate() > 0;
        }
    }

    private MesEquipmentRepairReport mapRow(ResultSet rs) throws SQLException {
        return new MesEquipmentRepairReport(
                rs.getLong("repair_report_id"),
                rs.getString("repair_report_no"),
                rs.getLong("equipment_id"),
                rs.getObject("work_order_id") == null ? null : rs.getLong("work_order_id"),
                rs.getString("fault_level"),
                rs.getString("fault_desc"),
                rs.getObject("reporter_id") == null ? null : rs.getLong("reporter_id"),
                rs.getObject("report_time", java.time.LocalDateTime.class),
                rs.getString("repair_status")
        );
    }
}
