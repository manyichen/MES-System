package com.example.messystem.warehouse.dao;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.warehouse.entity.MesInventory;
import com.example.messystem.warehouse.entity.MesInventoryTransaction;
import com.example.messystem.warehouse.entity.MesMaterial;
import com.example.messystem.warehouse.entity.MesMaterialRequisition;
import com.example.messystem.warehouse.entity.MesMaterialRequisitionItem;
import com.example.messystem.warehouse.entity.MesPickingTask;
import com.example.messystem.warehouse.entity.MesRobot;
import com.example.messystem.warehouse.entity.MesRobotDeliveryTask;
import com.example.messystem.warehouse.entity.MesWarehouse;
import com.example.messystem.warehouse.entity.MesWarehouseLocation;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WarehouseDao {
    public List<MesMaterial> listMaterials() throws SQLException {
        String sql = """
                select material_id, material_code, material_name, material_type, specification,
                       unit, shelf_life_days, enabled, created_at
                from mes_material
                order by material_id desc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesMaterial> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapMaterial(rs));
            }
            return rows;
        }
    }

    public MesMaterial findMaterial(long materialId) throws SQLException {
        String sql = """
                select material_id, material_code, material_name, material_type, specification,
                       unit, shelf_life_days, enabled, created_at
                from mes_material
                where material_id = ?
                """;
        return findOne(sql, materialId, this::mapMaterial, "material not found");
    }

    public MesMaterial insertMaterial(MesMaterial material) throws SQLException {
        String sql = """
                insert into mes_material
                    (material_code, material_name, material_type, specification, unit, shelf_life_days, enabled)
                values (?, ?, ?, ?, ?, ?, ?)
                returning material_id, material_code, material_name, material_type, specification,
                          unit, shelf_life_days, enabled, created_at
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, defaultCode(material.materialCode, "MAT"));
            statement.setString(2, material.materialName);
            statement.setString(3, defaultText(material.materialType, "RAW"));
            statement.setString(4, material.specification);
            statement.setString(5, defaultText(material.unit, "kg"));
            setInteger(statement, 6, material.shelfLifeDays);
            statement.setInt(7, material.enabled == null ? 1 : material.enabled);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapMaterial(rs);
            }
        }
    }

    public MesMaterial updateMaterial(long materialId, MesMaterial material) throws SQLException {
        String sql = """
                update mes_material
                set material_code = ?,
                    material_name = ?,
                    material_type = ?,
                    specification = ?,
                    unit = ?,
                    shelf_life_days = ?,
                    enabled = ?
                where material_id = ?
                returning material_id, material_code, material_name, material_type, specification,
                          unit, shelf_life_days, enabled, created_at
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            MesMaterial current = findMaterial(materialId);
            statement.setString(1, defaultText(material.materialCode, current.materialCode));
            statement.setString(2, defaultText(material.materialName, current.materialName));
            statement.setString(3, defaultText(material.materialType, current.materialType));
            statement.setString(4, material.specification == null ? current.specification : material.specification);
            statement.setString(5, defaultText(material.unit, current.unit));
            setInteger(statement, 6, material.shelfLifeDays == null ? current.shelfLifeDays : material.shelfLifeDays);
            statement.setInt(7, material.enabled == null ? current.enabled : material.enabled);
            statement.setLong(8, materialId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("material not found");
                }
                return mapMaterial(rs);
            }
        }
    }

    public void deleteMaterial(long materialId) throws SQLException {
        deleteById("delete from mes_material where material_id = ?", materialId, "material not found");
    }

    public List<MesWarehouse> listWarehouses() throws SQLException {
        String sql = """
                select warehouse_id, warehouse_code, warehouse_name, warehouse_type, enabled
                from mes_warehouse
                order by warehouse_id desc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesWarehouse> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapWarehouse(rs));
            }
            return rows;
        }
    }

    public MesWarehouse findWarehouse(long warehouseId) throws SQLException {
        String sql = """
                select warehouse_id, warehouse_code, warehouse_name, warehouse_type, enabled
                from mes_warehouse
                where warehouse_id = ?
                """;
        return findOne(sql, warehouseId, this::mapWarehouse, "warehouse not found");
    }

    public MesWarehouse insertWarehouse(MesWarehouse warehouse) throws SQLException {
        String sql = """
                insert into mes_warehouse (warehouse_code, warehouse_name, warehouse_type, enabled)
                values (?, ?, ?, ?)
                returning warehouse_id, warehouse_code, warehouse_name, warehouse_type, enabled
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, defaultCode(warehouse.warehouseCode, "WH"));
            statement.setString(2, warehouse.warehouseName);
            statement.setString(3, defaultText(warehouse.warehouseType, "RAW"));
            statement.setInt(4, warehouse.enabled == null ? 1 : warehouse.enabled);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapWarehouse(rs);
            }
        }
    }

    public MesWarehouse updateWarehouse(long warehouseId, MesWarehouse warehouse) throws SQLException {
        String sql = """
                update mes_warehouse
                set warehouse_code = ?,
                    warehouse_name = ?,
                    warehouse_type = ?,
                    enabled = ?
                where warehouse_id = ?
                returning warehouse_id, warehouse_code, warehouse_name, warehouse_type, enabled
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            MesWarehouse current = findWarehouse(warehouseId);
            statement.setString(1, defaultText(warehouse.warehouseCode, current.warehouseCode));
            statement.setString(2, defaultText(warehouse.warehouseName, current.warehouseName));
            statement.setString(3, defaultText(warehouse.warehouseType, current.warehouseType));
            statement.setInt(4, warehouse.enabled == null ? current.enabled : warehouse.enabled);
            statement.setLong(5, warehouseId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("warehouse not found");
                }
                return mapWarehouse(rs);
            }
        }
    }

    public void deleteWarehouse(long warehouseId) throws SQLException {
        deleteById("delete from mes_warehouse where warehouse_id = ?", warehouseId, "warehouse not found");
    }

    public List<MesWarehouseLocation> listLocations() throws SQLException {
        String sql = """
                select location_id, warehouse_id, location_code, location_name, enabled
                from mes_warehouse_location
                order by location_id desc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesWarehouseLocation> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapLocation(rs));
            }
            return rows;
        }
    }

    public MesWarehouseLocation findLocation(long locationId) throws SQLException {
        String sql = """
                select location_id, warehouse_id, location_code, location_name, enabled
                from mes_warehouse_location
                where location_id = ?
                """;
        return findOne(sql, locationId, this::mapLocation, "warehouse location not found");
    }

    public MesWarehouseLocation insertLocation(MesWarehouseLocation location) throws SQLException {
        String sql = """
                insert into mes_warehouse_location (warehouse_id, location_code, location_name, enabled)
                values (?, ?, ?, ?)
                returning location_id, warehouse_id, location_code, location_name, enabled
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, location.warehouseId);
            statement.setString(2, defaultCode(location.locationCode, "LOC"));
            statement.setString(3, defaultText(location.locationName, location.locationCode));
            statement.setInt(4, location.enabled == null ? 1 : location.enabled);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapLocation(rs);
            }
        }
    }

    public MesWarehouseLocation updateLocation(long locationId, MesWarehouseLocation location) throws SQLException {
        String sql = """
                update mes_warehouse_location
                set warehouse_id = ?,
                    location_code = ?,
                    location_name = ?,
                    enabled = ?
                where location_id = ?
                returning location_id, warehouse_id, location_code, location_name, enabled
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            MesWarehouseLocation current = findLocation(locationId);
            statement.setLong(1, location.warehouseId == null ? current.warehouseId : location.warehouseId);
            statement.setString(2, defaultText(location.locationCode, current.locationCode));
            statement.setString(3, defaultText(location.locationName, current.locationName));
            statement.setInt(4, location.enabled == null ? current.enabled : location.enabled);
            statement.setLong(5, locationId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("warehouse location not found");
                }
                return mapLocation(rs);
            }
        }
    }

    public void deleteLocation(long locationId) throws SQLException {
        deleteById("delete from mes_warehouse_location where location_id = ?", locationId, "warehouse location not found");
    }

    public List<MesInventory> listInventory() throws SQLException {
        String sql = """
                select inventory_id, material_id, warehouse_id, location_id, batch_no,
                       available_qty, reserved_qty, frozen_qty, quality_status, last_check_time
                from mes_inventory
                order by inventory_id desc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesInventory> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapInventory(rs));
            }
            return rows;
        }
    }

    public MesInventory findInventory(long inventoryId) throws SQLException {
        String sql = """
                select inventory_id, material_id, warehouse_id, location_id, batch_no,
                       available_qty, reserved_qty, frozen_qty, quality_status, last_check_time
                from mes_inventory
                where inventory_id = ?
                """;
        return findOne(sql, inventoryId, this::mapInventory, "inventory not found");
    }

    public List<MesInventory> listInventoryByMaterial(long materialId) throws SQLException {
        String sql = """
                select inventory_id, material_id, warehouse_id, location_id, batch_no,
                       available_qty, reserved_qty, frozen_qty, quality_status, last_check_time
                from mes_inventory
                where material_id = ?
                order by inventory_id desc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, materialId);
            try (ResultSet rs = statement.executeQuery()) {
                List<MesInventory> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapInventory(rs));
                }
                return rows;
            }
        }
    }

    public MesInventory insertInventory(MesInventory item) throws SQLException {
        String sql = """
                insert into mes_inventory
                    (material_id, warehouse_id, location_id, batch_no, available_qty,
                     reserved_qty, frozen_qty, quality_status, last_check_time)
                values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)
                returning inventory_id, material_id, warehouse_id, location_id, batch_no,
                          available_qty, reserved_qty, frozen_qty, quality_status, last_check_time
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, item.materialId);
            statement.setLong(2, item.warehouseId);
            statement.setLong(3, item.locationId);
            statement.setString(4, defaultText(item.batchNo, "DEFAULT-BATCH"));
            statement.setBigDecimal(5, nvl(item.availableQty));
            statement.setBigDecimal(6, nvl(item.reservedQty));
            statement.setBigDecimal(7, nvl(item.frozenQty));
            statement.setString(8, defaultText(item.qualityStatus, "QUALIFIED"));
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapInventory(rs);
            }
        }
    }

    public MesInventory updateInventory(long inventoryId, MesInventory item) throws SQLException {
        String sql = """
                update mes_inventory
                set material_id = ?,
                    warehouse_id = ?,
                    location_id = ?,
                    batch_no = ?,
                    available_qty = ?,
                    reserved_qty = ?,
                    frozen_qty = ?,
                    quality_status = ?,
                    last_check_time = current_timestamp
                where inventory_id = ?
                returning inventory_id, material_id, warehouse_id, location_id, batch_no,
                          available_qty, reserved_qty, frozen_qty, quality_status, last_check_time
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            MesInventory current = findInventory(inventoryId);
            statement.setLong(1, item.materialId == null ? current.materialId : item.materialId);
            statement.setLong(2, item.warehouseId == null ? current.warehouseId : item.warehouseId);
            statement.setLong(3, item.locationId == null ? current.locationId : item.locationId);
            statement.setString(4, defaultText(item.batchNo, current.batchNo));
            statement.setBigDecimal(5, item.availableQty == null ? current.availableQty : item.availableQty);
            statement.setBigDecimal(6, item.reservedQty == null ? current.reservedQty : item.reservedQty);
            statement.setBigDecimal(7, item.frozenQty == null ? current.frozenQty : item.frozenQty);
            statement.setString(8, defaultText(item.qualityStatus, current.qualityStatus));
            statement.setLong(9, inventoryId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("inventory not found");
                }
                return mapInventory(rs);
            }
        }
    }

    public void deleteInventory(long inventoryId) throws SQLException {
        deleteById("delete from mes_inventory where inventory_id = ?", inventoryId, "inventory not found");
    }

    public List<MesInventoryTransaction> listTransactions() throws SQLException {
        String sql = """
                select transaction_id, transaction_no, material_id, inventory_id, transaction_type,
                       qty, source_doc_type, source_doc_id, operator_id, created_at
                from mes_inventory_transaction
                order by transaction_id desc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesInventoryTransaction> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapTransaction(rs));
            }
            return rows;
        }
    }

    public MesInventoryTransaction findTransaction(long transactionId) throws SQLException {
        String sql = """
                select transaction_id, transaction_no, material_id, inventory_id, transaction_type,
                       qty, source_doc_type, source_doc_id, operator_id, created_at
                from mes_inventory_transaction
                where transaction_id = ?
                """;
        return findOne(sql, transactionId, this::mapTransaction, "inventory transaction not found");
    }

    public MesInventoryTransaction insertTransaction(MesInventoryTransaction transaction) throws SQLException {
        String sql = """
                insert into mes_inventory_transaction
                    (transaction_no, material_id, inventory_id, transaction_type, qty,
                     source_doc_type, source_doc_id, operator_id)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                returning transaction_id, transaction_no, material_id, inventory_id, transaction_type,
                          qty, source_doc_type, source_doc_id, operator_id, created_at
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, defaultCode(transaction.transactionNo, "TX"));
            statement.setLong(2, transaction.materialId);
            setLong(statement, 3, transaction.inventoryId);
            statement.setString(4, defaultText(transaction.transactionType, "ADJUST"));
            statement.setBigDecimal(5, nvl(transaction.qty));
            statement.setString(6, defaultText(transaction.sourceDocType, "MANUAL"));
            setLong(statement, 7, transaction.sourceDocId);
            statement.setLong(8, transaction.operatorId == null ? 1L : transaction.operatorId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapTransaction(rs);
            }
        }
    }

    public List<MesMaterialRequisition> listRequisitions() throws SQLException {
        String sql = """
                select r.requisition_id, r.requisition_no, r.work_order_id, r.requested_by,
                       r.request_status, r.request_time, r.approved_by, r.approved_time, r.remark,
                       i.requisition_item_id, i.material_id, i.required_qty, i.issued_qty,
                       i.unit, i.batch_no, i.item_status
                from mes_material_requisition r
                left join mes_material_requisition_item i on i.requisition_id = r.requisition_id
                order by r.requisition_id desc, i.requisition_item_id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            Map<Long, MesMaterialRequisition> rows = new LinkedHashMap<>();
            while (rs.next()) {
                MesMaterialRequisition requisition = rows.computeIfAbsent(
                        rs.getLong("requisition_id"),
                        ignored -> mapRequisitionUnchecked(rs)
                );
                Long itemId = getLong(rs, "requisition_item_id");
                if (itemId != null) {
                    requisition.items.add(mapRequisitionItem(rs, requisition.requisitionId));
                }
            }
            return new ArrayList<>(rows.values());
        }
    }

    public MesMaterialRequisition findRequisition(long requisitionId) throws SQLException {
        String sql = """
                select r.requisition_id, r.requisition_no, r.work_order_id, r.requested_by,
                       r.request_status, r.request_time, r.approved_by, r.approved_time, r.remark,
                       i.requisition_item_id, i.material_id, i.required_qty, i.issued_qty,
                       i.unit, i.batch_no, i.item_status
                from mes_material_requisition r
                left join mes_material_requisition_item i on i.requisition_id = r.requisition_id
                where r.requisition_id = ?
                order by i.requisition_item_id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requisitionId);
            try (ResultSet rs = statement.executeQuery()) {
                MesMaterialRequisition requisition = null;
                while (rs.next()) {
                    if (requisition == null) {
                        requisition = mapRequisition(rs);
                    }
                    Long itemId = getLong(rs, "requisition_item_id");
                    if (itemId != null) {
                        requisition.items.add(mapRequisitionItem(rs, requisition.requisitionId));
                    }
                }
                if (requisition == null) {
                    throw new NotFoundException("requisition not found");
                }
                return requisition;
            }
        }
    }

    public List<MesMaterialRequisition> listRequisitionsByWorkOrder(long workOrderId) throws SQLException {
        String sql = """
                select r.requisition_id, r.requisition_no, r.work_order_id, r.requested_by,
                       r.request_status, r.request_time, r.approved_by, r.approved_time, r.remark,
                       i.requisition_item_id, i.material_id, i.required_qty, i.issued_qty,
                       i.unit, i.batch_no, i.item_status
                from mes_material_requisition r
                left join mes_material_requisition_item i on i.requisition_id = r.requisition_id
                where r.work_order_id = ?
                order by r.requisition_id desc, i.requisition_item_id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, workOrderId);
            try (ResultSet rs = statement.executeQuery()) {
                return mapRequisitions(rs);
            }
        }
    }

    public MesMaterialRequisition insertRequisition(MesMaterialRequisition requisition) throws SQLException {
        String requisitionSql = """
                insert into mes_material_requisition
                    (requisition_no, work_order_id, requested_by, request_status, remark)
                values (?, ?, ?, 'CREATED', ?)
                returning requisition_id, requisition_no, work_order_id, requested_by,
                          request_status, request_time, approved_by, approved_time, remark
                """;
        String itemSql = """
                insert into mes_material_requisition_item
                    (requisition_id, material_id, required_qty, issued_qty, unit, batch_no, item_status)
                values (?, ?, ?, 0, ?, ?, 'CREATED')
                returning requisition_item_id, material_id, required_qty, issued_qty, unit, batch_no, item_status
                """;
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                MesMaterialRequisition created;
                ensureWorkOrderExecutable(connection, requisition.workOrderId);
                try (PreparedStatement statement = connection.prepareStatement(requisitionSql)) {
                    statement.setString(1, defaultCode(requisition.requisitionNo, "REQ"));
                    statement.setLong(2, requisition.workOrderId);
                    statement.setLong(3, requisition.requestedBy == null ? 1L : requisition.requestedBy);
                    statement.setString(4, requisition.remark);
                    try (ResultSet rs = statement.executeQuery()) {
                        rs.next();
                        created = mapRequisition(rs);
                    }
                }
                if (requisition.items != null) {
                    try (PreparedStatement statement = connection.prepareStatement(itemSql)) {
                        for (MesMaterialRequisitionItem item : requisition.items) {
                            statement.setLong(1, created.requisitionId);
                            statement.setLong(2, item.materialId);
                            statement.setBigDecimal(3, nvl(item.requiredQty));
                            statement.setString(4, defaultText(item.unit, "kg"));
                            statement.setString(5, item.batchNo);
                            try (ResultSet rs = statement.executeQuery()) {
                                rs.next();
                                created.items.add(mapRequisitionItem(rs, created.requisitionId));
                            }
                        }
                    }
                }
                connection.commit();
                return created;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public MesMaterialRequisition approveRequisition(long requisitionId, Long approvedBy) throws SQLException {
        String updateSql = """
                update mes_material_requisition
                set request_status = 'APPROVED',
                    approved_by = ?,
                    approved_time = current_timestamp
                where requisition_id = ? and request_status = 'CREATED'
                returning requisition_id, requisition_no, work_order_id, requested_by,
                          request_status, request_time, approved_by, approved_time, remark
                """;
        String pickingSql = """
                insert into mes_picking_task
                    (picking_task_no, requisition_id, warehouse_id, task_status)
                values (?, ?, ?, 'CREATED')
                """;
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                MesMaterialRequisition requisition;
                ensureInventoryEnoughForRequisition(connection, requisitionId);
                try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                    statement.setLong(1, approvedBy == null ? 1L : approvedBy);
                    statement.setLong(2, requisitionId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            ensureRequisitionExistsAndCreated(connection, requisitionId);
                            throw new BadRequestException("only CREATED requisitions can be approved");
                        }
                        requisition = mapRequisition(rs);
                    }
                }
                Long warehouseId = firstWarehouseId(connection);
                if (warehouseId == null) {
                    throw new BadRequestException("warehouse is required before approving requisition");
                }
                try (PreparedStatement statement = connection.prepareStatement(pickingSql)) {
                    statement.setString(1, IdGenerator.nextCode("PICK"));
                    statement.setLong(2, requisitionId);
                    statement.setLong(3, warehouseId);
                    statement.executeUpdate();
                }
                connection.commit();
                return requisition;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void ensureInventoryEnoughForRequisition(Connection connection, long requisitionId) throws SQLException {
        String sql = """
                select i.material_id, i.required_qty, i.batch_no,
                       coalesce(sum(inv.available_qty), 0) as available_qty
                from mes_material_requisition_item i
                left join mes_inventory inv on inv.material_id = i.material_id
                    and inv.available_qty > 0
                    and (i.batch_no is null or inv.batch_no = i.batch_no)
                where i.requisition_id = ?
                group by i.requisition_item_id, i.material_id, i.required_qty, i.batch_no
                order by i.requisition_item_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requisitionId);
            try (ResultSet rs = statement.executeQuery()) {
                boolean hasItems = false;
                while (rs.next()) {
                    hasItems = true;
                    BigDecimal requiredQty = rs.getBigDecimal("required_qty");
                    BigDecimal availableQty = rs.getBigDecimal("available_qty");
                    if (availableQty.compareTo(requiredQty) < 0) {
                        throw new BadRequestException("inventory is not enough for materialId " + rs.getLong("material_id"));
                    }
                }
                if (!hasItems) {
                    throw new BadRequestException("requisition items are required");
                }
            }
        }
    }

    public List<MesPickingTask> listPickingTasks() throws SQLException {
        String sql = """
                select picking_task_id, picking_task_no, requisition_id, warehouse_id,
                       task_status, assigned_to, start_time, finish_time
                from mes_picking_task
                order by picking_task_id desc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesPickingTask> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapPickingTask(rs));
            }
            return rows;
        }
    }

    public MesPickingTask findPickingTask(long pickingTaskId) throws SQLException {
        String sql = """
                select picking_task_id, picking_task_no, requisition_id, warehouse_id,
                       task_status, assigned_to, start_time, finish_time
                from mes_picking_task
                where picking_task_id = ?
                """;
        return findOne(sql, pickingTaskId, this::mapPickingTask, "picking task not found");
    }

    public MesPickingTask insertPickingTask(MesPickingTask task) throws SQLException {
        String sql = """
                insert into mes_picking_task
                    (picking_task_no, requisition_id, warehouse_id, task_status, assigned_to, start_time)
                values (?, ?, ?, ?, ?, current_timestamp)
                returning picking_task_id, picking_task_no, requisition_id, warehouse_id,
                          task_status, assigned_to, start_time, finish_time
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, defaultCode(task.pickingTaskNo, "PICK"));
            statement.setLong(2, task.requisitionId);
            statement.setLong(3, task.warehouseId);
            statement.setString(4, defaultText(task.taskStatus, "CREATED"));
            setLong(statement, 5, task.assignedTo);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapPickingTask(rs);
            }
        }
    }

    public MesPickingTask completePicking(long pickingTaskId) throws SQLException {
        String updateSql = """
                update mes_picking_task
                set task_status = 'COMPLETED',
                    finish_time = current_timestamp
                where picking_task_id = ? and task_status = 'CREATED'
                returning picking_task_id, picking_task_no, requisition_id, warehouse_id,
                          task_status, assigned_to, start_time, finish_time
                """;
        String deliverySql = """
                insert into mes_robot_delivery_task
                    (delivery_task_no, picking_task_id, robot_id, from_location_id,
                     to_line_id, delivery_status, load_time)
                values (?, ?, ?, ?, 1, 'PENDING', current_timestamp)
                """;
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                MesPickingTask task;
                try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                    statement.setLong(1, pickingTaskId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            ensurePickingExistsAndCreated(connection, pickingTaskId);
                            throw new BadRequestException("only CREATED picking tasks can be completed");
                        }
                        task = mapPickingTask(rs);
                    }
                }
                Long locationId = firstLocationId(connection);
                if (locationId == null) {
                    throw new BadRequestException("warehouse location is required before completing picking");
                }
                try (PreparedStatement statement = connection.prepareStatement(deliverySql)) {
                    statement.setString(1, IdGenerator.nextCode("RBT"));
                    statement.setLong(2, pickingTaskId);
                    setLong(statement, 3, firstRobotId(connection));
                    statement.setLong(4, locationId);
                    statement.executeUpdate();
                }
                connection.commit();
                return task;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<MesRobot> listRobots() throws SQLException {
        String sql = """
                select robot_id, robot_code, robot_name, robot_status, battery_level, current_location, enabled
                from mes_robot
                order by robot_id desc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesRobot> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapRobot(rs));
            }
            return rows;
        }
    }

    public MesRobot findRobot(long robotId) throws SQLException {
        String sql = """
                select robot_id, robot_code, robot_name, robot_status, battery_level, current_location, enabled
                from mes_robot
                where robot_id = ?
                """;
        return findOne(sql, robotId, this::mapRobot, "robot not found");
    }

    public MesRobot insertRobot(MesRobot robot) throws SQLException {
        String sql = """
                insert into mes_robot
                    (robot_code, robot_name, robot_status, battery_level, current_location, enabled)
                values (?, ?, ?, ?, ?, ?)
                returning robot_id, robot_code, robot_name, robot_status, battery_level, current_location, enabled
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, defaultCode(robot.robotCode, "ROB"));
            statement.setString(2, robot.robotName);
            statement.setString(3, defaultText(robot.robotStatus, "IDLE"));
            statement.setBigDecimal(4, robot.batteryLevel);
            statement.setString(5, robot.currentLocation);
            statement.setInt(6, robot.enabled == null ? 1 : robot.enabled);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapRobot(rs);
            }
        }
    }

    public MesRobot updateRobot(long robotId, MesRobot robot) throws SQLException {
        String sql = """
                update mes_robot
                set robot_code = ?,
                    robot_name = ?,
                    robot_status = ?,
                    battery_level = ?,
                    current_location = ?,
                    enabled = ?
                where robot_id = ?
                returning robot_id, robot_code, robot_name, robot_status, battery_level, current_location, enabled
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            MesRobot current = findRobot(robotId);
            statement.setString(1, defaultText(robot.robotCode, current.robotCode));
            statement.setString(2, defaultText(robot.robotName, current.robotName));
            statement.setString(3, defaultText(robot.robotStatus, current.robotStatus));
            statement.setBigDecimal(4, robot.batteryLevel == null ? current.batteryLevel : robot.batteryLevel);
            statement.setString(5, robot.currentLocation == null ? current.currentLocation : robot.currentLocation);
            statement.setInt(6, robot.enabled == null ? current.enabled : robot.enabled);
            statement.setLong(7, robotId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("robot not found");
                }
                return mapRobot(rs);
            }
        }
    }

    public void deleteRobot(long robotId) throws SQLException {
        deleteById("delete from mes_robot where robot_id = ?", robotId, "robot not found");
    }

    public List<MesRobotDeliveryTask> listDeliveryTasks() throws SQLException {
        String sql = """
                select delivery_task_id, delivery_task_no, picking_task_id, robot_id,
                       from_location_id, to_line_id, delivery_status, load_time, handover_time
                from mes_robot_delivery_task
                order by delivery_task_id desc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesRobotDeliveryTask> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapDeliveryTask(rs));
            }
            return rows;
        }
    }

    public MesRobotDeliveryTask findDeliveryTask(long deliveryTaskId) throws SQLException {
        String sql = """
                select delivery_task_id, delivery_task_no, picking_task_id, robot_id,
                       from_location_id, to_line_id, delivery_status, load_time, handover_time
                from mes_robot_delivery_task
                where delivery_task_id = ?
                """;
        return findOne(sql, deliveryTaskId, this::mapDeliveryTask, "delivery task not found");
    }

    public MesRobotDeliveryTask insertDeliveryTask(MesRobotDeliveryTask task) throws SQLException {
        String sql = """
                insert into mes_robot_delivery_task
                    (delivery_task_no, picking_task_id, robot_id, from_location_id,
                     to_line_id, delivery_status, load_time)
                values (?, ?, ?, ?, ?, ?, current_timestamp)
                returning delivery_task_id, delivery_task_no, picking_task_id, robot_id,
                          from_location_id, to_line_id, delivery_status, load_time, handover_time
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, defaultCode(task.deliveryTaskNo, "RBT"));
            statement.setLong(2, task.pickingTaskId);
            setLong(statement, 3, task.robotId);
            statement.setLong(4, task.fromLocationId);
            statement.setLong(5, task.toLineId == null ? 1L : task.toLineId);
            statement.setString(6, defaultText(task.deliveryStatus, "PENDING"));
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapDeliveryTask(rs);
            }
        }
    }

    public MesRobotDeliveryTask markDeliveryArrived(long deliveryTaskId) throws SQLException {
        String updateDeliverySql = """
                update mes_robot_delivery_task
                set delivery_status = 'ARRIVED',
                    handover_time = current_timestamp
                where delivery_task_id = ? and delivery_status = 'PENDING'
                returning delivery_task_id, delivery_task_no, picking_task_id, robot_id,
                          from_location_id, to_line_id, delivery_status, load_time, handover_time
                """;
        try (Connection connection = Db.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(updateDeliverySql)) {
                statement.setLong(1, deliveryTaskId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        ensureDeliveryExistsAndPending(connection, deliveryTaskId);
                        throw new BadRequestException("only PENDING delivery tasks can arrive");
                    }
                    return mapDeliveryTask(rs);
                }
            }
        }
    }

    public MesRobotDeliveryTask confirmDeliveryReceipt(long deliveryTaskId) throws SQLException {
        String deliverySql = """
                select delivery_task_id, delivery_task_no, picking_task_id, robot_id,
                       from_location_id, to_line_id, delivery_status, load_time, handover_time
                from mes_robot_delivery_task
                where delivery_task_id = ? and delivery_status = 'ARRIVED'
                for update
                """;
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                MesRobotDeliveryTask deliveryTask;
                try (PreparedStatement statement = connection.prepareStatement(deliverySql)) {
                    statement.setLong(1, deliveryTaskId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            ensureDeliveryExistsAndArrived(connection, deliveryTaskId);
                            throw new BadRequestException("only ARRIVED delivery tasks can be confirmed");
                        }
                        deliveryTask = mapDeliveryTask(rs);
                    }
                }
                ensureRequisitionCanReceive(connection, deliveryTask.pickingTaskId);
                deductInventoryForPicking(connection, deliveryTask.pickingTaskId);
                connection.commit();
                return deliveryTask;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void deductInventoryForPicking(Connection connection, long pickingTaskId) throws SQLException {
        String itemSql = """
                select i.requisition_item_id, i.material_id, i.required_qty, i.unit, i.batch_no
                from mes_material_requisition_item i
                join mes_picking_task p on p.requisition_id = i.requisition_id
                where p.picking_task_id = ?
                order by i.requisition_item_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(itemSql)) {
            statement.setLong(1, pickingTaskId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    long itemId = rs.getLong("requisition_item_id");
                    long materialId = rs.getLong("material_id");
                    BigDecimal qty = rs.getBigDecimal("required_qty");
                    String batchNo = rs.getString("batch_no");
                    MesInventory inventory = findInventoryForDeduction(connection, materialId, batchNo, qty);
                    updateInventoryAfterDeduction(connection, inventory.inventoryId, qty);
                    updateRequisitionItemCompleted(connection, itemId, qty);
                    insertInventoryTransaction(connection, materialId, inventory.inventoryId, qty, pickingTaskId);
                }
            }
        }
        markRequisitionCompleted(connection, pickingTaskId);
    }

    private MesInventory findInventoryForDeduction(Connection connection, long materialId, String batchNo, BigDecimal qty) throws SQLException {
        String sql = """
                select inventory_id, material_id, warehouse_id, location_id, batch_no,
                       available_qty, reserved_qty, frozen_qty, quality_status, last_check_time
                from mes_inventory
                where material_id = ?
                  and available_qty >= ?
                  and (? is null or batch_no = ?)
                order by inventory_id
                limit 1
                for update
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, materialId);
            statement.setBigDecimal(2, qty);
            statement.setString(3, batchNo);
            statement.setString(4, batchNo);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new BadRequestException("inventory is not enough for materialId " + materialId);
                }
                return mapInventory(rs);
            }
        }
    }

    private void updateInventoryAfterDeduction(Connection connection, long inventoryId, BigDecimal qty) throws SQLException {
        String sql = """
                update mes_inventory
                set available_qty = available_qty - ?,
                    last_check_time = current_timestamp
                where inventory_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, qty);
            statement.setLong(2, inventoryId);
            statement.executeUpdate();
        }
    }

    private void updateRequisitionItemCompleted(Connection connection, long itemId, BigDecimal qty) throws SQLException {
        String sql = """
                update mes_material_requisition_item
                set issued_qty = ?,
                    item_status = 'COMPLETED'
                where requisition_item_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, qty);
            statement.setLong(2, itemId);
            statement.executeUpdate();
        }
    }

    private void insertInventoryTransaction(Connection connection, long materialId, long inventoryId, BigDecimal qty, long pickingTaskId) throws SQLException {
        String sql = """
                insert into mes_inventory_transaction
                    (transaction_no, material_id, inventory_id, transaction_type, qty,
                     source_doc_type, source_doc_id, operator_id)
                values (?, ?, ?, 'OUT', ?, 'PICKING_TASK', ?, 1)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, IdGenerator.nextCode("TX"));
            statement.setLong(2, materialId);
            statement.setLong(3, inventoryId);
            statement.setBigDecimal(4, qty);
            statement.setLong(5, pickingTaskId);
            statement.executeUpdate();
        }
    }

    private void markRequisitionCompleted(Connection connection, long pickingTaskId) throws SQLException {
        String sql = """
                update mes_material_requisition
                set request_status = 'COMPLETED'
                where requisition_id = (
                    select requisition_id from mes_picking_task where picking_task_id = ?
                )
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pickingTaskId);
            statement.executeUpdate();
        }
    }

    private void ensureRequisitionExistsAndCreated(Connection connection, long requisitionId) throws SQLException {
        String sql = "select request_status from mes_material_requisition where requisition_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requisitionId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("requisition not found");
                }
            }
        }
    }

    private void ensureWorkOrderExecutable(Connection connection, long workOrderId) throws SQLException {
        String sql = "select work_order_status from mes_work_order where work_order_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, workOrderId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("work order not found");
                }
                String status = rs.getString("work_order_status");
                if (!"DISPATCHED".equals(status) && !"RECEIVED".equals(status) && !"RUNNING".equals(status)) {
                    throw new BadRequestException("work order status does not allow requisition: " + status);
                }
            }
        }
    }

    private void ensurePickingExistsAndCreated(Connection connection, long pickingTaskId) throws SQLException {
        String sql = "select task_status from mes_picking_task where picking_task_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pickingTaskId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("picking task not found");
                }
            }
        }
    }

    private void ensureDeliveryExistsAndPending(Connection connection, long deliveryTaskId) throws SQLException {
        String sql = "select delivery_status from mes_robot_delivery_task where delivery_task_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, deliveryTaskId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("delivery task not found");
                }
            }
        }
    }

    private void ensureDeliveryExistsAndArrived(Connection connection, long deliveryTaskId) throws SQLException {
        String sql = "select delivery_status from mes_robot_delivery_task where delivery_task_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, deliveryTaskId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("delivery task not found");
                }
            }
        }
    }

    private void ensureRequisitionCanReceive(Connection connection, long pickingTaskId) throws SQLException {
        String sql = """
                select r.request_status
                from mes_material_requisition r
                join mes_picking_task p on p.requisition_id = r.requisition_id
                where p.picking_task_id = ?
                for update
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pickingTaskId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("requisition not found");
                }
                if ("COMPLETED".equals(rs.getString("request_status"))) {
                    throw new BadRequestException("materials have already been received for this delivery task");
                }
            }
        }
    }

    private Long firstWarehouseId(Connection connection) throws SQLException {
        return firstId(connection, "select warehouse_id from mes_warehouse order by warehouse_id limit 1");
    }

    private Long firstLocationId(Connection connection) throws SQLException {
        return firstId(connection, "select location_id from mes_warehouse_location order by location_id limit 1");
    }

    private Long firstRobotId(Connection connection) throws SQLException {
        return firstId(connection, "select robot_id from mes_robot where enabled = 1 order by robot_id limit 1");
    }

    private Long firstId(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : null;
        }
    }

    private <T> T findOne(String sql, long id, RowMapper<T> mapper, String notFoundMessage) throws SQLException {
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException(notFoundMessage);
                }
                return mapper.map(rs);
            }
        }
    }

    private void deleteById(String sql, long id, String notFoundMessage) throws SQLException {
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new NotFoundException(notFoundMessage);
            }
        }
    }

    private List<MesMaterialRequisition> mapRequisitions(ResultSet rs) throws SQLException {
        Map<Long, MesMaterialRequisition> rows = new LinkedHashMap<>();
        while (rs.next()) {
            MesMaterialRequisition requisition = rows.computeIfAbsent(
                    rs.getLong("requisition_id"),
                    ignored -> mapRequisitionUnchecked(rs)
            );
            Long itemId = getLong(rs, "requisition_item_id");
            if (itemId != null) {
                requisition.items.add(mapRequisitionItem(rs, requisition.requisitionId));
            }
        }
        return new ArrayList<>(rows.values());
    }

    private MesMaterial mapMaterial(ResultSet rs) throws SQLException {
        MesMaterial item = new MesMaterial();
        item.materialId = rs.getLong("material_id");
        item.materialCode = rs.getString("material_code");
        item.materialName = rs.getString("material_name");
        item.materialType = rs.getString("material_type");
        item.specification = rs.getString("specification");
        item.unit = rs.getString("unit");
        item.shelfLifeDays = getInteger(rs, "shelf_life_days");
        item.enabled = rs.getInt("enabled");
        item.createdAt = getLocalDateTime(rs, "created_at");
        return item;
    }

    private MesWarehouse mapWarehouse(ResultSet rs) throws SQLException {
        MesWarehouse item = new MesWarehouse();
        item.warehouseId = rs.getLong("warehouse_id");
        item.warehouseCode = rs.getString("warehouse_code");
        item.warehouseName = rs.getString("warehouse_name");
        item.warehouseType = rs.getString("warehouse_type");
        item.enabled = rs.getInt("enabled");
        return item;
    }

    private MesWarehouseLocation mapLocation(ResultSet rs) throws SQLException {
        MesWarehouseLocation item = new MesWarehouseLocation();
        item.locationId = rs.getLong("location_id");
        item.warehouseId = rs.getLong("warehouse_id");
        item.locationCode = rs.getString("location_code");
        item.locationName = rs.getString("location_name");
        item.enabled = rs.getInt("enabled");
        return item;
    }

    private MesInventory mapInventory(ResultSet rs) throws SQLException {
        MesInventory item = new MesInventory();
        item.inventoryId = rs.getLong("inventory_id");
        item.materialId = rs.getLong("material_id");
        item.warehouseId = rs.getLong("warehouse_id");
        item.locationId = rs.getLong("location_id");
        item.batchNo = rs.getString("batch_no");
        item.availableQty = rs.getBigDecimal("available_qty");
        item.reservedQty = rs.getBigDecimal("reserved_qty");
        item.frozenQty = rs.getBigDecimal("frozen_qty");
        item.qualityStatus = rs.getString("quality_status");
        item.lastCheckTime = getLocalDateTime(rs, "last_check_time");
        return item;
    }

    private MesInventoryTransaction mapTransaction(ResultSet rs) throws SQLException {
        MesInventoryTransaction item = new MesInventoryTransaction();
        item.transactionId = rs.getLong("transaction_id");
        item.transactionNo = rs.getString("transaction_no");
        item.materialId = rs.getLong("material_id");
        item.inventoryId = getLong(rs, "inventory_id");
        item.transactionType = rs.getString("transaction_type");
        item.qty = rs.getBigDecimal("qty");
        item.sourceDocType = rs.getString("source_doc_type");
        item.sourceDocId = getLong(rs, "source_doc_id");
        item.operatorId = getLong(rs, "operator_id");
        item.createdAt = getLocalDateTime(rs, "created_at");
        return item;
    }

    private MesMaterialRequisition mapRequisition(ResultSet rs) throws SQLException {
        MesMaterialRequisition item = new MesMaterialRequisition();
        item.requisitionId = rs.getLong("requisition_id");
        item.requisitionNo = rs.getString("requisition_no");
        item.workOrderId = rs.getLong("work_order_id");
        item.requestedBy = rs.getLong("requested_by");
        item.requestStatus = rs.getString("request_status");
        item.requestTime = getLocalDateTime(rs, "request_time");
        item.approvedBy = getLong(rs, "approved_by");
        item.approvedTime = getLocalDateTime(rs, "approved_time");
        item.remark = rs.getString("remark");
        return item;
    }

    private MesMaterialRequisition mapRequisitionUnchecked(ResultSet rs) {
        try {
            return mapRequisition(rs);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private MesMaterialRequisitionItem mapRequisitionItem(ResultSet rs, long requisitionId) throws SQLException {
        MesMaterialRequisitionItem item = new MesMaterialRequisitionItem();
        item.requisitionItemId = rs.getLong("requisition_item_id");
        item.requisitionId = requisitionId;
        item.materialId = rs.getLong("material_id");
        item.requiredQty = rs.getBigDecimal("required_qty");
        item.issuedQty = rs.getBigDecimal("issued_qty");
        item.unit = rs.getString("unit");
        item.batchNo = rs.getString("batch_no");
        item.itemStatus = rs.getString("item_status");
        return item;
    }

    private MesPickingTask mapPickingTask(ResultSet rs) throws SQLException {
        MesPickingTask item = new MesPickingTask();
        item.pickingTaskId = rs.getLong("picking_task_id");
        item.pickingTaskNo = rs.getString("picking_task_no");
        item.requisitionId = rs.getLong("requisition_id");
        item.warehouseId = rs.getLong("warehouse_id");
        item.taskStatus = rs.getString("task_status");
        item.assignedTo = getLong(rs, "assigned_to");
        item.startTime = getLocalDateTime(rs, "start_time");
        item.finishTime = getLocalDateTime(rs, "finish_time");
        return item;
    }

    private MesRobot mapRobot(ResultSet rs) throws SQLException {
        MesRobot item = new MesRobot();
        item.robotId = rs.getLong("robot_id");
        item.robotCode = rs.getString("robot_code");
        item.robotName = rs.getString("robot_name");
        item.robotStatus = rs.getString("robot_status");
        item.batteryLevel = rs.getBigDecimal("battery_level");
        item.currentLocation = rs.getString("current_location");
        item.enabled = rs.getInt("enabled");
        return item;
    }

    private MesRobotDeliveryTask mapDeliveryTask(ResultSet rs) throws SQLException {
        MesRobotDeliveryTask item = new MesRobotDeliveryTask();
        item.deliveryTaskId = rs.getLong("delivery_task_id");
        item.deliveryTaskNo = rs.getString("delivery_task_no");
        item.pickingTaskId = rs.getLong("picking_task_id");
        item.robotId = getLong(rs, "robot_id");
        item.fromLocationId = rs.getLong("from_location_id");
        item.toLineId = rs.getLong("to_line_id");
        item.deliveryStatus = rs.getString("delivery_status");
        item.loadTime = getLocalDateTime(rs, "load_time");
        item.handoverTime = getLocalDateTime(rs, "handover_time");
        return item;
    }

    private static String defaultCode(String value, String prefix) {
        return value == null || value.isBlank() ? IdGenerator.nextCode(prefix) : value;
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static void setInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static void setLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}
