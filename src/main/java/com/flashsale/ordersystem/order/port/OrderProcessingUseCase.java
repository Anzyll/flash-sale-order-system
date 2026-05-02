package com.flashsale.ordersystem.order.port;

import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;

public interface OrderProcessingUseCase {
    void processOrder(OrderPlacedEvent event);
}
