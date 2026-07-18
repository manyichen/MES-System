/*
 * 答辩定位：设备与维修保养 模块的 MaintenanceOrderDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.equipment.dao;

import com.example.messystem.common.Db;
import com.example.messystem.equipment.entity.MesMaintenanceOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 设备与维修保养 的 MaintenanceOrderDao，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class MaintenanceOrderDao {

    /**
     * 数据访问：写入业务记录并返回主键。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public long insert(MesMaintenanceOrder order) throws SQLException {
        String sql = "INSERT INTO mes_maintenance_order (maintenance_order_no, repair_report_id, equipment_id, maintainer_id, maintenance_status, dispatch_time, finish_time, result_desc) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, order.maintenanceOrderNo());
            if (order.repairReportId() == null) {
                ps.setNull(2, Types.BIGINT);
            } else {
                ps.setLong(2, order.repairReportId());
            }
            if (order.equipmentId() == null) {
                ps.setNull(3, Types.BIGINT);
            } else {
                ps.setLong(3, order.equipmentId());
            }
            if (order.maintainerId() == null) {
                ps.setNull(4, Types.BIGINT);
            } else {
                ps.setLong(4, order.maintainerId());
            }
            ps.setString(5, order.maintenanceStatus());
            ps.setObject(6, order.dispatchTime());
            ps.setObject(7, order.finishTime());
            ps.setString(8, order.resultDesc());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert maintenance order failed, no ID obtained.");
            }
        }
    }

    /**
     * 数据访问：按主键查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public Optional<MesMaintenanceOrder> findById(long id) throws SQLException {
        String sql = "SELECT maintenance_order_id, maintenance_order_no, repair_report_id, equipment_id, maintainer_id, maintenance_status, dispatch_time, finish_time, result_desc FROM mes_maintenance_order WHERE maintenance_order_id = ?";
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
    public List<MesMaintenanceOrder> findByRepairReportId(long repairReportId) throws SQLException {
        String sql = "SELECT maintenance_order_id, maintenance_order_no, repair_report_id, equipment_id, maintainer_id, maintenance_status, dispatch_time, finish_time, result_desc FROM mes_maintenance_order WHERE repair_report_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, repairReportId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MesMaintenanceOrder> results = new ArrayList<>();
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
    public List<MesMaintenanceOrder> findAll() throws SQLException {
        String sql = "SELECT maintenance_order_id, maintenance_order_no, repair_report_id, equipment_id, maintainer_id, maintenance_status, dispatch_time, finish_time, result_desc FROM mes_maintenance_order";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<MesMaintenanceOrder> results = new ArrayList<>();
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
    public List<MesMaintenanceOrder> findByMaintainer(long userId) throws SQLException {
        String sql = "SELECT maintenance_order_id, maintenance_order_no, repair_report_id, equipment_id, maintainer_id, maintenance_status, dispatch_time, finish_time, result_desc FROM mes_maintenance_order WHERE maintainer_id = ? ORDER BY maintenance_order_id ASC";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MesMaintenanceOrder> results = new ArrayList<>();
                while (rs.next()) results.add(mapRow(rs));
                return results;
            }
        }
    }

    /**
     * 数据访问：分配执行人员或资源。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public boolean assign(long id, long maintainerId) throws SQLException {
        String sql = "UPDATE mes_maintenance_order SET maintainer_id = ?, maintenance_status = 'ASSIGNED', dispatch_time = current_timestamp WHERE maintenance_order_id = ? AND maintenance_status = 'CREATED'";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, maintainerId);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 数据访问：完成业务任务。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public boolean finishOwn(long id, long maintainerId, String resultDesc) throws SQLException {
        String sql = """
                UPDATE mes_maintenance_order
                SET maintenance_status = 'FINISHED',
                    finish_time = current_timestamp,
                    result_desc = COALESCE(NULLIF(?, ''), result_desc)
                WHERE maintenance_order_id = ?
                  AND maintainer_id = ?
                  AND maintenance_status IN ('ASSIGNED','IN_PROGRESS')
                """;
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resultDesc);
            ps.setLong(2, id);
            ps.setLong(3, maintainerId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 数据访问：完成业务任务。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public boolean finishAny(long id, String resultDesc) throws SQLException {
        String sql = """
                UPDATE mes_maintenance_order
                SET maintenance_status = 'FINISHED',
                    finish_time = current_timestamp,
                    result_desc = COALESCE(NULLIF(?, ''), result_desc)
                WHERE maintenance_order_id = ?
                  AND maintenance_status IN ('ASSIGNED','IN_PROGRESS')
                """;
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resultDesc);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 数据访问：更新业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public boolean updateStatus(long id, String status) throws SQLException {
        String sql = "UPDATE mes_maintenance_order SET maintenance_status = ?, finish_time = CASE WHEN ? IN ('FINISHED', 'ACCEPTED') THEN NOW() ELSE finish_time END WHERE maintenance_order_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setLong(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 数据访问：受理业务事项。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public boolean acceptFinished(long id) throws SQLException {
        String sql = "UPDATE mes_maintenance_order SET maintenance_status = 'ACCEPTED' WHERE maintenance_order_id = ? AND maintenance_status = 'FINISHED'";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 数据访问：把 JDBC 结果行映射为领域对象。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private MesMaintenanceOrder mapRow(ResultSet rs) throws SQLException {
        return new MesMaintenanceOrder(
                rs.getLong("maintenance_order_id"),
                rs.getString("maintenance_order_no"),
                rs.getObject("repair_report_id") == null ? null : rs.getLong("repair_report_id"),
                rs.getObject("equipment_id") == null ? null : rs.getLong("equipment_id"),
                rs.getObject("maintainer_id") == null ? null : rs.getLong("maintainer_id"),
                rs.getString("maintenance_status"),
                rs.getObject("dispatch_time", java.time.LocalDateTime.class),
                rs.getObject("finish_time", java.time.LocalDateTime.class),
                rs.getString("result_desc")
        );
    }
}
