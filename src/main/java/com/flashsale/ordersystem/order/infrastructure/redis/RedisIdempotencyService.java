package com.flashsale.ordersystem.order.infrastructure.redis;

import com.flashsale.ordersystem.order.application.port.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisIdempotencyService implements IdempotencyService {
    private final StringRedisTemplate redisTemplate;
    @Override
    public boolean tryAcquire(Long userId, Long saleId, Long productId) {

        String key = "purchase:%d:%d:%d".formatted(userId,saleId,productId);
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key,"1",5, TimeUnit.MINUTES);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public Boolean release(Long userId, Long saleId, Long productId) {
        String key = "purchase:%d:%d:%d".formatted(userId,saleId,productId);
        return redisTemplate.delete(key);
    }
}
