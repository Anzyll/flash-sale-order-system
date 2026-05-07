package com.flashsale.ordersystem.order.scheduler;

import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import com.flashsale.ordersystem.order.adapter.redis.RetryEvent;
import com.flashsale.ordersystem.order.port.ProducerRetryQueuePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProducerRetryWorker {

    private final ProducerRetryQueuePort retryQueue;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    private static final int MAX_RETRIES = 5;
    private static final int BATCH_SIZE = 50;

    @Scheduled(fixedDelay = 2000)
    public void processRetryQueue(){
        for(int i = 0; i <BATCH_SIZE; i++){
            RetryEvent retryEvent = retryQueue.pop();
            if (retryEvent == null) {
                return;
            }

            OrderPlacedEvent event = retryEvent.getEvent();

            try {
                kafkaTemplate.send(buildRecord(event)).get();
                log.info("Retry success. eventId={}",
                        event.getEventId());

            } catch (Exception e) {
                int attempts = retryEvent.incrementAndGet();

                if (attempts > MAX_RETRIES) {
                    log.error("Immediate failure → DLQ. eventId={}",
                            event.getEventId(), e);
                    retryQueue.pushToDLQ(retryEvent);

                } else {
                    log.warn("Immediate failure → requeue. eventId={}, attempt={}",
                            event.getEventId(), attempts);
                    retryQueue.push(retryEvent);
                }
            }
        }
    }
    private ProducerRecord<String, OrderPlacedEvent> buildRecord(OrderPlacedEvent event) {
        return new ProducerRecord<>(
                "order.placed",
                event.getProductId().toString(),
                event
        );
    }
}
