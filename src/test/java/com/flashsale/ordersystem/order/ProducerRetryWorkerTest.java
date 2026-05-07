package com.flashsale.ordersystem.order;

import com.flashsale.ordersystem.order.adapter.redis.RetryEvent;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import com.flashsale.ordersystem.order.port.ProducerRetryQueuePort;
import com.flashsale.ordersystem.order.scheduler.ProducerRetryWorker;
import com.flashsale.ordersystem.shared.exception.ErrorCode;
import com.flashsale.ordersystem.shared.exception.InfrastructureException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ProducerRetryWorkerTest {

    @Mock
    private ProducerRetryQueuePort retryQueue;

    @Mock
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Mock
    private CompletableFuture<SendResult<String, OrderPlacedEvent>> future;

    @InjectMocks
    private ProducerRetryWorker producerRetryWorker;

    private final String userId = "user-1";
    private final Long saleId = 1L;
    private final Long productId = 100L;

    @Test
    void shouldRepublishEventSuccessfully() throws Exception {

        OrderPlacedEvent event = new OrderPlacedEvent(
                "event-1",
                userId,
                saleId,
                productId,
                Instant.now()
        );

        RetryEvent retryEvent = new RetryEvent(event,0);

        when(retryQueue.pop())
                .thenReturn(retryEvent)
                .thenReturn(null);

        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(future);

        when(future.get()).thenReturn(null);

        producerRetryWorker.processRetryQueue();

        verify(kafkaTemplate)
                .send(any(ProducerRecord.class));

        verify(retryQueue, never())
                .push(any());

        verify(retryQueue, never())
                .pushToDLQ(any());
    }

    @Test
    void shouldRequeueEventWhenPublishFails() throws Exception {

        OrderPlacedEvent event = new OrderPlacedEvent(
                "event-2",
                userId,
                saleId,
                productId,
                Instant.now()
        );

        RetryEvent retryEvent = new RetryEvent(event,0);

        when(retryQueue.pop())
                .thenReturn(retryEvent)
                .thenReturn(null);

        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(future);

        when(future.get())
                .thenThrow(new InfrastructureException(ErrorCode.KAFKA_UNAVAILABLE));

        producerRetryWorker.processRetryQueue();

        verify(retryQueue)
                .push(retryEvent);

        verify(retryQueue, never())
                .pushToDLQ(any());

        assertEquals(1, retryEvent.getRetryCount());
    }

    @Test
    void shouldMoveToDLQWhenMaxRetriesExceeded() throws Exception {

        OrderPlacedEvent event = new OrderPlacedEvent(
                "event-3",
                userId,
                saleId,
                productId,
                Instant.now()
        );

        RetryEvent retryEvent = new RetryEvent(event,0);

        for (int i = 0; i < 5; i++) {
            retryEvent.incrementAndGet();
        }

        when(retryQueue.pop())
                .thenReturn(retryEvent)
                .thenReturn(null);

        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(future);

        when(future.get())
                .thenThrow(new RuntimeException("Kafka failure"));

        producerRetryWorker.processRetryQueue();

        verify(retryQueue)
                .pushToDLQ(retryEvent);

        verify(retryQueue, never())
                .push(any());
    }

}