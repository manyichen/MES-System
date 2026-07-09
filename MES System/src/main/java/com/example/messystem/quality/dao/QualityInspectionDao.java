package com.example.messystem.quality.dao;

import com.example.messystem.common.Db;
import com.example.messystem.quality.entity.MesQualityInspection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QualityInspectionDao {

    public long insert(MesQualityInspection inspection) throws SQLException {
        String sql = "INSERT INTO mes_quality_inspection (inspection_no, work_order_id, sample_qty, inspection_status, inspector_id, inspection_time, judgement_result) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, inspection.inspectionNo());
            ps.setLong(2, inspection.workOrderId());
            ps.setInt(3, inspection.sampleQty());
            ps.setString(4, inspection.inspectionStatus());
            ps.setLong(5, inspection.inspectorId());
            ps.setObject(6, inspection.inspectionTime());
            ps.setString(7, inspection.judgementResult());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert quality inspection failed, no ID obtained.");
            }
        }
    }

    public Optional<MesQualityInspection> findById(long id) throws SQLException {
        String sql = "SELECT inspection_id, inspection_no, work_order_id, sample_qty, inspection_status, inspector_id, inspection_time, judgement_result FROM mes_quality_inspection WHERE inspection_id = ?";
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

    public List<MesQualityInspection> findAll() throws SQLException {
        String sql = "SELECT inspection_id, inspection_no, work_order_id, sample_qty, inspection_status, inspector_id, inspection_time, judgement_result FROM mes_quality_inspection";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<MesQualityInspection> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    public boolean updateStatus(long inspectionId, String status, String judgementResult) throws SQLException {
        String sql = "UPDATE mes_quality_inspection SET inspection_status = ?, judgement_result = ? WHERE inspection_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, judgementResult);
            ps.setLong(3, inspectionId);
            return ps.executeUpdate() > 0;
        }
    }

    private MesQualityInspection mapRow(ResultSet rs) throws SQLException {
        return new MesQualityInspection(
                rs.getLong("inspection_id"),
                rs.getString("inspection_no"),
                rs.getLong("work_order_id"),
                rs.getInt("sample_qty"),
                rs.getString("inspection_status"),
                rs.getLong("inspector_id"),
                rs.getObject("inspection_time", java.time.LocalDateTime.class),
                rs.getString("judgement_result")
        );
    }
}
