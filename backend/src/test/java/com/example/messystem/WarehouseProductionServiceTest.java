/*
 * 答辩定位：MES 应用基础 模块的 WarehouseProductionServiceTest。
 * 分层职责：自动化回归测试：固定关键业务规则、接口契约和架构边界，防止重构时出现静默回归。
 * 典型调用链：Maven Surefire -> JUnit 5 -> 被测类；测试替身用于隔离远程数据库或文件系统。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem;

import com.example.messystem.common.Db;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.production.entity.MesWorkReport;
import com.example.messystem.production.service.ProductionService;
import com.example.messystem.warehouse.entity.ExternalPurchaseRequest;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MES 应用基础 的 WarehouseProductionServiceTest，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
@Execution(ExecutionMode.SAME_THREAD)
class WarehouseProductionServiceTest {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private WarehouseService warehouseService;
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private ProductionService productionService;

    /**
     * 回归场景：验证 protectRemoteDatabase 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @BeforeAll
    static void protectRemoteDatabase() {
        RemoteDatabaseTestGuard.requireExplicitOptInForRemoteDatabase();
    }

    /**
     * 回归场景：验证 setUp 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @BeforeEach
    void setUp() throws SQLException {
        cleanupTestData();
        warehouseService = new WarehouseService();
        productionService = new ProductionService();
    }

    /**
     * 回归场景：验证 tearDown 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @AfterEach
    void tearDown() throws SQLException {
        cleanupTestData();
    }

    /**
     * 回归场景：验证 approveRequisitionShouldReserveOrDeductInventory 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
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

    /**
     * 回归场景：验证 completePickingShouldCreateDeliveryTaskAndConfirmReceiptShouldDeductInventory 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
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

    /**
     * 回归场景：验证 approveRequisitionShouldFailBeforeWarehouseReceive 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @Test
    void approveRequisitionShouldFailBeforeWarehouseReceive() {
        MesMaterialRequisition requisition = createDatabaseRequisition(new BigDecimal("100"));

        assertThrows(BadRequestException.class, () -> warehouseService.approveRequisition(requisition.requisitionId, 1L));
    }

    /**
     * 回归场景：验证 approveWorkReportShouldCreatePieceworkWage 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
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
                .anyMatch(wage -> Objects.equals(wage.reportId, created.reportId)));
        assertEquals(new BigDecimal("240.00"), productionService.listWages().stream()
                .filter(wage -> Objects.equals(wage.reportId, created.reportId))
                .findFirst()
                .orElseThrow()
                .wageAmount);
    }

    /**
     * 回归场景：验证 byWorkOrderApisShouldReturnBModuleRecordsOnly 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
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

    /**
     * 回归场景：验证 approveRequisitionShouldFailWhenInventoryIsNotEnough 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @Test
    void approveRequisitionShouldFailWhenInventoryIsNotEnough() {
        MesMaterialRequisition requisition = createDatabaseRequisition(new BigDecimal("5"));

        warehouseService.receiveRequisition(requisition.requisitionId, 1L);
        assertThrows(BadRequestException.class, () -> warehouseService.approveRequisition(requisition.requisitionId, 1L));
    }

    /**
     * 回归场景：验证 externalPurchasesAcrossBatchesShouldSatisfyRequisitionWithoutSpecifiedBatch 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @Test
    void externalPurchasesAcrossBatchesShouldSatisfyRequisitionWithoutSpecifiedBatch() {
        long workOrderId = createTestWorkOrder("WO-TEST-" + System.nanoTime(), 100);
        MesMaterialRequisition requisition = createDatabaseRequisition(BigDecimal.ZERO, workOrderId, false);
        MesInventory inventory = warehouseService.listInventoryByMaterial(requisition.items.get(0).materialId)
                .stream().findFirst().orElseThrow();

        ExternalPurchaseRequest firstPurchase = new ExternalPurchaseRequest();
        firstPurchase.materialId = inventory.materialId;
        firstPurchase.warehouseId = inventory.warehouseId;
        firstPurchase.locationId = inventory.locationId;
        firstPurchase.batchNo = "BATCH-TEST-PUR-A-" + System.nanoTime();
        firstPurchase.qty = new BigDecimal("4");
        warehouseService.externalPurchase(firstPurchase, 1L);

        ExternalPurchaseRequest secondPurchase = new ExternalPurchaseRequest();
        secondPurchase.materialId = inventory.materialId;
        secondPurchase.warehouseId = inventory.warehouseId;
        secondPurchase.locationId = inventory.locationId;
        secondPurchase.batchNo = "BATCH-TEST-PUR-B-" + System.nanoTime();
        secondPurchase.qty = new BigDecimal("6");
        warehouseService.externalPurchase(secondPurchase, 1L);

        warehouseService.receiveRequisition(requisition.requisitionId, 1L);
        MesMaterialRequisition approved = warehouseService.approveRequisition(requisition.requisitionId, 1L);
        warehouseService.completePicking(approved.pickingTaskId);
        long deliveryTaskId = deliveryTaskIdFor(approved.pickingTaskId);
        warehouseService.markDeliveryArrived(deliveryTaskId);
        warehouseService.confirmDeliveryReceipt(deliveryTaskId);

        assertEquals("RECEIVED", warehouseService.getDeliveryTask(deliveryTaskId).deliveryStatus);
        assertEquals(0, warehouseService.listInventoryByMaterial(inventory.materialId).stream()
                .map(row -> row.availableQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(BigDecimal.ZERO));
    }

    /**
     * 回归场景：验证 confirmReceiptShouldFailWhenRepeated 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
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

    /**
     * 回归场景：验证 approveWorkReportShouldFailWhenRepeatedAndNotCreateDuplicateWage 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
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

    /**
     * 回归场景：验证 createDatabaseRequisition 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    private MesMaterialRequisition createDatabaseRequisition(BigDecimal inventoryQty) {
        /**
         * 回归场景：验证 createDatabaseRequisition 所描述的行为。
         * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
         */
        return createDatabaseRequisition(inventoryQty, createTestWorkOrder("WO-TEST-" + System.nanoTime(), 100));
    }

    /**
     * 回归场景：验证 pickingTaskIdFor 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    private long pickingTaskIdFor(long requisitionId) {
        return warehouseService.listPickingTasks().stream()
                .filter(task -> Objects.equals(task.requisitionId, requisitionId))
                .findFirst()
                .orElseThrow()
                .pickingTaskId;
    }

    /**
     * 回归场景：验证 deliveryTaskIdFor 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    private long deliveryTaskIdFor(long pickingTaskId) {
        return warehouseService.listDeliveryTasks().stream()
                .filter(task -> Objects.equals(task.pickingTaskId, pickingTaskId))
                .findFirst()
                .orElseThrow()
                .deliveryTaskId;
    }

    /**
     * 回归场景：验证 createDatabaseRequisition 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    private MesMaterialRequisition createDatabaseRequisition(BigDecimal inventoryQty, long workOrderId) {
        /**
         * 回归场景：验证 createDatabaseRequisition 所描述的行为。
         * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
         */
        return createDatabaseRequisition(inventoryQty, workOrderId, true);
    }

    /**
     * 回归场景：验证 createDatabaseRequisition 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    private MesMaterialRequisition createDatabaseRequisition(
            BigDecimal inventoryQty, long workOrderId, boolean specifyBatch) {
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
        item.batchNo = specifyBatch ? inventory.batchNo : "";

        MesMaterialRequisition requisition = new MesMaterialRequisition();
        requisition.requisitionNo = "REQ-" + suffix;
        requisition.workOrderId = workOrderId;
        requisition.warehouseId = createdWarehouse.warehouseId;
        requisition.requestedBy = 1L;
        requisition.items = List.of(item);
        return warehouseService.createRequisition(requisition);
    }

    /**
     * 回归场景：验证 cleanupTestData 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    private static void cleanupTestData() throws SQLException {
        try (Connection connection = Db.getConnection()) {
            delete(connection, """
                    delete from mes_piecework_wage
                    /**
                     * 回归场景：验证 in 所描述的行为。
                     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
                     */
                    where report_id in (
                        select report_id from mes_work_report
                        where report_no like 'WR-TEST-%'
                           /**
                            * 回归场景：验证 in 所描述的行为。
                            * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
                            */
                           or work_order_id in (
                               select work_order_id from mes_work_order where work_order_no like 'WO-TEST-%'
                           )
                    )
                    """);
            delete(connection, """
                    delete from mes_work_report
                    where report_no like 'WR-TEST-%'
                       /**
                        * 回归场景：验证 in 所描述的行为。
                        * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
                        */
                       or work_order_id in (
                           select work_order_id from mes_work_order where work_order_no like 'WO-TEST-%'
                       )
                    """);
            delete(connection, """
                    delete from mes_inventory_transaction
                    /**
                     * 回归场景：验证 in 所描述的行为。
                     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
                     */
                    where inventory_id in (
                        select inventory_id from mes_inventory where batch_no like 'BATCH-TEST-%'
                    )
                       /**
                        * 回归场景：验证 in 所描述的行为。
                        * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
                        */
                       or source_doc_id in (
                        select p.picking_task_id
                        from mes_picking_task p
                        join mes_material_requisition r on r.requisition_id = p.requisition_id
                        where r.requisition_no like 'REQ-TEST-%'
                    )
                    """);
            delete(connection, """
                    delete from mes_robot_delivery_task
                    /**
                     * 回归场景：验证 in 所描述的行为。
                     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
                     */
                    where picking_task_id in (
                        select p.picking_task_id
                        from mes_picking_task p
                        join mes_material_requisition r on r.requisition_id = p.requisition_id
                        where r.requisition_no like 'REQ-TEST-%'
                    )
                    """);
            delete(connection, """
                    delete from mes_picking_task
                    /**
                     * 回归场景：验证 in 所描述的行为。
                     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
                     */
                    where requisition_id in (
                        select requisition_id from mes_material_requisition where requisition_no like 'REQ-TEST-%'
                    )
                    """);
            delete(connection, """
                    delete from mes_material_requisition_item
                    /**
                     * 回归场景：验证 in 所描述的行为。
                     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
                     */
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
                    /**
                     * 回归场景：验证 in 所描述的行为。
                     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
                     */
                    where work_order_id in (
                        select work_order_id from mes_work_order where work_order_no like 'WO-TEST-%'
                    )
                    """);
            delete(connection, "delete from mes_work_order where work_order_no like 'WO-TEST-%'");
        }
    }

    /**
     * 回归场景：验证 createTestWorkOrder 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
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
            /**
             * 回归场景：验证 IllegalStateException 所描述的行为。
             * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
             */
            throw new IllegalStateException(e);
        }
    }

    /**
     * 回归场景：验证 delete 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    private static void delete(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
