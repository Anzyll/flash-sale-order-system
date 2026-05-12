package com.flashsale.ordersystem.shared.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    private final Counter purchaseSuccessCounter;
    private final Counter purchaseFailureCounter;
    private final Counter outOfStockCounter;
    private final Counter kafkaPublishFailureCounter;
    private final Counter consumerProcessingFailureCounter;
    private final Counter stockRevertCounter;
    private final Timer purchaseProcessingDuration;
    private final Timer consumerProcessingTimer;
    private final Counter consumerProcessingSuccessCounter;

    public MetricsService(MeterRegistry meterRegistry) {
        this.purchaseSuccessCounter = Counter
                .builder("purchase_success")
                .description("total successful purchase")
                .register(meterRegistry);
        this.purchaseFailureCounter =
                Counter.builder("purchase_failure")
                        .description("Total failed purchases")
                        .register(meterRegistry);

        this.outOfStockCounter =
                Counter.builder("out_of_stock")
                        .description("Total out of stock events")
                        .register(meterRegistry);

        this.kafkaPublishFailureCounter =
                Counter.builder("kafka_publish_failure")
                        .description("Kafka publish failures")
                        .register(meterRegistry);

        this.consumerProcessingFailureCounter =
                Counter.builder("consumer_processing_failure")
                        .description("Consumer processing failures")
                        .register(meterRegistry);

        this.stockRevertCounter =
                Counter.builder("stock_revert")
                        .description("Stock revert operations")
                        .register(meterRegistry);

        this.purchaseProcessingDuration =
                Timer.builder("purchase_processing")
                        .description("purchase processing latency")
                        .register(meterRegistry);

        this.consumerProcessingTimer = Timer.builder("consumer_processing_seconds")
                .description("Kafka consumer processing latency")
                .register(meterRegistry);

        this.consumerProcessingSuccessCounter =
                Counter.builder("consumer_processing_success")
                        .description("consumer processing success")
                        .register(meterRegistry);


    }


    public void incrementPurchaseSuccess(){
        System.out.println("SUCCESS METRIC INCREMENTED");
        purchaseSuccessCounter.increment();
    }

    public void incrementPurchaseFailure() {
        purchaseFailureCounter.increment();
    }

    public void incrementOutOfStock() {
        outOfStockCounter.increment();
    }

    public void incrementKafkaPublishFailure() {
        kafkaPublishFailureCounter.increment();
    }

    public void incrementConsumerProcessingFailure() {
        consumerProcessingFailureCounter.increment();
    }

    public void incrementConsumerProcessingSuccess() {
        consumerProcessingSuccessCounter.increment();
    }


    public void incrementStockRevert() {
        stockRevertCounter.increment();
    }

    public Timer getPurchaseProcessingTime(){
        return purchaseProcessingDuration;
    }

    public Timer getConsumerProcessingTime(){
        return consumerProcessingTimer;
    }

}
