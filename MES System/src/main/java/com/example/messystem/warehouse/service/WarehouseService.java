package com.example.messystem.warehouse.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.warehouse.dao.WarehouseDao;
import com.example.messystem.warehouse.entity.MesInventory;
import com.example.messystem.warehouse.entity.MesInventoryTransaction;
import com.example.messystem.warehouse.entity.MesMaterial;
import com.example.messystem.warehouse.entity.MesMaterialRequisition;
import com.example.messystem.warehouse.entity.MesPickingTask;
import com.example.messystem.warehouse.entity.MesRobot;
import com.example.messystem.warehouse.entity.MesRobotDeliveryTask;
import com.example.messystem.warehouse.entity.MesWarehouse;
import com.example.messystem.warehouse.entity.MesWarehouseLocation;
import java.sql.SQLException;
import java.util.List;

public class WarehouseService {
    private final WarehouseDao dao = new WarehouseDao();

    public List<MesMaterial> listMaterials() {
        return database(dao::listMaterials);
    }

    public MesMaterial getMaterial(long materialId) {
        return database(() -> dao.findMaterial(materialId));
    }

    public MesMaterial createMaterial(MesMaterial material) {
        requireText(material.materialName, "materialName is required");
        return database(() -> dao.insertMaterial(material));
    }

    public List<MesWarehouse> listWarehouses() {
        return database(dao::listWarehouses);
    }

    public MesWarehouse getWarehouse(long warehouseId) {
        return database(() -> dao.findWarehouse(warehouseId));
    }

    public MesWarehouse createWarehouse(MesWarehouse warehouse) {
        requireText(warehouse.warehouseName, "warehouseName is required");
        return database(() -> dao.insertWarehouse(warehouse));
    }

    public List<MesWarehouseLocation> listLocations() {
        return database(dao::listLocations);
    }

    public MesWarehouseLocation getLocation(long locationId) {
        return database(() -> dao.findLocation(locationId));
    }

    public MesWarehouseLocation createLocation(MesWarehouseLocation location) {
        requireId(location.warehouseId, "warehouseId is required");
        return database(() -> dao.insertLocation(location));
    }

    public List<MesInventory> listInventory() {
        return database(dao::listInventory);
    }

    public MesInventory getInventory(long inventoryId) {
        return database(() -> dao.findInventory(inventoryId));
    }

    public MesInventory createInventory(MesInventory item) {
        requireId(item.materialId, "materialId is required");
        requireId(item.warehouseId, "warehouseId is required");
        requireId(item.locationId, "locationId is required");
        return database(() -> dao.insertInventory(item));
    }

    public List<MesInventoryTransaction> listTransactions() {
        return database(dao::listTransactions);
    }

    public MesInventoryTransaction getTransaction(long transactionId) {
        return database(() -> dao.findTransaction(transactionId));
    }

    public List<MesMaterialRequisition> listRequisitions() {
        return database(dao::listRequisitions);
    }

    public List<MesMaterialRequisition> listRequisitionsByWorkOrder(long workOrderId) {
        requireId(workOrderId, "workOrderId is required");
        return database(() -> dao.listRequisitionsByWorkOrder(workOrderId));
    }

    public MesMaterialRequisition getRequisition(long requisitionId) {
        return database(() -> dao.findRequisition(requisitionId));
    }

    public MesMaterialRequisition createRequisition(MesMaterialRequisition requisition) {
        requireId(requisition.workOrderId, "workOrderId is required");
        if (requisition.items == null || requisition.items.isEmpty()) {
            throw new BadRequestException("requisition items are required");
        }
        return database(() -> dao.insertRequisition(requisition));
    }

    public MesMaterialRequisition approveRequisition(long requisitionId, Long approvedBy) {
        return database(() -> dao.approveRequisition(requisitionId, approvedBy));
    }

    public List<MesPickingTask> listPickingTasks() {
        return database(dao::listPickingTasks);
    }

    public MesPickingTask getPickingTask(long pickingTaskId) {
        return database(() -> dao.findPickingTask(pickingTaskId));
    }

    public MesPickingTask completePicking(long pickingTaskId) {
        return database(() -> dao.completePicking(pickingTaskId));
    }

    public List<MesRobot> listRobots() {
        return database(dao::listRobots);
    }

    public MesRobot getRobot(long robotId) {
        return database(() -> dao.findRobot(robotId));
    }

    public MesRobot createRobot(MesRobot robot) {
        requireText(robot.robotName, "robotName is required");
        return database(() -> dao.insertRobot(robot));
    }

    public List<MesRobotDeliveryTask> listDeliveryTasks() {
        return database(dao::listDeliveryTasks);
    }

    public MesRobotDeliveryTask getDeliveryTask(long deliveryTaskId) {
        return database(() -> dao.findDeliveryTask(deliveryTaskId));
    }

    public MesRobotDeliveryTask markDeliveryArrived(long deliveryTaskId) {
        return database(() -> dao.markDeliveryArrived(deliveryTaskId));
    }

    public MesRobotDeliveryTask confirmDeliveryReceipt(long deliveryTaskId) {
        return database(() -> dao.confirmDeliveryReceipt(deliveryTaskId));
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

    private static <T> T database(SqlCall<T> call) {
        try {
            return call.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface SqlCall<T> {
        T execute() throws SQLException;
    }
}
