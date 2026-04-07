package com.flashsale.ordersystem.order.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseRequest(
        @NotNull
        Long productId,
        Long userId
) {}
