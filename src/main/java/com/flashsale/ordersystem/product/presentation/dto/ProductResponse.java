package com.flashsale.ordersystem.product.presentation.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price
) {
}
