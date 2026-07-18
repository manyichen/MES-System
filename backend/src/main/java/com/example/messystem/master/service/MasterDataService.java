/*
 * 答辩定位：主数据与用户 模块的 MasterDataService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
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

/**
 * 主数据与用户 的 MasterDataService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class MasterDataService {
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final PlanningDao dao = new PlanningDao();

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesProduct> listProducts() {
        return database(dao::listProducts);
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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

    /**
     * 业务用例：查询匹配记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesProduct findProduct(long productId) {
        return database(() -> dao.findProduct(productId))
                .orElseThrow(() -> new NotFoundException("product not found"));
    }

    /**
     * 业务用例：更新业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesProduct updateProduct(long productId, MesProduct product) {
        requireId(productId, "productId is required");
        requireText(product.productCode, "productCode is required");
        requireText(product.productName, "productName is required");
        product.productModel = product.productModel == null || product.productModel.isBlank()
                ? product.productName : product.productModel;
        product.enabled = product.enabled == null ? 1 : product.enabled;
        return database(() -> dao.updateProduct(productId, product));
    }

    /**
     * 业务用例：停用业务对象。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesProduct disableProduct(long productId) {
        requireId(productId, "productId is required");
        return database(() -> dao.disableProduct(productId));
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesProductBom> listBom(long productId) {
        findProduct(productId);
        return database(() -> dao.listBom(productId));
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesProductBom> listAllBom() {
        return database(dao::listAllBom);
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesProductBom createBom(long productId, MesProductBom bom) {
        findProduct(productId);
        requireId(bom.materialId, "materialId is required");
        bom.productId = productId;
        if (bom.qtyPerUnit == null || bom.qtyPerUnit.signum() <= 0) {
            throw new BadRequestException("BOM物料单耗必须大于0");
        }
        bom.unit = bom.unit == null || bom.unit.isBlank() ? "kg" : bom.unit;
        bom.enabled = bom.enabled == null ? 1 : bom.enabled;
        return database(() -> dao.insertBom(bom));
    }

    /**
     * 业务用例：更新业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesProductBom updateBom(long bomId, MesProductBom bom) {
        requireId(bomId, "bomId is required");
        requireId(bom.productId, "productId is required");
        requireId(bom.materialId, "materialId is required");
        if (bom.qtyPerUnit == null || bom.qtyPerUnit.signum() <= 0) {
            throw new BadRequestException("qtyPerUnit must be positive");
        }
        bom.unit = bom.unit == null || bom.unit.isBlank() ? "kg" : bom.unit;
        bom.enabled = bom.enabled == null ? 1 : bom.enabled;
        return database(() -> dao.updateBom(bomId, bom));
    }

    /**
     * 业务用例：删除业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public void deleteBom(long bomId) {
        requireId(bomId, "bomId is required");
        database(() -> { dao.deleteBom(bomId); return null; });
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesProcessRoute> listProcessRoutes() {
        return database(dao::listProcessRoutes);
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesProcessRoute createProcessRoute(MesProcessRoute route) {
        requireText(route.processName, "processName is required");
        if (route.processCode == null || route.processCode.isBlank()) {
            route.processCode = IdGenerator.nextCode("PROC");
        }
        route.processSeq = route.processSeq == null ? 1 : route.processSeq;
        route.enabled = route.enabled == null ? 1 : route.enabled;
        return database(() -> dao.insertProcessRoute(route));
    }

    /**
     * 业务用例：更新业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesProcessRoute updateProcessRoute(long processId, MesProcessRoute route) {
        requireText(route.processName, "processName is required");
        if (route.processCode == null || route.processCode.isBlank()) {
            route.processCode = IdGenerator.nextCode("PROC");
        }
        route.processSeq = route.processSeq == null ? 1 : route.processSeq;
        route.enabled = route.enabled == null ? 1 : route.enabled;
        return database(() -> dao.updateProcessRoute(processId, route));
    }

    /**
     * 业务用例：删除业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public void deleteProcessRoute(long processId) {
        database(() -> {
            dao.deleteProcessRoute(processId);
            return null;
        });
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesProductionLine> listProductionLines() {
        return database(dao::listProductionLines);
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesProductionLine createProductionLine(MesProductionLine line) {
        requireText(line.lineName, "lineName is required");
        if (line.lineCode == null || line.lineCode.isBlank()) {
            line.lineCode = IdGenerator.nextCode("LINE");
        }
        line.lineStatus = line.lineStatus == null || line.lineStatus.isBlank() ? "IDLE" : line.lineStatus;
        line.enabled = line.enabled == null ? 1 : line.enabled;
        return database(() -> dao.insertProductionLine(line));
    }

    /**
     * 业务用例：更新业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesProductionLine updateProductionLine(long lineId, MesProductionLine line) {
        requireId(lineId, "lineId is required");
        requireText(line.lineCode, "lineCode is required");
        requireText(line.lineName, "lineName is required");
        line.lineStatus = line.lineStatus == null || line.lineStatus.isBlank() ? "IDLE" : line.lineStatus;
        line.enabled = line.enabled == null ? 1 : line.enabled;
        return database(() -> dao.updateProductionLine(lineId, line));
    }

    /**
     * 业务用例：停用业务对象。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesProductionLine disableProductionLine(long lineId) {
        requireId(lineId, "lineId is required");
        return database(() -> dao.disableProductionLine(lineId));
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesSyncLog> listSyncLogs() {
        return dao.listSyncLogs();
    }

    /**
     * 业务用例：执行 requireId 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static void requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BadRequestException(message);
        }
    }

    /**
     * 业务用例：执行 requireText 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }

    /**
     * 业务用例：执行 database 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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
