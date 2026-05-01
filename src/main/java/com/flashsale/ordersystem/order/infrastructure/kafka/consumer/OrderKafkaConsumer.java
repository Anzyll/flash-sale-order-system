package com.flashsale.ordersystem.order.infrastructure.kafka.consumer;

import com.flashsale.ordersystem.order.application.service.OrderService;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import com.flashsale.ordersystem.order.domain.model.ProcessedEvent;
import com.flashsale.ordersystem.order.infrastructure.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderKafkaConsumer {
    private final OrderService orderService;
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
        orderService.processOrder(event,correlationId);
    }
}