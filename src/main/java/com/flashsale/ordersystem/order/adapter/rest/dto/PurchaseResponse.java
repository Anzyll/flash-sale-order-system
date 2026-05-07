package com.flashsale.ordersystem.order.adapter.rest.dto;

import com.flashsale.ordersystem.order.domain.enums.OrderStatus;

public record PurchaseResponse(
        String eventId,
        String status,
        String message
) {
}
