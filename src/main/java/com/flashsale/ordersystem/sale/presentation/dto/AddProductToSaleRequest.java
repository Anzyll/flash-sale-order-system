package com.flashsale.ordersystem.sale.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AddProductToSaleRequest(
        Long productId,
        @NotNull @Positive
        BigDecimal salePrice,
        @NotNull @Positive
        Integer totalStock
) {
}
