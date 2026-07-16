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
import com.example.messystem.warehouse.entity.MesWarehouse;
import com.example.messystem.warehouse.entity.MesWarehouseLocation;
import com.example.messystem.warehouse.service.WarehouseService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanningServiceTest {
    private long materialId;
    private MasterDataService masterDataService;
    private CustomerOrderService orderService;
    private ProductionTaskService taskService;
    private KittingService kittingService;
    private WorkOrderService workOrderService;
    private WarehouseService warehouseService;

    @BeforeAll
    static void protectRemoteDatabase() {
        RemoteDatabaseTestGuard.requireExplicitOptInForRemoteDatabase();
    }

    @BeforeEach
    void setUp() {
        PlanningStore.clear();
        materialId = 8_000_000_000L + Math.floorMod(System.nanoTime(), 1_000_000_000L);
        masterDataService = new MasterDataService();
        orderService = new CustomerOrderService();
        taskService = new ProductionTaskService();
        kittingService = new KittingService();
        workOrderService = new WorkOrderService();
        warehouseService = new WarehouseService();
    }

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
        workOrder.lineId = line.lineId;
        workOrder.processId = route.processId;
        workOrder = workOrderService.createWorkOrder(workOrder);
        assertEquals("RELEASED", taskService.getTask(task.taskId).taskStatus);
        workOrderService.dispatch(workOrder.workOrderId, 1L);
        workOrderService.receive(workOrder.workOrderId, 1L);

        assertEquals("READY", analysis.kittingStatus);
        assertEquals("RECEIVED", workOrderService.getWorkOrder(workOrder.workOrderId).workOrderStatus);
        assertEquals(3, workOrderService.listLogs(workOrder.workOrderId).size());
    }

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

    private MesProduct createProduct() {
        MesProduct product = new MesProduct();
        product.productName = "半钢子午线轮胎";
        product.productModel = "205/55R16";
        return masterDataService.createProduct(product);
    }

    private void createBom(long productId) {
        MesProductBom bom = new MesProductBom();
        bom.materialId = materialId;
        bom.materialName = "天然橡胶";
        bom.qtyPerUnit = new BigDecimal("2.5");
        masterDataService.createBom(productId, bom);
    }

    private MesProductionLine createLine() {
        MesProductionLine line = new MesProductionLine();
        line.lineName = "成型一线";
        return masterDataService.createProductionLine(line);
    }

    private MesProcessRoute createRoute(long productId) {
        MesProcessRoute route = new MesProcessRoute();
        route.productId = productId;
        route.processName = "成型";
        return masterDataService.createProcessRoute(route);
    }

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
