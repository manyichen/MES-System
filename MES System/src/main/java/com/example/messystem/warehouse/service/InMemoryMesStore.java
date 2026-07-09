package com.example.messystem.warehouse.service;

import com.example.messystem.production.entity.MesPieceworkWage;
import com.example.messystem.production.entity.MesWorkReport;
import com.example.messystem.warehouse.entity.MesInventory;
import com.example.messystem.warehouse.entity.MesInventoryTransaction;
import com.example.messystem.warehouse.entity.MesMaterial;
import com.example.messystem.warehouse.entity.MesMaterialRequisition;
import com.example.messystem.warehouse.entity.MesPickingTask;
import com.example.messystem.warehouse.entity.MesRobot;
import com.example.messystem.warehouse.entity.MesRobotDeliveryTask;
import com.example.messystem.warehouse.entity.MesWarehouse;
import com.example.messystem.warehouse.entity.MesWarehouseLocation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryMesStore {
    public static final Map<Long, MesMaterial> materials = new LinkedHashMap<>();
    public static final Map<Long, MesWarehouse> warehouses = new LinkedHashMap<>();
    public static final Map<Long, MesWarehouseLocation> locations = new LinkedHashMap<>();
    public static final Map<Long, MesInventory> inventory = new LinkedHashMap<>();
    public static final Map<Long, MesInventoryTransaction> transactions = new LinkedHashMap<>();
    public static final Map<Long, MesMaterialRequisition> requisitions = new LinkedHashMap<>();
    public static final Map<Long, MesPickingTask> pickingTasks = new LinkedHashMap<>();
    public static final Map<Long, MesRobot> robots = new LinkedHashMap<>();
    public static final Map<Long, MesRobotDeliveryTask> deliveryTasks = new LinkedHashMap<>();
    public static final Map<Long, MesWorkReport> workReports = new LinkedHashMap<>();
    public static final Map<Long, MesPieceworkWage> wages = new LinkedHashMap<>();

    private static final AtomicLong ids = new AtomicLong();

    private InMemoryMesStore() {
    }

    public static long nextId() {
        return ids.incrementAndGet();
    }

    public static void clear() {
        materials.clear();
        warehouses.clear();
        locations.clear();
        inventory.clear();
        transactions.clear();
        requisitions.clear();
        pickingTasks.clear();
        robots.clear();
        deliveryTasks.clear();
        workReports.clear();
        wages.clear();
        ids.set(0);
    }
}
