package com.example.messystem.quality.dao;

import com.example.messystem.common.Db;
import com.example.messystem.common.BadRequestException;
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
        String sql = "INSERT INTO mes_quality_inspection (inspection_no, work_order_id, work_report_id, sample_qty, inspection_status, inspector_id, inspection_time, judgement_result) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ensureApprovedWorkReport(conn, inspection.workOrderId(), inspection.workReportId());
            ps.setString(1, inspection.inspectionNo());
            ps.setLong(2, inspection.workOrderId());
            if (inspection.workReportId() == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, inspection.workReportId());
            }
            ps.setInt(4, inspection.sampleQty());
            ps.setString(5, inspection.inspectionStatus());
            ps.setLong(6, inspection.inspectorId());
            ps.setObject(7, inspection.inspectionTime());
            ps.setString(8, inspection.judgementResult());
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
        String sql = "SELECT inspection_id, inspection_no, work_order_id, work_report_id, sample_qty, inspection_status, inspector_id, inspection_time, judgement_result FROM mes_quality_inspection WHERE inspection_id = ?";
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
        String sql = "SELECT inspection_id, inspection_no, work_order_id, work_report_id, sample_qty, inspection_status, inspector_id, inspection_time, judgement_result FROM mes_quality_inspection";
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

    private void ensureApprovedWorkReport(Connection conn, Long workOrderId, Long workReportId) throws SQLException {
        if (workReportId == null) {
            return;
        }
        String sql = "SELECT work_order_id, report_status FROM mes_work_report WHERE report_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, workReportId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new BadRequestException("work report not found");
                }
                if (workOrderId == null || rs.getLong("work_order_id") != workOrderId) {
                    throw new BadRequestException("work report does not belong to this work order");
                }
                if (!"APPROVED".equals(rs.getString("report_status"))) {
                    throw new BadRequestException("only APPROVED work reports can create quality inspections");
                }
            }
        }
    }

    private MesQualityInspection mapRow(ResultSet rs) throws SQLException {
        return new MesQualityInspection(
                rs.getLong("inspection_id"),
                rs.getString("inspection_no"),
                rs.getLong("work_order_id"),
                getLong(rs, "work_report_id"),
                rs.getInt("sample_qty"),
                rs.getString("inspection_status"),
                rs.getLong("inspector_id"),
                rs.getObject("inspection_time", java.time.LocalDateTime.class),
                rs.getString("judgement_result")
        );
    }

    private Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
