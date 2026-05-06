package com.flashsale.ordersystem.sale.presentation.dto;


import java.time.Instant;

public record SaleResponse(
        Long id,
        String title,
        Instant startTime,
        Instant endTime

) {
}
