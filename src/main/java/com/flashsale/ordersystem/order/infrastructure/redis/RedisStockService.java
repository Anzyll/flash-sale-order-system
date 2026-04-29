package com.flashsale.ordersystem.order.infrastructure.redis;

import com.flashsale.ordersystem.common.exception.CustomException;
import com.flashsale.ordersystem.common.exception.ErrorCode;
import com.flashsale.ordersystem.common.exception.InfrastructureException;
import com.flashsale.ordersystem.order.application.port.StockService;
import com.flashsale.ordersystem.order.domain.enums.OrderStatus;
import com.flashsale.ordersystem.order.infrastructure.repository.OrderRepository;
import com.flashsale.ordersystem.sale.domain.enums.SaleStatus;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.sale.domain.model.SaleItem;
import com.flashsale.ordersystem.sale.infrastructure.SaleItemRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisStockService implements StockService {
    private final StringRedisTemplate redisTemplate;
    private final SaleItemRepository saleItemRepository;
    private final OrderRepository orderRepository;
    private static final String LUA_SCRIPT = """
                   local stock = tonumber(redis.call('GET', KEYS[1]))
                   local purchased = tonumber(redis.call('EXISTS', KEYS[2]))
                   local qty = tonumber(ARGV[1])
            
                   if purchased == 1 then
                      return -4
                   end
          
                   if qty == nil or qty <= 0 then
                      return -3
                   end
   
                   if stock == nil then
                      return -2
                   end
            
                   if stock < qty then
                      return -1
                   end
           
                   redis.call('DECRBY',KEYS[1],qty)
                   local ttl = redis.call('TTL', KEYS[1])
                   if ttl > 0 then
                      redis.call('SET', KEYS[2], "1", 'EX', ttl)
                   else
                      redis.call('SET', KEYS[2], "1")
                   end
                   return 1
            
            """;

    private static final long OUT_OF_STOCK = -1;
    private static final long NOT_INITIALIZED = -2;
    private static final long INVALID_QTY = -3;
    private static final long ALREADY_PURCHASED = -4;
    private static final DefaultRedisScript<Long> SCRIPT;

    static {
        SCRIPT = new DefaultRedisScript<>();
        SCRIPT.setScriptText(LUA_SCRIPT);
        SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean processPurchase(String  userId, Long saleId, Long productId, int quantity) {
        String stock_key = "stock:%d:%d".formatted(saleId, productId);
        String purchase_done_key = "purchase_done:%s:%d:%d".formatted(userId, saleId, productId);
        Long result = redisTemplate.execute(
                SCRIPT,
                List.of(stock_key, purchase_done_key),
                String.valueOf(quantity)
        );
        if (result == null) {
            throw new InfrastructureException(ErrorCode.REDIS_EXECUTION_FAILED);
        }
        if (result == OUT_OF_STOCK) {
            return false;
        }
        if (result == NOT_INITIALIZED) {
            throw new InfrastructureException(ErrorCode.STOCK_NOT_INITIALIZED);
        }
        if (result == INVALID_QTY) {
            throw new CustomException(ErrorCode.INVALID_QUANTITY);
        }
        if (result == ALREADY_PURCHASED) {
            throw new CustomException(ErrorCode.ALREADY_PURCHASED);
        }
        return true;
    }

    @Override
    public void revertPurchase(String userId,Long saleId, Long productId, int quantity) {
        String stockKey = "stock:%d:%d".formatted(saleId, productId);
        String purchaseKey = "purchase_done:%s:%d:%d".formatted(userId, saleId, productId);
        Long result = redisTemplate.opsForValue().increment(stockKey,quantity);
        if (result==null){
            throw new InfrastructureException(ErrorCode.REDIS_EXECUTION_FAILED);
        }
        redisTemplate.delete(purchaseKey);
    }

    @Override
    public void recoverStock(Long saleId, Long productId) {
        int retries = 10;
        int waitMs = 20;

        String lockKey = "recover_lock:%d:%d".formatted(saleId, productId);
        String stockKey = "stock:%d:%d".formatted(saleId, productId);
        log.info("Trying to acquire lock for {} {}", saleId, productId);

        Boolean isOwner = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(10));

        if (!Boolean.TRUE.equals(isOwner)) {
            for (int i = 0; i < retries; i++) {
                String stock = redisTemplate.opsForValue().get(stockKey);
                if (stock != null) {
                    return;
                }

                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            throw new InfrastructureException(ErrorCode.STOCK_NOT_INITIALIZED);
        }
        log.info("Lock acquired, performing recovery...");


        SaleItem item = saleItemRepository.findBySaleIdAndProductId(saleId, productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        int initialStock = item.getTotalStock();
        long sold = orderRepository.countSoldQuantity(saleId, productId, OrderStatus.CONFIRMED);

        int remainingStock = Math.max(0, initialStock - (int) sold);

        redisTemplate.opsForValue()
                .set(stockKey, String.valueOf(remainingStock), Duration.ofHours(24));

        Sale sale = item.getSale();

        long ttlSeconds = Duration.between(
                LocalDateTime.now(),
                sale.getEndTime()
        ).getSeconds();

        if (ttlSeconds > 0) {
            redisTemplate.opsForValue()
                    .set("sale_active:" + saleId, "true", Duration.ofSeconds(ttlSeconds));
        } else {
            redisTemplate.opsForValue()
                    .set("sale_active:" + saleId, "false", Duration.ofMinutes(5));
        }
        log.info("Stock recovered and set in Redis: {}", remainingStock);
    }


    @PostConstruct
    public void recoverAllStock() {
        List<SaleItem> items = saleItemRepository.findBySaleStatus(SaleStatus.ACTIVE);
        for (SaleItem item : items) {

            Long saleId = item.getSale().getId();
            Long productId = item.getProduct().getId();

            recoverStock(saleId, productId);
        }
        log.info("Startup stock recovery completed");
    }
}
