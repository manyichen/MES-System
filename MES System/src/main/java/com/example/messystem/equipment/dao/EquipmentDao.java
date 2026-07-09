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

public class EquipmentDao {

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
            ps.setObject(7, equipment.enabled());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert equipment failed, no ID obtained.");
            }
        }
    }

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

    public boolean updateStatus(long equipmentId, String newStatus) throws SQLException {
        String sql = "UPDATE mes_equipment SET equipment_status = ? WHERE equipment_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setLong(2, equipmentId);
            return ps.executeUpdate() > 0;
        }
    }

    private MesEquipment mapRow(ResultSet rs) throws SQLException {
        return new MesEquipment(
                rs.getLong("equipment_id"),
                rs.getString("equipment_code"),
                rs.getString("equipment_name"),
                rs.getString("equipment_type"),
                rs.getObject("line_id") == null ? null : rs.getLong("line_id"),
                rs.getString("equipment_status"),
                rs.getObject("last_maintenance_time", java.time.LocalDateTime.class),
                rs.getBoolean("enabled")
        );
    }
}
