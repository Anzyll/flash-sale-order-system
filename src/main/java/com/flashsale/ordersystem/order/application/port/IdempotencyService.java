package com.flashsale.ordersystem.order.application.port;

public interface IdempotencyService {
    boolean tryAcquire(Long userId,Long saleId,Long ProductId);
    Boolean release(Long userId, Long saleId, Long productId);
}
