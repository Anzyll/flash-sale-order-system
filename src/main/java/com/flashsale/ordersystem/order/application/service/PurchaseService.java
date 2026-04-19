package com.flashsale.ordersystem.order.application.service;

import com.flashsale.ordersystem.common.exception.CustomException;
import com.flashsale.ordersystem.common.exception.ErrorCode;
import com.flashsale.ordersystem.order.application.port.OrderEventPublisher;
import com.flashsale.ordersystem.order.application.port.StockService;
import com.flashsale.ordersystem.order.domain.enums.OrderStatus;
import com.flashsale.ordersystem.order.domain.model.Order;
import com.flashsale.ordersystem.order.domain.model.OrderItem;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import com.flashsale.ordersystem.order.infrastructure.repository.OrderItemRepository;
import com.flashsale.ordersystem.order.infrastructure.repository.OrderRepository;
import com.flashsale.ordersystem.sale.domain.Sale;
import com.flashsale.ordersystem.sale.domain.SaleItem;
import com.flashsale.ordersystem.sale.infrastructure.SaleItemRepository;
import com.flashsale.ordersystem.sale.infrastructure.SaleRepository;
import com.flashsale.ordersystem.user.application.UserService;
import com.flashsale.ordersystem.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseService {
    private final SaleRepository saleRepository;
    private  final SaleItemRepository saleItemRepository;
    private final StockService stockService;
    private final UserService userService;
    private final OrderEventPublisher orderEventPublisher;

    public void purchase(String userId,Long saleId, Long productId,String correlationId) {
        int quantity = 1;
        userService.getUserOrThrow(userId);
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(()-> new CustomException(ErrorCode.SALE_NOT_FOUND));

        if(sale.getEndTime().isBefore(LocalDateTime.now())){
            throw new CustomException(ErrorCode.SALE_EXPIRED);
        }
        if (sale.getStartTime().isAfter(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.SALE_NOT_STARTED);
        }

         SaleItem item = saleItemRepository.findBySaleIdAndProductId(saleId,productId)
                .orElseThrow(()->new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

         long ttlSeconds = calculateTTL(sale);

            boolean success = stockService.processPurchase(userId,saleId, productId, quantity,ttlSeconds);
            if (!success) {
                throw new CustomException(ErrorCode.INSUFFICIENT_STOCK);
            }
          log.info("Publishing order event. correlationId={}, userId={}, productId={}",
                correlationId, userId, productId);
            OrderPlacedEvent event = new OrderPlacedEvent(UUID.randomUUID().toString(),userId,saleId,productId,System.currentTimeMillis());
            orderEventPublisher.publish(event,correlationId);

    }

    private long calculateTTL(Sale sale) {
        long seconds = java.time.Duration.between(LocalDateTime.now(),sale.getEndTime()).getSeconds();
        return Math.max(seconds,60);
    }
}
