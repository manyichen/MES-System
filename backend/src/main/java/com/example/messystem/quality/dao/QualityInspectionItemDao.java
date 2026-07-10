package com.example.messystem.quality.dao;

import com.example.messystem.common.Db;
import com.example.messystem.quality.entity.MesQualityInspectionItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QualityInspectionItemDao {

    public long insert(MesQualityInspectionItem item) throws SQLException {
        String sql = "INSERT INTO mes_quality_inspection_item (inspection_id, item_code, item_name, standard_value, actual_value, item_result, remark) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, item.inspectionId());
            ps.setString(2, item.itemCode());
            ps.setString(3, item.itemName());
            ps.setString(4, item.standardValue());
            ps.setString(5, item.actualValue());
            ps.setString(6, item.itemResult());
            ps.setString(7, item.remark());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert quality inspection item failed, no ID obtained.");
            }
        }
    }

    public List<MesQualityInspectionItem> findByInspectionId(long inspectionId) throws SQLException {
        String sql = "SELECT inspection_item_id, inspection_id, item_code, item_name, standard_value, actual_value, item_result, remark FROM mes_quality_inspection_item WHERE inspection_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, inspectionId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MesQualityInspectionItem> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    private MesQualityInspectionItem mapRow(ResultSet rs) throws SQLException {
        return new MesQualityInspectionItem(
                rs.getLong("inspection_item_id"),
                rs.getLong("inspection_id"),
                rs.getString("item_code"),
                rs.getString("item_name"),
                rs.getString("standard_value"),
                rs.getString("actual_value"),
                rs.getString("item_result"),
                rs.getString("remark")
        );
    }
}
