package com.flashsale.ordersystem.sale.presentation.dto;

import com.flashsale.ordersystem.product.domain.Product;
import com.flashsale.ordersystem.sale.domain.model.Sale;

import java.math.BigDecimal;

public record SaleData(
        Sale sale,
        Product product,
        BigDecimal price
) {}
