package com.flashsale.ordersystem.order.presentation.dto;

import java.math.BigDecimal;

public record PurchaseResponse(
        Long orderId,
        String status,
        BigDecimal totalAmount
) {}