package com.flashsale.ordersystem.order.application.service;

import com.flashsale.ordersystem.common.exception.CustomException;
import com.flashsale.ordersystem.common.exception.ErrorCode;
import com.flashsale.ordersystem.common.exception.InfrastructureException;
import com.flashsale.ordersystem.order.application.port.OrderEventPublisher;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final SaleRepository saleRepository;
    private  final SaleItemRepository saleItemRepository;
    private final StockService stockService;
    private final UserService userService;
    private final OrderEventPublisher orderEventPublisher;
    private final StringRedisTemplate redisTemplate;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProcessedEventRepository processedEventRepository;
    public void purchase(String userId,Long saleId, Long productId,String correlationId) {
        log.info("PURCHASE METHOD STARTED");
        int quantity = 1;
        userService.getUserOrThrow(userId);
        String key = "sale_active:" + saleId;
        String active = redisTemplate.opsForValue().get(key);

        if (active == null) {
            saleRepository.findById(saleId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SALE_NOT_FOUND));

            redisTemplate.opsForValue().set(key, "true", Duration.ofHours(24));

        } else if (!"true".equals(active)) {
            throw new CustomException(ErrorCode.SALE_NOT_ACTIVE);
        }

        saleItemRepository.findBySaleIdAndProductId(saleId,productId)
                .orElseThrow(()->new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

            boolean success;
            try{
                log.error("BEFORE processPurchase");
                success = stockService.processPurchase(userId,saleId, productId, quantity);
                log.error("AFTER processPurchase");
            }
            catch (InfrastructureException e){
                log.error("ENTERED CATCH BLOCK");
                log.error("ERROR CODE FROM EXCEPTION: {}", e.getErrorCode());
                if (e.getErrorCode()==ErrorCode.STOCK_NOT_INITIALIZED) {

                    log.warn("Stock not initialized. Triggering recovery. saleId={}, productId={}",
                            saleId, productId);
                    stockService.recoverStock(saleId,productId);
                    waitForStock(saleId, productId);
                    log.info("Retrying purchase after stock recovery. saleId={}, productId={}",
                            saleId, productId);
                   success = stockService.processPurchase(userId, saleId, productId, quantity);
                }
                else {
                    throw e;
                }
            }
            if (!success){
                log.warn("Purchase failed due to insufficient stock. saleId={}, productId={}",
                        saleId, productId);
                throw new CustomException(ErrorCode.INSUFFICIENT_STOCK);
            }
          log.info("Publishing order event. correlationId={}, userId={}, productId={}",
                correlationId, userId, productId);
            OrderPlacedEvent event = new OrderPlacedEvent(
                    correlationId,
                    userId,
                    saleId,
                    productId,
                    System.currentTimeMillis());
            orderEventPublisher.publish(event,correlationId);
    }

    private void waitForStock(Long saleId, Long productId) {
        String stockKey = "stock:%d:%d".formatted(saleId, productId);

        log.info("Waiting for stock key...");
        for (int i = 0; i < 10; i++) {
            String stock = redisTemplate.opsForValue().get(stockKey);
            if (stock != null) return;

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        throw new InfrastructureException(ErrorCode.STOCK_NOT_INITIALIZED);
    }

    @Transactional
    public void processOrder(OrderPlacedEvent event,String correlationId){
        try{
            processedEventRepository.save(new ProcessedEvent(event.getEventId()));
        }
        catch (DataIntegrityViolationException e) {
            log.warn("Duplicate event detected. eventId={}", event.getEventId());
            return;
        }
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

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(savedOrder.getProduct());
            orderItem.setQuantity(1);
            orderItem.setPrice(item.getSalePrice());

            orderItemRepository.save(orderItem);

            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            completed = true;
            log.info("Order CONFIRMED. eventId={}, correlationId={}", event.getEventId(), correlationId);
        }
        catch (DataIntegrityViolationException e) {
            log.warn("Duplicate order detected. eventId={}, productId={}, correlationId={}",
                    event.getEventId(), event.getProductId(), correlationId);
            return;
        }
        catch (CustomException e) {
            log.warn("Business failure. eventId={}", event.getEventId(), e);

            if (order != null) {
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);

            if(!completed) {
                stockService.revertPurchase(
                        event.getUserId(),
                        event.getSaleId(),
                        event.getProductId(),
                        1
                );
            }
            }
        }
        catch (Exception e) {
            log.error("System failure. Leaving order in PENDING. eventId={}",
                    event.getEventId(), e);
            throw e;
        }
    }
}
