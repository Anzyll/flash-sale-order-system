package com.flashsale.ordersystem.shared.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    PRODUCT_NOT_FOUND(404, "PRODUCT_NOT_FOUND", "Product not found"),
    SALE_NOT_FOUND(404, "SALE_NOT_FOUND", "Sale not found"),
    PRODUCT_ALREADY_IN_SALE(409, "PRODUCT_ALREADY_IN_SALE", "Product already in sale"),
    SALE_EXPIRED(400, "SALE_EXPIRED", "Sale expired"),
    SALE_NOT_ACTIVE(400, "SALE_NOT_ACTIVE", "Sale not active"),
    INVALID_PRICE(400, "INVALID_PRICE", "Invalid price"),
    INVALID_STOCK(400, "INVALID_STOCK", "Invalid stock"),
    INSUFFICIENT_STOCK(409, "INSUFFICIENT_STOCK", "Insufficient stock"),
    ALREADY_PURCHASED(409, "ALREADY_PURCHASED", "Product already purchased"),
    INVALID_QUANTITY(400, "INVALID_QUANTITY", "Invalid quantity"),
    STOCK_NOT_INITIALIZED(500, "STOCK_NOT_INITIALIZED", "Stock not initialized in Redis"),
    DUPLICATE_REQUEST(409, "DUPLICATE_REQUEST", "Duplicate request"),
    USER_NOT_FOUND(404, "USER_NOT_FOUND", "User not found"),
    EMAIL_ALREADY_EXISTS(409, "EMAIL_ALREADY_EXISTS", "Email already exists"),
    REDIS_EXECUTION_FAILED(500, "REDIS_EXECUTION_FAILED", "Redis execution failed"),
    KAFKA_UNAVAILABLE(500, "KAFKA_UNAVAILABLE", "Kafka broker unavailable");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}