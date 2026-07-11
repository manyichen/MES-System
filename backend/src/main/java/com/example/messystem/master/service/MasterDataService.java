package com.example.messystem.master.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.master.entity.MesProcessRoute;
import com.example.messystem.master.entity.MesProduct;
import com.example.messystem.master.entity.MesProductBom;
import com.example.messystem.master.entity.MesProductionLine;
import com.example.messystem.master.entity.MesSyncLog;
import com.example.messystem.planning.dao.PlanningDao;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class MasterDataService {
    private final PlanningDao dao = new PlanningDao();

    public List<MesProduct> listProducts() {
        return database(dao::listProducts);
    }

    public MesProduct createProduct(MesProduct product) {
        requireText(product.productName, "productName is required");
        if (product.productCode == null || product.productCode.isBlank()) {
            product.productCode = IdGenerator.nextCode("PRD");
        }
        product.productModel = product.productModel == null || product.productModel.isBlank()
                ? product.productName
                : product.productModel;
        product.enabled = product.enabled == null ? 1 : product.enabled;
        return database(() -> dao.insertProduct(product));
    }

    public MesProduct findProduct(long productId) {
        return database(() -> dao.findProduct(productId))
                .orElseThrow(() -> new NotFoundException("product not found"));
    }

    public List<MesProductBom> listBom(long productId) {
        findProduct(productId);
        return database(() -> dao.listBom(productId));
    }

    public MesProductBom createBom(long productId, MesProductBom bom) {
        findProduct(productId);
        requireId(bom.materialId, "materialId is required");
        bom.productId = productId;
        bom.qtyPerUnit = bom.qtyPerUnit == null ? BigDecimal.ONE : bom.qtyPerUnit;
        bom.unit = bom.unit == null || bom.unit.isBlank() ? "kg" : bom.unit;
        bom.enabled = bom.enabled == null ? 1 : bom.enabled;
        return database(() -> dao.insertBom(bom));
    }

    public List<MesProcessRoute> listProcessRoutes() {
        return database(dao::listProcessRoutes);
    }

    public MesProcessRoute createProcessRoute(MesProcessRoute route) {
        requireText(route.processName, "processName is required");
        if (route.processCode == null || route.processCode.isBlank()) {
            route.processCode = IdGenerator.nextCode("PROC");
        }
        route.processSeq = route.processSeq == null ? 1 : route.processSeq;
        route.enabled = route.enabled == null ? 1 : route.enabled;
        return database(() -> dao.insertProcessRoute(route));
    }

    public List<MesProductionLine> listProductionLines() {
        return database(dao::listProductionLines);
    }

    public MesProductionLine createProductionLine(MesProductionLine line) {
        requireText(line.lineName, "lineName is required");
        if (line.lineCode == null || line.lineCode.isBlank()) {
            line.lineCode = IdGenerator.nextCode("LINE");
        }
        line.lineStatus = line.lineStatus == null || line.lineStatus.isBlank() ? "IDLE" : line.lineStatus;
        line.enabled = line.enabled == null ? 1 : line.enabled;
        return database(() -> dao.insertProductionLine(line));
    }

    public List<MesSyncLog> listSyncLogs() {
        return dao.listSyncLogs();
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
