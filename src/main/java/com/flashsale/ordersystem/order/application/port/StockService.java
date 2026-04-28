package com.flashsale.ordersystem.order.application.port;

public interface StockService {
    boolean processPurchase(String userId,Long saleId, Long productId, int quantity);
    void revertPurchase(String userId, Long saleId, Long productId, int quantity);
    void recoverStock(Long saleId,Long productId);
}
