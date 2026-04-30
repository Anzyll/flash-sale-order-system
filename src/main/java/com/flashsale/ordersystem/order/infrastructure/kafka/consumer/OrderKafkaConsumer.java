package com.flashsale.ordersystem.order.infrastructure.kafka.consumer;

import com.flashsale.ordersystem.common.exception.CustomException;
import com.flashsale.ordersystem.common.exception.ErrorCode;
import com.flashsale.ordersystem.order.application.port.StockService;
import com.flashsale.ordersystem.order.domain.enums.OrderStatus;
import com.flashsale.ordersystem.order.domain.model.Order;
import com.flashsale.ordersystem.order.domain.model.OrderItem;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import com.flashsale.ordersystem.order.domain.model.ProcessedEvent;
import com.flashsale.ordersystem.order.infrastructure.repository.OrderItemRepository;
import com.flashsale.ordersystem.order.infrastructure.repository.OrderRepository;
import com.flashsale.ordersystem.order.infrastructure.repository.ProcessedEventRepository;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.sale.domain.model.SaleItem;
import com.flashsale.ordersystem.sale.infrastructure.SaleItemRepository;
import com.flashsale.ordersystem.sale.infrastructure.SaleRepository;
import com.flashsale.ordersystem.user.application.UserService;
import com.flashsale.ordersystem.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderKafkaConsumer {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final UserService userService;
    private final StockService stockService;
    private final ProcessedEventRepository processedEventRepository;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000,multiplier = 2),
            dltTopicSuffix = ".DLQ"
    )
    @KafkaListener(
            topics = "order.placed",
            groupId = "order-processing-group",
            concurrency = "3"
    )
    public void consume(OrderPlacedEvent event, @Header("correlationId") String correlationId) {
        String eventId = event.getEventId();
        String eventKey = "event_processed:"+eventId;

        try{
            processedEventRepository.save(new ProcessedEvent(eventId));
        }
        catch (DataIntegrityViolationException e) {
            log.warn("Duplicate event detected. eventId={}", eventId);
            return;
        }

        log.info("Processing order. eventId={}, correlationId={}, productId={}",
                eventId,
                correlationId,
                event.getProductId());

        Order order = null;
        boolean completed = false;

        try {
            User user = userService.getUserOrThrow(event.getUserId());

            Sale sale = saleRepository.findById(event.getSaleId())
                    .orElseThrow(() -> new CustomException(ErrorCode.SALE_NOT_FOUND));

            SaleItem item = saleItemRepository
                    .findBySaleIdAndProductId(event.getSaleId(), event.getProductId())
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

            order = new Order();
            order.setStatus(OrderStatus.PENDING);
            order.setUser(user);
            order.setSale(sale);
            order.setProduct(item.getProduct());
            order.setCreatedAt(LocalDateTime.now());
            order.setTotalAmount(item.getSalePrice());

            Order savedOrder = orderRepository.save(order);
            if (true) {
                throw new RuntimeException("simulate crash before confirmation");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(savedOrder.getProduct());
            orderItem.setQuantity(1);
            orderItem.setPrice(item.getSalePrice());

            orderItemRepository.save(orderItem);

            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            completed = true;
            log.info("Order CONFIRMED. eventId={}, correlationId={}", eventId, correlationId);
        }
        catch (DataIntegrityViolationException e) {

            log.warn("Duplicate order detected. eventId={}, productId={}, correlationId={}",
                    eventId, event.getProductId(), correlationId);
            return;
        }
        catch (CustomException e) {

            log.warn("Business failure. eventId={}", eventId, e);

            if (order != null) {
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);

                stockService.revertPurchase(
                        event.getUserId(),
                        event.getSaleId(),
                        event.getProductId(),
                        1
                );
            }
        }
        catch (Exception e) {

            log.error("System failure. Leaving order in PENDING. eventId={}",
                    eventId, e);
            throw e;
        }
    }
}