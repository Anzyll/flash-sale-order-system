package com.flashsale.ordersystem.order.port;

import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;

public interface OrderEventPublisher {
    void publish(OrderPlacedEvent orderPlacedEvent);
}
