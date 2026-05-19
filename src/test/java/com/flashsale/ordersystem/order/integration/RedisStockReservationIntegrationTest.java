package com.flashsale.ordersystem.order.integration;

import com.flashsale.ordersystem.order.adapter.redis.RedisStockAdapter;
import static org.junit.jupiter.api.Assertions.*;

import com.flashsale.ordersystem.shared.config.RedisConfig;
import com.flashsale.ordersystem.shared.exception.BusinessException;
import com.flashsale.ordersystem.shared.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@DataRedisTest
@Testcontainers
@Import({
        RedisStockAdapter.class,
        RedisConfig.class
})
public class RedisStockReservationIntegrationTest {

    @Container
    static GenericContainer<?> redisContainer =
            new GenericContainer<>("redis:7.2")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.data.redis.host",
                redisContainer::getHost
        );
        registry.add(
                "spring.data.redis.port",
                () -> redisContainer.getMappedPort(6379)
        );
    }
    @Autowired
    private RedisStockAdapter redisStockAdapter;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @MockBean
    private MetricsService metricsService;

    @BeforeEach
    void setup() {
        RedisConnection connection =
                Objects.requireNonNull(
                        redisTemplate.getConnectionFactory()
                ).getConnection();

        connection.serverCommands().flushDb();
    }

    @Test
    void shouldReserveStockSuccessfully(){
        redisTemplate.opsForValue().set(
                "stock:1:10","10"
        );
        boolean firstPurchase = redisStockAdapter.tryPurchase(
                "1L",
                1L,
                10L,
                1
        );
        boolean secondPurchase = redisStockAdapter.tryPurchase(
                "2L",
                1L,
                10L,
                1
        );
        String remainingStock =   redisTemplate.opsForValue().get("stock:1:10");
        assertTrue(firstPurchase);
        assertTrue(secondPurchase);
        assertEquals("8",remainingStock);
    }

    @Test
    void shouldPreventOverselling() throws InterruptedException {
        redisTemplate.opsForValue().set(
                "stock:1:10","1"
        );
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(20);
        AtomicInteger successCount = new AtomicInteger();

        Runnable purchaseTask = ()->{
            try{
               boolean success = redisStockAdapter.tryPurchase(
                       UUID.randomUUID().toString(),
                        1L,
                        10L,
                        1
                );
                if (success){
                    successCount.incrementAndGet();
                }
            }
            finally {
                latch.countDown();
            }
        };

        for (int i = 0; i < 20; i++) {
            executorService.submit(purchaseTask);
        }
        latch.await();
        String remainingStock = redisTemplate.opsForValue().get("stock:1:10");
        assertEquals(1,successCount.get());
        assertEquals("0",remainingStock);

    }

    @Test
    void shouldPreventDuplicatePurchase(){
        redisTemplate.opsForValue().set(
                "stock:1:10","10"
        );
        boolean firstPurchase = redisStockAdapter.tryPurchase(
                "1L",
                1L,
                10L,
                1
        );

        assertTrue(firstPurchase);
        BusinessException exception = assertThrows(BusinessException.class,()-> redisStockAdapter.tryPurchase(
                "1L",
                1L,
                10L,
                1
        ));
        String remainingStock =   redisTemplate.opsForValue().get("stock:1:10");
        assertEquals("ALREADY_PURCHASED",exception.getErrorCode().getCode());
        assertEquals("9",remainingStock);
    }

    @Test
    void shouldRevertStockSuccessfully(){
        redisTemplate.opsForValue().set(
                "stock:1:10","10"
        );
        boolean purchase = redisStockAdapter.tryPurchase(
                "1L",
                1L,
                10L,
                1
        );
        assertTrue(purchase);
        String stockAfterPurchase = redisTemplate.opsForValue().get("stock:1:10");
        assertEquals("9",stockAfterPurchase);
        redisStockAdapter.revertPurchase(
                "1L",
                1L,
                10L,
                1
        );
        String stockAfterRevert =   redisTemplate.opsForValue().get("stock:1:10");
        assertEquals("10",stockAfterRevert);
    }

    @Test
    void shouldConfirmPurchase(){
        redisTemplate.opsForValue().set(
                "stock:1:10","10"
        );
        boolean purchase = redisStockAdapter.tryPurchase(
                "1L",
                1L,
                10L,
                1
        );
        assertTrue(purchase);
        redisStockAdapter.confirmPurchase(
                "1L",
                1L,
                10L,
                Instant.now().plusSeconds(3600)
        );
        String purchaseReservedKey = redisTemplate.opsForValue().get("purchase_reserved:1L:1:10");
        assertNull(purchaseReservedKey);
        String purchaseDoneKey = redisTemplate.opsForValue().get("purchase_done:1L:1:10");
        assertEquals("1",purchaseDoneKey);
    }
}
