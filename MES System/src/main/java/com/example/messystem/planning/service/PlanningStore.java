package com.example.messystem.planning.service;

import com.example.messystem.master.entity.MesProcessRoute;
import com.example.messystem.master.entity.MesProduct;
import com.example.messystem.master.entity.MesProductBom;
import com.example.messystem.master.entity.MesProductionLine;
import com.example.messystem.master.entity.MesSyncLog;
import com.example.messystem.master.entity.MesUser;
import com.example.messystem.planning.entity.MesCustomerOrder;
import com.example.messystem.planning.entity.MesKittingAnalysis;
import com.example.messystem.planning.entity.MesKittingShortageItem;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.entity.MesShortageAlert;
import com.example.messystem.planning.entity.MesWorkOrder;
import com.example.messystem.planning.entity.MesWorkOrderOperationLog;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class PlanningStore {
    public static final Map<Long, MesUser> users = new LinkedHashMap<>();
    public static final Map<Long, MesProduct> products = new LinkedHashMap<>();
    public static final Map<Long, MesProductBom> productBoms = new LinkedHashMap<>();
    public static final Map<Long, MesProcessRoute> processRoutes = new LinkedHashMap<>();
    public static final Map<Long, MesProductionLine> productionLines = new LinkedHashMap<>();
    public static final Map<Long, MesSyncLog> syncLogs = new LinkedHashMap<>();
    public static final Map<Long, MesCustomerOrder> orders = new LinkedHashMap<>();
    public static final Map<Long, MesProductionTask> tasks = new LinkedHashMap<>();
    public static final Map<Long, MesKittingAnalysis> analyses = new LinkedHashMap<>();
    public static final Map<Long, MesKittingShortageItem> shortageItems = new LinkedHashMap<>();
    public static final Map<Long, MesShortageAlert> shortageAlerts = new LinkedHashMap<>();
    public static final Map<Long, MesWorkOrder> workOrders = new LinkedHashMap<>();
    public static final Map<Long, MesWorkOrderOperationLog> operationLogs = new LinkedHashMap<>();

    private static final AtomicLong ids = new AtomicLong();

    private PlanningStore() {
    }

    public static long nextId() {
        return ids.incrementAndGet();
    }

    public static void clear() {
        users.clear();
        products.clear();
        productBoms.clear();
        processRoutes.clear();
        productionLines.clear();
        syncLogs.clear();
        orders.clear();
        tasks.clear();
        analyses.clear();
        shortageItems.clear();
        shortageAlerts.clear();
        workOrders.clear();
        operationLogs.clear();
        ids.set(0);
    }
}
