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
import com.flashsale.ordersystem.sale.application.service.SaleService;
import com.flashsale.ordersystem.user.application.UserService;
import com.flashsale.ordersystem.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final StockService stockService;
    private final UserService userService;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final SaleService saleService;
    public void purchase(String userId,Long saleId, Long productId,String correlationId) {
        log.info("PURCHASE METHOD STARTED");
        int quantity = 1;
        userService.getUserOrThrow(userId);

        saleService.validateSaleExists(saleId);
        saleService.validateProductInSale(saleId, productId);

        if (!stockService.isSaleActive(saleId)) {
            throw new CustomException(ErrorCode.SALE_NOT_ACTIVE);
        }

            boolean success;
            try{
                log.debug("BEFORE processPurchase");
                success = stockService.processPurchase(userId,saleId, productId, quantity);
                log.debug("AFTER processPurchase");
            }
            catch (InfrastructureException e){
                log.debug("ENTERED CATCH BLOCK");
                log.error("ERROR CODE FROM EXCEPTION: {}", e.getErrorCode());
                if (e.getErrorCode()==ErrorCode.STOCK_NOT_INITIALIZED) {

                    log.warn("Stock not initialized. Triggering recovery. saleId={}, productId={}",
                            saleId, productId);
                    recoverStockFromDB(saleId,productId);
                    stockService.waitForStock(saleId, productId);
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

            var saleData = saleService.getSaleAndItem(
                    event.getSaleId(),
                    event.getProductId()
            );

            order = new Order();
            order.setStatus(OrderStatus.PENDING);
            order.setUser(user);
            order.setSale(saleData.sale());
            order.setProduct(saleData.product());
            order.setCreatedAt(LocalDateTime.now());
            order.setTotalAmount(saleData.price());

            Order savedOrder = orderRepository.save(order);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(savedOrder.getProduct());
            orderItem.setQuantity(1);
            orderItem.setPrice(saleData.price());

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

            if(!completed) {
                stockService.revertPurchase(
                        event.getUserId(),
                        event.getSaleId(),
                        event.getProductId(),
                        1
                );
                if (order != null) {
                    order.setStatus(OrderStatus.FAILED);
                    orderRepository.save(order);
            }
            }
        }
        catch (Exception e) {
            log.error("System failure. Leaving order in PENDING. eventId={}",
                    event.getEventId(), e);
            throw e;
        }
    }
    private void recoverStockFromDB(Long saleId, Long productId) {

        int initialStock = saleService.getTotalStock(saleId, productId);
        long sold = orderRepository.countSoldQuantity(
                saleId,
                productId,
                OrderStatus.CONFIRMED
        );
        int remaining = Math.max(0, initialStock - (int) sold);
        var saleData = saleService.getSaleAndItem(saleId, productId);
        long ttl = Duration.between(
                LocalDateTime.now(),
                saleData.sale().getEndTime()
        ).getSeconds();
        ttl = Math.max(ttl, 60);
        stockService.recoverStock(saleId, productId, remaining, ttl);
    }
}
