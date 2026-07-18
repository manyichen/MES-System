/*
 * 答辩定位：MES 应用基础 模块的 PlanningServiceTest。
 * 分层职责：自动化回归测试：固定关键业务规则、接口契约和架构边界，防止重构时出现静默回归。
 * 典型调用链：Maven Surefire -> JUnit 5 -> 被测类；测试替身用于隔离远程数据库或文件系统。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem;

import com.example.messystem.master.entity.MesProcessRoute;
import com.example.messystem.master.entity.MesProduct;
import com.example.messystem.master.entity.MesProductBom;
import com.example.messystem.master.entity.MesProductionLine;
import com.example.messystem.master.service.MasterDataService;
import com.example.messystem.planning.entity.MesCustomerOrder;
import com.example.messystem.planning.entity.MesKittingAnalysis;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.entity.MesWorkOrder;
import com.example.messystem.planning.service.CustomerOrderService;
import com.example.messystem.planning.service.KittingService;
import com.example.messystem.planning.service.PlanningStore;
import com.example.messystem.planning.service.ProductionTaskService;
import com.example.messystem.planning.service.WorkOrderService;
import com.example.messystem.warehouse.entity.MesInventory;
import com.example.messystem.warehouse.entity.MesMaterial;
import com.example.messystem.warehouse.entity.MesWarehouse;
import com.example.messystem.warehouse.entity.MesWarehouseLocation;
import com.example.messystem.warehouse.service.WarehouseService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MES 应用基础 的 PlanningServiceTest，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
class PlanningServiceTest {
    private long materialId;
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private MasterDataService masterDataService;
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private CustomerOrderService orderService;
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private ProductionTaskService taskService;
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private KittingService kittingService;
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private WorkOrderService workOrderService;
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private WarehouseService warehouseService;

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
    void setUp() {
        PlanningStore.clear();
        masterDataService = new MasterDataService();
        orderService = new CustomerOrderService();
        taskService = new ProductionTaskService();
        kittingService = new KittingService();
        workOrderService = new WorkOrderService();
        warehouseService = new WarehouseService();
        MesMaterial material = new MesMaterial();
        material.materialCode = "MAT-TEST-" + System.nanoTime();
        material.materialName = "测试天然橡胶";
        material.materialType = "RAW";
        material.specification = "TEST-RSS3";
        material.unit = "kg";
        materialId = warehouseService.createMaterial(material).materialId;
    }

    /**
     * 回归场景：验证 orderTaskKittingReleaseAndWorkOrderShouldRunAsPlanningMainFlow 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @Test
    void orderTaskKittingReleaseAndWorkOrderShouldRunAsPlanningMainFlow() {
        MesProduct product = createProduct();
        createBom(product.productId);
        MesProductionLine line = createLine();
        MesProcessRoute route = createRoute(product.productId);
        createInventory();

        MesCustomerOrder order = new MesCustomerOrder();
        order.customerName = "双星演示客户";
        order.productId = product.productId;
        order.orderQty = 100;
        order = orderService.createOrder(order);

        MesProductionTask task = new MesProductionTask();
        task.orderId = order.orderId;
        task.targetLineId = line.lineId;
        task.plannerId = 1L;
        task = taskService.createTask(task);

        MesKittingAnalysis analysis = kittingService.analyze(task.taskId);

        MesWorkOrder workOrder = new MesWorkOrder();
        workOrder.taskId = task.taskId;
        workOrder = workOrderService.createWorkOrder(workOrder);
        assertEquals("RELEASED", taskService.getTask(task.taskId).taskStatus);
        workOrderService.dispatch(workOrder.workOrderId, 1L);
        workOrderService.receive(workOrder.workOrderId, 1L);

        assertEquals("READY", analysis.kittingStatus);
        assertEquals(line.lineId, workOrder.lineId);
        assertEquals(route.processId, workOrder.processId);
        assertEquals("RECEIVED", workOrderService.getWorkOrder(workOrder.workOrderId).workOrderStatus);
        assertEquals(3, workOrderService.listLogs(workOrder.workOrderId).size());
    }

    /**
     * 回归场景：验证 kittingShouldCreateShortageAlertWhenInventoryIsNotEnough 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @Test
    void kittingShouldCreateShortageAlertWhenInventoryIsNotEnough() {
        MesProduct product = createProduct();
        createBom(product.productId);
        MesCustomerOrder order = new MesCustomerOrder();
        order.customerName = "欠料客户";
        order.productId = product.productId;
        order.orderQty = 100;
        order = orderService.createOrder(order);

        MesProductionTask task = new MesProductionTask();
        task.orderId = order.orderId;
        task.targetLineId = createLine().lineId;
        task.plannerId = 1L;
        task = taskService.createTask(task);

        MesKittingAnalysis analysis = kittingService.analyze(task.taskId);
        kittingService.publishShortageAlerts(task.taskId);

        assertEquals("SHORTAGE", analysis.kittingStatus);
        assertFalse(kittingService.listAlerts().isEmpty());
        assertEquals("SHORTAGE", taskService.getTask(task.taskId).taskStatus);

        createInventory();
        MesKittingAnalysis recovered = kittingService.analyze(task.taskId);
        long recoveredTaskId = task.taskId;

        assertEquals("READY", recovered.kittingStatus);
        assertTrue(kittingService.listAlerts().stream()
                .filter(alert -> alert.taskId.equals(recoveredTaskId))
                .allMatch(alert -> "RESOLVED".equals(alert.alertStatus)));
    }

    /**
     * 回归场景：验证 kittingShouldExplainMissingBomBeforeAnalysis 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @Test
    void kittingShouldExplainMissingBomBeforeAnalysis() {
        MesProduct product = createProduct();
        MesCustomerOrder order = new MesCustomerOrder();
        order.customerName = "未配置BOM客户";
        order.productId = product.productId;
        order.orderQty = 10;
        order = orderService.createOrder(order);

        MesProductionTask task = new MesProductionTask();
        task.orderId = order.orderId;
        task.plannerId = 1L;
        task = taskService.createTask(task);

        MesProductionTask pendingTask = taskService.getTask(task.taskId);
        assertFalse(pendingTask.kittingAnalyzable);
        assertEquals("产品未配置启用的BOM物料，请先维护产品BOM", pendingTask.kittingBlockedReason);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> kittingService.analyze(pendingTask.taskId));
        assertEquals("产品未配置启用的BOM物料，请先维护产品BOM", exception.getMessage());
    }

    /**
     * 回归场景：验证 createProduct 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    private MesProduct createProduct() {
        MesProduct product = new MesProduct();
        product.productName = "半钢子午线轮胎";
        product.productModel = "205/55R16";
        return masterDataService.createProduct(product);
    }

    /**
     * 回归场景：验证 createBom 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    private void createBom(long productId) {
        MesProductBom bom = new MesProductBom();
        bom.materialId = materialId;
        bom.materialName = "天然橡胶";
        bom.qtyPerUnit = new BigDecimal("2.5");
        masterDataService.createBom(productId, bom);
    }

    /**
     * 回归场景：验证 createLine 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    private MesProductionLine createLine() {
        MesProductionLine line = new MesProductionLine();
        line.lineName = "成型一线";
        return masterDataService.createProductionLine(line);
    }

    /**
     * 回归场景：验证 createRoute 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    private MesProcessRoute createRoute(long productId) {
        MesProcessRoute route = new MesProcessRoute();
        route.productId = productId;
        route.processName = "成型";
        return masterDataService.createProcessRoute(route);
    }

    /**
     * 回归场景：验证 createInventory 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    private void createInventory() {
        MesWarehouse warehouse = new MesWarehouse();
        warehouse.warehouseName = "原材料仓";
        warehouse = warehouseService.createWarehouse(warehouse);

        MesWarehouseLocation location = new MesWarehouseLocation();
        location.warehouseId = warehouse.warehouseId;
        location.locationName = "A-01";
        location = warehouseService.createLocation(location);

        MesInventory inventory = new MesInventory();
        inventory.materialId = materialId;
        inventory.warehouseId = warehouse.warehouseId;
        inventory.locationId = location.locationId;
        inventory.availableQty = new BigDecimal("300");
        warehouseService.createInventory(inventory);
    }
}
