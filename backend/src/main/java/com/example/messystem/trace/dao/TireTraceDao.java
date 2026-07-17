package com.example.messystem.trace.dao;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.trace.entity.TireGenerationContext;
import com.example.messystem.trace.entity.TireTraceItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TireTraceDao {
    private static final String SELECT_ITEM = """
            select ti.tire_id, ti.serial_no, ti.trace_code, ti.work_order_id, wo.work_order_no,
                   ti.inspection_id, qi.inspection_no, ti.work_report_id, ti.product_id,
                   p.product_code, p.product_name, p.product_model, pl.line_name as production_line,
                   ti.warehouse_id, w.warehouse_name, ti.location_id, wl.location_name,
                   ti.batch_no, ti.tire_status, ti.qualified_at, ti.inbound_at,
                   qr.access_token, qr.target_url, coalesce(qr.print_count, 0) as print_count
            from mes_tire_instance ti
            join mes_work_order wo on wo.work_order_id = ti.work_order_id
            join mes_quality_inspection qi on qi.inspection_id = ti.inspection_id
            left join mes_product p on p.product_id = ti.product_id
            left join mes_production_line pl on pl.line_id = wo.line_id
            left join mes_warehouse w on w.warehouse_id = ti.warehouse_id
            left join mes_warehouse_location wl on wl.location_id = ti.location_id
            left join mes_tire_qrcode qr on qr.tire_id = ti.tire_id
            """;

    /** 统一管理批量生成轮胎追溯数据所需的 JDBC 事务。 */
    public <T> T inTransaction(TransactionWork<T> work) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(new TraceTransaction(connection));
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public TireGenerationContext lockGenerationContext(TraceTransaction transaction, long workOrderId,
            long inspectionId, long warehouseId, Long locationId) throws SQLException {
        return lockGenerationContext(transaction.connection, workOrderId, inspectionId, warehouseId, locationId);
    }

    public TireGenerationContext lockGenerationContext(Connection connection, long workOrderId,
            long inspectionId, long warehouseId, Long locationId) throws SQLException {
        String sql = """
                select wo.work_order_id, wo.work_order_no, wo.product_id, wo.batch_no,
                       qi.inspection_id, qi.inspection_no, qi.work_report_id, qi.inspection_status,
                       qi.judgement_result, qi.reviewed_at,
                       p.product_code, p.product_name, p.product_model, pl.line_name,
                       w.warehouse_id, w.warehouse_name,
                       coalesce((select sum(wr.qualified_qty) from mes_work_report wr
                                 where wr.work_order_id = wo.work_order_id and wr.report_status = 'APPROVED'
                                   and (qi.work_report_id is null or wr.report_id = qi.work_report_id)), 0) as qualified_qty,
                       coalesce((select count(*) from mes_tire_instance ti
                                 where ti.work_order_id = wo.work_order_id and ti.tire_status <> 'VOID'
                                   and (qi.work_report_id is null or ti.work_report_id = qi.work_report_id)), 0) as generated_qty
                from mes_quality_inspection qi
                join mes_work_order wo on wo.work_order_id = qi.work_order_id
                left join mes_product p on p.product_id = wo.product_id
                left join mes_production_line pl on pl.line_id = wo.line_id
                join mes_warehouse w on w.warehouse_id = ?
                where wo.work_order_id = ? and qi.inspection_id = ?
                for update of qi, wo
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            statement.setLong(2, workOrderId);
            statement.setLong(3, inspectionId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("生产工单、质检单或入库仓库不存在");
                if (!"APPROVED".equals(rs.getString("inspection_status"))
                        || !"PASS".equals(rs.getString("judgement_result"))) {
                    throw new BadRequestException("只有质检合格并审核通过的生产工单才能生成轮胎二维码");
                }
                Long productId = getLong(rs, "product_id");
                if (productId == null) throw new BadRequestException("生产工单未关联产品，无法生成轮胎二维码");
                Location location = loadLocation(connection, warehouseId, locationId);
                return new TireGenerationContext(
                        rs.getLong("work_order_id"), rs.getString("work_order_no"),
                        rs.getLong("inspection_id"), rs.getString("inspection_no"),
                        getLong(rs, "work_report_id"), productId,
                        rs.getString("product_code"), rs.getString("product_name"), rs.getString("product_model"),
                        rs.getString("line_name"), rs.getLong("warehouse_id"), rs.getString("warehouse_name"),
                        location.id(), location.name(), rs.getString("batch_no"),
                        toLocalDateTime(rs.getTimestamp("reviewed_at")), rs.getInt("qualified_qty"), rs.getInt("generated_qty")
                );
            }
        }
    }

    public TireTraceItem insertTire(Connection connection, TireGenerationContext context, String serialNo,
            String traceCode, long createdBy) throws SQLException {
        String sql = """
                insert into mes_tire_instance
                    (serial_no, trace_code, work_order_id, inspection_id, work_report_id, product_id,
                     warehouse_id, location_id, batch_no, tire_status, qualified_at, inbound_at, created_by)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, 'IN_STOCK', ?, current_timestamp, ?)
                returning tire_id, inbound_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serialNo);
            statement.setString(2, traceCode);
            statement.setLong(3, context.workOrderId());
            statement.setLong(4, context.inspectionId());
            setLong(statement, 5, context.workReportId());
            statement.setLong(6, context.productId());
            statement.setLong(7, context.warehouseId());
            setLong(statement, 8, context.locationId());
            statement.setString(9, context.batchNo());
            statement.setTimestamp(10, context.qualifiedAt() == null ? null : Timestamp.valueOf(context.qualifiedAt()));
            statement.setLong(11, createdBy);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return new TireTraceItem(
                        rs.getLong("tire_id"), serialNo, traceCode, context.workOrderId(), context.workOrderNo(),
                        context.inspectionId(), context.inspectionNo(), context.workReportId(), context.productId(),
                        context.productCode(), context.productName(), context.productModel(), context.productionLine(),
                        context.warehouseId(), context.warehouseName(), context.locationId(), context.locationName(),
                        context.batchNo(), "IN_STOCK", context.qualifiedAt(),
                        toLocalDateTime(rs.getTimestamp("inbound_at")), null, null, 0
                );
            }
        }
    }

    public TireTraceItem insertTire(TraceTransaction transaction, TireGenerationContext context, String serialNo,
            String traceCode, long createdBy) throws SQLException {
        return insertTire(transaction.connection, context, serialNo, traceCode, createdBy);
    }

    public void insertQrCode(Connection connection, long tireId, String token, String targetUrl,
            String storagePath) throws SQLException {
        String sql = """
                insert into mes_tire_qrcode(tire_id, access_token, target_url, storage_path)
                values (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tireId);
            statement.setString(2, token);
            statement.setString(3, targetUrl);
            statement.setString(4, storagePath);
            statement.executeUpdate();
        }
    }

    public void insertQrCode(TraceTransaction transaction, long tireId, String token, String targetUrl,
            String storagePath) throws SQLException {
        insertQrCode(transaction.connection, tireId, token, targetUrl, storagePath);
    }

    public void insertDocument(Connection connection, long tireId, String type, String fileName,
            String storagePath, String hash) throws SQLException {
        String sql = """
                insert into mes_trace_document(tire_id, document_type, file_name, storage_path, file_hash)
                values (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tireId);
            statement.setString(2, type);
            statement.setString(3, fileName);
            statement.setString(4, storagePath);
            statement.setString(5, hash);
            statement.executeUpdate();
        }
    }

    public void insertDocument(TraceTransaction transaction, long tireId, String type, String fileName,
            String storagePath, String hash) throws SQLException {
        insertDocument(transaction.connection, tireId, type, fileName, storagePath, hash);
    }

    public List<TireTraceItem> findAll() throws SQLException {
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ITEM + " order by ti.tire_id desc");
             ResultSet rs = statement.executeQuery()) {
            return mapItems(rs);
        }
    }

    public Optional<TireTraceItem> findById(long tireId) throws SQLException {
        return findOne(SELECT_ITEM + " where ti.tire_id = ?", tireId);
    }

    public Optional<TireTraceItem> findByToken(String token) throws SQLException {
        String sql = SELECT_ITEM + " where qr.access_token = ? and qr.qrcode_status = 'ACTIVE'";
        try (Connection connection = Db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, token);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapItem(rs)) : Optional.empty();
            }
        }
    }

    public String findQrPath(long tireId) throws SQLException {
        return findPath("select storage_path from mes_tire_qrcode where tire_id = ? and qrcode_status = 'ACTIVE'", tireId);
    }

    public String findDocumentPath(long tireId, String documentType) throws SQLException {
        String sql = """
                select storage_path from mes_trace_document
                where tire_id = ? and document_type = ?
                order by version_no desc limit 1
                """;
        try (Connection connection = Db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tireId);
            statement.setString(2, documentType);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("轮胎追溯文件不存在");
                return rs.getString(1);
            }
        }
    }

    public void updateGeneratedFiles(long tireId, String qrPath, String labelPath, String labelHash,
            String pdfPath, String pdfHash) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement("""
                        update mes_tire_qrcode
                        set storage_path = ?, generated_at = current_timestamp
                        where tire_id = ? and qrcode_status = 'ACTIVE'
                        """)) {
                    statement.setString(1, qrPath);
                    statement.setLong(2, tireId);
                    if (statement.executeUpdate() == 0) throw new NotFoundException("轮胎二维码元数据不存在");
                }
                updateDocument(connection, tireId, "LABEL_PNG", "label.png", labelPath, labelHash);
                updateDocument(connection, tireId, "PDF", "product-info.pdf", pdfPath, pdfHash);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void recordPrint(long tireId, long printedBy, String remark) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement insert = connection.prepareStatement(
                    "insert into mes_label_print_task(tire_id, printed_by, remark) values (?, ?, ?)");
                 PreparedStatement update = connection.prepareStatement(
                    "update mes_tire_qrcode set print_count = print_count + 1, last_printed_at = current_timestamp where tire_id = ?")) {
                insert.setLong(1, tireId);
                insert.setLong(2, printedBy);
                insert.setString(3, remark);
                insert.executeUpdate();
                update.setLong(1, tireId);
                if (update.executeUpdate() == 0) throw new NotFoundException("轮胎二维码不存在");
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private Location loadLocation(Connection connection, long warehouseId, Long locationId) throws SQLException {
        if (locationId == null) return new Location(null, null);
        try (PreparedStatement statement = connection.prepareStatement(
                "select location_id, location_name from mes_warehouse_location where location_id = ? and warehouse_id = ?")) {
            statement.setLong(1, locationId);
            statement.setLong(2, warehouseId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new BadRequestException("所选库位不属于目标仓库");
                return new Location(rs.getLong(1), rs.getString(2));
            }
        }
    }

    private Optional<TireTraceItem> findOne(String sql, long id) throws SQLException {
        try (Connection connection = Db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapItem(rs)) : Optional.empty();
            }
        }
    }

    private String findPath(String sql, long tireId) throws SQLException {
        try (Connection connection = Db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tireId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("轮胎二维码文件不存在");
                return rs.getString(1);
            }
        }
    }

    private void updateDocument(Connection connection, long tireId, String type, String fileName,
            String storagePath, String hash) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
                update mes_trace_document
                set file_name = ?, storage_path = ?, file_hash = ?, generated_at = current_timestamp
                where document_id = (
                    select document_id from mes_trace_document
                    where tire_id = ? and document_type = ?
                    order by version_no desc limit 1
                )
                """)) {
            update.setString(1, fileName);
            update.setString(2, storagePath);
            update.setString(3, hash);
            update.setLong(4, tireId);
            update.setString(5, type);
            if (update.executeUpdate() > 0) return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
                insert into mes_trace_document
                    (tire_id, document_type, file_name, storage_path, file_hash, version_no)
                values (?, ?, ?, ?, ?, 1)
                """)) {
            insert.setLong(1, tireId);
            insert.setString(2, type);
            insert.setString(3, fileName);
            insert.setString(4, storagePath);
            insert.setString(5, hash);
            insert.executeUpdate();
        }
    }

    private List<TireTraceItem> mapItems(ResultSet rs) throws SQLException {
        List<TireTraceItem> items = new ArrayList<>();
        while (rs.next()) items.add(mapItem(rs));
        return items;
    }

    private TireTraceItem mapItem(ResultSet rs) throws SQLException {
        return new TireTraceItem(
                rs.getLong("tire_id"), rs.getString("serial_no"), rs.getString("trace_code"),
                rs.getLong("work_order_id"), rs.getString("work_order_no"),
                rs.getLong("inspection_id"), rs.getString("inspection_no"), getLong(rs, "work_report_id"),
                getLong(rs, "product_id"), rs.getString("product_code"), rs.getString("product_name"),
                rs.getString("product_model"), rs.getString("production_line"), getLong(rs, "warehouse_id"),
                rs.getString("warehouse_name"), getLong(rs, "location_id"), rs.getString("location_name"),
                rs.getString("batch_no"), rs.getString("tire_status"),
                toLocalDateTime(rs.getTimestamp("qualified_at")), toLocalDateTime(rs.getTimestamp("inbound_at")),
                rs.getString("access_token"), rs.getString("target_url"), rs.getInt("print_count")
        );
    }

    private static Long getLong(ResultSet rs, String name) throws SQLException {
        long value = rs.getLong(name);
        return rs.wasNull() ? null : value;
    }

    private static void setLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) statement.setNull(index, java.sql.Types.BIGINT);
        else statement.setLong(index, value);
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    public static final class TraceTransaction {
        private final Connection connection;

        private TraceTransaction(Connection connection) {
            this.connection = connection;
        }
    }

    @FunctionalInterface
    public interface TransactionWork<T> {
        T execute(TraceTransaction transaction) throws SQLException;
    }

    private record Location(Long id, String name) { }
}
