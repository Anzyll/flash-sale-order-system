package com.flashsale.ordersystem.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    PRODUCT_NOT_FOUND(404,"product not found"),
    SALE_NOT_FOUND(404,"sale not found"),
    PRODUCT_ALREADY_IN_SALE(409,"product already in sale"),
    SALE_EXPIRED(400,"sale expired"),
    INVALID_PRICE(400,"invalid price"),
    INVALID_STOCK(400,"invalid stock"),
    INSUFFICIENT_STOCK(409,"insufficient stock"),
    ALREADY_PURCHASED(409,"product is already purchased"),
    SALE_NOT_STARTED(400,"sale not started");

    private final int status;
    private final String message;

    ErrorCode( int status, String message) {
        this.status = status;
        this.message = message;
    }

}
