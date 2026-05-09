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
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.sale.presentation.dto.SaleData;
import com.flashsale.ordersystem.sale.service.SaleService;
import com.flashsale.ordersystem.shared.exception.BusinessException;
import com.flashsale.ordersystem.shared.exception.ErrorCode;
import com.flashsale.ordersystem.shared.exception.InfrastructureException;
import com.flashsale.ordersystem.shared.port.StockReservationPort;
import com.flashsale.ordersystem.user.domain.User;
import com.flashsale.ordersystem.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private  StockReservationPort stockReservationPort;
    @Mock
    private  UserService userService;
    @Mock
    private  OrderEventPublisher orderEventPublisher;
    @Mock
    private  OrderRepository orderRepository;
    @Mock
    private  OrderItemRepository orderItemRepository;
    @Mock
    private  ProcessedEventRepository processedEventRepository;
    @Mock
    private  SaleService saleService;
    @InjectMocks
    private OrderService orderService;
    private final String userId = "user-1";
    private final Long saleId = 1L;
    private final Long productId = 100L;

    @Test
    void shouldPurchaseSuccessfully(){
        when(stockReservationPort.isSaleActive(saleId))
                .thenReturn(true);
        when(stockReservationPort.tryPurchase(userId, saleId, productId,1))
                .thenReturn(true);
        MDC.put("correlationId", "test-correlation-id");
        PurchaseResponse response = orderService.purchase(userId, saleId, productId);
        assertNotNull(response);
        assertEquals("PENDING", response.status());
        assertEquals("order is being processed", response.message());
        assertNotNull(response.eventId());
        verify(userService)
                .getUserOrThrow(userId);
        verify(saleService)
                .validateSaleExists(saleId);
        verify(saleService)
                .validateProductInSale(saleId, productId);
        verify(stockReservationPort)
                .isSaleActive(saleId);
        verify(stockReservationPort)
                .tryPurchase(userId, saleId, productId, 1);
        verify(orderEventPublisher).publish(any(OrderPlacedEvent.class));
    }

    @Test
    void  shouldThrowWhenSaleNotActive(){
        when(stockReservationPort.isSaleActive(saleId))
                .thenReturn(false);

       BusinessException exception = assertThrows(BusinessException.class,()->{
            orderService.purchase(userId,saleId,productId);
        });
        assertNotNull(exception);
        assertEquals(ErrorCode.SALE_NOT_ACTIVE,exception.getErrorCode());
        verify(userService)
                .getUserOrThrow(userId);
        verify(saleService)
                .validateSaleExists(saleId);
        verify(saleService)
                .validateProductInSale(saleId, productId);
        verify(stockReservationPort)
                .isSaleActive(saleId);
        verify(stockReservationPort,never())
                .tryPurchase(userId, saleId, productId, 1);
        verify(orderEventPublisher,never()).publish(any(OrderPlacedEvent.class));
    }

    @Test
    void shouldThrowWhenStockUnavailable() {
        when(stockReservationPort.isSaleActive(saleId))
                .thenReturn(true);

        when(stockReservationPort.tryPurchase(
                userId,
                saleId,
                productId,
                1
        )).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orderService.purchase(userId, saleId, productId)
        );
        assertEquals(
                ErrorCode.INSUFFICIENT_STOCK,
                exception.getErrorCode()
        );
        verify(userService)
                .getUserOrThrow(userId);

        verify(saleService)
                .validateSaleExists(saleId);

        verify(saleService)
                .validateProductInSale(saleId, productId);

        verify(stockReservationPort)
                .isSaleActive(saleId);

        verify(stockReservationPort)
                .tryPurchase(userId, saleId, productId, 1);

        verify(orderEventPublisher, never())
                .publish(any(OrderPlacedEvent.class));
    }

    @Test
    void shouldRecoverStockWhenStockNotInitialized() {
        when(stockReservationPort.isSaleActive(saleId))
                .thenReturn(true);

        when(stockReservationPort.tryPurchase(
                userId,
                saleId,
                productId,
                1
        )).thenThrow(
                new InfrastructureException(
                        ErrorCode.STOCK_NOT_INITIALIZED
                        )
                )
                .thenReturn(true);

        when(saleService.getTotalStock(saleId, productId))
                .thenReturn(100);

        when(orderRepository.countSoldQuantity(
                saleId,
                productId,
                OrderStatus.CONFIRMED
        )).thenReturn(10L);

        Sale sale = new Sale();
        sale.setEndTime(Instant.now().plusSeconds(3600));

        SaleData saleData = mock(SaleData.class);

        when(saleData.sale())
                .thenReturn(sale);

        when(saleService.getSaleAndItem(saleId, productId))
                .thenReturn(saleData);

        MDC.put("correlationId", "test-correlation-id");
        PurchaseResponse response =
                orderService.purchase(userId, saleId, productId);

        assertNotNull(response);

        assertEquals("PENDING", response.status());

        assertEquals(
                "order is being processed",
                response.message()
        );

        assertNotNull(response.eventId());

        verify(userService)
                .getUserOrThrow(userId);

        verify(saleService)
                .validateSaleExists(saleId);

        verify(saleService)
                .validateProductInSale(saleId, productId);

        verify(stockReservationPort, times(2))
                .tryPurchase(userId, saleId, productId, 1);

        verify(stockReservationPort)
                .waitForStock(saleId, productId);

        verify(stockReservationPort)
                .recoverStock(
                        eq(saleId),
                        eq(productId),
                        anyInt(),
                        anyLong()
                );

        verify(orderEventPublisher)
                .publish(any(OrderPlacedEvent.class));
    }

    @Test
    void shouldThrowWhenRedisFails() {

        when(stockReservationPort.isSaleActive(saleId))
                .thenReturn(true);

        when(stockReservationPort.tryPurchase(
                userId,
                saleId,
                productId,
                1
        ))
                .thenThrow(
                        new InfrastructureException(
                                ErrorCode.REDIS_EXECUTION_FAILED
                        )
                );

        InfrastructureException exception = assertThrows(
                InfrastructureException.class,
                () -> orderService.purchase(userId, saleId, productId)
        );

        assertEquals(
                ErrorCode.REDIS_EXECUTION_FAILED,
                exception.getErrorCode()
        );

        verify(userService)
                .getUserOrThrow(userId);

        verify(saleService)
                .validateSaleExists(saleId);

        verify(saleService)
                .validateProductInSale(saleId, productId);

        verify(stockReservationPort)
                .isSaleActive(saleId);

        verify(stockReservationPort)
                .tryPurchase(userId, saleId, productId, 1);

        verify(stockReservationPort, never())
                .waitForStock(anyLong(), anyLong());

        verify(stockReservationPort, never())
                .recoverStock(
                        anyLong(),
                        anyLong(),
                        anyInt(),
                        anyLong()
                );
        verify(orderEventPublisher, never())
                .publish(any(OrderPlacedEvent.class));
    }

    @Test
    void shouldProcessOrderSuccessfully() {

        String eventId = "event-1";
        OrderPlacedEvent event = new OrderPlacedEvent(
                eventId,
                userId,
                saleId,
                productId,
                Instant.now()
        );

        User user = new User();
        Sale sale = new Sale();
        Product product = new Product();
        BigDecimal price = BigDecimal.valueOf(999);
        SaleData saleData = mock(SaleData.class);
        when(saleData.sale()).thenReturn(sale);
        when(saleData.product()).thenReturn(product);
        when(saleData.price()).thenReturn(price);
        when(processedEventRepository.existsById(eventId))
                .thenReturn(false);
        when(userService.getUserOrThrow(userId))
                .thenReturn(user);
        when(saleService.getSaleAndItem(saleId, productId))
                .thenReturn(saleData);

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService.processOrder(event);

        verify(orderRepository, times(2))
                .save(any(Order.class));

        verify(orderItemRepository)
                .save(any(OrderItem.class));

        verify(stockReservationPort)
                .confirmPurchase(userId, saleId, productId);

        verify(processedEventRepository)
                .save(any(ProcessedEvent.class));
    }

    @Test
    void shouldIgnoreDuplicateEvent() {

        OrderPlacedEvent event = new OrderPlacedEvent(
                "event-1",
                userId,
                saleId,
                productId,
                Instant.now()
        );

        when(processedEventRepository.existsById(event.getEventId()))
                .thenReturn(true);

        orderService.processOrder(event);

        verify(processedEventRepository)
                .existsById(event.getEventId());

        verifyNoInteractions(orderRepository);

        verifyNoInteractions(orderItemRepository);

        verify(stockReservationPort, never())
                .confirmPurchase(any(), any(), any());

        verify(processedEventRepository, never())
                .save(any());

        verify(userService, never())
                .getUserOrThrow(any());
    }

    @Test
    void shouldRevertStockWhenBusinessExceptionOccurs() {
        OrderPlacedEvent event = new OrderPlacedEvent(
                "event-1",
                userId,
                saleId,
                productId,
                Instant.now()
        );

        User user = new User();

        SaleData saleData = mock(SaleData.class);

        Sale sale = new Sale();
        Product product = new Product();

        when(processedEventRepository.existsById(event.getEventId()))
                .thenReturn(false);

        when(userService.getUserOrThrow(userId))
                .thenReturn(user);

        when(saleService.getSaleAndItem(saleId, productId))
                .thenReturn(saleData);

        when(saleData.sale()).thenReturn(sale);

        when(saleData.product()).thenReturn(product);

        when(saleData.price()).thenReturn(BigDecimal.valueOf(100));

        Order savedOrder = new Order();

        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        doThrow(new BusinessException(ErrorCode.INSUFFICIENT_STOCK))
                .when(stockReservationPort)
                .confirmPurchase(userId, saleId, productId);

        orderService.processOrder(event);

        verify(stockReservationPort)
                .revertPurchase(
                        userId,
                        saleId,
                        productId,
                        1
                );

        verify(orderRepository, atLeastOnce())
                .save(any(Order.class));
    }

    @Test
    void shouldLeaveOrderPendingWhenSystemFailureOccurs() {

        OrderPlacedEvent event = new OrderPlacedEvent(
                "event-1",
                userId,
                saleId,
                productId,
                Instant.now()
        );

        User user = new User();
        SaleData saleData = mock(SaleData.class);
        Sale sale = new Sale();
        Product product = new Product();

        when(processedEventRepository.existsById(event.getEventId()))
                .thenReturn(false);

        when(userService.getUserOrThrow(userId))
                .thenReturn(user);

        when(saleService.getSaleAndItem(saleId, productId))
                .thenReturn(saleData);

        when(saleData.sale()).thenReturn(sale);

        when(saleData.product()).thenReturn(product);

        when(saleData.price()).thenReturn(BigDecimal.valueOf(100));

        Order savedOrder = new Order();

        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        doThrow(new RuntimeException("Database temporarily unavailable"))
                .when(orderItemRepository)
                .save(any(OrderItem.class));

        orderService.processOrder(event);

        ArgumentCaptor<Order> orderCaptor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository, atLeastOnce())
                .save(orderCaptor.capture());

        Order capturedOrder = orderCaptor.getValue();

        assertEquals(OrderStatus.PENDING, capturedOrder.getStatus());

        verify(stockReservationPort, never())
                .revertPurchase(any(), any(), any(), anyInt());
    }

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

        when(orderRepository.findByIdAndUser_KeycloakId(
                orderId,
                userId
        )).thenReturn(Optional.of(order));

        OrderStatusResponse response =
                orderService.getOrderStatus(userId, orderId);

        assertNotNull(response);

        assertEquals(orderId, response.orderId());

        assertEquals(
                OrderStatus.CONFIRMED.name(),
                response.status()
        );

        assertEquals(saleId, response.saleId());

        assertEquals(productId, response.productId());

        verify(orderRepository)
                .findByIdAndUser_KeycloakId(orderId, userId);
    }

    @Test
    void shouldThrowWhenOrderNotFound() {

        Long orderId = 1L;

        when(orderRepository.findByIdAndUser_KeycloakId(
                orderId,
                userId
        )).thenReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> orderService.getOrderStatus(userId, orderId)
                );

        assertEquals(
                ErrorCode.ORDER_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(orderRepository)
                .findByIdAndUser_KeycloakId(orderId, userId);
    }
}
