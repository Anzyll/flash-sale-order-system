package com.flashsale.ordersystem.order.e2e;

import com.flashsale.ordersystem.order.adapter.persistence.OrderRepository;
import com.flashsale.ordersystem.order.domain.enums.OrderStatus;
import com.flashsale.ordersystem.order.domain.model.Order;
import com.flashsale.ordersystem.product.domain.Product;
import com.flashsale.ordersystem.product.repository.ProductRepository;
import com.flashsale.ordersystem.sale.domain.enums.SaleStatus;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.sale.domain.model.SaleItem;
import com.flashsale.ordersystem.sale.repository.SaleItemRepository;
import com.flashsale.ordersystem.sale.repository.SaleRepository;
import com.flashsale.ordersystem.user.domain.User;
import com.flashsale.ordersystem.user.repository.UserRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PurchaseFlowE2ETest {

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

    @Container
    static GenericContainer<?> redisContainer =
            new GenericContainer<>("redis:7.2")
                    .withExposedPorts(6379);

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
                "spring.data.redis.host",
                redisContainer::getHost
        );

        registry.add(
                "spring.data.redis.port",
                () -> redisContainer.getMappedPort(6379)
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
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SaleItemRepository saleItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private Keycloak keycloak;

    @MockBean
    private JwtDecoder jwtDecoder;

    private User savedUser;
    private Product savedProduct;
    private Sale savedSale;

    @BeforeEach
    void setupData() {

        orderRepository.deleteAll();
        saleItemRepository.deleteAll();
        saleRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();

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
        sale.setStartTime(Instant.now().minusSeconds(3600));  // 1 hour ago
        sale.setEndTime(Instant.now().plusSeconds(3600));
        sale.setStatus(SaleStatus.ACTIVE);

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
    void shouldProcessPurchaseEndToEndSuccessfully()
            throws Exception {

        String stockKey =
                "stock:%d:%d".formatted(
                        savedSale.getId(),
                        savedProduct.getId()
                );

        redisTemplate.opsForValue().set(
                stockKey,
                "100"
        );

        String saleActiveKey = "sale_active:" + savedSale.getId();
        redisTemplate.opsForValue().set(saleActiveKey, "true");

        mockMvc.perform(
                        post(
                                "/api/v1/sales/{saleId}/purchase",
                                savedSale.getId()
                        )
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .subject("user-1")
                                                .claim("realm_access", Map.of("roles", List.of("USER")))
                                        )
                                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "productId": %d
                                        
                                        }
                                        """.formatted(savedProduct.getId()))
                )
                .andExpect(status().isAccepted());

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {

                    Order order =
                            orderRepository.findAll()
                                    .stream()
                                    .findFirst()
                                    .orElse(null);

                    assertNotNull(order);

                    assertEquals(
                            OrderStatus.CONFIRMED,
                            order.getStatus()
                    );

                    String remainingStock =
                            redisTemplate.opsForValue()
                                    .get(stockKey);

                    assertEquals("99", remainingStock);
                });
    }
}