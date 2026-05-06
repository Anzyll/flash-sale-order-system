package com.flashsale.ordersystem.shared.exception;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        String correlationId
) {}