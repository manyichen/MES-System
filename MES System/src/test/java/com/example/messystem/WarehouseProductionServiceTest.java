package com.example.messystem;

import com.example.messystem.production.entity.MesWorkReport;
import com.example.messystem.production.service.ProductionService;
import com.example.messystem.warehouse.entity.MesInventory;
import com.example.messystem.warehouse.entity.MesMaterialRequisition;
import com.example.messystem.warehouse.entity.MesMaterialRequisitionItem;
import com.example.messystem.warehouse.service.InMemoryMesStore;
import com.example.messystem.warehouse.service.WarehouseService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WarehouseProductionServiceTest {
    private WarehouseService warehouseService;
    private ProductionService productionService;

    @BeforeEach
    void setUp() {
        InMemoryMesStore.clear();
        warehouseService = new WarehouseService();
        productionService = new ProductionService();
    }

    @Test
    void approveRequisitionShouldReserveOrDeductInventory() {
        MesMaterialRequisition requisition = createRequisition();

        MesMaterialRequisition approved = warehouseService.approveRequisition(requisition.requisitionId, 1L);

        assertEquals("APPROVED", approved.requestStatus);
        assertEquals(1, warehouseService.listPickingTasks().size());
    }

    @Test
    void completePickingShouldCreateDeliveryTask() {
        MesMaterialRequisition requisition = createRequisition();
        warehouseService.approveRequisition(requisition.requisitionId, 1L);

        warehouseService.completePicking(warehouseService.listPickingTasks().get(0).pickingTaskId);

        assertEquals("COMPLETED", warehouseService.listPickingTasks().get(0).taskStatus);
        assertEquals(1, warehouseService.listDeliveryTasks().size());
    }

    @Test
    void approveWorkReportShouldCreatePieceworkWage() {
        MesWorkReport report = new MesWorkReport();
        report.workOrderId = 1L;
        report.operatorId = 9L;
        report.reportQty = 100;
        report.qualifiedQty = 96;
        report.defectQty = 4;
        MesWorkReport created = productionService.createWorkReport(report);

        productionService.approveWorkReport(created.reportId);

        assertEquals("APPROVED", productionService.listWorkReports().get(0).reportStatus);
        assertFalse(productionService.listWages().isEmpty());
        assertEquals(new BigDecimal("240.00"), productionService.listWages().get(0).wageAmount);
    }

    private MesMaterialRequisition createRequisition() {
        MesInventory inventory = new MesInventory();
        inventory.materialId = 1L;
        inventory.availableQty = new BigDecimal("100");
        warehouseService.createInventory(inventory);

        MesMaterialRequisitionItem item = new MesMaterialRequisitionItem();
        item.materialId = 1L;
        item.requiredQty = new BigDecimal("10");

        MesMaterialRequisition requisition = new MesMaterialRequisition();
        requisition.workOrderId = 1L;
        requisition.items = List.of(item);
        return warehouseService.createRequisition(requisition);
    }
}
