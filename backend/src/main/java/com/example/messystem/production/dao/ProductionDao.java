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

public class ProductionDao {
    private static final BigDecimal DEFAULT_PIECE_RATE = new BigDecimal("2.50");

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

    public MesWorkReport insertWorkReport(MesWorkReport report) throws SQLException {
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
            validateOperatorOwnsWorkOrder(workOrder, operatorId);
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

    public MesWorkReport updateWorkReport(long reportId, MesWorkReport report) throws SQLException {
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
            validateOperatorOwnsWorkOrder(workOrder, operatorId);
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

    public void deleteWorkReport(long reportId) throws SQLException {
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement("delete from mes_work_report where report_id = ?")) {
            statement.setLong(1, reportId);
            if (statement.executeUpdate() == 0) {
                throw new NotFoundException("work report not found");
            }
        }
    }

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

    public MesWorkReport rejectWorkReport(long reportId) throws SQLException {
        return rejectWorkReport(reportId, null);
    }

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

    private void validateOperatorOwnsWorkOrder(WorkOrderSnapshot workOrder, long operatorId) {
        boolean assigned = workOrder.assignedTo() != null && workOrder.assignedTo() == operatorId;
        boolean accepted = workOrder.acceptedBy() != null && workOrder.acceptedBy() == operatorId;
        if (!assigned && !accepted) {
            throw new BadRequestException("只能提交本人被派或已接收工单的报工");
        }
    }

    private void validateReportQtyAgainstWorkOrder(WorkOrderSnapshot workOrder, int reportQty) {
        int maxAllowed = (int) Math.floor(workOrder.plannedQty() * 1.1);
        if (workOrder.actualQty() + reportQty > maxAllowed) {
            throw new BadRequestException("reported quantity exceeds work order planned quantity by more than 10%");
        }
    }

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

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

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

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private static String defaultCode(String value, String prefix) {
        return value == null || value.isBlank() ? IdGenerator.nextCode(prefix) : value;
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private record WorkOrderSnapshot(String status, int plannedQty, int actualQty, String batchNo,
                                     Long assignedTo, Long acceptedBy) {
    }
}
