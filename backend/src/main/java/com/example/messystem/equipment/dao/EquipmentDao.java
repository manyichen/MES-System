/*
 * 答辩定位：设备与维修保养 模块的 EquipmentDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.equipment.dao;

import com.example.messystem.common.Db;
import com.example.messystem.equipment.entity.MesEquipment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 设备与维修保养 的 EquipmentDao，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class EquipmentDao {

    /**
     * 数据访问：写入业务记录并返回主键。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public long insert(MesEquipment equipment) throws SQLException {
        String sql = "INSERT INTO mes_equipment (equipment_code, equipment_name, equipment_type, line_id, equipment_status, last_maintenance_time, enabled) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, equipment.equipmentCode());
            ps.setString(2, equipment.equipmentName());
            ps.setString(3, equipment.equipmentType());
            if (equipment.lineId() == null) {
                ps.setNull(4, Types.BIGINT);
            } else {
                ps.setLong(4, equipment.lineId());
            }
            ps.setString(5, equipment.equipmentStatus());
            ps.setObject(6, equipment.lastMaintenanceTime());
            ps.setInt(7, Boolean.FALSE.equals(equipment.enabled()) ? 0 : 1);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert equipment failed, no ID obtained.");
            }
        }
    }

    /**
     * 数据访问：按主键查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public Optional<MesEquipment> findById(long id) throws SQLException {
        String sql = "SELECT equipment_id, equipment_code, equipment_name, equipment_type, line_id, equipment_status, last_maintenance_time, enabled FROM mes_equipment WHERE equipment_id = ?";
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
     * 数据访问：查询全部可见记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesEquipment> findAll() throws SQLException {
        String sql = "SELECT equipment_id, equipment_code, equipment_name, equipment_type, line_id, equipment_status, last_maintenance_time, enabled FROM mes_equipment";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<MesEquipment> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;
        }
    }

    /**
     * 数据访问：按业务条件查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesEquipment> findByLineId(long lineId) throws SQLException {
        String sql = "SELECT equipment_id, equipment_code, equipment_name, equipment_type, line_id, equipment_status, last_maintenance_time, enabled FROM mes_equipment WHERE line_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, lineId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MesEquipment> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
                return results;
            }
        }
    }

    /**
     * 数据访问：更新业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public boolean updateStatus(long equipmentId, String newStatus) throws SQLException {
        String sql = "UPDATE mes_equipment SET equipment_status = ?, enabled = ? WHERE equipment_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, "RUNNING".equals(newStatus) ? 1 : 0);
            ps.setLong(3, equipmentId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 数据访问：把 JDBC 结果行映射为领域对象。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private MesEquipment mapRow(ResultSet rs) throws SQLException {
        return new MesEquipment(
                rs.getLong("equipment_id"),
                rs.getString("equipment_code"),
                rs.getString("equipment_name"),
                rs.getString("equipment_type"),
                rs.getObject("line_id") == null ? null : rs.getLong("line_id"),
                rs.getString("equipment_status"),
                rs.getObject("last_maintenance_time", java.time.LocalDateTime.class),
                readEnabled(rs)
        );
    }

    /**
     * 数据访问：执行 readEnabled 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private boolean readEnabled(ResultSet rs) throws SQLException {
        Object value = rs.getObject("enabled");
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() != 0;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }
}
