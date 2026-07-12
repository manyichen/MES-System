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

public class CustomerOrderService {
    private final PlanningDao dao = new PlanningDao();

    public List<MesCustomerOrder> listOrders() {
        return database(dao::listOrders);
    }

    public MesCustomerOrder getOrder(long orderId) {
        return database(() -> dao.findOrder(orderId))
                .orElseThrow(() -> new NotFoundException("order not found"));
    }

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
