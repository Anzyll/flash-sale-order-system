package com.flashsale.ordersystem.shared.port;

public interface SaleStockPort {
    void initializeSaleStock(Long saleId, Long productId, int stock, long ttlSeconds);
    void activateSale(Long saleId, long ttlSeconds);
    void deactivateSale(Long saleId, Long productId);
    void deactivateSale(Long saleId);
}
