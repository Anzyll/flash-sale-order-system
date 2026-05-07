package com.flashsale.ordersystem.shared.port;

public interface StockReservationPort {
    boolean tryPurchase(String userId,Long saleId, Long productId, int quantity);
    void revertPurchase(String userId, Long saleId, Long productId, int quantity);
    void recoverStock(Long saleId,Long productId, int remainingStock,long ttlSeconds) ;
    void waitForStock(Long saleId, Long productId);
    boolean isSaleActive(Long saleId);
    void confirmPurchase(String userId, Long saleId, Long productId);
    int getAvailableStock(Long saleId, Long productId);
}
