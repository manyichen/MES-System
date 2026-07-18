/*
 * 答辩定位：仓储、领料、拣货与机器人物流 模块的 WarehouseService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.warehouse.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.warehouse.dao.WarehouseDao;
import com.example.messystem.warehouse.entity.ExternalPurchaseRequest;
import com.example.messystem.warehouse.entity.ExternalPurchaseResult;
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
import java.sql.SQLException;
import java.util.List;

/**
 * 仓储、领料、拣货与机器人物流 的 WarehouseService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class WarehouseService {
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final WarehouseDao dao = new WarehouseDao();

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesMaterial> listMaterials() {
        return database(dao::listMaterials);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesMaterial getMaterial(long materialId) {
        return database(() -> dao.findMaterial(materialId));
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesMaterial createMaterial(MesMaterial material) {
        requireText(material.materialName, "materialName is required");
        return database(() -> dao.insertMaterial(material));
    }

    /**
     * 业务用例：更新业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesMaterial updateMaterial(long materialId, MesMaterial material) {
        requireId(materialId, "materialId is required");
        return database(() -> dao.updateMaterial(materialId, material));
    }

    /**
     * 业务用例：删除业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public void deleteMaterial(long materialId) {
        requireId(materialId, "materialId is required");
        database(() -> {
            dao.deleteMaterial(materialId);
            return null;
        });
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesWarehouse> listWarehouses() {
        return database(dao::listWarehouses);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWarehouse getWarehouse(long warehouseId) {
        return database(() -> dao.findWarehouse(warehouseId));
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWarehouse createWarehouse(MesWarehouse warehouse) {
        requireText(warehouse.warehouseName, "warehouseName is required");
        return database(() -> dao.insertWarehouse(warehouse));
    }

    /**
     * 业务用例：更新业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWarehouse updateWarehouse(long warehouseId, MesWarehouse warehouse) {
        requireId(warehouseId, "warehouseId is required");
        return database(() -> dao.updateWarehouse(warehouseId, warehouse));
    }

    /**
     * 业务用例：删除业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public void deleteWarehouse(long warehouseId) {
        requireId(warehouseId, "warehouseId is required");
        database(() -> {
            dao.deleteWarehouse(warehouseId);
            return null;
        });
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesWarehouseLocation> listLocations() {
        return database(dao::listLocations);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWarehouseLocation getLocation(long locationId) {
        return database(() -> dao.findLocation(locationId));
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWarehouseLocation createLocation(MesWarehouseLocation location) {
        requireId(location.warehouseId, "warehouseId is required");
        return database(() -> dao.insertLocation(location));
    }

    /**
     * 业务用例：更新业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWarehouseLocation updateLocation(long locationId, MesWarehouseLocation location) {
        requireId(locationId, "locationId is required");
        return database(() -> dao.updateLocation(locationId, location));
    }

    /**
     * 业务用例：删除业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public void deleteLocation(long locationId) {
        requireId(locationId, "locationId is required");
        database(() -> {
            dao.deleteLocation(locationId);
            return null;
        });
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesInventory> listInventory() {
        return database(dao::listInventory);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesInventory getInventory(long inventoryId) {
        return database(() -> dao.findInventory(inventoryId));
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesInventory> listInventoryByMaterial(long materialId) {
        requireId(materialId, "materialId is required");
        return database(() -> dao.listInventoryByMaterial(materialId));
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesInventory createInventory(MesInventory item) {
        requireId(item.materialId, "materialId is required");
        requireId(item.warehouseId, "warehouseId is required");
        requireId(item.locationId, "locationId is required");
        return database(() -> dao.insertInventory(item));
    }

    /**
     * 业务用例：更新业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesInventory updateInventory(long inventoryId, MesInventory item) {
        requireId(inventoryId, "inventoryId is required");
        validateInventoryQuantities(item);
        return database(() -> dao.updateInventory(inventoryId, item));
    }

    /**
     * 业务用例：删除业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public void deleteInventory(long inventoryId) {
        requireId(inventoryId, "inventoryId is required");
        database(() -> {
            dao.deleteInventory(inventoryId);
            return null;
        });
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesInventoryTransaction> listTransactions() {
        return database(dao::listTransactions);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesInventoryTransaction getTransaction(long transactionId) {
        return database(() -> dao.findTransaction(transactionId));
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesInventoryTransaction createTransaction(MesInventoryTransaction transaction) {
        requireId(transaction.materialId, "materialId is required");
        if (transaction.qty == null || transaction.qty.signum() <= 0) {
            throw new BadRequestException("qty must be positive");
        }
        return database(() -> dao.insertTransaction(transaction));
    }

    /**
     * 业务用例：模拟外部采购入库。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public ExternalPurchaseResult externalPurchase(ExternalPurchaseRequest request, long operatorId) {
        if (request == null) {
            throw new BadRequestException("purchase request is required");
        }
        requireId(request.materialId, "materialId is required");
        requireId(request.warehouseId, "warehouseId is required");
        requireId(operatorId, "operatorId is required");
        if (request.qty == null || request.qty.signum() <= 0) {
            throw new BadRequestException("purchase qty must be positive");
        }
        return database(() -> dao.externalPurchase(request, operatorId));
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesMaterialRequisition> listRequisitions() {
        return database(dao::listRequisitions);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesMaterialRequisition> listRequisitionsByWorkOrder(long workOrderId) {
        requireId(workOrderId, "workOrderId is required");
        return database(() -> dao.listRequisitionsByWorkOrder(workOrderId));
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesMaterialRequisition> listRequisitionsByRequester(long requesterId) {
        requireId(requesterId, "requesterId is required");
        return database(() -> dao.listRequisitionsByRequester(requesterId));
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesMaterialRequisition getRequisition(long requisitionId) {
        return database(() -> dao.findRequisition(requisitionId));
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesMaterialRequisition getRequisitionForRequester(long requisitionId, long requesterId) {
        requireId(requisitionId, "requisitionId is required");
        requireId(requesterId, "requesterId is required");
        return database(() -> dao.findRequisitionForRequester(requisitionId, requesterId));
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesMaterialRequisition createRequisition(MesMaterialRequisition requisition) {
        validateRequisition(requisition);
        return database(() -> dao.insertRequisition(requisition));
    }

    /** 仅当操作工拥有相关生产工单时才允许创建领料申请。 */
    public MesMaterialRequisition createOperatorRequisition(MesMaterialRequisition requisition, long operatorId) {
        return createRequisitionForRequester(requisition, operatorId, false);
    }

    /**
     * 超级管理员可以代表任意可执行工单发起领料；普通操作工仍只能选择本人被派或已接收的工单。
     */
    public MesMaterialRequisition createRequisitionForRequester(MesMaterialRequisition requisition,
            long requesterId, boolean allowAdministrativeOverride) {
        requireId(requesterId, "requesterId is required");
        validateRequisition(requisition);
        if (!allowAdministrativeOverride) {
            requireOperatorWorkOrderAccess(requisition.workOrderId, requesterId);
        }
        requisition.requestedBy = requesterId;
        return database(() -> dao.insertRequisition(requisition));
    }

    /** 在读取或修改领料申请前校验操作工的工单归属边界。 */
    public void requireOperatorWorkOrderAccess(long workOrderId, long operatorId) {
        requireId(workOrderId, "workOrderId is required");
        requireId(operatorId, "operatorId is required");
        if (!database(() -> dao.isWorkOrderAssignedTo(workOrderId, operatorId))) {
            throw new BadRequestException("只能访问本人被派或已接收工单的领料信息");
        }
    }

    /**
     * 业务用例：校验业务输入与约束。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static void validateRequisition(MesMaterialRequisition requisition) {
        if (requisition == null) {
            throw new BadRequestException("requisition body is required");
        }
        requireId(requisition.workOrderId, "workOrderId is required");
        requireId(requisition.warehouseId, "warehouseId is required");
        if (requisition.items == null || requisition.items.isEmpty()) {
            throw new BadRequestException("requisition items are required");
        }
        for (MesMaterialRequisitionItem item : requisition.items) {
            if (item == null) {
                throw new BadRequestException("requisition items are required");
            }
            requireId(item.materialId, "materialId is required");
            if (item.requiredQty == null || item.requiredQty.signum() <= 0) {
                throw new BadRequestException("requiredQty must be positive");
            }
            if (item.batchNo != null && item.batchNo.isBlank()) {
                item.batchNo = null;
            }
        }
    }

    /**
     * 业务用例：接收已派发任务。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesMaterialRequisition receiveRequisition(long requisitionId, Long receivedBy) {
        requireId(requisitionId, "requisitionId is required");
        requireId(receivedBy, "receivedBy is required");
        return database(() -> dao.receiveRequisition(requisitionId, receivedBy));
    }

    /**
     * 业务用例：审核通过业务事项。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesMaterialRequisition approveRequisition(long requisitionId, Long approvedBy) {
        requireId(requisitionId, "requisitionId is required");
        requireId(approvedBy, "approvedBy is required");
        return database(() -> dao.approveRequisition(requisitionId, approvedBy));
    }

    /**
     * 业务用例：驳回业务事项。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesMaterialRequisition rejectRequisition(long requisitionId, Long approvedBy, String reason) {
        requireId(requisitionId, "requisitionId is required");
        requireId(approvedBy, "approvedBy is required");
        return database(() -> dao.rejectRequisition(requisitionId, approvedBy, reason));
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesPickingTask> listPickingTasks() {
        return database(dao::listPickingTasks);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesPickingTask getPickingTask(long pickingTaskId) {
        return database(() -> dao.findPickingTask(pickingTaskId));
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesPickingTask createPickingTask(MesPickingTask task) {
        requireId(task.requisitionId, "requisitionId is required");
        requireId(task.warehouseId, "warehouseId is required");
        return database(() -> dao.insertPickingTask(task));
    }

    /**
     * 业务用例：完成业务任务。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesPickingTask completePicking(long pickingTaskId) {
        return database(() -> dao.completePicking(pickingTaskId));
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesRobot> listRobots() {
        return database(dao::listRobots);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesRobot getRobot(long robotId) {
        return database(() -> dao.findRobot(robotId));
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesRobot createRobot(MesRobot robot) {
        requireText(robot.robotName, "robotName is required");
        requireId(robot.warehouseId, "warehouseId is required");
        return database(() -> dao.insertRobot(robot));
    }

    /**
     * 业务用例：更新业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesRobot updateRobot(long robotId, MesRobot robot) {
        requireId(robotId, "robotId is required");
        return database(() -> dao.updateRobot(robotId, robot));
    }

    /**
     * 业务用例：删除业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public void deleteRobot(long robotId) {
        requireId(robotId, "robotId is required");
        database(() -> {
            dao.deleteRobot(robotId);
            return null;
        });
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesRobotDeliveryTask> listDeliveryTasks() {
        return database(dao::listDeliveryTasks);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesRobotDeliveryTask getDeliveryTask(long deliveryTaskId) {
        return database(() -> dao.findDeliveryTask(deliveryTaskId));
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesRobotDeliveryTask createDeliveryTask(MesRobotDeliveryTask task) {
        requireId(task.pickingTaskId, "pickingTaskId is required");
        requireId(task.fromLocationId, "fromLocationId is required");
        return database(() -> dao.insertDeliveryTask(task));
    }

    /**
     * 业务用例：执行 markDeliveryArrived 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesRobotDeliveryTask markDeliveryArrived(long deliveryTaskId) {
        return database(() -> dao.markDeliveryArrived(deliveryTaskId));
    }

    /**
     * 业务用例：执行 confirmDeliveryReceipt 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesRobotDeliveryTask confirmDeliveryReceipt(long deliveryTaskId) {
        return database(() -> dao.confirmDeliveryReceipt(deliveryTaskId));
    }

    /**
     * 业务用例：执行 confirmDeliveryReceipt 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesRobotDeliveryTask confirmDeliveryReceipt(long deliveryTaskId, long requesterId) {
        requireId(deliveryTaskId, "deliveryTaskId is required");
        requireId(requesterId, "requesterId is required");
        return database(() -> dao.confirmDeliveryReceipt(deliveryTaskId, requesterId));
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
     * 业务用例：校验业务输入与约束。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static void validateInventoryQuantities(MesInventory item) {
        if (item.availableQty != null && item.availableQty.signum() < 0) {
            throw new BadRequestException("availableQty cannot be negative");
        }
        if (item.reservedQty != null && item.reservedQty.signum() < 0) {
            throw new BadRequestException("reservedQty cannot be negative");
        }
        if (item.frozenQty != null && item.frozenQty.signum() < 0) {
            throw new BadRequestException("frozenQty cannot be negative");
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
