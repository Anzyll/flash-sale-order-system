package com.flashsale.ordersystem.order.application.service;

import com.flashsale.ordersystem.common.exception.CustomException;
import com.flashsale.ordersystem.common.exception.ErrorCode;
import com.flashsale.ordersystem.common.exception.InfrastructureException;
import com.flashsale.ordersystem.order.application.port.OrderEventPublisher;
import com.flashsale.ordersystem.order.application.port.StockService;
import com.flashsale.ordersystem.order.domain.model.OrderPlacedEvent;
import com.flashsale.ordersystem.sale.infrastructure.SaleItemRepository;
import com.flashsale.ordersystem.sale.infrastructure.SaleRepository;
import com.flashsale.ordersystem.user.application.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseService {
    private final SaleRepository saleRepository;
    private  final SaleItemRepository saleItemRepository;
    private final StockService stockService;
    private final UserService userService;
    private final OrderEventPublisher orderEventPublisher;
    private final StringRedisTemplate redisTemplate;
    public void purchase(String userId,Long saleId, Long productId,String correlationId) {
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
                success = stockService.processPurchase(userId,saleId, productId, quantity);
            }
            catch (InfrastructureException e){
                if (e.getErrorCode()==ErrorCode.STOCK_NOT_INITIALIZED) {
                    stockService.recoverStock(saleId,productId);
                   success = stockService.processPurchase(userId, saleId, productId, quantity);
                }
                else {
                    throw e;
                }
            }
            if (!success){
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
}
