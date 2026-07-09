package com.example.messystem.warehouse.service;

import com.example.messystem.common.BadRequestException;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WarehouseService {
    public List<MesMaterial> listMaterials() {
        return new ArrayList<>(InMemoryMesStore.materials.values());
    }

    public MesMaterial createMaterial(MesMaterial material) {
        requireText(material.materialName, "materialName is required");
        material.materialId = InMemoryMesStore.nextId();
        if (material.materialCode == null || material.materialCode.isBlank()) {
            material.materialCode = IdGenerator.nextCode("MAT");
        }
        material.enabled = material.enabled == null ? 1 : material.enabled;
        material.createdAt = LocalDateTime.now();
        InMemoryMesStore.materials.put(material.materialId, material);
        return material;
    }

    public List<MesWarehouse> listWarehouses() {
        return new ArrayList<>(InMemoryMesStore.warehouses.values());
    }

    public MesWarehouse createWarehouse(MesWarehouse warehouse) {
        requireText(warehouse.warehouseName, "warehouseName is required");
        warehouse.warehouseId = InMemoryMesStore.nextId();
        if (warehouse.warehouseCode == null || warehouse.warehouseCode.isBlank()) {
            warehouse.warehouseCode = IdGenerator.nextCode("WH");
        }
        warehouse.enabled = warehouse.enabled == null ? 1 : warehouse.enabled;
        InMemoryMesStore.warehouses.put(warehouse.warehouseId, warehouse);
        return warehouse;
    }

    public List<MesWarehouseLocation> listLocations() {
        return new ArrayList<>(InMemoryMesStore.locations.values());
    }

    public MesWarehouseLocation createLocation(MesWarehouseLocation location) {
        requireId(location.warehouseId, "warehouseId is required");
        location.locationId = InMemoryMesStore.nextId();
        if (location.locationCode == null || location.locationCode.isBlank()) {
            location.locationCode = IdGenerator.nextCode("LOC");
        }
        location.enabled = location.enabled == null ? 1 : location.enabled;
        InMemoryMesStore.locations.put(location.locationId, location);
        return location;
    }

    public List<MesInventory> listInventory() {
        return new ArrayList<>(InMemoryMesStore.inventory.values());
    }

    public MesInventory createInventory(MesInventory item) {
        requireId(item.materialId, "materialId is required");
        item.inventoryId = InMemoryMesStore.nextId();
        item.availableQty = nvl(item.availableQty);
        item.reservedQty = nvl(item.reservedQty);
        item.frozenQty = nvl(item.frozenQty);
        item.qualityStatus = item.qualityStatus == null ? "PASS" : item.qualityStatus;
        item.lastCheckTime = LocalDateTime.now();
        InMemoryMesStore.inventory.put(item.inventoryId, item);
        return item;
    }

    public List<MesInventoryTransaction> listTransactions() {
        return new ArrayList<>(InMemoryMesStore.transactions.values());
    }

    public List<MesMaterialRequisition> listRequisitions() {
        return new ArrayList<>(InMemoryMesStore.requisitions.values());
    }

    public MesMaterialRequisition createRequisition(MesMaterialRequisition requisition) {
        requireId(requisition.workOrderId, "workOrderId is required");
        requisition.requisitionId = InMemoryMesStore.nextId();
        requisition.requisitionNo = IdGenerator.nextCode("REQ");
        requisition.requestStatus = "CREATED";
        requisition.requestTime = LocalDateTime.now();
        if (requisition.items != null) {
            for (MesMaterialRequisitionItem item : requisition.items) {
                item.requisitionItemId = InMemoryMesStore.nextId();
                item.requisitionId = requisition.requisitionId;
                item.issuedQty = nvl(item.issuedQty);
                item.itemStatus = "CREATED";
            }
        }
        InMemoryMesStore.requisitions.put(requisition.requisitionId, requisition);
        return requisition;
    }

    public MesMaterialRequisition approveRequisition(long requisitionId, Long approvedBy) {
        MesMaterialRequisition requisition = find(InMemoryMesStore.requisitions, requisitionId, "requisition not found");
        if (!"CREATED".equals(requisition.requestStatus)) {
            throw new BadRequestException("only CREATED requisitions can be approved");
        }
        requisition.requestStatus = "APPROVED";
        requisition.approvedBy = approvedBy;
        requisition.approvedTime = LocalDateTime.now();
        MesPickingTask pickingTask = new MesPickingTask();
        pickingTask.pickingTaskId = InMemoryMesStore.nextId();
        pickingTask.pickingTaskNo = IdGenerator.nextCode("PICK");
        pickingTask.requisitionId = requisitionId;
        pickingTask.warehouseId = firstWarehouseId();
        pickingTask.taskStatus = "CREATED";
        InMemoryMesStore.pickingTasks.put(pickingTask.pickingTaskId, pickingTask);
        return requisition;
    }

    public List<MesPickingTask> listPickingTasks() {
        return new ArrayList<>(InMemoryMesStore.pickingTasks.values());
    }

    public MesPickingTask completePicking(long pickingTaskId) {
        MesPickingTask task = find(InMemoryMesStore.pickingTasks, pickingTaskId, "picking task not found");
        task.taskStatus = "COMPLETED";
        task.finishTime = LocalDateTime.now();
        MesRobotDeliveryTask deliveryTask = new MesRobotDeliveryTask();
        deliveryTask.deliveryTaskId = InMemoryMesStore.nextId();
        deliveryTask.deliveryTaskNo = IdGenerator.nextCode("RBT");
        deliveryTask.pickingTaskId = pickingTaskId;
        deliveryTask.robotId = firstRobotId();
        deliveryTask.fromLocationId = firstLocationId();
        deliveryTask.toLineId = 1L;
        deliveryTask.deliveryStatus = "PENDING";
        deliveryTask.loadTime = LocalDateTime.now();
        InMemoryMesStore.deliveryTasks.put(deliveryTask.deliveryTaskId, deliveryTask);
        return task;
    }

    public List<MesRobot> listRobots() {
        return new ArrayList<>(InMemoryMesStore.robots.values());
    }

    public MesRobot createRobot(MesRobot robot) {
        requireText(robot.robotName, "robotName is required");
        robot.robotId = InMemoryMesStore.nextId();
        if (robot.robotCode == null || robot.robotCode.isBlank()) {
            robot.robotCode = IdGenerator.nextCode("ROB");
        }
        robot.robotStatus = robot.robotStatus == null ? "IDLE" : robot.robotStatus;
        robot.enabled = robot.enabled == null ? 1 : robot.enabled;
        InMemoryMesStore.robots.put(robot.robotId, robot);
        return robot;
    }

    public List<MesRobotDeliveryTask> listDeliveryTasks() {
        return new ArrayList<>(InMemoryMesStore.deliveryTasks.values());
    }

    public MesRobotDeliveryTask markDeliveryArrived(long deliveryTaskId) {
        MesRobotDeliveryTask task = find(InMemoryMesStore.deliveryTasks, deliveryTaskId, "delivery task not found");
        task.deliveryStatus = "ARRIVED";
        task.handoverTime = LocalDateTime.now();
        deductInventoryForPicking(task.pickingTaskId);
        return task;
    }

    private void deductInventoryForPicking(Long pickingTaskId) {
        MesPickingTask pickingTask = find(InMemoryMesStore.pickingTasks, pickingTaskId, "picking task not found");
        MesMaterialRequisition requisition = find(InMemoryMesStore.requisitions, pickingTask.requisitionId, "requisition not found");
        if (requisition.items == null) {
            return;
        }
        for (MesMaterialRequisitionItem reqItem : requisition.items) {
            MesInventory inv = findInventory(reqItem.materialId);
            BigDecimal qty = nvl(reqItem.requiredQty);
            if (inv.availableQty.compareTo(qty) < 0) {
                throw new BadRequestException("inventory is not enough for materialId " + reqItem.materialId);
            }
            inv.availableQty = inv.availableQty.subtract(qty);
            reqItem.issuedQty = qty;
            reqItem.itemStatus = "COMPLETED";
            MesInventoryTransaction tx = new MesInventoryTransaction();
            tx.transactionId = InMemoryMesStore.nextId();
            tx.transactionNo = IdGenerator.nextCode("TX");
            tx.materialId = reqItem.materialId;
            tx.inventoryId = inv.inventoryId;
            tx.transactionType = "OUT";
            tx.qty = qty;
            tx.sourceDocType = "PICKING_TASK";
            tx.sourceDocId = pickingTaskId;
            tx.createdAt = LocalDateTime.now();
            InMemoryMesStore.transactions.put(tx.transactionId, tx);
        }
        requisition.requestStatus = "COMPLETED";
    }

    private MesInventory findInventory(Long materialId) {
        return InMemoryMesStore.inventory.values().stream()
                .filter(item -> materialId != null && materialId.equals(item.materialId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("inventory not found for materialId " + materialId));
    }

    private Long firstWarehouseId() {
        return InMemoryMesStore.warehouses.keySet().stream().findFirst().orElse(null);
    }

    private Long firstLocationId() {
        return InMemoryMesStore.locations.keySet().stream().findFirst().orElse(null);
    }

    private Long firstRobotId() {
        return InMemoryMesStore.robots.keySet().stream().findFirst().orElse(null);
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static void requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BadRequestException(message);
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private static <T> T find(Map<Long, T> map, Long id, String message) {
        T value = map.get(id);
        if (value == null) {
            throw new NotFoundException(message);
        }
        return value;
    }
}
