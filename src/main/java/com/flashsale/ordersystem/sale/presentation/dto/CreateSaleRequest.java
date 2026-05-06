package com.flashsale.ordersystem.sale.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateSaleRequest(
        @NotBlank
        String title,
        @NotNull
        Instant startTime,
        @NotNull
        Instant endTime
) {
}
