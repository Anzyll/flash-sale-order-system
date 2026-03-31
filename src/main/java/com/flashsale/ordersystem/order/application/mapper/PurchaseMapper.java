package com.flashsale.ordersystem.order.application.mapper;

import com.flashsale.ordersystem.order.domain.model.Order;
import com.flashsale.ordersystem.order.presentation.dto.PurchaseResponse;

public class PurchaseMapper {
    public static PurchaseResponse toResponse(Order order) {
        return new PurchaseResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotalAmount()
        );
    }
}
