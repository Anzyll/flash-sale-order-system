package com.flashsale.ordersystem.order.adapter.rest.dto;

import jakarta.validation.constraints.NotNull;

public record PurchaseRequest(
        @NotNull
        Long productId
) {}
