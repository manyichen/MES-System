package com.example.messystem.warehouse.dao;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.warehouse.entity.ExternalPurchaseRequest;
import com.example.messystem.warehouse.entity.ExternalPurchaseResult;
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
                select m.material_id, m.material_code, m.material_name, m.material_type, m.specification,
                       m.unit, m.shelf_life_days, m.enabled, m.created_at,
                       case when m.material_type in ('RAW', 'AUX') then 'RAW'
                            when m.material_type = 'WIP' then 'WIP'
                            when m.material_type = 'FINISHED' then 'FINISHED'
                       end as default_warehouse_type,
                       dw.warehouse_id as default_warehouse_id,
                       dw.warehouse_code as default_warehouse_code,
                       dw.warehouse_name as default_warehouse_name
                from mes_material m
                left join lateral (
                    select w.warehouse_id, w.warehouse_code, w.warehouse_name
                    from mes_warehouse w
                    where w.enabled = 1
                      and w.warehouse_type = case when m.material_type in ('RAW', 'AUX') then 'RAW'
                                                   when m.material_type = 'WIP' then 'WIP'
                                                   when m.material_type = 'FINISHED' then 'FINISHED'
                                              end
                    order by w.warehouse_id
                    limit 1
                ) dw on true
                order by m.material_id asc
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
                order by warehouse_id asc
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
                order by location_id asc
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
                select i.inventory_id, i.material_id,
                       m.material_code, m.material_name, m.material_type, m.specification, m.unit,
                       i.warehouse_id, w.warehouse_code, w.warehouse_name,
                       i.location_id, l.location_code, l.location_name, i.batch_no,
                       i.available_qty,
                       sum(case when i.quality_status = 'QUALIFIED' then i.available_qty else 0 end)
                           over (partition by i.warehouse_id, i.material_id)
                           as warehouse_material_available_qty,
                       i.reserved_qty, i.frozen_qty, i.quality_status, i.last_check_time
                from mes_inventory i
                left join mes_material m on m.material_id = i.material_id
                left join mes_warehouse w on w.warehouse_id = i.warehouse_id
                left join mes_warehouse_location l on l.location_id = i.location_id
                order by w.warehouse_name, m.material_name, i.batch_no, i.inventory_id
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
                order by inventory_id asc
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
                order by created_at asc, transaction_id asc
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

    public ExternalPurchaseResult externalPurchase(ExternalPurchaseRequest request, long operatorId) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                MesMaterial material = findMaterial(request.materialId);
                MesWarehouse warehouse = findWarehouse(request.warehouseId);
                String expectedWarehouseType = warehouseTypeForMaterial(material.materialType);
                if (warehouse.enabled == null || warehouse.enabled != 1) {
                    throw new BadRequestException("目标仓库已停用，不能办理采购入库");
                }
                if (expectedWarehouseType != null
                        && !expectedWarehouseType.equals(warehouse.warehouseType)) {
                    throw new BadRequestException(material.materialName + "应入库到"
                            + expectedWarehouseType + "类型仓库");
                }
                Long locationId = request.locationId == null || request.locationId <= 0
                        ? ensurePurchaseLocation(connection, request.warehouseId)
                        : request.locationId;
                ensureLocationInWarehouse(connection, locationId, request.warehouseId);

                String batchNo = defaultText(request.batchNo, IdGenerator.nextCode("PUR-BATCH"));
                MesInventory inventory = findInventoryForPurchase(connection,
                        request.materialId, request.warehouseId, locationId, batchNo);
                if (inventory == null) {
                    inventory = insertInventoryForPurchase(connection,
                            request.materialId, request.warehouseId, locationId, batchNo, request.qty);
                } else {
                    inventory = increaseInventoryForPurchase(connection, inventory.inventoryId, request.qty);
                }
                MesInventoryTransaction transaction = insertPurchaseTransaction(
                        connection, request.materialId, inventory.inventoryId, request.qty, operatorId);

                ExternalPurchaseResult result = new ExternalPurchaseResult();
                result.purchaseNo = transaction.transactionNo.replaceFirst("^TX-", "PO-");
                result.supplierStatus = "CONFIRMED";
                result.message = "external supplier purchase completed";
                result.inventory = inventory;
                result.transaction = transaction;
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

    public void receiveFinishedGoodsFromQuality(long inspectionId, long operatorId) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (hasFinishedGoodsReceipt(connection, inspectionId)) {
                    connection.commit();
                    return;
                }

                FinishedGoodsReceiptContext context = findFinishedGoodsReceiptContext(connection, inspectionId);
                if (context.inboundQty().signum() <= 0) {
                    connection.commit();
                    return;
                }

                long warehouseId = ensureFinishedGoodsWarehouse(connection);
                long locationId = ensureFinishedGoodsLocation(connection, warehouseId);
                long materialId = ensureFinishedGoodsMaterial(connection, context);
                String batchNo = defaultText(context.batchNo(), "FG-QI-" + inspectionId);

                MesInventory inventory = findInventoryForPurchase(connection, materialId, warehouseId, locationId, batchNo);
                if (inventory == null) {
                    inventory = insertInventoryForPurchase(connection, materialId, warehouseId, locationId, batchNo,
                            context.inboundQty());
                } else {
                    inventory = increaseInventoryForPurchase(connection, inventory.inventoryId, context.inboundQty());
                }
                insertFinishedGoodsTransaction(connection, materialId, inventory.inventoryId, context.inboundQty(),
                        inspectionId, operatorId);

                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<MesMaterialRequisition> listRequisitions() throws SQLException {
        String sql = """
                select r.requisition_id, r.requisition_no, r.work_order_id, r.warehouse_id, r.requested_by,
                       r.request_status, r.request_time, r.approved_by, r.approved_time, r.remark,
                       p.picking_task_id, p.task_status as picking_task_status,
                       d.delivery_task_id, d.delivery_status,
                       i.requisition_item_id, i.material_id, i.required_qty, i.issued_qty,
                       i.unit, i.batch_no, i.item_status
                from mes_material_requisition r
                left join mes_material_requisition_item i on i.requisition_id = r.requisition_id
                left join mes_picking_task p on p.requisition_id = r.requisition_id
                left join mes_robot_delivery_task d on d.picking_task_id = p.picking_task_id
                order by r.requisition_id asc, i.requisition_item_id asc
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
                select r.requisition_id, r.requisition_no, r.work_order_id, r.warehouse_id, r.requested_by,
                       r.request_status, r.request_time, r.approved_by, r.approved_time, r.remark,
                       p.picking_task_id, p.task_status as picking_task_status,
                       d.delivery_task_id, d.delivery_status,
                       i.requisition_item_id, i.material_id, i.required_qty, i.issued_qty,
                       i.unit, i.batch_no, i.item_status
                from mes_material_requisition r
                left join mes_material_requisition_item i on i.requisition_id = r.requisition_id
                left join mes_picking_task p on p.requisition_id = r.requisition_id
                left join mes_robot_delivery_task d on d.picking_task_id = p.picking_task_id
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
                select r.requisition_id, r.requisition_no, r.work_order_id, r.warehouse_id, r.requested_by,
                       r.request_status, r.request_time, r.approved_by, r.approved_time, r.remark,
                       p.picking_task_id, p.task_status as picking_task_status,
                       d.delivery_task_id, d.delivery_status,
                       i.requisition_item_id, i.material_id, i.required_qty, i.issued_qty,
                       i.unit, i.batch_no, i.item_status
                from mes_material_requisition r
                left join mes_material_requisition_item i on i.requisition_id = r.requisition_id
                left join mes_picking_task p on p.requisition_id = r.requisition_id
                left join mes_robot_delivery_task d on d.picking_task_id = p.picking_task_id
                where r.work_order_id = ?
                order by r.requisition_id asc, i.requisition_item_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, workOrderId);
            try (ResultSet rs = statement.executeQuery()) {
                return mapRequisitions(rs);
            }
        }
    }

    public List<MesMaterialRequisition> listRequisitionsByRequester(long requesterId) throws SQLException {
        String sql = """
                select r.requisition_id, r.requisition_no, r.work_order_id, r.warehouse_id, r.requested_by,
                       r.request_status, r.request_time, r.approved_by, r.approved_time, r.remark,
                       p.picking_task_id, p.task_status as picking_task_status,
                       d.delivery_task_id, d.delivery_status,
                       i.requisition_item_id, i.material_id, i.required_qty, i.issued_qty,
                       i.unit, i.batch_no, i.item_status
                from mes_material_requisition r
                left join mes_material_requisition_item i on i.requisition_id = r.requisition_id
                left join mes_picking_task p on p.requisition_id = r.requisition_id
                left join mes_robot_delivery_task d on d.picking_task_id = p.picking_task_id
                where r.requested_by = ?
                order by r.requisition_id asc, i.requisition_item_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requesterId);
            try (ResultSet rs = statement.executeQuery()) {
                return mapRequisitions(rs);
            }
        }
    }

    public MesMaterialRequisition findRequisitionForRequester(long requisitionId, long requesterId) throws SQLException {
        String sql = """
                select r.requisition_id, r.requisition_no, r.work_order_id, r.warehouse_id, r.requested_by,
                       r.request_status, r.request_time, r.approved_by, r.approved_time, r.remark,
                       p.picking_task_id, p.task_status as picking_task_status,
                       d.delivery_task_id, d.delivery_status,
                       i.requisition_item_id, i.material_id, i.required_qty, i.issued_qty,
                       i.unit, i.batch_no, i.item_status
                from mes_material_requisition r
                left join mes_material_requisition_item i on i.requisition_id = r.requisition_id
                left join mes_picking_task p on p.requisition_id = r.requisition_id
                left join mes_robot_delivery_task d on d.picking_task_id = p.picking_task_id
                where r.requisition_id = ? and r.requested_by = ?
                order by i.requisition_item_id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requisitionId);
            statement.setLong(2, requesterId);
            try (ResultSet rs = statement.executeQuery()) {
                List<MesMaterialRequisition> rows = mapRequisitions(rs);
                if (rows.isEmpty()) {
                    throw new NotFoundException("requisition not found");
                }
                return rows.get(0);
            }
        }
    }

    /**
     * 检查生产操作工是否为工单的被派工人或接收人。
     * 归属查询放在 DAO 中，避免控制器直接执行 SQL。
     */
    public boolean isWorkOrderAssignedTo(long workOrderId, long userId) throws SQLException {
        String sql = """
                select 1
                from mes_work_order
                where work_order_id = ?
                  and (assigned_to = ? or accepted_by = ?)
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, workOrderId);
            statement.setLong(2, userId);
            statement.setLong(3, userId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public MesMaterialRequisition insertRequisition(MesMaterialRequisition requisition) throws SQLException {
        String requisitionSql = """
                insert into mes_material_requisition
                    (requisition_no, work_order_id, warehouse_id, requested_by, request_status, remark)
                values (?, ?, ?, ?, 'CREATED', ?)
                returning requisition_id, requisition_no, work_order_id, warehouse_id, requested_by,
                          request_status, request_time, approved_by, approved_time, remark,
                          null::bigint as picking_task_id, null::varchar as picking_task_status,
                          null::bigint as delivery_task_id, null::varchar as delivery_status
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
                    statement.setLong(3, requisition.warehouseId);
                    statement.setLong(4, requisition.requestedBy == null ? 1L : requisition.requestedBy);
                    statement.setString(5, requisition.remark);
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

    public MesMaterialRequisition receiveRequisition(long requisitionId, Long receivedBy) throws SQLException {
        String sql = """
                update mes_material_requisition
                set request_status = 'RECEIVED',
                    remark = coalesce(remark, '')
                where requisition_id = ? and request_status = 'CREATED'
                returning requisition_id, requisition_no, work_order_id, warehouse_id, requested_by,
                          request_status, request_time, approved_by, approved_time, remark,
                          null::bigint as picking_task_id, null::varchar as picking_task_status,
                          null::bigint as delivery_task_id, null::varchar as delivery_status
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requisitionId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    ensureRequisitionExists(connection, requisitionId);
                    throw new BadRequestException("only CREATED requisitions can be received");
                }
                MesMaterialRequisition requisition = mapRequisition(rs);
                requisition.items = findRequisitionItems(connection, requisitionId);
                return requisition;
            }
        }
    }

    public MesMaterialRequisition approveRequisition(long requisitionId, Long approvedBy) throws SQLException {
        String updateSql = """
                update mes_material_requisition
                set request_status = 'APPROVED',
                    approved_by = ?,
                    approved_time = current_timestamp
                where requisition_id = ? and request_status = 'RECEIVED'
                returning requisition_id, requisition_no, work_order_id, warehouse_id, requested_by,
                          request_status, request_time, approved_by, approved_time, remark,
                          null::bigint as picking_task_id, null::varchar as picking_task_status,
                          null::bigint as delivery_task_id, null::varchar as delivery_status
                """;
        String pickingSql = """
                insert into mes_picking_task
                    (picking_task_no, requisition_id, warehouse_id, task_status)
                values (?, ?, ?, 'CREATED')
                returning picking_task_id, task_status
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
                            ensureRequisitionExists(connection, requisitionId);
                            throw new BadRequestException("only RECEIVED requisitions can be approved");
                        }
                        requisition = mapRequisition(rs);
                    }
                }
                Long warehouseId = requisition.warehouseId;
                if (warehouseId == null) {
                    throw new BadRequestException("warehouse is required before approving requisition");
                }
                try (PreparedStatement statement = connection.prepareStatement(pickingSql)) {
                    statement.setString(1, IdGenerator.nextCode("PICK"));
                    statement.setLong(2, requisitionId);
                    statement.setLong(3, warehouseId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            requisition.pickingTaskId = rs.getLong("picking_task_id");
                            requisition.pickingTaskStatus = rs.getString("task_status");
                        }
                    }
                }
                requisition.items = findRequisitionItems(connection, requisitionId);
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

    public MesMaterialRequisition rejectRequisition(long requisitionId, Long approvedBy, String reason) throws SQLException {
        String sql = """
                update mes_material_requisition
                set request_status = 'REJECTED',
                    approved_by = ?,
                    approved_time = current_timestamp,
                    remark = coalesce(nullif(?, ''), remark)
                where requisition_id = ? and request_status in ('CREATED','RECEIVED')
                returning requisition_id, requisition_no, work_order_id, warehouse_id, requested_by,
                          request_status, request_time, approved_by, approved_time, remark,
                          null::bigint as picking_task_id, null::varchar as picking_task_status,
                          null::bigint as delivery_task_id, null::varchar as delivery_status
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, approvedBy == null ? 1L : approvedBy);
            statement.setString(2, reason);
            statement.setLong(3, requisitionId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    ensureRequisitionExists(connection, requisitionId);
                    throw new BadRequestException("only CREATED or RECEIVED requisitions can be rejected");
                }
                MesMaterialRequisition requisition = mapRequisition(rs);
                requisition.items = findRequisitionItems(connection, requisitionId);
                return requisition;
            }
        }
    }

    private void ensureInventoryEnoughForRequisition(Connection connection, long requisitionId) throws SQLException {
        String sql = """
                select i.material_id, i.required_qty, i.batch_no,
                       m.material_name, m.unit, w.warehouse_name,
                       coalesce(sum(inv.available_qty), 0) as available_qty
                from mes_material_requisition_item i
                join mes_material_requisition r on r.requisition_id = i.requisition_id
                left join mes_material m on m.material_id = i.material_id
                left join mes_warehouse w on w.warehouse_id = r.warehouse_id
                left join mes_inventory inv on inv.material_id = i.material_id
                    and inv.available_qty > 0
                    and inv.quality_status = 'QUALIFIED'
                    and inv.warehouse_id = r.warehouse_id
                    and (nullif(btrim(i.batch_no), '') is null or inv.batch_no = i.batch_no)
                where i.requisition_id = ?
                group by i.requisition_item_id, i.material_id, i.required_qty, i.batch_no,
                         m.material_name, m.unit, w.warehouse_name
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
                        String materialName = defaultText(rs.getString("material_name"),
                                "物料 #" + rs.getLong("material_id"));
                        String warehouseName = defaultText(rs.getString("warehouse_name"), "目标仓库");
                        String unit = defaultText(rs.getString("unit"), "");
                        throw new BadRequestException(materialName + "在" + warehouseName
                                + "的可用库存为 " + availableQty.stripTrailingZeros().toPlainString() + unit
                                + "，领料需要 " + requiredQty.stripTrailingZeros().toPlainString() + unit
                                + "；请采购入同一仓库后重新批准");
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
                order by picking_task_id asc
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
                Long locationId = firstLocationId(connection, task.warehouseId);
                if (locationId == null) {
                    throw new BadRequestException("warehouse location is required before completing picking");
                }
                try (PreparedStatement statement = connection.prepareStatement(deliverySql)) {
                    statement.setString(1, IdGenerator.nextCode("RBT"));
                    statement.setLong(2, pickingTaskId);
                    setLong(statement, 3, firstRobotId(connection, task.warehouseId));
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
                select robot_id, robot_code, robot_name, warehouse_id, robot_status, battery_level, current_location, enabled
                from mes_robot
                order by robot_id asc
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
                select robot_id, robot_code, robot_name, warehouse_id, robot_status, battery_level, current_location, enabled
                from mes_robot
                where robot_id = ?
                """;
        return findOne(sql, robotId, this::mapRobot, "robot not found");
    }

    public MesRobot insertRobot(MesRobot robot) throws SQLException {
        String sql = """
                insert into mes_robot
                    (robot_code, robot_name, warehouse_id, robot_status, battery_level, current_location, enabled)
                values (?, ?, ?, ?, ?, ?, ?)
                returning robot_id, robot_code, robot_name, warehouse_id, robot_status, battery_level, current_location, enabled
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, defaultCode(robot.robotCode, "ROB"));
            statement.setString(2, robot.robotName);
            setLong(statement, 3, robot.warehouseId);
            statement.setString(4, defaultText(robot.robotStatus, "IDLE"));
            statement.setBigDecimal(5, robot.batteryLevel);
            statement.setString(6, robot.currentLocation);
            statement.setInt(7, robot.enabled == null ? 1 : robot.enabled);
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
                    warehouse_id = ?,
                    robot_status = ?,
                    battery_level = ?,
                    current_location = ?,
                    enabled = ?
                where robot_id = ?
                returning robot_id, robot_code, robot_name, warehouse_id, robot_status, battery_level, current_location, enabled
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            MesRobot current = findRobot(robotId);
            statement.setString(1, defaultText(robot.robotCode, current.robotCode));
            statement.setString(2, defaultText(robot.robotName, current.robotName));
            setLong(statement, 3, robot.warehouseId == null ? current.warehouseId : robot.warehouseId);
            statement.setString(4, defaultText(robot.robotStatus, current.robotStatus));
            statement.setBigDecimal(5, robot.batteryLevel == null ? current.batteryLevel : robot.batteryLevel);
            statement.setString(6, robot.currentLocation == null ? current.currentLocation : robot.currentLocation);
            statement.setInt(7, robot.enabled == null ? current.enabled : robot.enabled);
            statement.setLong(8, robotId);
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
                order by delivery_task_id asc
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
        return confirmDeliveryReceipt(deliveryTaskId, null);
    }

    public MesRobotDeliveryTask confirmDeliveryReceipt(long deliveryTaskId, Long requesterId) throws SQLException {
        String deliverySql = """
                select delivery_task_id, delivery_task_no, picking_task_id, robot_id,
                       from_location_id, to_line_id, delivery_status, load_time, handover_time
                from mes_robot_delivery_task
                where delivery_task_id = ? and delivery_status = 'ARRIVED'
                for update
                """;
        String receiveSql = """
                update mes_robot_delivery_task
                set delivery_status = 'RECEIVED',
                    handover_time = current_timestamp
                where delivery_task_id = ?
                returning delivery_task_id, delivery_task_no, picking_task_id, robot_id,
                          from_location_id, to_line_id, delivery_status, load_time, handover_time
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
                if (requesterId != null) {
                    ensureDeliveryRequestedBy(connection, deliveryTask.pickingTaskId, requesterId);
                }
                ensureRequisitionCanReceive(connection, deliveryTask.pickingTaskId);
                deductInventoryForPicking(connection, deliveryTask.pickingTaskId);
                try (PreparedStatement statement = connection.prepareStatement(receiveSql)) {
                    statement.setLong(1, deliveryTaskId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            deliveryTask = mapDeliveryTask(rs);
                        }
                    }
                }
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

    private void ensureDeliveryRequestedBy(Connection connection, long pickingTaskId, long requesterId) throws SQLException {
        String sql = """
                select r.requested_by
                from mes_material_requisition r
                join mes_picking_task p on p.requisition_id = r.requisition_id
                where p.picking_task_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pickingTaskId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("requisition not found");
                }
                if (rs.getLong("requested_by") != requesterId) {
                    throw new BadRequestException("delivery task is not requested by current operator");
                }
            }
        }
    }

    private void deductInventoryForPicking(Connection connection, long pickingTaskId) throws SQLException {
        String itemSql = """
                select i.requisition_item_id, i.material_id, i.required_qty, i.unit, i.batch_no, p.warehouse_id
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
                    long warehouseId = rs.getLong("warehouse_id");
                    deductAvailableInventory(connection, warehouseId, materialId, batchNo, qty, pickingTaskId);
                    updateRequisitionItemCompleted(connection, itemId, qty);
                }
            }
        }
        markRequisitionCompleted(connection, pickingTaskId);
    }

    private void deductAvailableInventory(Connection connection, long warehouseId, long materialId,
            String batchNo, BigDecimal qty, long pickingTaskId) throws SQLException {
        String normalizedBatchNo = batchNo == null || batchNo.isBlank() ? null : batchNo;
        String sql = """
                select inventory_id, material_id, warehouse_id, location_id, batch_no,
                       available_qty, reserved_qty, frozen_qty, quality_status, last_check_time
                from mes_inventory
                where material_id = ?
                  and warehouse_id = ?
                  and available_qty > 0
                  and quality_status = 'QUALIFIED'
                  and (? is null or batch_no = ?)
                order by last_check_time, inventory_id
                for update
                """;
        BigDecimal remaining = qty;
        List<MesInventory> inventories = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, materialId);
            statement.setLong(2, warehouseId);
            statement.setString(3, normalizedBatchNo);
            statement.setString(4, normalizedBatchNo);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) inventories.add(mapInventory(rs));
            }
        }
        for (MesInventory inventory : inventories) {
            if (remaining.signum() <= 0) break;
            BigDecimal deducted = inventory.availableQty.min(remaining);
            updateInventoryAfterDeduction(connection, inventory.inventoryId, deducted);
            insertInventoryTransaction(
                    connection, materialId, inventory.inventoryId, deducted, pickingTaskId);
            remaining = remaining.subtract(deducted);
        }
        if (remaining.signum() > 0) {
            throw new BadRequestException("inventory is not enough for materialId " + materialId);
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

    private void ensureRequisitionExists(Connection connection, long requisitionId) throws SQLException {
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
                String status = rs.getString("request_status");
                if ("COMPLETED".equals(status)) {
                    throw new BadRequestException("materials have already been received for this delivery task");
                }
                if (!"APPROVED".equals(status)) {
                    throw new BadRequestException("only APPROVED requisitions can be received");
                }
            }
        }
    }

    private Long firstWarehouseId(Connection connection) throws SQLException {
        return firstId(connection, "select warehouse_id from mes_warehouse order by warehouse_id limit 1");
    }

    private Long firstLocationId(Connection connection, long warehouseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select location_id from mes_warehouse_location where warehouse_id = ? order by location_id limit 1")) {
            statement.setLong(1, warehouseId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    private Long ensurePurchaseLocation(Connection connection, long warehouseId) throws SQLException {
        Long existing = firstLocationId(connection, warehouseId);
        if (existing != null) {
            return existing;
        }
        String sql = """
                insert into mes_warehouse_location (warehouse_id, location_code, location_name, enabled)
                values (?, ?, ?, 1)
                returning location_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            statement.setString(2, IdGenerator.nextCode("PUR-LOC"));
            statement.setString(3, "采购补料暂存库位");
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong("location_id");
            }
        }
    }

    private void ensureLocationInWarehouse(Connection connection, long locationId, long warehouseId) throws SQLException {
        String sql = "select 1 from mes_warehouse_location where location_id = ? and warehouse_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, locationId);
            statement.setLong(2, warehouseId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new BadRequestException("warehouse location does not belong to target warehouse");
                }
            }
        }
    }

    private MesInventory findInventoryForPurchase(
            Connection connection,
            long materialId,
            long warehouseId,
            long locationId,
            String batchNo
    ) throws SQLException {
        String sql = """
                select inventory_id, material_id, warehouse_id, location_id, batch_no,
                       available_qty, reserved_qty, frozen_qty, quality_status, last_check_time
                from mes_inventory
                where material_id = ?
                  and warehouse_id = ?
                  and location_id = ?
                  and batch_no = ?
                  and quality_status = 'QUALIFIED'
                order by inventory_id
                limit 1
                for update
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, materialId);
            statement.setLong(2, warehouseId);
            statement.setLong(3, locationId);
            statement.setString(4, batchNo);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapInventory(rs) : null;
            }
        }
    }

    private MesInventory insertInventoryForPurchase(
            Connection connection,
            long materialId,
            long warehouseId,
            long locationId,
            String batchNo,
            BigDecimal qty
    ) throws SQLException {
        String sql = """
                insert into mes_inventory
                    (material_id, warehouse_id, location_id, batch_no, available_qty,
                     reserved_qty, frozen_qty, quality_status, last_check_time)
                values (?, ?, ?, ?, ?, 0, 0, 'QUALIFIED', current_timestamp)
                returning inventory_id, material_id, warehouse_id, location_id, batch_no,
                          available_qty, reserved_qty, frozen_qty, quality_status, last_check_time
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, materialId);
            statement.setLong(2, warehouseId);
            statement.setLong(3, locationId);
            statement.setString(4, batchNo);
            statement.setBigDecimal(5, qty);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapInventory(rs);
            }
        }
    }

    private MesInventory increaseInventoryForPurchase(
            Connection connection,
            long inventoryId,
            BigDecimal qty
    ) throws SQLException {
        String sql = """
                update mes_inventory
                set available_qty = available_qty + ?,
                    last_check_time = current_timestamp
                where inventory_id = ?
                returning inventory_id, material_id, warehouse_id, location_id, batch_no,
                          available_qty, reserved_qty, frozen_qty, quality_status, last_check_time
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, qty);
            statement.setLong(2, inventoryId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapInventory(rs);
            }
        }
    }

    private MesInventoryTransaction insertPurchaseTransaction(
            Connection connection,
            long materialId,
            long inventoryId,
            BigDecimal qty,
            long operatorId
    ) throws SQLException {
        String sql = """
                insert into mes_inventory_transaction
                    (transaction_no, material_id, inventory_id, transaction_type, qty,
                     source_doc_type, operator_id)
                values (?, ?, ?, 'PURCHASE_IN', ?, 'EXTERNAL_PURCHASE', ?)
                returning transaction_id, transaction_no, material_id, inventory_id, transaction_type,
                          qty, source_doc_type, source_doc_id, operator_id, created_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, IdGenerator.nextCode("TX"));
            statement.setLong(2, materialId);
            statement.setLong(3, inventoryId);
            statement.setBigDecimal(4, qty);
            statement.setLong(5, operatorId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapTransaction(rs);
            }
        }
    }

    private boolean hasFinishedGoodsReceipt(Connection connection, long inspectionId) throws SQLException {
        String sql = """
                select 1
                from mes_inventory_transaction
                where source_doc_type = 'QUALITY_INSPECTION'
                  and source_doc_id = ?
                  and transaction_type = 'FINISHED_GOODS_IN'
                limit 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, inspectionId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private FinishedGoodsReceiptContext findFinishedGoodsReceiptContext(Connection connection, long inspectionId)
            throws SQLException {
        String sql = """
                select qi.inspection_id, qi.inspection_no, qi.work_order_id, qi.work_report_id,
                       coalesce(wr.batch_no, wo.batch_no, 'FG-QI-' || qi.inspection_id) as batch_no,
                       wo.product_id,
                       coalesce(p.product_code, co.product_code, 'WO-' || wo.work_order_id) as product_code,
                       coalesce(p.product_name, co.product_model, '工单 ' || wo.work_order_no || ' 成品') as product_name,
                       coalesce(p.specification, p.product_model, co.product_model, wo.work_order_no) as specification,
                       greatest(coalesce(
                           case when qi.work_report_id is not null
                                then coalesce(wr.qualified_qty, 0)::numeric - received_report.received_qty
                                else approved.total_qualified_qty - received_work_order.received_qty
                           end, 0), 0) as inbound_qty
                from mes_quality_inspection qi
                join mes_work_order wo on wo.work_order_id = qi.work_order_id
                left join mes_work_report wr on wr.report_id = qi.work_report_id
                left join mes_product p on p.product_id = wo.product_id
                left join mes_production_task pt on pt.task_id = wo.task_id
                left join mes_customer_order co on co.order_id = pt.order_id
                left join lateral (
                    select coalesce(sum(wr2.qualified_qty), 0)::numeric as total_qualified_qty
                    from mes_work_report wr2
                    where wr2.work_order_id = wo.work_order_id
                      and wr2.report_status = 'APPROVED'
                ) approved on true
                left join lateral (
                    select coalesce(sum(t.qty), 0)::numeric as received_qty
                    from mes_inventory_transaction t
                    join mes_quality_inspection qi2 on qi2.inspection_id = t.source_doc_id
                    where t.source_doc_type = 'QUALITY_INSPECTION'
                      and t.transaction_type = 'FINISHED_GOODS_IN'
                      and qi2.work_order_id = wo.work_order_id
                ) received_work_order on true
                left join lateral (
                    select coalesce(sum(t.qty), 0)::numeric as received_qty
                    from mes_inventory_transaction t
                    join mes_quality_inspection qi2 on qi2.inspection_id = t.source_doc_id
                    where t.source_doc_type = 'QUALITY_INSPECTION'
                      and t.transaction_type = 'FINISHED_GOODS_IN'
                      and qi2.work_report_id = qi.work_report_id
                ) received_report on true
                where qi.inspection_id = ?
                  and qi.inspection_status = 'APPROVED'
                  and qi.judgement_result = 'PASS'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, inspectionId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new BadRequestException("only approved passed quality inspections can receive finished goods");
                }
                return new FinishedGoodsReceiptContext(
                        rs.getLong("inspection_id"),
                        rs.getString("inspection_no"),
                        rs.getLong("work_order_id"),
                        getLong(rs, "work_report_id"),
                        rs.getString("batch_no"),
                        getLong(rs, "product_id"),
                        rs.getString("product_code"),
                        rs.getString("product_name"),
                        rs.getString("specification"),
                        nvl(rs.getBigDecimal("inbound_qty"))
                );
            }
        }
    }

    private long ensureFinishedGoodsWarehouse(Connection connection) throws SQLException {
        Long existing = firstId(connection, """
                select warehouse_id
                from mes_warehouse
                where enabled = 1
                  and (warehouse_code = 'WH-FG-01' or warehouse_type in ('FINISHED','FG'))
                order by case when warehouse_code = 'WH-FG-01' then 0 else 1 end, warehouse_id
                limit 1
                """);
        if (existing != null) {
            grantWarehouseToWarehouseAdmins(connection, existing);
            return existing;
        }
        String sql = """
                insert into mes_warehouse (warehouse_code, warehouse_name, warehouse_type, enabled)
                values ('WH-FG-01', '成品仓', 'FINISHED', 1)
                on conflict (warehouse_code) do update set
                    warehouse_name = excluded.warehouse_name,
                    warehouse_type = excluded.warehouse_type,
                    enabled = 1
                returning warehouse_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            rs.next();
            long warehouseId = rs.getLong("warehouse_id");
            grantWarehouseToWarehouseAdmins(connection, warehouseId);
            return warehouseId;
        }
    }

    private long ensureFinishedGoodsLocation(Connection connection, long warehouseId) throws SQLException {
        Long existing = firstFinishedGoodsLocationId(connection, warehouseId);
        if (existing != null) {
            return existing;
        }
        String sql = """
                insert into mes_warehouse_location (warehouse_id, location_code, location_name, enabled)
                values (?, 'FG-C01', '成品 C01', 1)
                on conflict (location_code) do update set
                    warehouse_id = excluded.warehouse_id,
                    location_name = excluded.location_name,
                    enabled = 1
                returning location_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong("location_id");
            }
        }
    }

    private Long firstFinishedGoodsLocationId(Connection connection, long warehouseId) throws SQLException {
        String sql = """
                select location_id
                from mes_warehouse_location
                where warehouse_id = ?
                  and enabled = 1
                  and (location_code = 'FG-C01' or location_code like 'FG-%')
                order by case when location_code = 'FG-C01' then 0 else 1 end, location_id
                limit 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong("location_id") : null;
            }
        }
    }

    private long ensureFinishedGoodsMaterial(Connection connection, FinishedGoodsReceiptContext context)
            throws SQLException {
        String productCode = sanitizeCode(context.productCode() == null || context.productCode().isBlank()
                ? "P" + context.productId()
                : context.productCode());
        String materialCode = productCode.startsWith("FG-") ? productCode : "FG-" + productCode;
        if (materialCode.length() > 40) {
            materialCode = materialCode.substring(0, 40);
        }
        Long existing = findMaterialIdByCode(connection, materialCode);
        if (existing != null) {
            return existing;
        }
        String sql = """
                insert into mes_material
                    (material_code, material_name, material_type, specification, unit, shelf_life_days, enabled)
                values (?, ?, 'FINISHED', ?, '条', null, 1)
                on conflict (material_code) do update set
                    material_name = excluded.material_name,
                    material_type = excluded.material_type,
                    specification = excluded.specification,
                    unit = excluded.unit,
                    enabled = 1
                returning material_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, materialCode);
            statement.setString(2, defaultText(context.productName(), "质检合格成品"));
            statement.setString(3, context.specification());
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong("material_id");
            }
        }
    }

    private Long findMaterialIdByCode(Connection connection, String materialCode) throws SQLException {
        String sql = "select material_id from mes_material where material_code = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, materialCode);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong("material_id") : null;
            }
        }
    }

    private void insertFinishedGoodsTransaction(
            Connection connection,
            long materialId,
            long inventoryId,
            BigDecimal qty,
            long inspectionId,
            long operatorId
    ) throws SQLException {
        String sql = """
                insert into mes_inventory_transaction
                    (transaction_no, material_id, inventory_id, transaction_type, qty,
                     source_doc_type, source_doc_id, operator_id)
                values (?, ?, ?, 'FINISHED_GOODS_IN', ?, 'QUALITY_INSPECTION', ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, IdGenerator.nextCode("FG-IN"));
            statement.setLong(2, materialId);
            statement.setLong(3, inventoryId);
            statement.setBigDecimal(4, qty);
            statement.setLong(5, inspectionId);
            statement.setLong(6, operatorId);
            statement.executeUpdate();
        }
    }

    private void grantWarehouseToWarehouseAdmins(Connection connection, long warehouseId) throws SQLException {
        String sql = """
                insert into mes_user_warehouse_scope (user_id, warehouse_id)
                select distinct user_id, ?
                from (
                    select u.user_id
                    from mes_user u
                    where u.role_code = 'WAREHOUSE_ADMIN'
                    union
                    select u.user_id
                    from mes_user u
                    join mes_user_role ur on ur.user_id = u.user_id
                    join mes_role r on r.role_id = ur.role_id
                    where r.role_code = 'WAREHOUSE_ADMIN'
                ) admins
                on conflict (user_id, warehouse_id) do nothing
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warehouseId);
            statement.executeUpdate();
        }
    }

    private static String sanitizeCode(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase()
                .replaceAll("[^A-Z0-9_-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return normalized.isBlank() ? "AUTO" : normalized;
    }

    private record FinishedGoodsReceiptContext(
            long inspectionId,
            String inspectionNo,
            long workOrderId,
            Long workReportId,
            String batchNo,
            Long productId,
            String productCode,
            String productName,
            String specification,
            BigDecimal inboundQty
    ) {
    }

    private Long firstRobotId(Connection connection, long warehouseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select robot_id from mes_robot where enabled = 1 and warehouse_id = ? order by robot_id limit 1")) {
            statement.setLong(1, warehouseId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
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

    private List<MesMaterialRequisitionItem> findRequisitionItems(Connection connection, long requisitionId)
            throws SQLException {
        String sql = """
                select requisition_item_id, material_id, required_qty, issued_qty, unit, batch_no, item_status
                from mes_material_requisition_item
                where requisition_id = ?
                order by requisition_item_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requisitionId);
            try (ResultSet rs = statement.executeQuery()) {
                List<MesMaterialRequisitionItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapRequisitionItem(rs, requisitionId));
                }
                return items;
            }
        }
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
        item.defaultWarehouseType = getOptionalString(rs, "default_warehouse_type");
        item.defaultWarehouseId = getOptionalLong(rs, "default_warehouse_id");
        item.defaultWarehouseCode = getOptionalString(rs, "default_warehouse_code");
        item.defaultWarehouseName = getOptionalString(rs, "default_warehouse_name");
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
        item.materialCode = getOptionalString(rs, "material_code");
        item.materialName = getOptionalString(rs, "material_name");
        item.materialType = getOptionalString(rs, "material_type");
        item.specification = getOptionalString(rs, "specification");
        item.unit = getOptionalString(rs, "unit");
        item.warehouseId = rs.getLong("warehouse_id");
        item.warehouseCode = getOptionalString(rs, "warehouse_code");
        item.warehouseName = getOptionalString(rs, "warehouse_name");
        item.locationId = rs.getLong("location_id");
        item.locationCode = getOptionalString(rs, "location_code");
        item.locationName = getOptionalString(rs, "location_name");
        item.batchNo = rs.getString("batch_no");
        item.availableQty = rs.getBigDecimal("available_qty");
        item.warehouseMaterialAvailableQty = getOptionalBigDecimal(rs, "warehouse_material_available_qty");
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
        item.warehouseId = getLong(rs, "warehouse_id");
        item.requestedBy = rs.getLong("requested_by");
        item.requestStatus = rs.getString("request_status");
        item.requestTime = getLocalDateTime(rs, "request_time");
        item.approvedBy = getLong(rs, "approved_by");
        item.approvedTime = getLocalDateTime(rs, "approved_time");
        item.remark = rs.getString("remark");
        item.pickingTaskId = getLong(rs, "picking_task_id");
        item.pickingTaskStatus = rs.getString("picking_task_status");
        item.deliveryTaskId = getLong(rs, "delivery_task_id");
        item.deliveryStatus = rs.getString("delivery_status");
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
        item.warehouseId = getLong(rs, "warehouse_id");
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

    private static String warehouseTypeForMaterial(String materialType) {
        if (materialType == null) return null;
        return switch (materialType.toUpperCase()) {
            case "RAW", "AUX" -> "RAW";
            case "WIP" -> "WIP";
            case "FINISHED" -> "FINISHED";
            default -> null;
        };
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

    private static String getOptionalString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private static BigDecimal getOptionalBigDecimal(ResultSet rs, String column) {
        try {
            return rs.getBigDecimal(column);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private static Long getOptionalLong(ResultSet rs, String column) {
        try {
            return getLong(rs, column);
        } catch (SQLException ignored) {
            return null;
        }
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
