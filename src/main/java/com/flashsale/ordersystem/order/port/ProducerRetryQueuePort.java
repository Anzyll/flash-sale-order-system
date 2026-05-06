package com.flashsale.ordersystem.order.port;

import com.flashsale.ordersystem.order.adapter.redis.RetryEvent;

public interface ProducerRetryQueuePort {
    void push(RetryEvent event);
    RetryEvent pop();
    void pushToDLQ(RetryEvent event);
    RetryEvent popFromDlq();
}