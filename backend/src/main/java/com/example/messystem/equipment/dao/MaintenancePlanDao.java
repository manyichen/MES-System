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

public class MaintenancePlanDao {

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
