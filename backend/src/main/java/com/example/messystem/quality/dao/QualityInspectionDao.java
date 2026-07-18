/*
 * 答辩定位：质检、质量追溯与返工 模块的 QualityInspectionDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
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

/**
 * 质检、质量追溯与返工 的 QualityInspectionDao，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class QualityInspectionDao {

    /**
     * 数据访问：写入业务记录并返回主键。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public long insert(MesQualityInspection inspection) throws SQLException {
        String sql = "INSERT INTO mes_quality_inspection (inspection_no, work_order_id, work_report_id, sample_qty, inspection_status, inspector_id, assigned_to, inspection_time, judgement_result) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            setLong(ps, 6, inspection.inspectorId());
            setLong(ps, 7, inspection.assignedTo() == null ? inspection.inspectorId() : inspection.assignedTo());
            ps.setObject(8, inspection.inspectionTime());
            ps.setString(9, inspection.judgementResult());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert quality inspection failed, no ID obtained.");
            }
        }
    }

    /**
     * 数据访问：按主键查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public Optional<MesQualityInspection> findById(long id) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE inspection_id = ?";
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
    public List<MesQualityInspection> findAll() throws SQLException {
        String sql = SELECT_COLUMNS + " ORDER BY inspection_id ASC";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<MesQualityInspection> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    /**
     * 数据访问：查询匹配记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesQualityInspection> findAssignedTo(long userId) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE assigned_to = ? OR inspector_id = ? ORDER BY inspection_id ASC";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MesQualityInspection> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }

    /**
     * 数据访问：分配执行人员或资源。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public boolean assign(long inspectionId, long inspectorId) throws SQLException {
        String sql = """
                update mes_quality_inspection
                set assigned_to = ?, inspector_id = ?, inspection_status = 'CREATED'
                where inspection_id = ? and inspection_status = 'CREATED'
                """;
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, inspectorId);
            ps.setLong(2, inspectorId);
            ps.setLong(3, inspectionId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 数据访问：提交业务事项。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public boolean submit(long inspectionId, long inspectorId, String submittedResult, String resultNote) throws SQLException {
        return submit(inspectionId, inspectorId, submittedResult, resultNote, true);
    }

    /**
     * 数据访问：提交业务事项。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public boolean submit(long inspectionId, long inspectorId, String submittedResult,
            String resultNote, boolean requireAssignment) throws SQLException {
        String sql = """
                update mes_quality_inspection
                set inspection_status = 'SUBMITTED', submitted_by = ?, submitted_at = current_timestamp,
                    submitted_result = ?, result_note = ?,
                    inspection_time = coalesce(inspection_time, current_timestamp)
                where inspection_id = ? %s
                  and inspection_status in ('CREATED','IN_PROGRESS')
                """.formatted(requireAssignment ? "and (assigned_to = ? or inspector_id = ?)" : "");
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, inspectorId);
            ps.setString(2, submittedResult);
            ps.setString(3, resultNote);
            ps.setLong(4, inspectionId);
            if (requireAssignment) {
                ps.setLong(5, inspectorId);
                ps.setLong(6, inspectorId);
            }
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 数据访问：更新业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public boolean updateStatus(long inspectionId, String status, String judgementResult, long reviewedBy) throws SQLException {
        String sql = """
                update mes_quality_inspection
                set inspection_status = ?, judgement_result = ?, reviewed_by = ?, reviewed_at = current_timestamp
                where inspection_id = ? and inspection_status = 'SUBMITTED'
                """;
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, judgementResult);
            ps.setLong(3, reviewedBy);
            ps.setLong(4, inspectionId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 数据访问：查询匹配记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public Optional<TraceContext> findTraceContext(long inspectionId) throws SQLException {
        String sql = """
                select co.order_id, wo.task_id, qi.work_order_id, coalesce(wr.batch_no, wo.batch_no) as batch_no
                from mes_quality_inspection qi
                join mes_work_order wo on wo.work_order_id = qi.work_order_id
                join mes_production_task pt on pt.task_id = wo.task_id
                join mes_customer_order co on co.order_id = pt.order_id
                left join mes_work_report wr on wr.report_id = qi.work_report_id
                where qi.inspection_id = ?
                """;
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, inspectionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new TraceContext(
                            rs.getLong("order_id"),
                            rs.getLong("task_id"),
                            rs.getLong("work_order_id"),
                            rs.getString("batch_no")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 数据访问：执行 ensureApprovedWorkReport 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：把 JDBC 结果行映射为领域对象。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private MesQualityInspection mapRow(ResultSet rs) throws SQLException {
        return new MesQualityInspection(
                rs.getLong("inspection_id"),
                rs.getString("inspection_no"),
                rs.getLong("work_order_id"),
                getLong(rs, "work_report_id"),
                rs.getInt("sample_qty"),
                rs.getString("inspection_status"),
                getLong(rs, "inspector_id"),
                getLong(rs, "assigned_to"),
                rs.getObject("inspection_time", java.time.LocalDateTime.class),
                rs.getString("judgement_result"),
                getLong(rs, "submitted_by"),
                rs.getObject("submitted_at", java.time.LocalDateTime.class),
                rs.getString("submitted_result"),
                rs.getString("result_note"),
                getLong(rs, "reviewed_by"),
                rs.getObject("reviewed_at", java.time.LocalDateTime.class)
        );
    }

    private static final String SELECT_COLUMNS = """
            SELECT inspection_id, inspection_no, work_order_id, work_report_id, sample_qty,
                   inspection_status, inspector_id, assigned_to, inspection_time, judgement_result,
                   submitted_by, submitted_at, submitted_result, result_note, reviewed_by, reviewed_at
            FROM mes_quality_inspection
            """;

    /**
     * 数据访问：查询单条记录或详情。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * 数据访问：执行 setLong 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static void setLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) statement.setNull(index, java.sql.Types.BIGINT);
        else statement.setLong(index, value);
    }

    /**
     * 数据访问：执行 TraceContext 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record TraceContext(Long orderId, Long taskId, Long workOrderId, String batchNo) {
    }
}
