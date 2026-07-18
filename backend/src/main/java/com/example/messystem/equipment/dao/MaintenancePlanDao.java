/*
 * 答辩定位：设备与维修保养 模块的 MaintenancePlanDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.equipment.dao;

import com.example.messystem.common.Db;
import com.example.messystem.equipment.entity.MesMaintenancePlan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 设备与维修保养 的 MaintenancePlanDao，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class MaintenancePlanDao {

    /**
     * 数据访问：写入业务记录并返回主键。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public long insert(MesMaintenancePlan plan) throws SQLException {
        String sql = "INSERT INTO mes_maintenance_plan (equipment_id, plan_cycle, next_plan_time, plan_status, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, plan.equipmentId());
            ps.setString(2, plan.planCycle());
            ps.setObject(3, plan.nextPlanTime());
            ps.setString(4, plan.planStatus());
            ps.setObject(5, plan.createdAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert maintenance plan failed, no ID obtained.");
            }
        }
    }

    /**
     * 数据访问：按主键查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public Optional<MesMaintenancePlan> findById(long id) throws SQLException {
        String sql = "SELECT maintenance_plan_id, equipment_id, plan_cycle, next_plan_time, plan_status, created_at FROM mes_maintenance_plan WHERE maintenance_plan_id = ?";
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
    public List<MesMaintenancePlan> findByEquipmentId(long equipmentId) throws SQLException {
        String sql = "SELECT maintenance_plan_id, equipment_id, plan_cycle, next_plan_time, plan_status, created_at FROM mes_maintenance_plan WHERE equipment_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, equipmentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MesMaintenancePlan> results = new ArrayList<>();
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
    public List<MesMaintenancePlan> findAll() throws SQLException {
        String sql = "SELECT maintenance_plan_id, equipment_id, plan_cycle, next_plan_time, plan_status, created_at FROM mes_maintenance_plan";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<MesMaintenancePlan> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;
        }
    }

    /**
     * 数据访问：把 JDBC 结果行映射为领域对象。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private MesMaintenancePlan mapRow(ResultSet rs) throws SQLException {
        return new MesMaintenancePlan(
                rs.getLong("maintenance_plan_id"),
                rs.getLong("equipment_id"),
                rs.getString("plan_cycle"),
                rs.getObject("next_plan_time", java.time.LocalDateTime.class),
                rs.getString("plan_status"),
                rs.getObject("created_at", java.time.LocalDateTime.class)
        );
    }
}
