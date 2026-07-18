/*
 * 答辩定位：生产报工与计件工资 模块的 ProductionDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.production.dao;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.production.entity.MesPieceworkWage;
import com.example.messystem.production.entity.MesWorkReport;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 生产报工与计件工资 的 ProductionDao，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class ProductionDao {
    private static final BigDecimal DEFAULT_PIECE_RATE = new BigDecimal("2.50");

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesWorkReport> listWorkReports() throws SQLException {
        String sql = """
                select report_id, report_no, work_order_id, batch_no, operator_id, report_qty,
                       qualified_qty, defect_qty, work_hours, report_time, report_status, remark, reject_reason
                from mes_work_report
                order by report_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesWorkReport> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapWorkReport(rs));
            }
            return rows;
        }
    }

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesWorkReport> listWorkReportsByOperator(long operatorId) throws SQLException {
        String sql = """
                select report_id, report_no, work_order_id, batch_no, operator_id, report_qty,
                       qualified_qty, defect_qty, work_hours, report_time, report_status, remark, reject_reason
                from mes_work_report where operator_id = ? order by report_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, operatorId);
            try (ResultSet rs = statement.executeQuery()) {
                List<MesWorkReport> rows = new ArrayList<>();
                while (rs.next()) rows.add(mapWorkReport(rs));
                return rows;
            }
        }
    }

    /**
     * 数据访问：写入业务记录并返回主键。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public MesWorkReport insertWorkReport(MesWorkReport report) throws SQLException {
        return insertWorkReport(report, true);
    }

    /**
     * 数据访问：写入业务记录并返回主键。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public MesWorkReport insertWorkReport(MesWorkReport report, boolean requireOperatorOwnership) throws SQLException {
        String sql = """
                insert into mes_work_report
                    (report_no, work_order_id, batch_no, operator_id, report_qty, qualified_qty,
                     defect_qty, work_hours, report_status, remark)
                values (?, ?, ?, ?, ?, ?, ?, ?, 'SUBMITTED', ?)
                returning report_id, report_no, work_order_id, batch_no, operator_id, report_qty,
                          qualified_qty, defect_qty, work_hours, report_time, report_status, remark, reject_reason
        """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            WorkOrderSnapshot workOrder = findExecutableWorkOrder(connection, report.workOrderId, false);
            long operatorId = report.operatorId == null ? 1L : report.operatorId;
            if (requireOperatorOwnership) validateOperatorOwnsWorkOrder(workOrder, operatorId);
            validateReportQtyAgainstWorkOrder(workOrder, nvl(report.reportQty));
            statement.setString(1, defaultCode(report.reportNo, "WR"));
            statement.setLong(2, report.workOrderId);
            statement.setString(3, defaultText(report.batchNo, workOrder.batchNo()));
            statement.setLong(4, operatorId);
            statement.setInt(5, nvl(report.reportQty));
            statement.setInt(6, nvl(report.qualifiedQty));
            statement.setInt(7, nvl(report.defectQty));
            statement.setBigDecimal(8, report.workHours == null ? BigDecimal.ZERO : report.workHours);
            statement.setString(9, report.remark);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapWorkReport(rs);
            }
        }
    }

    /**
     * 数据访问：查询匹配记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public MesWorkReport findWorkReport(long reportId) throws SQLException {
        String sql = """
                select report_id, report_no, work_order_id, batch_no, operator_id, report_qty,
                       qualified_qty, defect_qty, work_hours, report_time, report_status, remark, reject_reason
                from mes_work_report
                where report_id = ?
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reportId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("work report not found");
                }
                return mapWorkReport(rs);
            }
        }
    }

    /**
     * 数据访问：更新业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public MesWorkReport updateWorkReport(long reportId, MesWorkReport report) throws SQLException {
        return updateWorkReport(reportId, report, true);
    }

    /**
     * 数据访问：更新业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public MesWorkReport updateWorkReport(long reportId, MesWorkReport report,
            boolean requireOperatorOwnership) throws SQLException {
        String sql = """
                update mes_work_report
                set report_no = ?,
                    work_order_id = ?,
                    batch_no = ?,
                    operator_id = ?,
                    report_qty = ?,
                    qualified_qty = ?,
                    defect_qty = ?,
                    work_hours = ?,
                    report_status = ?,
                    remark = ?,
                    reject_reason = case when ? = 'SUBMITTED' then null else reject_reason end
                where report_id = ?
                returning report_id, report_no, work_order_id, batch_no, operator_id, report_qty,
                          qualified_qty, defect_qty, work_hours, report_time, report_status, remark, reject_reason
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            MesWorkReport current = findWorkReport(reportId);
            WorkOrderSnapshot workOrder = findExecutableWorkOrder(
                    connection,
                    report.workOrderId == null ? current.workOrderId : report.workOrderId,
                    false
            );
            long operatorId = report.operatorId == null ? current.operatorId : report.operatorId;
            if (requireOperatorOwnership) validateOperatorOwnsWorkOrder(workOrder, operatorId);
            validateReportQtyAgainstWorkOrder(workOrder, report.reportQty == null ? current.reportQty : report.reportQty);
            statement.setString(1, defaultText(report.reportNo, current.reportNo));
            statement.setLong(2, report.workOrderId == null ? current.workOrderId : report.workOrderId);
            statement.setString(3, defaultText(report.batchNo, current.batchNo));
            statement.setLong(4, operatorId);
            statement.setInt(5, report.reportQty == null ? current.reportQty : report.reportQty);
            statement.setInt(6, report.qualifiedQty == null ? current.qualifiedQty : report.qualifiedQty);
            statement.setInt(7, report.defectQty == null ? current.defectQty : report.defectQty);
            statement.setBigDecimal(8, report.workHours == null ? current.workHours : report.workHours);
            String nextStatus = defaultText(report.reportStatus, current.reportStatus);
            statement.setString(9, nextStatus);
            statement.setString(10, defaultText(report.remark, current.remark));
            statement.setString(11, nextStatus);
            statement.setLong(12, reportId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("work report not found");
                }
                return mapWorkReport(rs);
            }
        }
    }

    /**
     * 数据访问：删除业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public void deleteWorkReport(long reportId) throws SQLException {
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement("delete from mes_work_report where report_id = ?")) {
            statement.setLong(1, reportId);
            if (statement.executeUpdate() == 0) {
                throw new NotFoundException("work report not found");
            }
        }
    }

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesWorkReport> listWorkReportsByWorkOrder(long workOrderId) throws SQLException {
        String sql = """
                select report_id, report_no, work_order_id, batch_no, operator_id, report_qty,
                       qualified_qty, defect_qty, work_hours, report_time, report_status, remark, reject_reason
                from mes_work_report
                where work_order_id = ?
                order by report_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, workOrderId);
            try (ResultSet rs = statement.executeQuery()) {
                List<MesWorkReport> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapWorkReport(rs));
                }
                return rows;
            }
        }
    }

    /**
     * 数据访问：审核通过业务事项。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public MesWorkReport approveWorkReport(long reportId) throws SQLException {
        String updateReportSql = """
                update mes_work_report
                set report_status = 'APPROVED'
                where report_id = ? and report_status = 'SUBMITTED'
                returning report_id, report_no, work_order_id, batch_no, operator_id, report_qty,
                          qualified_qty, defect_qty, work_hours, report_time, report_status, remark, reject_reason
                """;
        String wageSql = """
                insert into mes_piecework_wage
                    (report_id, operator_id, piece_rate, qualified_qty, wage_amount, settlement_status)
                values (?, ?, ?, ?, ?, 'PENDING')
                """;
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                MesWorkReport report;
                try (PreparedStatement statement = connection.prepareStatement(updateReportSql)) {
                    statement.setLong(1, reportId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            ensureReportExistsAndSubmitted(connection, reportId);
                            throw new BadRequestException("only SUBMITTED reports can be approved");
                        }
                        report = mapWorkReport(rs);
                    }
                }
                WorkOrderSnapshot workOrder = findExecutableWorkOrder(connection, report.workOrderId, true);
                validateReportQtyAgainstWorkOrder(workOrder, report.reportQty);
                BigDecimal wageAmount = DEFAULT_PIECE_RATE.multiply(BigDecimal.valueOf(report.qualifiedQty));
                try (PreparedStatement statement = connection.prepareStatement(wageSql)) {
                    statement.setLong(1, report.reportId);
                    statement.setLong(2, report.operatorId);
                    statement.setBigDecimal(3, DEFAULT_PIECE_RATE);
                    statement.setInt(4, report.qualifiedQty);
                    statement.setBigDecimal(5, wageAmount);
                    statement.executeUpdate();
                }
                updateWorkOrderActualQty(connection, report.workOrderId, report.qualifiedQty);
                refreshPlanningCompletion(connection, report.workOrderId);
                connection.commit();
                return report;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /**
     * 数据访问：驳回业务事项。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public MesWorkReport rejectWorkReport(long reportId) throws SQLException {
        return rejectWorkReport(reportId, null);
    }

    /**
     * 数据访问：驳回业务事项。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public MesWorkReport rejectWorkReport(long reportId, String reason) throws SQLException {
        String sql = """
                update mes_work_report
                set report_status = 'REJECTED',
                    reject_reason = ?
                where report_id = ? and report_status = 'SUBMITTED'
                returning report_id, report_no, work_order_id, batch_no, operator_id, report_qty,
                          qualified_qty, defect_qty, work_hours, report_time, report_status, remark, reject_reason
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reason);
            statement.setLong(2, reportId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    ensureReportExistsAndSubmitted(connection, reportId);
                    throw new BadRequestException("only SUBMITTED reports can be rejected");
                }
                return mapWorkReport(rs);
            }
        }
    }

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesPieceworkWage> listWages() throws SQLException {
        String sql = """
                select wage_id, report_id, operator_id, piece_rate, qualified_qty,
                       wage_amount, settlement_status, created_at
                from mes_piecework_wage
                order by wage_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesPieceworkWage> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapWage(rs));
            }
            return rows;
        }
    }

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesWorkReport> listWorkReportsByWorkOrderAndOperator(long workOrderId, long operatorId) throws SQLException {
        String sql = """
                select report_id, report_no, work_order_id, batch_no, operator_id, report_qty,
                       qualified_qty, defect_qty, work_hours, report_time, report_status, remark, reject_reason
                from mes_work_report where work_order_id = ? and operator_id = ? order by report_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, workOrderId);
            statement.setLong(2, operatorId);
            try (ResultSet rs = statement.executeQuery()) {
                List<MesWorkReport> rows = new ArrayList<>();
                while (rs.next()) rows.add(mapWorkReport(rs));
                return rows;
            }
        }
    }

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesPieceworkWage> listWagesByOperator(long operatorId) throws SQLException {
        String sql = """
                select wage_id, report_id, operator_id, piece_rate, qualified_qty,
                       wage_amount, settlement_status, created_at
                from mes_piecework_wage where operator_id = ? order by wage_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, operatorId);
            try (ResultSet rs = statement.executeQuery()) {
                List<MesPieceworkWage> rows = new ArrayList<>();
                while (rs.next()) rows.add(mapWage(rs));
                return rows;
            }
        }
    }

    /**
     * 数据访问：执行 wageSummary 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public java.util.Map<String, Object> wageSummary() throws SQLException {
        String sql = "select count(*) record_count, count(distinct operator_id) operator_count, coalesce(sum(qualified_qty),0) qualified_qty, coalesce(sum(wage_amount),0) wage_amount from mes_piecework_wage";
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            rs.next();
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("recordCount", rs.getLong("record_count"));
            result.put("operatorCount", rs.getLong("operator_count"));
            result.put("qualifiedQty", rs.getLong("qualified_qty"));
            result.put("wageAmount", rs.getBigDecimal("wage_amount"));
            return result;
        }
    }

    /**
     * 数据访问：执行 wageSummaryForWorkshop 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public java.util.Map<String, Object> wageSummaryForWorkshop(long userId) throws SQLException {
        String sql = """
                select count(*) record_count, count(distinct w.operator_id) operator_count,
                       coalesce(sum(w.qualified_qty),0) qualified_qty,
                       coalesce(sum(w.wage_amount),0) wage_amount
                from mes_piecework_wage w
                join mes_work_report r on r.report_id = w.report_id
                join mes_work_order wo on wo.work_order_id = r.work_order_id
                join mes_user_line_scope s on s.line_id = wo.line_id
                where s.user_id = ?
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
                result.put("recordCount", rs.getLong("record_count"));
                result.put("operatorCount", rs.getLong("operator_count"));
                result.put("qualifiedQty", rs.getLong("qualified_qty"));
                result.put("wageAmount", rs.getBigDecimal("wage_amount"));
                return result;
            }
        }
    }

    /**
     * 数据访问：查询匹配记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public MesPieceworkWage findWage(long wageId) throws SQLException {
        String sql = """
                select wage_id, report_id, operator_id, piece_rate, qualified_qty,
                       wage_amount, settlement_status, created_at
                from mes_piecework_wage
                where wage_id = ?
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, wageId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("piecework wage not found");
                }
                return mapWage(rs);
            }
        }
    }

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesPieceworkWage> listWagesByReport(long reportId) throws SQLException {
        String sql = """
                select wage_id, report_id, operator_id, piece_rate, qualified_qty,
                       wage_amount, settlement_status, created_at
                from mes_piecework_wage
                where report_id = ?
                order by wage_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reportId);
            try (ResultSet rs = statement.executeQuery()) {
                List<MesPieceworkWage> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapWage(rs));
                }
                return rows;
            }
        }
    }

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesPieceworkWage> listWagesByReportAndOperator(long reportId, long operatorId) throws SQLException {
        String sql = """
                select wage_id, report_id, operator_id, piece_rate, qualified_qty,
                       wage_amount, settlement_status, created_at
                from mes_piecework_wage where report_id = ? and operator_id = ? order by wage_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reportId);
            statement.setLong(2, operatorId);
            try (ResultSet rs = statement.executeQuery()) {
                List<MesPieceworkWage> rows = new ArrayList<>();
                while (rs.next()) rows.add(mapWage(rs));
                return rows;
            }
        }
    }

    /**
     * 数据访问：执行 ensureReportExistsAndSubmitted 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private void ensureReportExistsAndSubmitted(Connection connection, long reportId) throws SQLException {
        String sql = "select report_status from mes_work_report where report_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reportId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("work report not found");
                }
            }
        }
    }

    /**
     * 数据访问：查询匹配记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private WorkOrderSnapshot findExecutableWorkOrder(Connection connection, long workOrderId, boolean forUpdate) throws SQLException {
        String sql = """
                select work_order_status, planned_qty, actual_qty, batch_no, assigned_to, accepted_by
                from mes_work_order
                where work_order_id = ?
                """ + (forUpdate ? " for update" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, workOrderId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("work order not found");
                }
                String status = rs.getString("work_order_status");
                if (!"DISPATCHED".equals(status) && !"RECEIVED".equals(status) && !"RUNNING".equals(status)) {
                    throw new BadRequestException("work order status does not allow work report: " + status);
                }
                return new WorkOrderSnapshot(
                        status,
                        rs.getInt("planned_qty"),
                        rs.getInt("actual_qty"),
                        rs.getString("batch_no"),
                        nullableLong(rs, "assigned_to"),
                        nullableLong(rs, "accepted_by")
                );
            }
        }
    }

    /**
     * 数据访问：校验业务输入与约束。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private void validateOperatorOwnsWorkOrder(WorkOrderSnapshot workOrder, long operatorId) {
        boolean assigned = workOrder.assignedTo() != null && workOrder.assignedTo() == operatorId;
        boolean accepted = workOrder.acceptedBy() != null && workOrder.acceptedBy() == operatorId;
        if (!assigned && !accepted) {
            throw new BadRequestException("只能提交本人被派或已接收工单的报工");
        }
    }

    /**
     * 数据访问：校验业务输入与约束。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private void validateReportQtyAgainstWorkOrder(WorkOrderSnapshot workOrder, int reportQty) {
        int maxAllowed = (int) Math.floor(workOrder.plannedQty() * 1.1);
        if (workOrder.actualQty() + reportQty > maxAllowed) {
            throw new BadRequestException("reported quantity exceeds work order planned quantity by more than 10%");
        }
    }

    /**
     * 数据访问：更新业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private void updateWorkOrderActualQty(Connection connection, long workOrderId, int qualifiedQty) throws SQLException {
        String sql = """
                update mes_work_order
                set actual_qty = actual_qty + ?,
                    work_order_status = case
                        when actual_qty + ? >= planned_qty then 'FINISHED'
                        else 'RUNNING'
                    end,
                    completed_time = case
                        when actual_qty + ? >= planned_qty then current_timestamp
                        else completed_time
                    end,
                    updated_at = current_timestamp
                where work_order_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, qualifiedQty);
            statement.setInt(2, qualifiedQty);
            statement.setInt(3, qualifiedQty);
            statement.setLong(4, workOrderId);
            statement.executeUpdate();
        }
    }

    /**
     * 数据访问：执行 refreshPlanningCompletion 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private void refreshPlanningCompletion(Connection connection, long workOrderId) throws SQLException {
        String taskSql = """
                update mes_production_task t
                set task_status = 'COMPLETED',
                    close_time = coalesce(close_time, current_timestamp),
                    updated_at = current_timestamp
                where t.task_id = (select wo.task_id from mes_work_order wo where wo.work_order_id = ?)
                  and exists (select 1 from mes_work_order wo where wo.task_id = t.task_id)
                  and not exists (
                      select 1 from mes_work_order wo
                      where wo.task_id = t.task_id
                        and wo.work_order_status not in ('FINISHED', 'COMPLETED', 'CLOSED')
                  )
                """;
        try (PreparedStatement statement = connection.prepareStatement(taskSql)) {
            statement.setLong(1, workOrderId);
            statement.executeUpdate();
        }

        String orderSql = """
                update mes_customer_order o
                set order_status = case
                        when exists (select 1 from mes_production_task t where t.order_id = o.order_id)
                         and not exists (
                             select 1 from mes_production_task t
                             where t.order_id = o.order_id and t.task_status <> 'COMPLETED'
                         ) then 'COMPLETED'
                        when exists (
                            select 1
                            from mes_production_task t
                            join mes_work_order wo on wo.task_id = t.task_id
                            where t.order_id = o.order_id
                              and (coalesce(wo.actual_qty, 0) > 0
                                   or wo.work_order_status in ('RUNNING', 'IN_PROGRESS', 'FINISHED', 'COMPLETED', 'CLOSED'))
                        ) then 'IN_PROGRESS'
                        else 'PLANNED'
                    end,
                    updated_at = current_timestamp
                where o.order_id = (
                    select t.order_id
                    from mes_work_order wo
                    join mes_production_task t on t.task_id = wo.task_id
                    where wo.work_order_id = ?
                )
                """;
        try (PreparedStatement statement = connection.prepareStatement(orderSql)) {
            statement.setLong(1, workOrderId);
            statement.executeUpdate();
        }
    }

    /**
     * 数据访问：把 JDBC 结果行映射为领域对象。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private MesWorkReport mapWorkReport(ResultSet rs) throws SQLException {
        MesWorkReport item = new MesWorkReport();
        item.reportId = rs.getLong("report_id");
        item.reportNo = rs.getString("report_no");
        item.workOrderId = rs.getLong("work_order_id");
        item.batchNo = rs.getString("batch_no");
        item.operatorId = rs.getLong("operator_id");
        item.reportQty = rs.getInt("report_qty");
        item.qualifiedQty = rs.getInt("qualified_qty");
        item.defectQty = rs.getInt("defect_qty");
        item.workHours = rs.getBigDecimal("work_hours");
        item.reportTime = getLocalDateTime(rs, "report_time");
        item.reportStatus = rs.getString("report_status");
        item.remark = rs.getString("remark");
        item.rejectReason = rs.getString("reject_reason");
        return item;
    }

    /**
     * 数据访问：执行 nullableLong 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * 数据访问：把 JDBC 结果行映射为领域对象。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private MesPieceworkWage mapWage(ResultSet rs) throws SQLException {
        MesPieceworkWage item = new MesPieceworkWage();
        item.wageId = rs.getLong("wage_id");
        item.reportId = rs.getLong("report_id");
        item.operatorId = rs.getLong("operator_id");
        item.pieceRate = rs.getBigDecimal("piece_rate");
        item.qualifiedQty = rs.getInt("qualified_qty");
        item.wageAmount = rs.getBigDecimal("wage_amount");
        item.settlementStatus = rs.getString("settlement_status");
        item.createdAt = getLocalDateTime(rs, "created_at");
        return item;
    }

    /**
     * 数据访问：执行 nvl 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 数据访问：执行 defaultCode 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static String defaultCode(String value, String prefix) {
        return value == null || value.isBlank() ? IdGenerator.nextCode(prefix) : value;
    }

    /**
     * 数据访问：执行 defaultText 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 数据访问：查询单条记录或详情。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    /**
     * 数据访问：执行 WorkOrderSnapshot 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private record WorkOrderSnapshot(String status, int plannedQty, int actualQty, String batchNo,
                                     Long assignedTo, Long acceptedBy) {
    }
}
