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
                select report_id, report_no, work_order_id, operator_id, report_qty,
                       qualified_qty, defect_qty, work_hours, report_time, report_status
                from mes_work_report
                order by report_id desc
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

    public MesWorkReport insertWorkReport(MesWorkReport report) throws SQLException {
        String sql = """
                insert into mes_work_report
                    (report_no, work_order_id, operator_id, report_qty, qualified_qty,
                     defect_qty, work_hours, report_status)
                values (?, ?, ?, ?, ?, ?, ?, 'SUBMITTED')
                returning report_id, report_no, work_order_id, operator_id, report_qty,
                          qualified_qty, defect_qty, work_hours, report_time, report_status
        """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, defaultCode(report.reportNo, "WR"));
            statement.setLong(2, report.workOrderId);
            statement.setLong(3, report.operatorId == null ? 1L : report.operatorId);
            statement.setInt(4, nvl(report.reportQty));
            statement.setInt(5, nvl(report.qualifiedQty));
            statement.setInt(6, nvl(report.defectQty));
            statement.setBigDecimal(7, report.workHours == null ? BigDecimal.ZERO : report.workHours);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapWorkReport(rs);
            }
        }
    }

    public MesWorkReport findWorkReport(long reportId) throws SQLException {
        String sql = """
                select report_id, report_no, work_order_id, operator_id, report_qty,
                       qualified_qty, defect_qty, work_hours, report_time, report_status
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

    public List<MesWorkReport> listWorkReportsByWorkOrder(long workOrderId) throws SQLException {
        String sql = """
                select report_id, report_no, work_order_id, operator_id, report_qty,
                       qualified_qty, defect_qty, work_hours, report_time, report_status
                from mes_work_report
                where work_order_id = ?
                order by report_id desc
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
                returning report_id, report_no, work_order_id, operator_id, report_qty,
                          qualified_qty, defect_qty, work_hours, report_time, report_status
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
                BigDecimal wageAmount = DEFAULT_PIECE_RATE.multiply(BigDecimal.valueOf(report.qualifiedQty));
                try (PreparedStatement statement = connection.prepareStatement(wageSql)) {
                    statement.setLong(1, report.reportId);
                    statement.setLong(2, report.operatorId);
                    statement.setBigDecimal(3, DEFAULT_PIECE_RATE);
                    statement.setInt(4, report.qualifiedQty);
                    statement.setBigDecimal(5, wageAmount);
                    statement.executeUpdate();
                }
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

    public List<MesPieceworkWage> listWages() throws SQLException {
        String sql = """
                select wage_id, report_id, operator_id, piece_rate, qualified_qty,
                       wage_amount, settlement_status, created_at
                from mes_piecework_wage
                order by wage_id desc
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

    private MesWorkReport mapWorkReport(ResultSet rs) throws SQLException {
        MesWorkReport item = new MesWorkReport();
        item.reportId = rs.getLong("report_id");
        item.reportNo = rs.getString("report_no");
        item.workOrderId = rs.getLong("work_order_id");
        item.operatorId = rs.getLong("operator_id");
        item.reportQty = rs.getInt("report_qty");
        item.qualifiedQty = rs.getInt("qualified_qty");
        item.defectQty = rs.getInt("defect_qty");
        item.workHours = rs.getBigDecimal("work_hours");
        item.reportTime = getLocalDateTime(rs, "report_time");
        item.reportStatus = rs.getString("report_status");
        return item;
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

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}
