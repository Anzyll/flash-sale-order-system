package com.flashsale.ordersystem.order.infrastructure.kafka.consumer;

import com.flashsale.ordersystem.common.exception.CustomException;
import com.flashsale.ordersystem.common.exception.ErrorCode;
import com.flashsale.ordersystem.order.application.port.StockService;
import com.flashsale.ordersystem.order.domain.enums.OrderStatus;
import com.flashsale.ordersystem.order.domain.model.Order;
import com.flashsale.ordersystem.order.domain.model.OrderItem;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import com.flashsale.ordersystem.order.infrastructure.repository.OrderItemRepository;
import com.flashsale.ordersystem.order.infrastructure.repository.OrderRepository;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.sale.domain.model.SaleItem;
import com.flashsale.ordersystem.sale.infrastructure.SaleItemRepository;
import com.flashsale.ordersystem.sale.infrastructure.SaleRepository;
import com.flashsale.ordersystem.user.application.UserService;
import com.flashsale.ordersystem.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final StringRedisTemplate redisTemplate;
    private final StockService stockService;

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
    @Transactional
    public void consume(OrderPlacedEvent event, @Header("correlationId") String correlationId) {
        String eventKey = "event_processed:"+event.getEventId();

        Boolean exists = redisTemplate.hasKey(eventKey);
        if (Boolean.TRUE.equals(exists)){
            log.warn("Duplicate event skipped. eventId={}, correlationId={}",
                    event.getEventId(), correlationId);
            return;
        }

        log.info("Processing order. eventId={}, correlationId={}, productId={}",
                event.getEventId(),
                correlationId,
                event.getProductId());

        Order order = null;

        try {
            User user = userService.getUserOrThrow(event.getUserId());

            Sale sale = saleRepository.findById(event.getSaleId())
                    .orElseThrow(() -> new CustomException(ErrorCode.SALE_NOT_FOUND));

            SaleItem item = saleItemRepository
                    .findBySaleIdAndProductId(event.getSaleId(), event.getProductId())
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

            order = new Order();
            order.setUser(user);
            order.setSale(sale);
            order.setStatus(OrderStatus.CONFIRMED);
            order.setCreatedAt(LocalDateTime.now());
            order.setTotalAmount(item.getSalePrice());

            Order savedOrder = orderRepository.save(order);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(item.getProduct());
            orderItem.setQuantity(1);
            orderItem.setPrice(item.getSalePrice());

            orderItemRepository.save(orderItem);
            if (true){
                throw new RuntimeException("forced failure of consumer");
            }
            redisTemplate.opsForValue().set(
                    eventKey,
                    "1",
                    Duration.ofHours(24)
            );
            log.info("Order saved successfully. eventId={} correlationId={}", event.getEventId(), correlationId);
        }
        catch (Exception e){
            log.error("Order processing failed. eventId={} correlationId={}", event.getEventId(), correlationId,e);

            String purchaseKey = "purchase_done:%s:%d:%d"
                    .formatted(event.getUserId(), event.getSaleId(), event.getProductId());

            Boolean purchaseExists = redisTemplate.hasKey(purchaseKey);

            if (Boolean.TRUE.equals(purchaseExists)) {
                log.warn("Reverting stock for eventId={}", event.getEventId());
                stockService.revertPurchase(
                        event.getUserId(),
                        event.getSaleId(),
                        event.getProductId(),
                        1
                );
            }

            if(order!=null && order.getUser()!=null){
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
            }
           throw e;
        }
    }
}