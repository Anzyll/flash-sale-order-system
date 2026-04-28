package com.flashsale.ordersystem.common.exception;

public class InfrastructureException extends RuntimeException {
    private final ErrorCode errorCode = null;
    public InfrastructureException(ErrorCode message) {
        super(String.valueOf(message));
    }
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
