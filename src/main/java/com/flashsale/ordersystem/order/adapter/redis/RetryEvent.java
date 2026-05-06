package com.flashsale.ordersystem.order.adapter.redis;


import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@AllArgsConstructor
@Getter
@NoArgsConstructor
public class RetryEvent {
    private OrderPlacedEvent event;
    private int retryCount;

    public int incrementAndGet() {
       return this.retryCount++;
    }
}
