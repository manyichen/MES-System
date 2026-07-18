/*
 * 答辩定位：订单、计划、齐套与工单 模块的 CustomerOrderService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.master.entity.MesProduct;
import com.example.messystem.planning.dao.PlanningDao;
import com.example.messystem.planning.entity.MesCustomerOrder;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * 订单、计划、齐套与工单 的 CustomerOrderService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class CustomerOrderService {
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final PlanningDao dao = new PlanningDao();

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesCustomerOrder> listOrders() {
        return database(dao::listOrders);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesCustomerOrder getOrder(long orderId) {
        return database(() -> dao.findOrder(orderId))
                .orElseThrow(() -> new NotFoundException("order not found"));
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesCustomerOrder createOrder(MesCustomerOrder order) {
        requireText(order.customerName, "customerName is required");
        requireId(order.productId, "productId is required");
        if (order.orderQty == null || order.orderQty <= 0) {
            throw new BadRequestException("orderQty must be greater than 0");
        }
        if (order.deliveryDate != null && order.deliveryDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("deliveryDate cannot be earlier than today");
        }
        if (order.priorityLevel != null && (order.priorityLevel < 1 || order.priorityLevel > 5)) {
            throw new BadRequestException("priorityLevel must be between 1 and 5");
        }
        MesProduct product = database(() -> dao.findProduct(order.productId))
                .orElseThrow(() -> new BadRequestException("product not found"));
        if (order.orderNo == null || order.orderNo.isBlank()) {
            order.orderNo = IdGenerator.nextCode("ORD");
        }
        order.productCode = product.productCode;
        order.productModel = product.productModel;
        order.orderQty = order.orderQty;
        order.unit = order.unit == null || order.unit.isBlank() ? "条" : order.unit;
        order.deliveryDate = order.deliveryDate == null ? LocalDate.now().plusDays(14) : order.deliveryDate;
        order.priorityLevel = order.priorityLevel == null ? 3 : order.priorityLevel;
        order.orderStatus = order.orderStatus == null || order.orderStatus.isBlank() ? "PENDING_PLAN" : order.orderStatus;
        order.sourceSystem = order.sourceSystem == null || order.sourceSystem.isBlank() ? "MES" : order.sourceSystem;
        return database(() -> dao.insertOrder(order));
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
