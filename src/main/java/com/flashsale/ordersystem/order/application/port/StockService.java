package com.flashsale.ordersystem.order.application.port;

public interface StockService {
    boolean processPurchase(String userId,Long saleId, Long productId, int quantity);
    void revertPurchase(String userId, Long saleId, Long productId, int quantity);
    void initializeSaleStock(Long saleId, Long productId, int stock, long ttlSeconds);
    void recoverStock(Long saleId,Long productId, int remainingStock,long ttlSeconds) ;
    void activateSale(Long saleId, long ttlSeconds);
    void deactivateSale(Long saleId, Long productId);
    void deactivateSale(Long saleId);
    boolean isSaleActive(Long saleId);
    void waitForStock(Long saleId, Long productId);
}
