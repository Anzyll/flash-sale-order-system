package com.flashsale.ordersystem.order.adapter.kafka.producer;

import com.flashsale.ordersystem.shared.exception.ErrorCode;
import com.flashsale.ordersystem.shared.exception.InfrastructureException;
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
            log.info("Order event published. eventId={}, productId={}",
                    event.getEventId(), event.getProductId());
        }
        catch (Exception e){
            log.error("Kafka publish failed. eventId={}, productId={}",
                    event.getEventId(), event.getProductId(), e);
            throw new InfrastructureException(ErrorCode.KAFKA_UNAVAILABLE);
        }

    }
}
