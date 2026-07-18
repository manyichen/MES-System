/*
 * 答辩定位：质检、质量追溯与返工 模块的 QualityInspectionItemDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.quality.dao;

import com.example.messystem.common.Db;
import com.example.messystem.quality.entity.MesQualityInspectionItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 质检、质量追溯与返工 的 QualityInspectionItemDao，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class QualityInspectionItemDao {

    /**
     * 数据访问：写入业务记录并返回主键。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：按业务条件查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：把 JDBC 结果行映射为领域对象。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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
