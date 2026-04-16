package com.flashsale.ordersystem.order.infrastructure.kafka.producer;

import com.flashsale.ordersystem.order.application.port.OrderEventPublisher;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderKafkaProducer implements OrderEventPublisher {
    private final KafkaTemplate<String,Object> kafkaTemplate;
    @Override
    public void publish(OrderPlacedEvent event) {
        kafkaTemplate.send(
                "order.placed",
                event.getProductId().toString(),
                event
        );
        System.out.println("kafka event sent: "+event.getProductId());
    }
}
