/*
 * 答辩定位：质检、质量追溯与返工 模块的 ReworkOrderDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.quality.dao;

import com.example.messystem.common.Db;
import com.example.messystem.quality.entity.MesReworkOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 质检、质量追溯与返工 的 ReworkOrderDao，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class ReworkOrderDao {

    /**
     * 数据访问：写入业务记录并返回主键。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public long insert(MesReworkOrder order) throws SQLException {
        String sql = "INSERT INTO mes_rework_order (rework_order_no, source_work_order_id, inspection_id, rework_reason, rework_status, assigned_line_id, created_at, closed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, order.reworkOrderNo());
            if (order.sourceWorkOrderId() == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, order.sourceWorkOrderId());
            }
            if (order.inspectionId() == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, order.inspectionId());
            }
            ps.setString(4, order.reworkReason());
            ps.setString(5, order.reworkStatus());
            if (order.assignedLineId() == null) {
                ps.setNull(6, java.sql.Types.BIGINT);
            } else {
                ps.setLong(6, order.assignedLineId());
            }
            ps.setObject(7, order.createdAt());
            ps.setObject(8, order.closedAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert rework order failed, no ID obtained.");
            }
        }
    }

    /**
     * 数据访问：按业务条件查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesReworkOrder> findByInspectionId(long inspectionId) throws SQLException {
        String sql = "SELECT rework_order_id, rework_order_no, source_work_order_id, inspection_id, rework_reason, rework_status, assigned_line_id, created_at, closed_at FROM mes_rework_order WHERE inspection_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, inspectionId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MesReworkOrder> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    /**
     * 数据访问：按主键查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public Optional<MesReworkOrder> findById(long id) throws SQLException {
        String sql = "SELECT rework_order_id, rework_order_no, source_work_order_id, inspection_id, rework_reason, rework_status, assigned_line_id, created_at, closed_at FROM mes_rework_order WHERE rework_order_id = ?";
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
    public List<MesReworkOrder> findAll() throws SQLException {
        String sql = "SELECT rework_order_id, rework_order_no, source_work_order_id, inspection_id, rework_reason, rework_status, assigned_line_id, created_at, closed_at FROM mes_rework_order";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<MesReworkOrder> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    /**
     * 数据访问：更新业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public boolean updateStatus(long id, String status) throws SQLException {
        String sql = "UPDATE mes_rework_order SET rework_status = ?, closed_at = CASE WHEN ? IN ('FINISHED', 'CLOSED') THEN NOW() ELSE closed_at END WHERE rework_order_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setLong(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 数据访问：更新业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public boolean updateStatus(long id, String status, String expectedStatus) throws SQLException {
        String sql = "UPDATE mes_rework_order SET rework_status = ?, closed_at = CASE WHEN ? = 'FINISHED' THEN NOW() ELSE closed_at END WHERE rework_order_id = ? AND rework_status = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setLong(3, id);
            ps.setString(4, expectedStatus);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 数据访问：把 JDBC 结果行映射为领域对象。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private MesReworkOrder mapRow(ResultSet rs) throws SQLException {
        return new MesReworkOrder(
                rs.getLong("rework_order_id"),
                rs.getString("rework_order_no"),
                rs.getLong("source_work_order_id"),
                rs.getObject("inspection_id") == null ? null : rs.getLong("inspection_id"),
                rs.getString("rework_reason"),
                rs.getString("rework_status"),
                rs.getObject("assigned_line_id") == null ? null : rs.getLong("assigned_line_id"),
                rs.getObject("created_at", java.time.LocalDateTime.class),
                rs.getObject("closed_at", java.time.LocalDateTime.class)
        );
    }
}
