package com.example.messystem.planning.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MesCustomerOrder {
    public Long orderId;
    public String orderNo;
    public String customerName;
    public Long productId;
    public String productCode;
    public String productModel;
    public Integer orderQty;
    public String unit;
    public LocalDate deliveryDate;
    public Integer priorityLevel;
    public String orderStatus;
    public String sourceSystem;
    public String remark;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
