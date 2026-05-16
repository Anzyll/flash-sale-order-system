package com.flashsale.ordersystem.order.unit;

import com.flashsale.ordersystem.order.adapter.persistence.OrderItemRepository;
import com.flashsale.ordersystem.order.adapter.persistence.OrderRepository;
import com.flashsale.ordersystem.order.adapter.persistence.ProcessedEventRepository;
import com.flashsale.ordersystem.order.adapter.rest.dto.OrderStatusResponse;
import com.flashsale.ordersystem.order.adapter.rest.dto.PurchaseResponse;
import com.flashsale.ordersystem.order.domain.enums.OrderStatus;
import com.flashsale.ordersystem.order.domain.model.Order;
import com.flashsale.ordersystem.order.domain.model.OrderItem;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import com.flashsale.ordersystem.order.domain.model.ProcessedEvent;
import com.flashsale.ordersystem.order.port.OrderEventPublisher;
import com.flashsale.ordersystem.order.service.OrderService;
import com.flashsale.ordersystem.product.domain.Product;
import com.flashsale.ordersystem.product.service.ProductService;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.sale.presentation.dto.CachedSaleData;
import com.flashsale.ordersystem.sale.service.SaleService;
import com.flashsale.ordersystem.shared.exception.BusinessException;
import com.flashsale.ordersystem.shared.exception.ErrorCode;
import com.flashsale.ordersystem.shared.exception.InfrastructureException;
import com.flashsale.ordersystem.shared.port.StockReservationPort;
import com.flashsale.ordersystem.shared.service.MetricsService;
import com.flashsale.ordersystem.user.domain.User;
import com.flashsale.ordersystem.user.service.UserService;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // shared @BeforeEach and helper stubs intentionally over-stub
public class OrderServiceTest {

    @Mock private StockReservationPort stockReservationPort;
    @Mock private UserService userService;
    @Mock private OrderEventPublisher orderEventPublisher;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private SaleService saleService;
    @Mock private MetricsService metricsService;
    @Mock private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    private final String userId    = "user-1";
    private final Long   saleId    = 1L;
    private final Long   productId = 100L;

    @BeforeEach
    void setUpMetrics() {
        // Needed by every purchase() test; lenient so it doesn't fire for processOrder/getOrderStatus tests
        Timer noopTimer = mock(Timer.class);
        when(metricsService.getPurchaseProcessingTime()).thenReturn(noopTimer);
    }

    @Test
    void shouldPurchaseSuccessfully() {
        when(stockReservationPort.isSaleActive(saleId)).thenReturn(true);
        when(stockReservationPort.tryPurchase(userId, saleId, productId, 1)).thenReturn(true);
        MDC.put("correlationId", "test-correlation-id");

        PurchaseResponse response = orderService.purchase(userId, saleId, productId);

        assertNotNull(response);
        assertEquals("PENDING", response.status());
        assertEquals("order is being processed", response.message());
        assertNotNull(response.eventId());

        verify(saleService).validateSaleExists(saleId);
        verify(saleService).validateProductInSale(saleId, productId);
        verify(stockReservationPort).isSaleActive(saleId);
        verify(stockReservationPort).tryPurchase(userId, saleId, productId, 1);
        verify(orderEventPublisher).publish(any(OrderPlacedEvent.class));
    }

    @Test
    void shouldThrowWhenSaleNotActive() {
        when(stockReservationPort.isSaleActive(saleId)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.purchase(userId, saleId, productId));

        assertEquals(ErrorCode.SALE_NOT_ACTIVE, ex.getErrorCode());
        verify(saleService).validateSaleExists(saleId);
        verify(saleService).validateProductInSale(saleId, productId);
        verify(stockReservationPort).isSaleActive(saleId);
        verify(stockReservationPort, never()).tryPurchase(any(), any(), any(), anyInt());
        verify(orderEventPublisher, never()).publish(any(OrderPlacedEvent.class));
    }

    @Test
    void shouldThrowWhenStockUnavailable() {
        when(stockReservationPort.isSaleActive(saleId)).thenReturn(true);
        when(stockReservationPort.tryPurchase(userId, saleId, productId, 1)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.purchase(userId, saleId, productId));

        assertEquals(ErrorCode.INSUFFICIENT_STOCK, ex.getErrorCode());
        verify(stockReservationPort).isSaleActive(saleId);
        verify(stockReservationPort).tryPurchase(userId, saleId, productId, 1);
        verify(orderEventPublisher, never()).publish(any(OrderPlacedEvent.class));
    }

    @Test
    void shouldRecoverStockWhenStockNotInitialized() {
        when(stockReservationPort.isSaleActive(saleId)).thenReturn(true);
        when(stockReservationPort.tryPurchase(userId, saleId, productId, 1))
                .thenThrow(new InfrastructureException(ErrorCode.STOCK_NOT_INITIALIZED))
                .thenReturn(true);

        when(saleService.getTotalStock(saleId, productId)).thenReturn(100);
        when(orderRepository.countSoldQuantity(saleId, productId, OrderStatus.CONFIRMED)).thenReturn(10L);

        CachedSaleData cachedSaleData = mock(CachedSaleData.class);
        when(cachedSaleData.endTime()).thenReturn(Instant.now().plusSeconds(3600));
        when(saleService.getSaleAndItem(saleId, productId)).thenReturn(cachedSaleData);

        MDC.put("correlationId", "test-correlation-id");

        PurchaseResponse response = orderService.purchase(userId, saleId, productId);

        assertNotNull(response);
        assertEquals("PENDING", response.status());
        assertEquals("order is being processed", response.message());
        assertNotNull(response.eventId());

        verify(saleService).validateSaleExists(saleId);
        verify(saleService).validateProductInSale(saleId, productId);
        verify(stockReservationPort, times(2)).tryPurchase(userId, saleId, productId, 1);
        verify(stockReservationPort).waitForStock(saleId, productId);
        verify(stockReservationPort).recoverStock(eq(saleId), eq(productId), anyInt(), anyLong());
        verify(orderEventPublisher).publish(any(OrderPlacedEvent.class));
    }

    @Test
    void shouldThrowWhenRedisFails() {
        when(stockReservationPort.isSaleActive(saleId)).thenReturn(true);
        when(stockReservationPort.tryPurchase(userId, saleId, productId, 1))
                .thenThrow(new InfrastructureException(ErrorCode.REDIS_EXECUTION_FAILED));

        InfrastructureException ex = assertThrows(InfrastructureException.class,
                () -> orderService.purchase(userId, saleId, productId));

        assertEquals(ErrorCode.REDIS_EXECUTION_FAILED, ex.getErrorCode());
        verify(stockReservationPort).isSaleActive(saleId);
        verify(stockReservationPort).tryPurchase(userId, saleId, productId, 1);
        verify(stockReservationPort, never()).waitForStock(anyLong(), anyLong());
        verify(stockReservationPort, never()).recoverStock(anyLong(), anyLong(), anyInt(), anyLong());
        verify(orderEventPublisher, never()).publish(any(OrderPlacedEvent.class));
    }

    // ───────────────────────── processOrder() ─────────────────────────

    /**
     * Stubs every dependency that processOrder() may touch.
     * LENIENT strictness means unused stubs in a given test don't cause failures.
     */
    private CachedSaleData stubProcessOrderDependencies() {
        CachedSaleData cachedSaleData = mock(CachedSaleData.class);
        when(cachedSaleData.saleId()).thenReturn(saleId);
        when(cachedSaleData.productId()).thenReturn(productId);
        when(cachedSaleData.salePrice()).thenReturn(BigDecimal.valueOf(999));
        when(cachedSaleData.productName()).thenReturn("iPhone");
        when(cachedSaleData.saleStatus()).thenReturn("ACTIVE");
        when(cachedSaleData.endTime()).thenReturn(Instant.now().plusSeconds(3600));
        when(saleService.getSaleAndItem(saleId, productId)).thenReturn(cachedSaleData);

        Sale sale = new Sale();
        sale.setId(saleId);
        when(saleService.getSaleEntity(saleId)).thenReturn(sale);

        Product product = new Product();
        product.setId(productId);
        product.setPrice(BigDecimal.valueOf(999));
        when(productService.getProductEntity(productId)).thenReturn(product);

        // Return the same Order instance that was passed in so the service
        // can keep calling setStatus() on the same object reference.
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        return cachedSaleData;
    }

    @Test
    void shouldProcessOrderSuccessfully() {
        String eventId = "event-1";
        OrderPlacedEvent event = new OrderPlacedEvent(eventId, userId, saleId, productId, Instant.now());

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(userService.getUserOrThrow(userId)).thenReturn(new User());
        stubProcessOrderDependencies();

        orderService.processOrder(event);

        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(stockReservationPort).confirmPurchase(eq(userId), eq(saleId), eq(productId), any(Instant.class));
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void shouldRevertStockWhenBusinessExceptionOccurs() {
        OrderPlacedEvent event = new OrderPlacedEvent("event-1", userId, saleId, productId, Instant.now());

        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(userService.getUserOrThrow(userId)).thenReturn(new User());
        stubProcessOrderDependencies();

        doThrow(new BusinessException(ErrorCode.INSUFFICIENT_STOCK))
                .when(stockReservationPort)
                .confirmPurchase(eq(userId), eq(saleId), eq(productId), any(Instant.class));

        orderService.processOrder(event);

        verify(stockReservationPort).revertPurchase(userId, saleId, productId, 1);
        verify(orderRepository, atLeastOnce()).save(any(Order.class));
    }

    @Test
    void shouldLeaveOrderPendingWhenSystemFailureOccurs() {
        OrderPlacedEvent event = new OrderPlacedEvent("event-1", userId, saleId, productId, Instant.now());

        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(userService.getUserOrThrow(userId)).thenReturn(new User());
        stubProcessOrderDependencies();

        doThrow(new RuntimeException("Database temporarily unavailable"))
                .when(orderItemRepository)
                .save(any(OrderItem.class));

        orderService.processOrder(event);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, atLeastOnce()).save(captor.capture());

        Order lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(OrderStatus.PENDING, lastSaved.getStatus());

        verify(stockReservationPort, never()).revertPurchase(any(), any(), any(), anyInt());
    }

    // ───────────────────────── getOrderStatus() ─────────────────────────

    @Test
    void shouldReturnOrderStatus() {
        Long orderId = 1L;

        User user = new User();
        user.setKeycloakId(userId);

        Sale sale = new Sale();
        sale.setId(saleId);

        Product product = new Product();
        product.setId(productId);

        Order order = new Order();
        order.setId(orderId);
        order.setUser(user);
        order.setSale(sale);
        order.setProduct(product);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCreatedAt(Instant.now());

        when(orderRepository.findByIdAndUser_KeycloakId(orderId, userId))
                .thenReturn(Optional.of(order));

        OrderStatusResponse response = orderService.getOrderStatus(userId, orderId);

        assertNotNull(response);
        assertEquals(orderId, response.orderId());
        assertEquals(OrderStatus.CONFIRMED.name(), response.status());
        assertEquals(saleId, response.saleId());
        assertEquals(productId, response.productId());
        verify(orderRepository).findByIdAndUser_KeycloakId(orderId, userId);
    }

    @Test
    void shouldThrowWhenOrderNotFound() {
        Long orderId = 1L;

        when(orderRepository.findByIdAndUser_KeycloakId(orderId, userId))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.getOrderStatus(userId, orderId));

        assertEquals(ErrorCode.ORDER_NOT_FOUND, ex.getErrorCode());
        verify(orderRepository).findByIdAndUser_KeycloakId(orderId, userId);
    }
}