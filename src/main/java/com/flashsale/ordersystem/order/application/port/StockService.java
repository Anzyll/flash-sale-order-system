package com.flashsale.ordersystem.order.application.port;

public interface StockService {
    boolean processPurchase(String userId,Long saleId, Long productId, int quantity,long ttl);
    void revertPurchase(String userId, Long saleId, Long productId, int quantity);
}
