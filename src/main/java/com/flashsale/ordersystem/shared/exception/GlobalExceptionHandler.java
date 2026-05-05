package com.flashsale.ordersystem.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(
            BusinessException ex,
            HttpServletRequest request) {

        ErrorCode errorCode = ex.getErrorCode();

        log.error("Business exception. errorCode={}, path={}, correlationId={}",
                errorCode.name(),
                request.getRequestURI(),
                MDC.get("correlationId"),
                ex);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ErrorResponse(
                        LocalDateTime.now(),
                        errorCode.getStatus(),
                        errorCode.name(),
                        errorCode.getMessage(),
                        request.getRequestURI(),
                        MDC.get("correlationId")
                ));
    }

    @ExceptionHandler(InfrastructureException.class)
    public ResponseEntity<ErrorResponse> handleInfraException(
            InfrastructureException ex,
            HttpServletRequest request) {

        ErrorCode errorCode = ex.getErrorCode();

        log.error("Infra failure. errorCode={}, path={}, correlationId={}",
                errorCode.name(),
                request.getRequestURI(),
                MDC.get("correlationId"),
                ex);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ErrorResponse(
                        LocalDateTime.now(),
                        errorCode.getStatus(),
                        errorCode.name(),
                        errorCode.getMessage(),
                        request.getRequestURI(),
                        MDC.get("correlationId")
                ));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");

        return ResponseEntity.badRequest().body(
                new ErrorResponse(
                        LocalDateTime.now(),
                        400,
                        "VALIDATION_ERROR",
                        errorMessage,
                        request.getRequestURI(),
                        MDC.get("correlationId")
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception", ex);

        return ResponseEntity.status(500).body(
                new ErrorResponse(
                        LocalDateTime.now(),
                        500,
                        "INTERNAL_SERVER_ERROR",
                        "Something went wrong",
                        request.getRequestURI(),
                        MDC.get("correlationId")
                )
        );
    }

}
