package com.example.messystem.master.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.master.entity.MesProcessRoute;
import com.example.messystem.master.entity.MesProduct;
import com.example.messystem.master.entity.MesProductBom;
import com.example.messystem.master.entity.MesProductionLine;
import com.example.messystem.master.entity.MesSyncLog;
import com.example.messystem.planning.service.PlanningStore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MasterDataService {
    public List<MesProduct> listProducts() {
        return new ArrayList<>(PlanningStore.products.values());
    }

    public MesProduct createProduct(MesProduct product) {
        requireText(product.productName, "productName is required");
        product.productId = PlanningStore.nextId();
        if (product.productCode == null || product.productCode.isBlank()) {
            product.productCode = IdGenerator.nextCode("PRD");
        }
        product.unit = product.unit == null || product.unit.isBlank() ? "条" : product.unit;
        product.enabled = product.enabled == null ? 1 : product.enabled;
        product.createdAt = LocalDateTime.now();
        product.updatedAt = product.createdAt;
        PlanningStore.products.put(product.productId, product);
        log("PRODUCT", "mes_product", "SUCCESS", "product created " + product.productCode);
        return product;
    }

    public MesProduct findProduct(long productId) {
        MesProduct product = PlanningStore.products.get(productId);
        if (product == null) {
            throw new NotFoundException("product not found");
        }
        return product;
    }

    public List<MesProductBom> listBom(long productId) {
        findProduct(productId);
        return PlanningStore.productBoms.values().stream()
                .filter(item -> item.productId != null && item.productId == productId)
                .toList();
    }

    public MesProductBom createBom(long productId, MesProductBom bom) {
        findProduct(productId);
        requireId(bom.materialId, "materialId is required");
        bom.bomId = PlanningStore.nextId();
        bom.productId = productId;
        bom.qtyPerUnit = bom.qtyPerUnit == null ? BigDecimal.ONE : bom.qtyPerUnit;
        bom.unit = bom.unit == null || bom.unit.isBlank() ? "kg" : bom.unit;
        bom.enabled = bom.enabled == null ? 1 : bom.enabled;
        bom.createdAt = LocalDateTime.now();
        PlanningStore.productBoms.put(bom.bomId, bom);
        return bom;
    }

    public List<MesProcessRoute> listProcessRoutes() {
        return PlanningStore.processRoutes.values().stream()
                .sorted(Comparator.comparing(item -> item.processSeq == null ? Integer.MAX_VALUE : item.processSeq))
                .toList();
    }

    public MesProcessRoute createProcessRoute(MesProcessRoute route) {
        requireText(route.processName, "processName is required");
        route.processId = PlanningStore.nextId();
        if (route.processCode == null || route.processCode.isBlank()) {
            route.processCode = IdGenerator.nextCode("PROC");
        }
        route.processSeq = route.processSeq == null ? PlanningStore.processRoutes.size() + 1 : route.processSeq;
        route.enabled = route.enabled == null ? 1 : route.enabled;
        route.createdAt = LocalDateTime.now();
        PlanningStore.processRoutes.put(route.processId, route);
        return route;
    }

    public List<MesProductionLine> listProductionLines() {
        return new ArrayList<>(PlanningStore.productionLines.values());
    }

    public MesProductionLine createProductionLine(MesProductionLine line) {
        requireText(line.lineName, "lineName is required");
        line.lineId = PlanningStore.nextId();
        if (line.lineCode == null || line.lineCode.isBlank()) {
            line.lineCode = IdGenerator.nextCode("LINE");
        }
        line.lineStatus = line.lineStatus == null || line.lineStatus.isBlank() ? "IDLE" : line.lineStatus;
        line.enabled = line.enabled == null ? 1 : line.enabled;
        line.createdAt = LocalDateTime.now();
        PlanningStore.productionLines.put(line.lineId, line);
        return line;
    }

    public List<MesSyncLog> listSyncLogs() {
        return new ArrayList<>(PlanningStore.syncLogs.values());
    }

    private static void log(String type, String table, String status, String message) {
        MesSyncLog log = new MesSyncLog();
        log.syncLogId = PlanningStore.nextId();
        log.syncType = type;
        log.sourceSystem = "MES";
        log.targetTable = table;
        log.syncStatus = status;
        log.message = message;
        log.createdAt = LocalDateTime.now();
        PlanningStore.syncLogs.put(log.syncLogId, log);
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
}
