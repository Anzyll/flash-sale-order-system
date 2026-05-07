package com.flashsale.ordersystem.order.adapter.redis;

import com.flashsale.ordersystem.shared.exception.BusinessException;
import com.flashsale.ordersystem.shared.exception.ErrorCode;
import com.flashsale.ordersystem.shared.exception.InfrastructureException;
import com.flashsale.ordersystem.shared.port.StockReservationPort;
import com.flashsale.ordersystem.shared.port.SaleStockPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisStockAdapter implements StockReservationPort, SaleStockPort {
    private final StringRedisTemplate redisTemplate;
    private  final DefaultRedisScript<Long> purchaseScript;
    private static final long OUT_OF_STOCK = -1;
    private static final long NOT_INITIALIZED = -2;
    private static final long INVALID_QTY = -3;
    private static final long ALREADY_PURCHASED = -4;


    @Override
    public boolean tryPurchase(String  userId, Long saleId, Long productId, int quantity) {
        String stock_key = "stock:%d:%d".formatted(saleId, productId);
        String purchase_reserved_key = "purchase_reserved:%s:%d:%d".formatted(userId, saleId, productId);
        Long result = redisTemplate.execute(
                purchaseScript,
                List.of(stock_key, purchase_reserved_key),
                String.valueOf(quantity)
        );
        if (result == null) {
            log.error("Redis increment failed during revert. saleId={}, productId={}",
                    saleId, productId);
            throw new InfrastructureException(ErrorCode.REDIS_EXECUTION_FAILED);
        }
        if (result == OUT_OF_STOCK) {
            return false;
        }
        if (result == NOT_INITIALIZED) {
            log.error("Stock recovery failed. saleId={}, productId={}",
                    saleId, productId);
            throw new InfrastructureException(ErrorCode.STOCK_NOT_INITIALIZED);
        }
        if (result == INVALID_QTY) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
        if (result == ALREADY_PURCHASED) {
            throw new BusinessException(ErrorCode.ALREADY_PURCHASED);
        }
        return true;
    }

    @Override
    public void revertPurchase(String userId,Long saleId, Long productId, int quantity) {
        log.warn("Reverting stock. userId={}, saleId={}, productId={}, quantity={}",
                userId, saleId, productId, quantity);
        String stockKey = "stock:%d:%d".formatted(saleId, productId);
        String purchaseKey = "purchase_reserved:%s:%d:%d".formatted(userId, saleId, productId);
        Long result = redisTemplate.opsForValue().increment(stockKey,quantity);
        if (result==null){
            throw new InfrastructureException(ErrorCode.REDIS_EXECUTION_FAILED);
        }
        redisTemplate.delete(purchaseKey);
    }

    @Override
    public void recoverStock(Long saleId,
                             Long productId,
                             int remainingStock,
                             long ttlSeconds) {
        log.warn("Stock recovery started. saleId={}, productId={}",
                saleId, productId);
        ttlSeconds = Math.max(ttlSeconds, 60);
        int retries = 10;
        int waitMs = 20;

        String lockKey = "recover_lock:%d:%d".formatted(saleId, productId);
        String stockKey = "stock:%d:%d".formatted(saleId, productId);
        Boolean isOwner = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
        boolean owner = Boolean.TRUE.equals(isOwner);

        try {
        if (!owner) {
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
            log.info("Recovery lock acquired. saleId={}, productId={}",
                    saleId, productId);

        redisTemplate.opsForValue().set(
                stockKey,
                String.valueOf(remainingStock),
                Duration.ofSeconds(ttlSeconds)
        );

        redisTemplate.opsForValue()
                .set("sale_active:" + saleId, "true", Duration.ofSeconds(ttlSeconds));

        log.info("Stock recovered. saleId={}, productId={}, remaining={}, ttl={}",
                    saleId, productId, remainingStock, ttlSeconds);
        } finally {
            if (owner) {
                redisTemplate.delete(lockKey);
            }
        }
        }

    @Override
    public void initializeSaleStock(Long saleId, Long productId, int stock, long ttlSeconds) {
        ttlSeconds = Math.max(ttlSeconds, 60);
        String key = "stock:"+saleId+":"+productId;
        redisTemplate.opsForValue().set(
                key,
                String.valueOf(stock),
                Duration.ofSeconds(ttlSeconds)
        );
    }

    @Override
    public void activateSale(Long saleId, long ttlSeconds) {
        ttlSeconds = Math.max(ttlSeconds, 60);
        redisTemplate.opsForValue().set(
                "sale_active:"+saleId,
                "true",
                Duration.ofSeconds(ttlSeconds)
        );
    }

    @Override
    public void deactivateSale(Long saleId, Long productId) {
        String key = "stock:" + saleId + ":" + productId;
        redisTemplate.delete(key);
    }

    @Override
    public void deactivateSale(Long saleId) {
        redisTemplate.delete("sale_active:"+saleId);
    }


    @Override
    public boolean isSaleActive(Long saleId) {
        String value = redisTemplate.opsForValue().get("sale_active:" + saleId);
        return "true".equals(value);
    }

    @Override
    public void confirmPurchase(String userId, Long saleId, Long productId) {
        String reservedKey = "purchase_reserved:%s:%d:%d"
                .formatted(userId, saleId, productId);
        String doneKey = "purchase_done:%s:%d:%d"
                .formatted(userId, saleId, productId);
        redisTemplate.delete(reservedKey);
        redisTemplate.opsForValue().set(doneKey, "1");
    }

    @Override
    public int getAvailableStock(Long saleId, Long productId) {
        String stockKey = "stock:%d:%d".formatted(saleId,productId);
        String stockValue = redisTemplate.opsForValue().get(stockKey);
        if (stockValue == null) {
            return 0;
        }
        return Integer.parseInt(stockValue);
    }


    public void waitForStock(Long saleId, Long productId) {
        String stockKey = "stock:%d:%d".formatted(saleId, productId);

        log.warn("Waiting for stock initialization. saleId={}, productId={}",
                saleId, productId);
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
}
