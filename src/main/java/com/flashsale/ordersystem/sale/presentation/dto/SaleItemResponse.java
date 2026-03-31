package com.flashsale.ordersystem.sale.presentation.dto;

import java.math.BigDecimal;

public record SaleItemResponse(
        Long id,
        Long saleId,
        Long productId,
        BigDecimal salePrice,
        Integer availableStock
) {
}
