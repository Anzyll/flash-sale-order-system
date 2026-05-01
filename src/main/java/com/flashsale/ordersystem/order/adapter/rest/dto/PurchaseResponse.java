package com.flashsale.ordersystem.order.adapter.rest.dto;

import java.math.BigDecimal;

public record PurchaseResponse(
        Long orderId,
        String status,
        BigDecimal totalAmount
) {}