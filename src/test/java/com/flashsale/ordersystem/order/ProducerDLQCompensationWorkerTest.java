package com.flashsale.ordersystem.order;

import com.flashsale.ordersystem.order.adapter.persistence.ProcessedEventRepository;
import com.flashsale.ordersystem.order.adapter.redis.RetryEvent;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import com.flashsale.ordersystem.order.domain.model.ProcessedEvent;
import com.flashsale.ordersystem.order.port.ProducerRetryQueuePort;
import com.flashsale.ordersystem.order.scheduler.ProducerDLQCompensationWorker;
import com.flashsale.ordersystem.shared.port.StockReservationPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProducerDLQCompensationWorkerTest {

    @Mock
    private ProducerRetryQueuePort retryQueue;

    @Mock
    private StockReservationPort stockReservationPort;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private ProducerDLQCompensationWorker worker;

    private final String userId = "user-1";
    private final Long saleId = 1L;
    private final Long productId = 100L;

    @Test
    void shouldRevertStockSuccessfully() {
        OrderPlacedEvent event = new OrderPlacedEvent(
                "event-1",
                userId,
                saleId,
                productId,
                Instant.now()
        );
        RetryEvent retryEvent =
                new RetryEvent(event, 0);

        when(retryQueue.popFromDlq())
                .thenReturn(retryEvent)
                .thenReturn(null);

        when(processedEventRepository.existsById(any()))
                .thenReturn(false);

        worker.processDLQ();

        verify(stockReservationPort)
                .revertPurchase(
                        userId,
                        saleId,
                        productId,
                        1
                );

        verify(processedEventRepository)
                .save(any(ProcessedEvent.class));
    }

    @Test
    void shouldIgnoreDuplicateCompensation() {
        OrderPlacedEvent event = new OrderPlacedEvent(
                "event-2",
                userId,
                saleId,
                productId,
                Instant.now()
        );

        RetryEvent retryEvent =
                new RetryEvent(event, 0);

        when(retryQueue.popFromDlq())
                .thenReturn(retryEvent)
                .thenReturn(null);

        when(processedEventRepository.existsById(any()))
                .thenReturn(true);

        worker.processDLQ();

        verify(stockReservationPort, never())
                .revertPurchase(any(), any(), any(), anyInt());

        verify(processedEventRepository, never())
                .save(any());
    }

    @Test
    void shouldHandleCompensationFailure() {

        OrderPlacedEvent event = new OrderPlacedEvent(
                "event-3",
                userId,
                saleId,
                productId,
                Instant.now()
        );

        RetryEvent retryEvent =
                new RetryEvent(event, 0);

        when(retryQueue.popFromDlq())
                .thenReturn(retryEvent)
                .thenReturn(null);

        when(processedEventRepository.existsById(any()))
                .thenReturn(false);

        doThrow(new RuntimeException("Redis failure"))
                .when(stockReservationPort)
                .revertPurchase(any(), any(), any(), anyInt());

        worker.processDLQ();

        verify(stockReservationPort)
                .revertPurchase(
                        userId,
                        saleId,
                        productId,
                        1
                );

        verify(processedEventRepository, never())
                .save(any());
    }
}