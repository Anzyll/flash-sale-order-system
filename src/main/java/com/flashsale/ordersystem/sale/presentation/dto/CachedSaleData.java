package com.flashsale.ordersystem.sale.presentation.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record CachedSaleData(
        Long saleId,
        String saleStatus,
        Instant endTime,
        Long productId,
        String productName,
        BigDecimal salePrice
) implements Serializable {
}