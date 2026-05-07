package com.flashsale.ordersystem.sale.presentation.dto;

public record ProductAvailabilityResponse(
        Long saleId,
        Long productId,
        int availableStock,
        boolean soldOut,
        String saleStatus
) {
}