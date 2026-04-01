package com.flashsale.ordersystem.common.exception;

public class InfrastructureException extends RuntimeException {
    public InfrastructureException(ErrorCode message) {
        super(message);
    }
}
