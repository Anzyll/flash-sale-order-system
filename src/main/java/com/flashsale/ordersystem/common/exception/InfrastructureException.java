package com.flashsale.ordersystem.common.exception;

public class InfrastructureException extends RuntimeException {
    private final ErrorCode errorCode;
    public InfrastructureException(ErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode=errorCode;
    }
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
