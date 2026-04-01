package com.flashsale.ordersystem.order.application.port;

public interface StockService {
    boolean decrement(Long saleId,Long productId,int quantity);
}
