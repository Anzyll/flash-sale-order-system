package com.flashsale.ordersystem.order;

import com.flashsale.ordersystem.order.adapter.kafka.producer.OrderEventPublisherAdapter;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import com.flashsale.ordersystem.order.port.ProducerRetryQueuePort;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Testcontainers
@SpringBootTest(
        classes = {
                OrderEventPublisherAdapter.class,
                KafkaAutoConfiguration.class
        },
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration"
        }
)
public class OrderPublisherIntegrationTest {
    @Container
    static KafkaContainer kafkaContainer =
            new KafkaContainer(
                    DockerImageName.parse(
                            "apache/kafka-native:3.8.0"
                    )
            );
    @DynamicPropertySource
    static void configureKafka(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.kafka.bootstrap-servers",
                kafkaContainer::getBootstrapServers
        );
    }
    @Autowired
    private OrderEventPublisherAdapter publisherAdapter;
    @MockBean
    private ProducerRetryQueuePort producerRetryQueuePort;

    @Test
    void shouldPublishOrderPlacedEvent(){
        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps(
                        kafkaContainer.getBootstrapServers(),
                        "test-group",
                        "true"

                );

        ConsumerFactory<String, OrderPlacedEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        consumerProps,
                        new StringDeserializer(),
                        new JsonDeserializer<>(
                                OrderPlacedEvent.class,
                                false
                        )
                );
        Consumer<String, OrderPlacedEvent> consumer =
                consumerFactory.createConsumer();

        consumer.subscribe(
                List.of("order.placed")
        );

        OrderPlacedEvent event =
                new OrderPlacedEvent(
                        UUID.randomUUID().toString(),
                        "user-1",
                        1L,
                        10L,
                        Instant.now()
                );
        publisherAdapter.publish(event);
        ConsumerRecord<String, OrderPlacedEvent> record =
                KafkaTestUtils.getSingleRecord(
                        consumer,
                        "order.placed"
                );
        assertNotNull(record);
        assertEquals(event.getProductId(),record.value().getProductId());
        consumer.close();

    }
}
