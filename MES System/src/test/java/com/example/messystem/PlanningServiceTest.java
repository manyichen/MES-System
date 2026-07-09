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
import com.example.messystem.warehouse.service.InMemoryMesStore;
import com.example.messystem.warehouse.service.WarehouseService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PlanningServiceTest {
    private MasterDataService masterDataService;
    private CustomerOrderService orderService;
    private ProductionTaskService taskService;
    private KittingService kittingService;
    private WorkOrderService workOrderService;
    private WarehouseService warehouseService;

    @BeforeEach
    void setUp() {
        PlanningStore.clear();
        InMemoryMesStore.clear();
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
        task = taskService.createTask(task);

        MesKittingAnalysis analysis = kittingService.analyze(task.taskId);
        taskService.releaseTask(task.taskId);

        MesWorkOrder workOrder = new MesWorkOrder();
        workOrder.taskId = task.taskId;
        workOrder.lineId = line.lineId;
        workOrder.processId = route.processId;
        workOrder = workOrderService.createWorkOrder(workOrder);
        workOrderService.dispatch(workOrder.workOrderId, 1L);
        workOrderService.receive(workOrder.workOrderId, 2L);

        assertEquals("READY", analysis.kittingStatus);
        assertEquals("RELEASED", taskService.getTask(task.taskId).taskStatus);
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
        task = taskService.createTask(task);

        MesKittingAnalysis analysis = kittingService.analyze(task.taskId);

        assertEquals("SHORTAGE", analysis.kittingStatus);
        assertFalse(kittingService.listAlerts().isEmpty());
        assertEquals("SHORTAGE", taskService.getTask(task.taskId).taskStatus);
    }

    private MesProduct createProduct() {
        MesProduct product = new MesProduct();
        product.productName = "半钢子午线轮胎";
        product.productModel = "205/55R16";
        return masterDataService.createProduct(product);
    }

    private void createBom(long productId) {
        MesProductBom bom = new MesProductBom();
        bom.materialId = 1L;
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
        MesInventory inventory = new MesInventory();
        inventory.materialId = 1L;
        inventory.availableQty = new BigDecimal("300");
        warehouseService.createInventory(inventory);
    }
}
