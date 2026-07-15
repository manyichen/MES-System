package com.example.messystem;

import com.example.messystem.common.Db;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.production.entity.MesWorkReport;
import com.example.messystem.production.service.ProductionService;
import com.example.messystem.warehouse.entity.MesInventory;
import com.example.messystem.warehouse.entity.MesMaterial;
import com.example.messystem.warehouse.entity.MesMaterialRequisition;
import com.example.messystem.warehouse.entity.MesMaterialRequisitionItem;
import com.example.messystem.warehouse.entity.MesWarehouse;
import com.example.messystem.warehouse.entity.MesWarehouseLocation;
import com.example.messystem.warehouse.service.WarehouseService;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
class WarehouseProductionServiceTest {
    private WarehouseService warehouseService;
    private ProductionService productionService;

    @BeforeEach
    void setUp() throws SQLException {
        cleanupTestData();
        warehouseService = new WarehouseService();
        productionService = new ProductionService();
    }

    @AfterEach
    void tearDown() throws SQLException {
        cleanupTestData();
    }

    @Test
    void approveRequisitionShouldReserveOrDeductInventory() {
        MesMaterialRequisition requisition = createDatabaseRequisition(new BigDecimal("100"));

        MesMaterialRequisition received = warehouseService.receiveRequisition(requisition.requisitionId, 1L);
        MesMaterialRequisition approved = warehouseService.approveRequisition(requisition.requisitionId, 1L);

        assertEquals("RECEIVED", received.requestStatus);
        assertEquals("APPROVED", approved.requestStatus);
        assertTrue(approved.pickingTaskId != null && approved.pickingTaskId > 0);
        assertTrue(warehouseService.listPickingTasks().stream()
                .anyMatch(task -> Objects.equals(task.requisitionId, requisition.requisitionId)));
        assertEquals("APPROVED", warehouseService.getRequisition(requisition.requisitionId).requestStatus);
    }

    @Test
    void completePickingShouldCreateDeliveryTaskAndConfirmReceiptShouldDeductInventory() {
        MesMaterialRequisition requisition = createDatabaseRequisition(new BigDecimal("100"));
        warehouseService.receiveRequisition(requisition.requisitionId, 1L);
        warehouseService.approveRequisition(requisition.requisitionId, 1L);
        long pickingTaskId = warehouseService.listPickingTasks().stream()
                .filter(task -> Objects.equals(task.requisitionId, requisition.requisitionId))
                .findFirst()
                .orElseThrow()
                .pickingTaskId;

        warehouseService.completePicking(pickingTaskId);
        long deliveryTaskId = warehouseService.listDeliveryTasks().stream()
                .filter(task -> task.pickingTaskId == pickingTaskId)
                .findFirst()
                .orElseThrow()
                .deliveryTaskId;
        warehouseService.markDeliveryArrived(deliveryTaskId);
        warehouseService.confirmDeliveryReceipt(deliveryTaskId);

        assertEquals("COMPLETED", warehouseService.getPickingTask(pickingTaskId).taskStatus);
        assertEquals("RECEIVED", warehouseService.getDeliveryTask(deliveryTaskId).deliveryStatus);
        assertTrue(warehouseService.listTransactions().stream()
                .anyMatch(transaction -> Long.valueOf(pickingTaskId).equals(transaction.sourceDocId)));
    }

    @Test
    void approveRequisitionShouldFailBeforeWarehouseReceive() {
        MesMaterialRequisition requisition = createDatabaseRequisition(new BigDecimal("100"));

        assertThrows(BadRequestException.class, () -> warehouseService.approveRequisition(requisition.requisitionId, 1L));
    }

    @Test
    void approveWorkReportShouldCreatePieceworkWage() {
        String suffix = "TEST-" + System.nanoTime();
        long workOrderId = createTestWorkOrder("WO-" + suffix, 100);
        MesWorkReport report = new MesWorkReport();
        report.reportNo = "WR-" + suffix;
        report.workOrderId = workOrderId;
        report.operatorId = 9L;
        report.reportQty = 100;
        report.qualifiedQty = 96;
        report.defectQty = 4;
        MesWorkReport created = productionService.createWorkReport(report);

        productionService.approveWorkReport(created.reportId);

        assertEquals("APPROVED", productionService.getWorkReport(created.reportId).reportStatus);
        assertTrue(productionService.listWages().stream()
                .anyMatch(wage -> wage.reportId == created.reportId));
        assertEquals(new BigDecimal("240.00"), productionService.listWages().stream()
                .filter(wage -> wage.reportId == created.reportId)
                .findFirst()
                .orElseThrow()
                .wageAmount);
    }

    @Test
    void byWorkOrderApisShouldReturnBModuleRecordsOnly() {
        long workOrderId = createTestWorkOrder("WO-TEST-" + System.nanoTime(), 100);
        MesMaterialRequisition requisition = createDatabaseRequisition(new BigDecimal("100"), workOrderId);

        MesWorkReport report = new MesWorkReport();
        report.reportNo = "WR-TEST-" + System.nanoTime();
        report.workOrderId = workOrderId;
        report.operatorId = 9L;
        report.reportQty = 30;
        report.qualifiedQty = 28;
        report.defectQty = 2;
        MesWorkReport createdReport = productionService.createWorkReport(report);

        assertTrue(warehouseService.listRequisitionsByWorkOrder(workOrderId).stream()
                .anyMatch(row -> row.requisitionId.equals(requisition.requisitionId) && !row.items.isEmpty()));
        assertTrue(productionService.listWorkReportsByWorkOrder(workOrderId).stream()
                .anyMatch(row -> row.reportId.equals(createdReport.reportId)));
    }

    @Test
    void approveRequisitionShouldFailWhenInventoryIsNotEnough() {
        MesMaterialRequisition requisition = createDatabaseRequisition(new BigDecimal("5"));

        warehouseService.receiveRequisition(requisition.requisitionId, 1L);
        assertThrows(BadRequestException.class, () -> warehouseService.approveRequisition(requisition.requisitionId, 1L));
    }

    @Test
    void confirmReceiptShouldFailWhenRepeated() {
        MesMaterialRequisition requisition = createDatabaseRequisition(new BigDecimal("100"));
        warehouseService.receiveRequisition(requisition.requisitionId, 1L);
        warehouseService.approveRequisition(requisition.requisitionId, 1L);
        long pickingTaskId = pickingTaskIdFor(requisition.requisitionId);
        warehouseService.completePicking(pickingTaskId);
        long deliveryTaskId = deliveryTaskIdFor(pickingTaskId);
        warehouseService.markDeliveryArrived(deliveryTaskId);
        warehouseService.confirmDeliveryReceipt(deliveryTaskId);

        assertThrows(BadRequestException.class, () -> warehouseService.confirmDeliveryReceipt(deliveryTaskId));
    }

    @Test
    void approveWorkReportShouldFailWhenRepeatedAndNotCreateDuplicateWage() {
        long workOrderId = createTestWorkOrder("WO-TEST-" + System.nanoTime(), 100);
        MesWorkReport report = new MesWorkReport();
        report.reportNo = "WR-TEST-" + System.nanoTime();
        report.workOrderId = workOrderId;
        report.operatorId = 9L;
        report.reportQty = 20;
        report.qualifiedQty = 18;
        report.defectQty = 2;
        MesWorkReport created = productionService.createWorkReport(report);

        productionService.approveWorkReport(created.reportId);

        assertThrows(BadRequestException.class, () -> productionService.approveWorkReport(created.reportId));
        assertEquals(1, productionService.listWages().stream()
                .filter(wage -> wage.reportId.equals(created.reportId))
                .count());
    }

    private MesMaterialRequisition createDatabaseRequisition(BigDecimal inventoryQty) {
        return createDatabaseRequisition(inventoryQty, createTestWorkOrder("WO-TEST-" + System.nanoTime(), 100));
    }

    private long pickingTaskIdFor(long requisitionId) {
        return warehouseService.listPickingTasks().stream()
                .filter(task -> Objects.equals(task.requisitionId, requisitionId))
                .findFirst()
                .orElseThrow()
                .pickingTaskId;
    }

    private long deliveryTaskIdFor(long pickingTaskId) {
        return warehouseService.listDeliveryTasks().stream()
                .filter(task -> Objects.equals(task.pickingTaskId, pickingTaskId))
                .findFirst()
                .orElseThrow()
                .deliveryTaskId;
    }

    private MesMaterialRequisition createDatabaseRequisition(BigDecimal inventoryQty, long workOrderId) {
        String suffix = "TEST-" + System.nanoTime();

        MesMaterial material = new MesMaterial();
        material.materialCode = "MAT-" + suffix;
        material.materialName = "Test Material";
        material.materialType = "RAW";
        material.unit = "kg";
        MesMaterial createdMaterial = warehouseService.createMaterial(material);

        MesWarehouse warehouse = new MesWarehouse();
        warehouse.warehouseCode = "WH-" + suffix;
        warehouse.warehouseName = "Test Warehouse";
        warehouse.warehouseType = "RAW";
        MesWarehouse createdWarehouse = warehouseService.createWarehouse(warehouse);

        MesWarehouseLocation location = new MesWarehouseLocation();
        location.warehouseId = createdWarehouse.warehouseId;
        location.locationCode = "LOC-" + suffix;
        location.locationName = "Test Location";
        MesWarehouseLocation createdLocation = warehouseService.createLocation(location);

        MesInventory inventory = new MesInventory();
        inventory.materialId = createdMaterial.materialId;
        inventory.warehouseId = createdWarehouse.warehouseId;
        inventory.locationId = createdLocation.locationId;
        inventory.batchNo = "BATCH-" + suffix;
        inventory.availableQty = inventoryQty;
        warehouseService.createInventory(inventory);

        MesMaterialRequisitionItem item = new MesMaterialRequisitionItem();
        item.materialId = createdMaterial.materialId;
        item.requiredQty = new BigDecimal("10");
        item.unit = "kg";
        item.batchNo = inventory.batchNo;

        MesMaterialRequisition requisition = new MesMaterialRequisition();
        requisition.requisitionNo = "REQ-" + suffix;
        requisition.workOrderId = workOrderId;
        requisition.warehouseId = createdWarehouse.warehouseId;
        requisition.requestedBy = 1L;
        requisition.items = List.of(item);
        return warehouseService.createRequisition(requisition);
    }

    private static void cleanupTestData() throws SQLException {
        try (Connection connection = Db.getConnection()) {
            delete(connection, """
                    delete from mes_piecework_wage
                    where report_id in (
                        select report_id from mes_work_report
                        where report_no like 'WR-TEST-%'
                           or work_order_id in (
                               select work_order_id from mes_work_order where work_order_no like 'WO-TEST-%'
                           )
                    )
                    """);
            delete(connection, """
                    delete from mes_work_report
                    where report_no like 'WR-TEST-%'
                       or work_order_id in (
                           select work_order_id from mes_work_order where work_order_no like 'WO-TEST-%'
                       )
                    """);
            delete(connection, """
                    delete from mes_inventory_transaction
                    where inventory_id in (
                        select inventory_id from mes_inventory where batch_no like 'BATCH-TEST-%'
                    )
                       or source_doc_id in (
                        select p.picking_task_id
                        from mes_picking_task p
                        join mes_material_requisition r on r.requisition_id = p.requisition_id
                        where r.requisition_no like 'REQ-TEST-%'
                    )
                    """);
            delete(connection, """
                    delete from mes_robot_delivery_task
                    where picking_task_id in (
                        select p.picking_task_id
                        from mes_picking_task p
                        join mes_material_requisition r on r.requisition_id = p.requisition_id
                        where r.requisition_no like 'REQ-TEST-%'
                    )
                    """);
            delete(connection, """
                    delete from mes_picking_task
                    where requisition_id in (
                        select requisition_id from mes_material_requisition where requisition_no like 'REQ-TEST-%'
                    )
                    """);
            delete(connection, """
                    delete from mes_material_requisition_item
                    where requisition_id in (
                        select requisition_id from mes_material_requisition where requisition_no like 'REQ-TEST-%'
                    )
                    """);
            delete(connection, "delete from mes_material_requisition where requisition_no like 'REQ-TEST-%'");
            delete(connection, "delete from mes_inventory where batch_no like 'BATCH-TEST-%'");
            delete(connection, "delete from mes_warehouse_location where location_code like 'LOC-TEST-%'");
            delete(connection, "delete from mes_warehouse where warehouse_code like 'WH-TEST-%'");
            delete(connection, "delete from mes_robot where robot_code like 'ROB-TEST-%'");
            delete(connection, "delete from mes_material where material_code like 'MAT-TEST-%'");
            delete(connection, """
                    delete from mes_work_order_operation_log
                    where work_order_id in (
                        select work_order_id from mes_work_order where work_order_no like 'WO-TEST-%'
                    )
                    """);
            delete(connection, "delete from mes_work_order where work_order_no like 'WO-TEST-%'");
        }
    }

    private static long createTestWorkOrder(String workOrderNo, int plannedQty) {
        String sql = """
                insert into mes_work_order
                    (work_order_no, task_id, product_id, line_id, process_id, planned_qty,
                     actual_qty, priority_level, work_order_status, batch_no, assigned_to, accepted_by)
                values (?, 1, 1, 1, 1, ?, 0, 3, 'RECEIVED', ?, 9, 9)
                returning work_order_id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, workOrderNo);
            statement.setInt(2, plannedQty);
            statement.setString(3, "BATCH-" + workOrderNo.replace("WO-", ""));
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void delete(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
