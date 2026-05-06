package com.flashsale.ordersystem.sale.presentation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateSaleRequest(
        @NotBlank
        String title,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @NotNull
        Instant startTime,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @NotNull
        Instant endTime
) {
}
