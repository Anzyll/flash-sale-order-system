package com.flashsale.ordersystem.order;

import com.flashsale.ordersystem.order.adapter.persistence.OrderItemRepository;
import com.flashsale.ordersystem.order.adapter.persistence.OrderRepository;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import com.flashsale.ordersystem.product.domain.Product;
import com.flashsale.ordersystem.product.repository.ProductRepository;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.sale.domain.model.SaleItem;
import com.flashsale.ordersystem.sale.repository.SaleItemRepository;
import com.flashsale.ordersystem.sale.repository.SaleRepository;
import com.flashsale.ordersystem.user.domain.User;
import com.flashsale.ordersystem.user.repository.UserRepository;
import com.flashsale.ordersystem.user.service.UserService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
class OrderConsumerIntegrationTest {

    @Container
    static KafkaContainer kafkaContainer =
            new KafkaContainer(
                    DockerImageName.parse("apache/kafka-native:3.8.0")
            );
    @Container
    static PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("test-db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry
    ) {

        registry.add(
                "spring.kafka.bootstrap-servers",
                kafkaContainer::getBootstrapServers
        );

        registry.add(
                "spring.datasource.url",
                postgresContainer::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgresContainer::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgresContainer::getPassword
        );

        registry.add(
                "keycloak.server-url",
                () -> "http://localhost"
        );

        registry.add(
                "keycloak.admin-realm",
                () -> "master"
        );

        registry.add(
                "keycloak.app-realm",
                () -> "flash-sale"
        );

        registry.add(
                "keycloak.client-id",
                () -> "admin-cli"
        );

        registry.add(
                "keycloak.username",
                () -> "admin"
        );

        registry.add(
                "keycloak.password",
                () -> "admin"
        );
    }

    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SaleItemRepository saleItemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @MockBean
    private UserService userService;

    @MockBean
    private Keycloak keycloak;

    @MockBean
    private JwtDecoder jwtDecoder;

    private User savedUser;
    private Product savedProduct;
    private Sale savedSale;

    @BeforeEach
    void setupData() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();

        saleItemRepository.deleteAll();
        saleRepository.deleteAll();

        productRepository.deleteAll();
        userRepository.deleteAll();
        User user = new User();
        user.setKeycloakId("user-1");
        user.setEmail("test@example.com");

        savedUser = userRepository.save(user);

        Product product = new Product();
        product.setName("iPhone 15");
        product.setDescription("Apple Phone");
        product.setPrice(BigDecimal.valueOf(99999));

        savedProduct = productRepository.save(product);

        Sale sale = new Sale();
        sale.setTitle("Flash Sale");
        sale.setStartTime(
                Instant.now().minus(Duration.ofHours(1))
        );
        sale.setEndTime(
                Instant.now().plus(Duration.ofHours(1))
        );

        savedSale = saleRepository.save(sale);

        SaleItem saleItem = new SaleItem();
        saleItem.setSale(savedSale);
        saleItem.setProduct(savedProduct);
        saleItem.setSalePrice(BigDecimal.valueOf(79999));
        saleItem.setTotalStock(100);
        saleItem.setAvailableStock(100);

        saleItemRepository.save(saleItem);
    }

    @Test
    void shouldConsumeAndProcessOrder() {

        when(userService.getUserOrThrow("user-1"))
                .thenReturn(savedUser);

        OrderPlacedEvent event =
                new OrderPlacedEvent(
                        UUID.randomUUID().toString(),
                        "user-1",
                        savedSale.getId(),
                        savedProduct.getId(),
                        Instant.now()
                );

        ProducerRecord<String, OrderPlacedEvent> record =
                new ProducerRecord<>(
                        "order.placed",
                        event.getProductId().toString(),
                        event
                );

        record.headers().add(
                "correlationId",
                UUID.randomUUID().toString().getBytes()
        );

        kafkaTemplate.send(record);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        assertTrue(orderRepository.count() > 0)
                );
    }

    @Test
    void shouldIgnoreDuplicateEvent(){
        when(userService.getUserOrThrow("user-1"))
                .thenReturn(savedUser);

        OrderPlacedEvent event =
                new OrderPlacedEvent(
                        UUID.randomUUID().toString(),
                        "user-1",
                        savedSale.getId(),
                        savedProduct.getId(),
                        Instant.now()
                );

        ProducerRecord<String, OrderPlacedEvent> firstRecord =
                new ProducerRecord<>(
                        "order.placed",
                        event.getProductId().toString(),
                        event
                );

        firstRecord.headers().add(
                "correlationId",
                UUID.randomUUID().toString().getBytes()
        );

        ProducerRecord<String, OrderPlacedEvent> duplicateRecord =
                new ProducerRecord<>(
                        "order.placed",
                        event.getProductId().toString(),
                        event
                );

        duplicateRecord.headers().add(
                "correlationId",
                UUID.randomUUID().toString().getBytes()
        );
        kafkaTemplate.send(firstRecord);
        kafkaTemplate.send(duplicateRecord);
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    long orderCount = orderRepository.count();
                    assertEquals(1, orderCount);
                });

    }

    @Test
    void shouldMoveEventToDLQ() {
        when(userService.getUserOrThrow("user-1"))
                .thenThrow(new RuntimeException("Simulated failure"));

        OrderPlacedEvent event =
                new OrderPlacedEvent(
                        UUID.randomUUID().toString(),
                        "user-1",
                        savedSale.getId(),
                        savedProduct.getId(),
                        Instant.now()
                );

        ProducerRecord<String, OrderPlacedEvent> record =
                new ProducerRecord<>(
                        "order.placed",
                        event.getProductId().toString(),
                        event
                );
        record.headers().add(
                "correlationId",
                UUID.randomUUID().toString().getBytes()
        );

        kafkaTemplate.send(record);

        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps(
                        kafkaContainer.getBootstrapServers(),
                        "dlq-test-group",
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
                List.of("order.placed.DLQ")
        );

        ConsumerRecord<String, OrderPlacedEvent> dlqRecord =
                KafkaTestUtils.getSingleRecord(
                        consumer,
                        "order.placed.DLQ",
                        Duration.ofSeconds(20)
                );

        assertNotNull(dlqRecord);

        assertEquals(
                event.getEventId(),
                dlqRecord.value().getEventId()
        );

        consumer.close();
    }

}