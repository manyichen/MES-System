package com.example.messystem.planning.dao;

import com.example.messystem.common.Db;
import com.example.messystem.common.NotFoundException;
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
                select product_id, product_code, product_name, product_model, specification, enabled
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
                select product_id, product_code, product_name, product_model, specification, enabled
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
                returning product_id, product_code, product_name, product_model, specification, enabled
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.productCode);
            statement.setString(2, product.productName);
            statement.setString(3, product.productModel);
            statement.setString(4, product.specification == null ? product.productModel : product.specification);
            statement.setInt(5, product.enabled == null ? 1 : product.enabled);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapProduct(rs);
            }
        }
    }

    public MesProduct updateProduct(long productId, MesProduct product) throws SQLException {
        String sql = """
                update mes_product
                set product_code = ?, product_name = ?, product_model = ?, specification = ?, enabled = ?
                where product_id = ?
                returning product_id, product_code, product_name, product_model, specification, enabled
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.productCode);
            statement.setString(2, product.productName);
            statement.setString(3, product.productModel);
            statement.setString(4, product.specification);
            statement.setInt(5, product.enabled == null ? 1 : product.enabled);
            statement.setLong(6, productId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("product not found");
                return mapProduct(rs);
            }
        }
    }

    public MesProduct disableProduct(long productId) throws SQLException {
        String sql = """
                update mes_product set enabled = 0 where product_id = ?
                returning product_id, product_code, product_name, product_model, specification, enabled
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("product not found");
                return mapProduct(rs);
            }
        }
    }

    public List<MesProductBom> listAllBom() throws SQLException {
        String sql = """
                select b.bom_id, b.product_id, p.product_code, p.product_name,
                       b.material_id, m.material_code, m.material_name,
                       b.usage_qty, b.unit, b.enabled, null::timestamp as created_at
                from mes_product_bom b
                join mes_product p on p.product_id = b.product_id
                left join mes_material m on m.material_id = b.material_id
                order by p.product_code, b.bom_id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesProductBom> rows = new ArrayList<>();
            while (rs.next()) rows.add(mapBom(rs));
            return rows;
        }
    }

    public List<MesProductBom> listBom(long productId) throws SQLException {
        String sql = """
                select b.bom_id, b.product_id, null::varchar as product_code,
                       null::varchar as product_name, b.material_id, m.material_code, m.material_name,
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
                returning bom_id, product_id, null::varchar as product_code,
                          null::varchar as product_name, material_id, null::varchar as material_code,
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

    public MesProductBom updateBom(long bomId, MesProductBom bom) throws SQLException {
        String sql = """
                update mes_product_bom
                set product_id = ?, material_id = ?, usage_qty = ?, unit = ?, enabled = ?
                where bom_id = ?
                returning bom_id, product_id, null::varchar as product_code,
                          null::varchar as product_name, material_id,
                          null::varchar as material_code, null::varchar as material_name,
                          usage_qty, unit, enabled, null::timestamp as created_at
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bom.productId);
            statement.setLong(2, bom.materialId);
            statement.setBigDecimal(3, bom.qtyPerUnit);
            statement.setString(4, bom.unit);
            statement.setInt(5, bom.enabled == null ? 1 : bom.enabled);
            statement.setLong(6, bomId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("bom item not found");
                return mapBom(rs);
            }
        }
    }

    public void deleteBom(long bomId) throws SQLException {
        deleteById("delete from mes_product_bom where bom_id = ?", bomId, "bom item not found");
    }

    public List<MesProcessRoute> listProcessRoutes() throws SQLException {
        String sql = """
                select r.process_id, r.product_id, p.product_code, p.product_name, p.product_model,
                       r.process_code, r.process_name, r.process_seq,
                       r.required_equipment_type, r.enabled, null::timestamp as created_at
                from mes_process_route r
                left join mes_product p on p.product_id = r.product_id
                order by r.product_id nulls last, r.process_seq, r.process_id
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

    public MesProcessRoute updateProcessRoute(long processId, MesProcessRoute route) throws SQLException {
        String sql = """
                update mes_process_route
                set product_id = ?,
                    process_code = ?,
                    process_name = ?,
                    process_seq = ?,
                    required_equipment_type = ?,
                    enabled = ?
                where process_id = ?
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
            statement.setLong(7, processId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapProcessRoute(rs);
                }
                throw new NotFoundException("process route not found");
            }
        }
    }

    public void deleteProcessRoute(long processId) throws SQLException {
        deleteById("delete from mes_process_route where process_id = ?", processId, "process route not found");
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

    public MesProductionLine updateProductionLine(long lineId, MesProductionLine line) throws SQLException {
        String sql = """
                update mes_production_line
                set line_code = ?, line_name = ?, line_type = ?, daily_capacity = ?,
                    line_status = ?, enabled = ?
                where line_id = ?
                returning line_id, line_code, line_name, line_type, daily_capacity,
                          line_status, enabled, null::timestamp as created_at
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, line.lineCode);
            statement.setString(2, line.lineName);
            statement.setString(3, line.lineType);
            setInteger(statement, 4, line.capacityPerDay);
            statement.setString(5, line.lineStatus);
            statement.setInt(6, line.enabled == null ? 1 : line.enabled);
            statement.setLong(7, lineId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("production line not found");
                return mapProductionLine(rs);
            }
        }
    }

    public MesProductionLine disableProductionLine(long lineId) throws SQLException {
        String sql = """
                update mes_production_line set enabled = 0, line_status = 'DISABLED'
                where line_id = ?
                returning line_id, line_code, line_name, line_type, daily_capacity,
                          line_status, enabled, null::timestamp as created_at
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, lineId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("production line not found");
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
        String sql = taskSelectSql(false);
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
        String sql = taskSelectSql(true);
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, taskId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapTask(rs)) : Optional.empty();
            }
        }
    }

    private static String taskSelectSql(boolean singleTask) {
        String where = singleTask ? "where t.task_id = ?\n" : "";
        return """
                select t.task_id, t.task_no, t.order_id, t.planner_id, t.plan_qty,
                       t.planned_start_time, t.planned_end_time, t.target_line_id,
                       t.task_status, t.kitting_status, t.release_time, t.close_time,
                       t.remark, t.created_at, t.updated_at, o.product_id,
                       readiness.blocked_reason is null as kitting_analyzable,
                       readiness.blocked_reason as kitting_blocked_reason,
                       exists(select 1 from mes_shortage_alert a where a.task_id = t.task_id)
                           as shortage_alert_published,
                       (select string_agg(s.resource_name || ' 缺口 ' || s.shortage_qty::text, '；'
                                order by s.shortage_item_id)
                        from mes_kitting_shortage_item s
                        where s.task_id = t.task_id
                          and s.analysis_id = (select max(a.analysis_id) from mes_kitting_analysis a
                                               where a.task_id = t.task_id)
                          and s.shortage_type = 'MATERIAL'
                          and coalesce(s.shortage_qty, 0) > 0) as shortage_summary
                from mes_production_task t
                left join mes_customer_order o on o.order_id = t.order_id
                left join mes_product p on p.product_id = o.product_id
                cross join lateral (
                    select case
                        when o.product_id is null
                            then '生产任务未关联产品，请先补全客户订单的产品信息'
                        when p.product_id is null or coalesce(p.enabled, 0) <> 1
                            then '生产任务关联的产品不存在或已停用，请先修复产品信息'
                        when coalesce(t.plan_qty, 0) <= 0
                            then '生产任务计划数量必须大于0，请先修复任务数据'
                        when not exists (
                            select 1 from mes_product_bom b
                            where b.product_id = o.product_id and coalesce(b.enabled, 1) = 1
                        )
                            then '产品未配置启用的BOM物料，请先维护产品BOM'
                        when exists (
                            select 1
                            from mes_product_bom b
                            left join mes_material m on m.material_id = b.material_id
                            where b.product_id = o.product_id and coalesce(b.enabled, 1) = 1
                              and (m.material_id is null or coalesce(m.enabled, 0) <> 1)
                        )
                            then '产品BOM包含已停用或不存在的物料，请先修复BOM'
                        when exists (
                            select 1 from mes_product_bom b
                            where b.product_id = o.product_id and coalesce(b.enabled, 1) = 1
                              and coalesce(b.usage_qty, 0) <= 0
                        )
                            then '产品BOM物料单耗必须大于0，请先修复BOM'
                        else null
                    end as blocked_reason
                ) readiness
                """ + where + "order by t.task_id asc";
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
        return listBom(productId).stream()
                .filter(item -> item.enabled == null || item.enabled == 1)
                .toList();
    }

    public BigDecimal availableQty(long materialId) throws SQLException {
        String sql = """
                select coalesce(sum(available_qty), 0)
                from mes_inventory
                where material_id = ? and quality_status = 'QUALIFIED'
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, materialId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    public boolean hasPublishedShortageAlert(long taskId) throws SQLException {
        String sql = "select exists(select 1 from mes_shortage_alert where task_id = ?)";
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, taskId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
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
                select alert_id, alert_no, task_id, analysis_id, material_id, material_code, material_name,
                       required_qty, available_qty, shortage_qty, severity, alert_status, accepted_by, accepted_at, resolved_at, created_at
                from mes_shortage_alert
                where material_id is not null
                  and coalesce(shortage_qty, 0) > 0
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
                alert.analysisId = getLong(rs, "analysis_id");
                alert.materialId = getLong(rs, "material_id");
                alert.materialCode = rs.getString("material_code");
                alert.materialName = rs.getString("material_name");
                alert.requiredQty = rs.getBigDecimal("required_qty");
                alert.availableQty = rs.getBigDecimal("available_qty");
                alert.shortageQty = rs.getBigDecimal("shortage_qty");
                alert.alertLevel = rs.getString("severity");
                alert.alertStatus = rs.getString("alert_status");
                alert.acceptedBy = getLong(rs, "accepted_by");
                alert.acceptedAt = getLocalDateTime(rs, "accepted_at");
                alert.resolvedAt = getLocalDateTime(rs, "resolved_at");
                alert.createdAt = getLocalDateTime(rs, "created_at");
                rows.add(alert);
            }
            return rows;
        }
    }

    public List<MesShortageAlert> publishShortageAlerts(long taskId) throws SQLException {
        String query = """
                select s.analysis_id,
                       s.resource_id as material_id,
                       s.resource_code as material_code,
                       s.resource_name as material_name,
                       s.required_qty,
                       s.available_qty,
                       s.shortage_qty
                from mes_kitting_shortage_item s
                where s.task_id = ?
                  and s.shortage_type = 'MATERIAL'
                  and s.resource_id is not null
                  and coalesce(s.shortage_qty, 0) > 0
                  and s.analysis_id = (
                      select max(analysis_id)
                      from mes_kitting_shortage_item
                      where task_id = ? and shortage_type = 'MATERIAL'
                  )
                order by s.shortage_item_id
                """;
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setLong(1, taskId); statement.setLong(2, taskId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        insertAlert(connection, taskId, rs.getLong("analysis_id"), rs.getLong("material_id"),
                                rs.getString("material_code"), rs.getString("material_name"), rs.getBigDecimal("required_qty"),
                                rs.getBigDecimal("available_qty"), rs.getBigDecimal("shortage_qty"));
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) { connection.rollback(); throw ex; } finally { connection.setAutoCommit(true); }
        }
        return listAlerts();
    }

    public MesShortageAlert acceptShortageAlert(long alertId, long userId) throws SQLException {
        String sql = """
                update mes_shortage_alert set alert_status = 'ACCEPTED', accepted_by = ?, accepted_at = current_timestamp
                where alert_id = ? and alert_status = 'OPEN'
                returning alert_id, alert_no, task_id, analysis_id, material_id, material_code, material_name,
                          required_qty, available_qty, shortage_qty, severity, alert_status, accepted_by, accepted_at, resolved_at, created_at
                """;
        try (Connection connection = Db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId); statement.setLong(2, alertId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("shortage alert is not open");
                MesShortageAlert alert = new MesShortageAlert();
                alert.alertId = rs.getLong("alert_id"); alert.alertNo = rs.getString("alert_no"); alert.taskId = rs.getLong("task_id");
                alert.materialId = getLong(rs, "material_id"); alert.materialCode = rs.getString("material_code"); alert.materialName = rs.getString("material_name");
                alert.requiredQty = rs.getBigDecimal("required_qty"); alert.availableQty = rs.getBigDecimal("available_qty"); alert.shortageQty = rs.getBigDecimal("shortage_qty");
                alert.alertLevel = rs.getString("severity"); alert.alertStatus = rs.getString("alert_status"); alert.acceptedBy = getLong(rs, "accepted_by"); alert.acceptedAt = getLocalDateTime(rs, "accepted_at"); alert.resolvedAt = getLocalDateTime(rs, "resolved_at"); alert.createdAt = getLocalDateTime(rs, "created_at");
                return alert;
            }
        }
    }

    public int resolveRecoveredShortageAlerts(long taskId) throws SQLException {
        String sql = """
                update mes_shortage_alert a
                set alert_status = 'RESOLVED', resolved_at = current_timestamp
                where a.task_id = ?
                  and a.alert_status in ('OPEN', 'ACCEPTED')
                  and not exists (
                      select 1
                      from mes_kitting_shortage_item s
                      where s.task_id = a.task_id
                        and s.analysis_id = (
                            select max(latest.analysis_id)
                            from mes_kitting_analysis latest
                            where latest.task_id = a.task_id
                        )
                        and s.shortage_type = 'MATERIAL'
                        and s.resource_id = a.material_id
                        and coalesce(s.shortage_qty, 0) > 0
                  )
                """;
        try (Connection connection = Db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, taskId);
            return statement.executeUpdate();
        }
    }

    public List<MesSyncLog> listSyncLogs() {
        String sql = """
                select sync_log_id, sync_object, source_system, business_key, sync_status, error_message, sync_time
                from mes_sync_log
                order by sync_time desc, sync_log_id desc
                limit 50
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesSyncLog> rows = new ArrayList<>();
            while (rs.next()) {
                MesSyncLog log = new MesSyncLog();
                log.syncLogId = rs.getLong("sync_log_id");
                log.syncType = rs.getString("sync_object");
                log.sourceSystem = rs.getString("source_system");
                log.targetTable = rs.getString("business_key");
                log.syncStatus = rs.getString("sync_status");
                log.message = rs.getString("error_message");
                log.createdAt = getLocalDateTime(rs, "sync_time");
                rows.add(log);
            }
            return rows;
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
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

    private static void insertAlert(Connection connection, long taskId, long analysisId, long materialId, String materialCode, String materialName,
            BigDecimal requiredQty, BigDecimal availableQty, BigDecimal shortageQty) throws SQLException {
        String sql = """
                insert into mes_shortage_alert
                    (alert_no, task_id, analysis_id, material_id, material_code, material_name, required_qty, available_qty, shortage_qty,
                     alert_type, severity, alert_status, receiver_role, alert_content)
                select ?, ?, ?, ?, ?, ?, ?, ?, ?, 'MATERIAL', ?, 'OPEN', 'WAREHOUSE_ADMIN', ?
                where not exists (select 1 from mes_shortage_alert where task_id = ? and material_id = ? and alert_status in ('OPEN', 'ACCEPTED'))
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "ALERT-" + System.currentTimeMillis() + "-" + materialId);
            statement.setLong(2, taskId);
            statement.setLong(3, analysisId);
            statement.setLong(4, materialId); statement.setString(5, materialCode); statement.setString(6, materialName);
            statement.setBigDecimal(7, requiredQty); statement.setBigDecimal(8, availableQty); statement.setBigDecimal(9, shortageQty);
            statement.setString(10, shortageQty.compareTo(new BigDecimal("100")) > 0 ? "HIGH" : "MEDIUM");
            statement.setString(11, "物料 " + materialName + " 缺口 " + shortageQty);
            statement.setLong(12, taskId); statement.setLong(13, materialId);
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
        item.specification = getOptionalString(rs, "specification");
        item.unit = "条";
        item.enabled = rs.getInt("enabled");
        return item;
    }

    private static MesProductBom mapBom(ResultSet rs) throws SQLException {
        MesProductBom item = new MesProductBom();
        item.bomId = rs.getLong("bom_id");
        item.productId = rs.getLong("product_id");
        item.productCode = getOptionalString(rs, "product_code");
        item.productName = getOptionalString(rs, "product_name");
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
        item.productCode = getOptionalString(rs, "product_code");
        item.productName = getOptionalString(rs, "product_name");
        item.productModel = getOptionalString(rs, "product_model");
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
        item.kittingAnalyzable = getOptionalBoolean(rs, "kitting_analyzable");
        item.kittingBlockedReason = getOptionalString(rs, "kitting_blocked_reason");
        item.shortageAlertPublished = getOptionalBoolean(rs, "shortage_alert_published");
        item.shortageSummary = getOptionalString(rs, "shortage_summary");
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

    private static void deleteById(String sql, long id, String notFoundMessage) throws SQLException {
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new NotFoundException(notFoundMessage);
            }
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

    private static String getOptionalString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private static Boolean getOptionalBoolean(ResultSet rs, String column) {
        try {
            boolean value = rs.getBoolean(column);
            return rs.wasNull() ? null : value;
        } catch (SQLException ignored) {
            return null;
        }
    }

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}
