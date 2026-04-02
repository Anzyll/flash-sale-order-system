package com.flashsale.ordersystem.order.application.service;

import com.flashsale.ordersystem.common.exception.CustomException;
import com.flashsale.ordersystem.common.exception.ErrorCode;
import com.flashsale.ordersystem.order.application.port.IdempotencyService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PurchaseService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SaleRepository saleRepository;
    private  final SaleItemRepository saleItemRepository;
    private final StockService stockService;
    private final IdempotencyService idempotencyService;
    @Transactional
    public Order purchase(Long saleId, Long productId) {
        Long userId = 1L;
        int quantity = 1;
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

        boolean allowed = idempotencyService.tryAcquire(userId,saleId,productId);
        if(!allowed){
            throw new CustomException(ErrorCode.DUPLICATE_REQUEST);
        }


        try {
            boolean success = stockService.decrement(saleId, productId, quantity);
            if (!success) {
                throw new CustomException(ErrorCode.INSUFFICIENT_STOCK);
            }

            Order order = new Order();
            order.setSaleId(sale.getId());
            order.setUserId(userId);
            order.setStatus(OrderStatus.PENDING);
            order.setCreatedAt(LocalDateTime.now());
            order.setTotalAmount((item.getSalePrice()));
            Order savedOrder = orderRepository.save(order);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(quantity);
            orderItem.setPrice(item.getSalePrice());
            orderItem.setUserId(userId);
            orderItem.setSaleId(saleId);

            try {
                orderItemRepository.save(orderItem);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                throw new CustomException(ErrorCode.ALREADY_PURCHASED);
            }
            return savedOrder;
        }
        catch (RuntimeException e){
            idempotencyService.release(userId,saleId,productId);
            throw e;
        }

    }
}
