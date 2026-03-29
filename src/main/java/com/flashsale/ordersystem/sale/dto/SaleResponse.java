package com.flashsale.ordersystem.sale.dto;

import java.time.LocalDateTime;

public record SaleResponse(
        Long id,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime

) {
}
