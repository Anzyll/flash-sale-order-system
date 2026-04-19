package com.flashsale.ordersystem.order.infrastructure.kafka.producer;

import com.flashsale.ordersystem.common.exception.ErrorCode;
import com.flashsale.ordersystem.common.exception.InfrastructureException;
import com.flashsale.ordersystem.order.application.port.OrderEventPublisher;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderKafkaProducer implements OrderEventPublisher {
    private final KafkaTemplate<String,OrderPlacedEvent> kafkaTemplate;
    @Override
    public void publish(OrderPlacedEvent event,String correlationId) {
        ProducerRecord<String,OrderPlacedEvent> record = new ProducerRecord<>(
                "order.placed",
                event.getProductId().toString(),
                event
        );
        record.headers().add(
                "correlationId",
                correlationId.getBytes()
        );
        try{
            kafkaTemplate.send(record).get();
            log.info("Kafka event sent. correlationId={}, productId={}",
                    correlationId, event.getProductId());
        }
        catch (Exception e){
            log.error("Kafka send failed. correlationId={}, productId={}",
                    correlationId, event.getProductId(), e);
            throw new InfrastructureException(ErrorCode.KAFKA_UNAVAILABLE);
        }

    }
}
