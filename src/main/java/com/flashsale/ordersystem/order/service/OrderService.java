package com.flashsale.ordersystem.order.service;

import com.flashsale.ordersystem.order.port.OrderProcessingUseCase;
import com.flashsale.ordersystem.shared.exception.BusinessException;
import com.flashsale.ordersystem.shared.exception.ErrorCode;
import com.flashsale.ordersystem.shared.exception.InfrastructureException;
import com.flashsale.ordersystem.order.port.OrderEventPublisher;
import com.flashsale.ordersystem.shared.port.StockReservationPort;
import com.flashsale.ordersystem.order.domain.enums.OrderStatus;
import com.flashsale.ordersystem.order.domain.model.Order;
import com.flashsale.ordersystem.order.domain.model.OrderItem;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import com.flashsale.ordersystem.order.domain.model.ProcessedEvent;
import com.flashsale.ordersystem.order.adapter.persistence.OrderItemRepository;
import com.flashsale.ordersystem.order.adapter.persistence.OrderRepository;
import com.flashsale.ordersystem.order.adapter.persistence.ProcessedEventRepository;
import com.flashsale.ordersystem.sale.service.SaleService;
import com.flashsale.ordersystem.user.service.UserService;
import com.flashsale.ordersystem.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService implements OrderProcessingUseCase {
    private final StockReservationPort stockReservationPort;
    private final UserService userService;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final SaleService saleService;
    private static final int DEFAULT_QUANTITY = 1;
    public void purchase(String userId,Long saleId, Long productId) {
        log.info("Purchased started. userId={}, saleId={}, productId={}",
                userId, saleId, productId);
            validatePurchaseRequest(userId,saleId,productId);
            boolean success;
            try{
                success = stockReservationPort.tryPurchase(userId,saleId, productId, DEFAULT_QUANTITY);
            }
            catch (InfrastructureException e){
                if (e.getErrorCode()==ErrorCode.STOCK_NOT_INITIALIZED) {

                    log.warn("Stock not initialized. Triggering recovery. saleId={}, productId={}",
                            saleId, productId);
                    recoverStockFromDB(saleId,productId);
                    stockReservationPort.waitForStock(saleId, productId);
                    log.info("Retrying purchase after stock recovery. saleId={}, productId={}",
                            saleId, productId);
                   success = stockReservationPort.tryPurchase(userId, saleId, productId, DEFAULT_QUANTITY);
                }
                else {
                    log.error("Redis failure. errorCode={}, saleId={}, productId={}",
                            e.getErrorCode(), saleId, productId, e);
                    throw new InfrastructureException(ErrorCode.REDIS_EXECUTION_FAILED);
                }
            }
            if (!success){
                log.warn("Purchase failed due to insufficient stock. saleId={}, productId={}",
                        saleId, productId);
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);

            }
        String correlationId = MDC.get("correlationId");
        log.info("Purchase successful. Publishing event. userId={}, saleId={}, productId={}",
                userId, saleId, productId);
            OrderPlacedEvent event = new OrderPlacedEvent(
                    correlationId,
                    userId,
                    saleId,
                    productId,
                    Instant.now());
            orderEventPublisher.publish(event);
    }

    private void validatePurchaseRequest(String userId, Long saleId, Long productId) {
        userService.getUserOrThrow(userId);
        saleService.validateSaleExists(saleId);
        saleService.validateProductInSale(saleId, productId);

        if (!stockReservationPort.isSaleActive(saleId)) {
            throw new BusinessException(ErrorCode.SALE_NOT_ACTIVE);
        }
    }

    @Override
    @Transactional
    public void processOrder(OrderPlacedEvent event){
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
            order.setCreatedAt(Instant.now());
            order.setTotalAmount(saleData.price());

            Order savedOrder = orderRepository.save(order);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(savedOrder.getProduct());
            orderItem.setQuantity(DEFAULT_QUANTITY);
            orderItem.setPrice(saleData.price());

            orderItemRepository.save(orderItem);

            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            completed = true;
            log.info("Purchase CONFIRMED. eventId={}", event.getEventId());
        }
        catch (DataIntegrityViolationException e) {
            log.warn("Duplicate order detected. eventId={}, productId={}",
                    event.getEventId(), event.getProductId());
            return;
        }
        catch (BusinessException e) {
            log.warn("Business failure. eventId={}, errorCode={}",
                    event.getEventId(), e.getErrorCode());
            if(!completed) {
                stockReservationPort.revertPurchase(
                        event.getUserId(),
                        event.getSaleId(),
                        event.getProductId(),
                        DEFAULT_QUANTITY
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
        log.warn("Recovering stock from DB. saleId={}, productId={}",
                saleId, productId);
        int initialStock = saleService.getTotalStock(saleId, productId);
        long sold = orderRepository.countSoldQuantity(
                saleId,
                productId,
                OrderStatus.CONFIRMED
        );
        int remaining = Math.max(0, initialStock - (int) sold);
        var saleData = saleService.getSaleAndItem(saleId, productId);
        long ttl = Duration.between(
                Instant.now(),
                saleData.sale().getEndTime()
        ).getSeconds();
        ttl = Math.max(ttl, 60);
        stockReservationPort.recoverStock(saleId, productId, remaining, ttl);
        log.info("Stock recovered. saleId={}, productId={}, remaining={}, ttl={}",
                saleId, productId, remaining, ttl);
    }
}
