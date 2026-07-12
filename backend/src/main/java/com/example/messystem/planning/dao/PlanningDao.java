package com.example.messystem.planning.dao;

import com.example.messystem.common.Db;
import com.example.messystem.master.entity.MesProcessRoute;
import com.example.messystem.master.entity.MesProduct;
import com.example.messystem.master.entity.MesProductBom;
import com.example.messystem.master.entity.MesProductionLine;
import com.example.messystem.master.entity.MesSyncLog;
import com.example.messystem.master.entity.MesUser;
import com.example.messystem.planning.entity.MesCustomerOrder;
import com.example.messystem.planning.entity.MesKittingAnalysis;
import com.example.messystem.planning.entity.MesKittingShortageItem;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.entity.MesShortageAlert;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlanningDao {
    public List<MesUser> listUsers() throws SQLException {
        String sql = """
                select user_id, username, real_name, role_code, phone, enabled, created_at
                from mes_user
                order by user_id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesUser> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapUser(rs));
            }
            return rows;
        }
    }

    public MesUser insertUser(MesUser user) throws SQLException {
        String sql = """
                insert into mes_user (username, real_name, role_code, phone, enabled)
                values (?, ?, ?, ?, ?)
                returning user_id, username, real_name, role_code, phone, enabled, created_at
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.username);
            statement.setString(2, user.realName);
            statement.setString(3, user.roleCode);
            statement.setString(4, user.phone);
            statement.setInt(5, user.enabled == null ? 1 : user.enabled);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapUser(rs);
            }
        }
    }

    public List<MesProduct> listProducts() throws SQLException {
        String sql = """
                select product_id, product_code, product_name, product_model, enabled
                from mes_product
                order by product_id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesProduct> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapProduct(rs));
            }
            return rows;
        }
    }

    public Optional<MesProduct> findProduct(long productId) throws SQLException {
        String sql = """
                select product_id, product_code, product_name, product_model, enabled
                from mes_product
                where product_id = ?
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapProduct(rs)) : Optional.empty();
            }
        }
    }

    public MesProduct insertProduct(MesProduct product) throws SQLException {
        String sql = """
                insert into mes_product (product_code, product_name, product_model, specification, enabled)
                values (?, ?, ?, ?, ?)
                returning product_id, product_code, product_name, product_model, enabled
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.productCode);
            statement.setString(2, product.productName);
            statement.setString(3, product.productModel);
            statement.setString(4, product.productModel);
            statement.setInt(5, product.enabled == null ? 1 : product.enabled);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapProduct(rs);
            }
        }
    }

    public List<MesProductBom> listBom(long productId) throws SQLException {
        String sql = """
                select b.bom_id, b.product_id, b.material_id, m.material_code, m.material_name,
                       b.usage_qty, b.unit, b.enabled, null::timestamp as created_at
                from mes_product_bom b
                left join mes_material m on m.material_id = b.material_id
                where b.product_id = ?
                order by b.bom_id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            try (ResultSet rs = statement.executeQuery()) {
                List<MesProductBom> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapBom(rs));
                }
                return rows;
            }
        }
    }

    public MesProductBom insertBom(MesProductBom bom) throws SQLException {
        String sql = """
                insert into mes_product_bom (product_id, material_id, usage_qty, unit, enabled)
                values (?, ?, ?, ?, ?)
                returning bom_id, product_id, material_id, null::varchar as material_code,
                          null::varchar as material_name, usage_qty, unit, enabled, null::timestamp as created_at
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bom.productId);
            statement.setLong(2, bom.materialId);
            statement.setBigDecimal(3, bom.qtyPerUnit == null ? BigDecimal.ONE : bom.qtyPerUnit);
            statement.setString(4, bom.unit == null || bom.unit.isBlank() ? "kg" : bom.unit);
            statement.setInt(5, bom.enabled == null ? 1 : bom.enabled);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapBom(rs);
            }
        }
    }

    public List<MesProcessRoute> listProcessRoutes() throws SQLException {
        String sql = """
                select process_id, product_id, process_code, process_name, process_seq,
                       required_equipment_type, enabled, null::timestamp as created_at
                from mes_process_route
                order by process_seq, process_id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesProcessRoute> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapProcessRoute(rs));
            }
            return rows;
        }
    }

    public MesProcessRoute insertProcessRoute(MesProcessRoute route) throws SQLException {
        String sql = """
                insert into mes_process_route
                    (product_id, process_code, process_name, process_seq, required_equipment_type, enabled)
                values (?, ?, ?, ?, ?, ?)
                returning process_id, product_id, process_code, process_name, process_seq,
                          required_equipment_type, enabled, null::timestamp as created_at
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setLong(statement, 1, route.productId);
            statement.setString(2, route.processCode);
            statement.setString(3, route.processName);
            statement.setInt(4, route.processSeq == null ? 1 : route.processSeq);
            statement.setString(5, route.workCenter);
            statement.setInt(6, route.enabled == null ? 1 : route.enabled);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapProcessRoute(rs);
            }
        }
    }

    public List<MesProductionLine> listProductionLines() throws SQLException {
        String sql = """
                select line_id, line_code, line_name, line_type, daily_capacity, line_status, enabled,
                       null::timestamp as created_at
                from mes_production_line
                order by line_id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesProductionLine> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapProductionLine(rs));
            }
            return rows;
        }
    }

    public MesProductionLine insertProductionLine(MesProductionLine line) throws SQLException {
        String sql = """
                insert into mes_production_line
                    (line_code, line_name, line_type, daily_capacity, line_status, enabled)
                values (?, ?, ?, ?, ?, ?)
                returning line_id, line_code, line_name, line_type, daily_capacity, line_status, enabled,
                          null::timestamp as created_at
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, line.lineCode);
            statement.setString(2, line.lineName);
            statement.setString(3, line.lineType);
            setInteger(statement, 4, line.capacityPerDay);
            statement.setString(5, line.lineStatus == null || line.lineStatus.isBlank() ? "IDLE" : line.lineStatus);
            statement.setInt(6, line.enabled == null ? 1 : line.enabled);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapProductionLine(rs);
            }
        }
    }

    public List<MesCustomerOrder> listOrders() throws SQLException {
        String sql = """
                select order_id, order_no, customer_name, product_id, product_code, product_model,
                       order_qty, unit, delivery_date, priority_level, order_status, source_system,
                       remark, created_at, updated_at
                from mes_customer_order
                order by order_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesCustomerOrder> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapOrder(rs));
            }
            return rows;
        }
    }

    public Optional<MesCustomerOrder> findOrder(long orderId) throws SQLException {
        String sql = """
                select order_id, order_no, customer_name, product_id, product_code, product_model,
                       order_qty, unit, delivery_date, priority_level, order_status, source_system,
                       remark, created_at, updated_at
                from mes_customer_order
                where order_id = ?
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapOrder(rs)) : Optional.empty();
            }
        }
    }

    public MesCustomerOrder insertOrder(MesCustomerOrder order) throws SQLException {
        String sql = """
                insert into mes_customer_order
                    (order_no, customer_name, product_id, product_code, product_model, order_qty,
                     unit, delivery_date, priority_level, order_status, source_system, remark)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                returning order_id, order_no, customer_name, product_id, product_code, product_model,
                          order_qty, unit, delivery_date, priority_level, order_status, source_system,
                          remark, created_at, updated_at
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, order.orderNo);
            statement.setString(2, order.customerName);
            statement.setLong(3, order.productId);
            statement.setString(4, order.productCode);
            statement.setString(5, order.productModel);
            statement.setInt(6, order.orderQty == null ? 0 : order.orderQty);
            statement.setString(7, order.unit == null || order.unit.isBlank() ? "条" : order.unit);
            statement.setDate(8, Date.valueOf(order.deliveryDate == null ? LocalDate.now().plusDays(14) : order.deliveryDate));
            statement.setInt(9, order.priorityLevel == null ? 3 : order.priorityLevel);
            statement.setString(10, order.orderStatus);
            statement.setString(11, order.sourceSystem);
            statement.setString(12, order.remark);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapOrder(rs);
            }
        }
    }

    public void updateOrderStatus(long orderId, String status) throws SQLException {
        String sql = "update mes_customer_order set order_status = ?, updated_at = current_timestamp where order_id = ?";
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, orderId);
            statement.executeUpdate();
        }
    }

    public List<MesProductionTask> listTasks() throws SQLException {
        String sql = """
                select task_id, task_no, order_id, planner_id, plan_qty, planned_start_time,
                       planned_end_time, target_line_id, task_status, kitting_status, release_time,
                       close_time, remark, created_at, updated_at,
                       (select o.product_id from mes_customer_order o where o.order_id = t.order_id) as product_id
                from mes_production_task t
                order by task_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesProductionTask> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapTask(rs));
            }
            return rows;
        }
    }

    public Optional<MesProductionTask> findTask(long taskId) throws SQLException {
        String sql = """
                select task_id, task_no, order_id, planner_id, plan_qty, planned_start_time,
                       planned_end_time, target_line_id, task_status, kitting_status, release_time,
                       close_time, remark, created_at, updated_at,
                       (select o.product_id from mes_customer_order o where o.order_id = t.order_id) as product_id
                from mes_production_task t
                where task_id = ?
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, taskId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapTask(rs)) : Optional.empty();
            }
        }
    }

    public MesProductionTask insertTask(MesProductionTask task) throws SQLException {
        String sql = """
                insert into mes_production_task
                    (task_no, order_id, planner_id, plan_qty, planned_start_time, planned_end_time,
                     target_line_id, task_status, kitting_status, remark)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                returning task_id, task_no, order_id, planner_id, plan_qty, planned_start_time,
                          planned_end_time, target_line_id, task_status, kitting_status, release_time,
                          close_time, remark, created_at, updated_at,
                          ?::bigint as product_id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, task.taskNo);
            statement.setLong(2, task.orderId);
            statement.setLong(3, task.plannerId == null ? 1L : task.plannerId);
            statement.setInt(4, task.planQty == null ? 0 : task.planQty);
            statement.setTimestamp(5, Timestamp.valueOf(task.plannedStartTime == null ? LocalDateTime.now() : task.plannedStartTime));
            statement.setTimestamp(6, Timestamp.valueOf(task.plannedEndTime == null ? LocalDateTime.now().plusDays(3) : task.plannedEndTime));
            setLong(statement, 7, task.targetLineId);
            statement.setString(8, task.taskStatus);
            statement.setString(9, task.kittingStatus);
            statement.setString(10, task.remark);
            setLong(statement, 11, task.productId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapTask(rs);
            }
        }
    }

    public MesProductionTask updateTaskKitting(long taskId, String taskStatus, String kittingStatus) throws SQLException {
        String sql = """
                update mes_production_task
                set task_status = ?, kitting_status = ?, updated_at = current_timestamp
                where task_id = ?
                returning task_id, task_no, order_id, planner_id, plan_qty, planned_start_time,
                          planned_end_time, target_line_id, task_status, kitting_status, release_time,
                          close_time, remark, created_at, updated_at,
                          (select o.product_id from mes_customer_order o where o.order_id = mes_production_task.order_id) as product_id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, taskStatus);
            statement.setString(2, kittingStatus);
            statement.setLong(3, taskId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapTask(rs);
            }
        }
    }

    public Optional<MesProductionTask> releaseTask(long taskId) throws SQLException {
        String sql = """
                update mes_production_task
                set task_status = 'RELEASED', release_time = current_timestamp, updated_at = current_timestamp
                where task_id = ?
                returning task_id, task_no, order_id, planner_id, plan_qty, planned_start_time,
                          planned_end_time, target_line_id, task_status, kitting_status, release_time,
                          close_time, remark, created_at, updated_at,
                          (select o.product_id from mes_customer_order o where o.order_id = mes_production_task.order_id) as product_id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, taskId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapTask(rs)) : Optional.empty();
            }
        }
    }

    public long firstLineId() throws SQLException {
        String sql = "select line_id from mes_production_line where enabled = 1 order by line_id limit 1";
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getLong("line_id") : 0L;
        }
    }

    public long firstProcessId(Long productId) throws SQLException {
        String sql = """
                select process_id from mes_process_route
                where (? is null or product_id = ?)
                order by process_seq, process_id
                limit 1
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setLong(statement, 1, productId);
            setLong(statement, 2, productId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong("process_id") : 0L;
            }
        }
    }

    public List<MesProductBom> listBomForProduct(long productId) throws SQLException {
        return listBom(productId);
    }

    public BigDecimal availableQty(long materialId) throws SQLException {
        String sql = "select coalesce(sum(available_qty), 0) from mes_inventory where material_id = ?";
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, materialId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    public MesKittingAnalysis insertAnalysis(MesKittingAnalysis analysis, List<MesKittingShortageItem> shortages) throws SQLException {
        String analysisSql = """
                insert into mes_kitting_analysis
                    (analysis_no, task_id, analysis_scope, result_status, snapshot_time,
                     material_ok, line_ok, equipment_ok, process_ok, created_by)
                values (?, ?, ?, ?, current_timestamp, ?, 1, 1, 1, 1)
                returning analysis_id, analysis_no, task_id, result_status, snapshot_time
                """;
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(analysisSql)) {
                statement.setString(1, analysis.analysisNo);
                statement.setLong(2, analysis.taskId);
                statement.setString(3, "PRODUCT:" + analysis.productId + ",QTY:" + analysis.planQty);
                statement.setString(4, analysis.kittingStatus);
                statement.setInt(5, shortages.isEmpty() ? 1 : 0);
                try (ResultSet rs = statement.executeQuery()) {
                    rs.next();
                    analysis.analysisId = rs.getLong("analysis_id");
                    analysis.analysisTime = getLocalDateTime(rs, "snapshot_time");
                }
                for (MesKittingShortageItem item : shortages) {
                    insertShortage(connection, analysis.analysisId, item);
                    insertAlert(connection, analysis.analysisId, item);
                }
                connection.commit();
                analysis.shortageItems = shortages;
                return analysis;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<MesKittingAnalysis> listAnalyses() throws SQLException {
        String sql = """
                select analysis_id, analysis_no, task_id, result_status, snapshot_time
                from mes_kitting_analysis
                order by snapshot_time asc, analysis_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesKittingAnalysis> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapAnalysis(rs));
            }
            return rows;
        }
    }

    public List<MesShortageAlert> listAlerts() throws SQLException {
        String sql = """
                select alert_id, alert_no, task_id, severity, alert_status, created_at
                from mes_shortage_alert
                order by created_at asc, alert_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesShortageAlert> rows = new ArrayList<>();
            while (rs.next()) {
                MesShortageAlert alert = new MesShortageAlert();
                alert.alertId = rs.getLong("alert_id");
                alert.alertNo = rs.getString("alert_no");
                alert.taskId = rs.getLong("task_id");
                alert.alertLevel = rs.getString("severity");
                alert.alertStatus = rs.getString("alert_status");
                alert.createdAt = getLocalDateTime(rs, "created_at");
                rows.add(alert);
            }
            return rows;
        }
    }

    public List<MesSyncLog> listSyncLogs() {
        return List.of();
    }

    private static void insertShortage(Connection connection, long analysisId, MesKittingShortageItem item) throws SQLException {
        String sql = """
                insert into mes_kitting_shortage_item
                    (analysis_id, task_id, shortage_type, resource_id, resource_code, resource_name,
                     required_qty, available_qty, shortage_qty, impact_desc, suggestion)
                values (?, ?, 'MATERIAL', ?, ?, ?, ?, ?, ?, ?, ?)
                returning shortage_item_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, analysisId);
            statement.setLong(2, item.taskId);
            setLong(statement, 3, item.materialId);
            statement.setString(4, item.materialCode);
            statement.setString(5, item.materialName);
            statement.setBigDecimal(6, item.requiredQty);
            statement.setBigDecimal(7, item.availableQty);
            statement.setBigDecimal(8, item.shortageQty);
            statement.setString(9, "齐套分析发现物料不足");
            statement.setString(10, "请补充采购或调整生产计划");
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                item.shortageItemId = rs.getLong(1);
                item.analysisId = analysisId;
            }
        }
    }

    private static void insertAlert(Connection connection, long analysisId, MesKittingShortageItem item) throws SQLException {
        String sql = """
                insert into mes_shortage_alert
                    (alert_no, task_id, analysis_id, alert_type, severity, alert_status, receiver_role, alert_content)
                values (?, ?, ?, 'MATERIAL', ?, 'OPEN', 'WAREHOUSE_KEEPER', ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "ALERT-" + System.currentTimeMillis() + "-" + item.materialId);
            statement.setLong(2, item.taskId);
            statement.setLong(3, analysisId);
            statement.setString(4, item.shortageQty.compareTo(new BigDecimal("100")) > 0 ? "HIGH" : "MEDIUM");
            statement.setString(5, "物料 " + item.materialName + " 缺口 " + item.shortageQty);
            statement.executeUpdate();
        }
    }

    private static MesUser mapUser(ResultSet rs) throws SQLException {
        MesUser item = new MesUser();
        item.userId = rs.getLong("user_id");
        item.username = rs.getString("username");
        item.realName = rs.getString("real_name");
        item.roleCode = rs.getString("role_code");
        item.phone = rs.getString("phone");
        item.enabled = rs.getInt("enabled");
        item.createdAt = getLocalDateTime(rs, "created_at");
        item.updatedAt = item.createdAt;
        return item;
    }

    private static MesProduct mapProduct(ResultSet rs) throws SQLException {
        MesProduct item = new MesProduct();
        item.productId = rs.getLong("product_id");
        item.productCode = rs.getString("product_code");
        item.productName = rs.getString("product_name");
        item.productModel = rs.getString("product_model");
        item.unit = "条";
        item.enabled = rs.getInt("enabled");
        return item;
    }

    private static MesProductBom mapBom(ResultSet rs) throws SQLException {
        MesProductBom item = new MesProductBom();
        item.bomId = rs.getLong("bom_id");
        item.productId = rs.getLong("product_id");
        item.materialId = rs.getLong("material_id");
        item.materialCode = rs.getString("material_code");
        item.materialName = rs.getString("material_name");
        item.qtyPerUnit = rs.getBigDecimal("usage_qty");
        item.unit = rs.getString("unit");
        item.enabled = rs.getInt("enabled");
        item.createdAt = getLocalDateTime(rs, "created_at");
        return item;
    }

    private static MesProcessRoute mapProcessRoute(ResultSet rs) throws SQLException {
        MesProcessRoute item = new MesProcessRoute();
        item.processId = rs.getLong("process_id");
        item.productId = getLong(rs, "product_id");
        item.processCode = rs.getString("process_code");
        item.processName = rs.getString("process_name");
        item.processSeq = rs.getInt("process_seq");
        item.workCenter = rs.getString("required_equipment_type");
        item.enabled = rs.getInt("enabled");
        item.createdAt = getLocalDateTime(rs, "created_at");
        return item;
    }

    private static MesProductionLine mapProductionLine(ResultSet rs) throws SQLException {
        MesProductionLine item = new MesProductionLine();
        item.lineId = rs.getLong("line_id");
        item.lineCode = rs.getString("line_code");
        item.lineName = rs.getString("line_name");
        item.lineType = rs.getString("line_type");
        item.capacityPerDay = getInteger(rs, "daily_capacity");
        item.lineStatus = rs.getString("line_status");
        item.enabled = rs.getInt("enabled");
        item.createdAt = getLocalDateTime(rs, "created_at");
        return item;
    }

    private static MesCustomerOrder mapOrder(ResultSet rs) throws SQLException {
        MesCustomerOrder item = new MesCustomerOrder();
        item.orderId = rs.getLong("order_id");
        item.orderNo = rs.getString("order_no");
        item.customerName = rs.getString("customer_name");
        item.productId = rs.getLong("product_id");
        item.productCode = rs.getString("product_code");
        item.productModel = rs.getString("product_model");
        item.orderQty = rs.getInt("order_qty");
        item.unit = rs.getString("unit");
        Date deliveryDate = rs.getDate("delivery_date");
        item.deliveryDate = deliveryDate == null ? null : deliveryDate.toLocalDate();
        item.priorityLevel = rs.getInt("priority_level");
        item.orderStatus = rs.getString("order_status");
        item.sourceSystem = rs.getString("source_system");
        item.remark = rs.getString("remark");
        item.createdAt = getLocalDateTime(rs, "created_at");
        item.updatedAt = getLocalDateTime(rs, "updated_at");
        return item;
    }

    private static MesProductionTask mapTask(ResultSet rs) throws SQLException {
        MesProductionTask item = new MesProductionTask();
        item.taskId = rs.getLong("task_id");
        item.taskNo = rs.getString("task_no");
        item.orderId = rs.getLong("order_id");
        item.productId = getLong(rs, "product_id");
        item.plannerId = rs.getLong("planner_id");
        item.planQty = rs.getInt("plan_qty");
        item.plannedStartTime = getLocalDateTime(rs, "planned_start_time");
        item.plannedEndTime = getLocalDateTime(rs, "planned_end_time");
        item.targetLineId = getLong(rs, "target_line_id");
        item.taskStatus = rs.getString("task_status");
        item.kittingStatus = rs.getString("kitting_status");
        item.releaseTime = getLocalDateTime(rs, "release_time");
        item.closeTime = getLocalDateTime(rs, "close_time");
        item.remark = rs.getString("remark");
        item.createdAt = getLocalDateTime(rs, "created_at");
        item.updatedAt = getLocalDateTime(rs, "updated_at");
        return item;
    }

    private static MesKittingAnalysis mapAnalysis(ResultSet rs) throws SQLException {
        MesKittingAnalysis item = new MesKittingAnalysis();
        item.analysisId = rs.getLong("analysis_id");
        item.analysisNo = rs.getString("analysis_no");
        item.taskId = rs.getLong("task_id");
        item.kittingStatus = rs.getString("result_status");
        item.analysisResult = rs.getString("result_status");
        item.analysisTime = getLocalDateTime(rs, "snapshot_time");
        return item;
    }

    private static void setLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null || value == 0) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static void setInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}
