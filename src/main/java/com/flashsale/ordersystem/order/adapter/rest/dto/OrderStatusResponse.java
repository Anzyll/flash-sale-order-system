package com.flashsale.ordersystem.order.adapter.rest.dto;

import java.time.Instant;

public record OrderStatusResponse(
        Long orderId,
        String status,
        Long saleId,
        Long productId,
        Instant createdAt
) {
}