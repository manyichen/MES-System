/*
 * 答辩定位：设备与维修保养 模块的 EquipmentRepairReportDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
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

/**
 * 设备与维修保养 的 EquipmentRepairReportDao，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class EquipmentRepairReportDao {

    /**
     * 数据访问：写入业务记录并返回主键。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：按主键查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：按业务条件查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：查询全部可见记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：更新业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public boolean updateStatus(long id, String status) throws SQLException {
        String sql = "UPDATE mes_equipment_repair_report SET repair_status = ? WHERE repair_report_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 数据访问：更新业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public boolean updateStatus(long id, String status, String expectedStatus) throws SQLException {
        String sql = "UPDATE mes_equipment_repair_report SET repair_status = ? WHERE repair_report_id = ? AND repair_status = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, id);
            ps.setString(3, expectedStatus);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 数据访问：把 JDBC 结果行映射为领域对象。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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
