package com.flashsale.ordersystem.order.unit;

import com.flashsale.ordersystem.order.adapter.persistence.OrderRepository;
import com.flashsale.ordersystem.order.domain.enums.OrderStatus;
import com.flashsale.ordersystem.order.domain.model.Order;
import com.flashsale.ordersystem.order.scheduler.OrderExpiryScheduler;
import com.flashsale.ordersystem.product.domain.Product;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.shared.port.StockReservationPort;
import com.flashsale.ordersystem.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderExpirySchedulerTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StockReservationPort stockReservationPort;

    @InjectMocks
    private OrderExpiryScheduler scheduler;

    private final String userId = "user-1";

    @Test
    void shouldExpirePendingOrders() {
        User user = new User();
        user.setKeycloakId(userId);

        Sale sale = new Sale();
        sale.setId(1L);

        Product product = new Product();
        product.setId(100L);

        Order order = new Order();
        order.setId(10L);
        order.setUser(user);
        order.setSale(sale);
        order.setProduct(product);

        when(orderRepository.findByStatusAndCreatedAtBefore(
                eq(OrderStatus.PENDING),
                any(Instant.class)
        )).thenReturn(List.of(order));

        when(orderRepository.expireIfPending(
                order.getId(),
                OrderStatus.PENDING,
                OrderStatus.EXPIRED
        )).thenReturn(1);


        scheduler.expirePendingOrders();

        verify(orderRepository)
                .expireIfPending(
                        order.getId(),
                        OrderStatus.PENDING,
                        OrderStatus.EXPIRED
                );

        verify(stockReservationPort)
                .revertPurchase(
                        userId,
                        1L,
                        100L,
                        1
                );
    }

    @Test
    void shouldSkipAlreadyConfirmedOrders() {

        User user = new User();
        user.setKeycloakId(userId);

        Sale sale = new Sale();
        sale.setId(3L);

        Product product = new Product();
        product.setId(300L);

        Order order = new Order();
        order.setId(30L);
        order.setUser(user);
        order.setSale(sale);
        order.setProduct(product);

        when(orderRepository.findByStatusAndCreatedAtBefore(
                eq(OrderStatus.PENDING),
                any(Instant.class)
        )).thenReturn(List.of(order));

        when(orderRepository.expireIfPending(
                any(),
                any(),
                any()
        )).thenReturn(0);

        scheduler.expirePendingOrders();

        verify(stockReservationPort, never())
                .revertPurchase(any(), any(), any(), anyInt());
    }
}