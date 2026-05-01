package com.flashsale.ordersystem.order.adapter.kafka.consumer;

import com.flashsale.ordersystem.order.port.OrderProcessingUseCase;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPlacedEventConsumer {
    private final OrderProcessingUseCase orderProcessingUseCase;
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000,multiplier = 2),
            dltTopicSuffix = ".DLQ"
    )
    @KafkaListener(
            topics = "order.placed",
            groupId = "order-processing-group",
            concurrency = "3"
    )
    public void consume(OrderPlacedEvent event, @Header("correlationId") String correlationId) {
        log.info("Processing order. eventId={}, correlationId={}, productId={}",
                event.getEventId(),
                correlationId,
                event.getProductId());
        orderProcessingUseCase.processOrder(event,correlationId);
    }
}