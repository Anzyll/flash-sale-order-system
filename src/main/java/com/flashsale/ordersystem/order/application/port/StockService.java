package com.flashsale.ordersystem.order.application.port;

public interface StockService {
    boolean processPurchase(Long userId,Long saleId, Long productId, int quantity,long ttl);
    void revertPurchase(Long userId, Long saleId, Long productId, int quantity);
}
