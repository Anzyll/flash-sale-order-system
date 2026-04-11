package com.flashsale.ordersystem.order.application.service;

import com.flashsale.ordersystem.common.exception.CustomException;
import com.flashsale.ordersystem.common.exception.ErrorCode;
import com.flashsale.ordersystem.order.application.port.StockService;
import com.flashsale.ordersystem.order.domain.enums.OrderStatus;
import com.flashsale.ordersystem.order.domain.model.Order;
import com.flashsale.ordersystem.order.domain.model.OrderItem;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SaleRepository saleRepository;
    private  final SaleItemRepository saleItemRepository;
    private final StockService stockService;
    private final UserService userService;

    public Order purchase(String userId,Long saleId, Long productId) {
        int quantity = 1;
        User user = userService.getUserOrThrow(userId);
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

            Order order = new Order();
            order.setSale(sale);
            order.setUser(user);
            order.setStatus(OrderStatus.PENDING);
            order.setCreatedAt(LocalDateTime.now());
            order.setTotalAmount((item.getSalePrice()));
            Order savedOrder;
            try {
            savedOrder = orderRepository.save(order);
            } catch (Exception e) {
            stockService.revertPurchase(userId,saleId, productId, quantity);
            throw e;
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(item.getProduct());
            orderItem.setQuantity(quantity);
            orderItem.setPrice(item.getSalePrice());

            try {
                orderItemRepository.save(orderItem);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                stockService.revertPurchase(userId,saleId,productId,quantity);
                throw new CustomException(ErrorCode.ALREADY_PURCHASED);
            }
            return savedOrder;
    }

    private long calculateTTL(Sale sale) {
        long seconds = java.time.Duration.between(LocalDateTime.now(),sale.getEndTime()).getSeconds();
        return Math.max(seconds,60);
    }
}
