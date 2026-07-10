package com.example.messystem.planning.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.master.entity.MesProduct;
import com.example.messystem.planning.entity.MesCustomerOrder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CustomerOrderService {
    public List<MesCustomerOrder> listOrders() {
        return new ArrayList<>(PlanningStore.orders.values());
    }

    public MesCustomerOrder getOrder(long orderId) {
        MesCustomerOrder order = PlanningStore.orders.get(orderId);
        if (order == null) {
            throw new NotFoundException("order not found");
        }
        return order;
    }

    public MesCustomerOrder createOrder(MesCustomerOrder order) {
        requireText(order.customerName, "customerName is required");
        requireId(order.productId, "productId is required");
        MesProduct product = PlanningStore.products.get(order.productId);
        if (product == null) {
            throw new BadRequestException("product not found");
        }
        order.orderId = PlanningStore.nextId();
        if (order.orderNo == null || order.orderNo.isBlank()) {
            order.orderNo = IdGenerator.nextCode("ORD");
        }
        order.productCode = product.productCode;
        order.productModel = product.productModel;
        order.orderQty = order.orderQty == null ? 0 : order.orderQty;
        order.unit = order.unit == null || order.unit.isBlank() ? product.unit : order.unit;
        order.priorityLevel = order.priorityLevel == null ? 3 : order.priorityLevel;
        order.orderStatus = order.orderStatus == null || order.orderStatus.isBlank() ? "CREATED" : order.orderStatus;
        order.sourceSystem = order.sourceSystem == null || order.sourceSystem.isBlank() ? "MES" : order.sourceSystem;
        order.createdAt = LocalDateTime.now();
        order.updatedAt = order.createdAt;
        PlanningStore.orders.put(order.orderId, order);
        return order;
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
