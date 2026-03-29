package com.flashsale.ordersystem.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank
        String name,
        @NotBlank
        String description,
        @NotNull
        @Positive
        BigDecimal price
) {
}
