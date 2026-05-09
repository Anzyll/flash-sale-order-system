package com.flashsale.ordersystem.order.integration;

import com.flashsale.ordersystem.order.adapter.persistence.OrderRepository;
import com.flashsale.ordersystem.order.domain.enums.OrderStatus;
import com.flashsale.ordersystem.order.domain.model.Order;
import com.flashsale.ordersystem.order.domain.model.OrderItem;
import com.flashsale.ordersystem.product.domain.Product;
import com.flashsale.ordersystem.sale.domain.enums.SaleStatus;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.user.domain.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");
    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldCountSoldQuantityByStatus() {
        User user = new User();
        user.setKeycloakId("user-1");
        user.setEmail("test@gmail.com");
        entityManager.persist(user);

        Sale sale = new Sale();
        sale.setTitle("Flash Sale");
        sale.setStartTime(Instant.now());
        sale.setEndTime(Instant.now().plusSeconds(3600));
        sale.setStatus(SaleStatus.ACTIVE);
        entityManager.persist(sale);

        Product product = new Product();
        product.setName("iPhone");
        product.setPrice(BigDecimal.valueOf(10000));
        entityManager.persist(product);

        Order order = new Order();
        order.setUser(user);
        order.setSale(sale);
        order.setProduct(product);
        order.setStatus(OrderStatus.CONFIRMED);

        entityManager.persist(order);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(1);
        item.setPrice(BigDecimal.valueOf(10000));
        entityManager.persist(item);

        User user2 = new User();
        user2.setKeycloakId("user-2");
        user2.setEmail("test2@gmail.com");
        entityManager.persist(user2);

        Order order2 = new Order();
        order2.setUser(user2);
        order2.setSale(sale);
        order2.setProduct(product);
        order2.setStatus(OrderStatus.CONFIRMED);
        entityManager.persist(order2);

        OrderItem item2 = new OrderItem();
        item2.setOrder(order2);
        item2.setProduct(product);
        item2.setQuantity(1);
        item2.setPrice(BigDecimal.valueOf(10000));
        entityManager.persist(item2);

        long soldQuantity =
                orderRepository.countSoldQuantity(
                        sale.getId(),
                        product.getId(),
                        OrderStatus.CONFIRMED
                );

        assertEquals(2, soldQuantity);
    }

    @Test
    void shouldReturnZeroWhenNoOrdersExist() {

        Sale sale = new Sale();
        sale.setTitle("Flash Sale");
        sale.setStartTime(Instant.now());
        sale.setEndTime(Instant.now().plusSeconds(3600));
        sale.setStatus(SaleStatus.ACTIVE);
        entityManager.persist(sale);

        Product product = new Product();
        product.setName("iPhone");
        product.setPrice(BigDecimal.valueOf(10000));
        entityManager.persist(product);

        long soldQuantity =
                orderRepository.countSoldQuantity(
                        sale.getId(),
                        product.getId(),
                        OrderStatus.CONFIRMED
                );

        assertEquals(0, soldQuantity);
    }

    @Test
    void shouldExpirePendingOrder() {

        User user = new User();
        user.setKeycloakId("user-1");
        user.setEmail("test@gmail.com");
        entityManager.persist(user);

        Sale sale = new Sale();
        sale.setTitle("Flash Sale");
        sale.setStartTime(Instant.now());
        sale.setEndTime(Instant.now().plusSeconds(3600));
        sale.setStatus(SaleStatus.ACTIVE);
        entityManager.persist(sale);

        Product product = new Product();
        product.setName("iPhone");
        product.setPrice(BigDecimal.valueOf(10000));
        entityManager.persist(product);

        Order order = new Order();
        order.setUser(user);
        order.setSale(sale);
        order.setProduct(product);
        order.setStatus(OrderStatus.PENDING);

        entityManager.persist(order);

        int updated =
                orderRepository.expireIfPending(
                        order.getId(),
                        OrderStatus.PENDING,
                        OrderStatus.EXPIRED
                );

        entityManager.flush();
        entityManager.clear();

        Order updatedOrder =
                entityManager.find(Order.class, order.getId());

        assertEquals(1, updated);

        assertEquals(
                OrderStatus.EXPIRED,
                updatedOrder.getStatus()
        );
    }

    @Test
    void shouldNotExpireNonPendingOrder() {

        User user = new User();
        user.setKeycloakId("user-1");
        user.setEmail("test@gmail.com");
        entityManager.persist(user);

        Sale sale = new Sale();
        sale.setTitle("Flash Sale");
        sale.setStartTime(Instant.now());
        sale.setEndTime(Instant.now().plusSeconds(3600));
        sale.setStatus(SaleStatus.ACTIVE);
        entityManager.persist(sale);

        Product product = new Product();
        product.setName("iPhone");
        product.setPrice(BigDecimal.valueOf(10000));
        entityManager.persist(product);

        Order order = new Order();
        order.setUser(user);
        order.setSale(sale);
        order.setProduct(product);
        order.setStatus(OrderStatus.CONFIRMED);

        entityManager.persist(order);

        int updated =
                orderRepository.expireIfPending(
                        order.getId(),
                        OrderStatus.PENDING,
                        OrderStatus.EXPIRED
                );

        entityManager.flush();
        entityManager.clear();

        Order unchangedOrder =
                entityManager.find(Order.class, order.getId());

        assertEquals(0, updated);

        assertEquals(
                OrderStatus.CONFIRMED,
                unchangedOrder.getStatus()
        );
    }
}
