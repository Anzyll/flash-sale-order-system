package com.flashsale.ordersystem.order.adapter.kafka.producer;

import com.flashsale.ordersystem.order.adapter.redis.RetryEvent;
import com.flashsale.ordersystem.order.port.ProducerRetryQueuePort;
import com.flashsale.ordersystem.order.port.OrderEventPublisher;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventPublisherAdapter implements OrderEventPublisher {
    private final KafkaTemplate<String,OrderPlacedEvent> kafkaTemplate;
    private final ProducerRetryQueuePort retryQueue;
    @Override
    public void publish(OrderPlacedEvent event) {
        String correlationId = MDC.get("correlationId");
        ProducerRecord<String,OrderPlacedEvent> record = new ProducerRecord<>(
                "order.placed",
                event.getProductId().toString(),
                event
        );
        if (correlationId != null) {
            record.headers().add("correlationId", correlationId.getBytes());
        }
        try{
            log.info("Publishing order event. eventId={}, productId={}",
                    event.getEventId(), event.getProductId());
            kafkaTemplate.send(record).get();
            log.info("Kafka success. eventId={}", event.getEventId());
    }
        catch (Exception e){
            log.error("Immediate Kafka failure → retry queue. eventId={}", event.getEventId(), e);
            retryQueue.push(new RetryEvent(event,0));
        }

    }
}
