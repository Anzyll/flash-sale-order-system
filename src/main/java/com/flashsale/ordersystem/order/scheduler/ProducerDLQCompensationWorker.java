package com.flashsale.ordersystem.order.scheduler;

import com.flashsale.ordersystem.order.adapter.persistence.ProcessedEventRepository;
import com.flashsale.ordersystem.order.adapter.redis.RetryEvent;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import com.flashsale.ordersystem.order.domain.model.ProcessedEvent;
import com.flashsale.ordersystem.order.port.ProducerRetryQueuePort;
import com.flashsale.ordersystem.shared.port.StockReservationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProducerDLQCompensationWorker {

    private final ProducerRetryQueuePort retryQueue;
    private final StockReservationPort stockReservationPort;
    private final ProcessedEventRepository processedEventRepository;

    @Scheduled(fixedDelay = 2000)
    public void processDLQ() {

        for (int i = 0; i < 50; i++) {
            RetryEvent retryEvent = retryQueue.popFromDlq();
            if (retryEvent == null) {
                return;
            }
            OrderPlacedEvent event = retryEvent.getEvent();

            String compensationId =
                    "producer-revert-" + event.getEventId();

            try {
                if (processedEventRepository.existsById(compensationId)) {
                    continue;
                }
                stockReservationPort.revertPurchase(
                        event.getUserId(),
                        event.getSaleId(),
                        event.getProductId(),
                        1
                );
                processedEventRepository.save(
                        new ProcessedEvent(compensationId)
                );
                log.warn(
                        "Stock reverted from producer DLQ. eventId={}",
                        event.getEventId()
                );
            } catch (Exception e) {

                log.error(
                        "Failed to revert stock from producer DLQ. eventId={}",
                        event.getEventId(),
                        e
                );
            }
        }
    }
}