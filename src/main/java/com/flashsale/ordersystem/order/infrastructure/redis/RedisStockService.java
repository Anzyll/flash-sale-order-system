package com.flashsale.ordersystem.order.infrastructure.redis;

import com.flashsale.ordersystem.common.exception.CustomException;
import com.flashsale.ordersystem.common.exception.ErrorCode;
import com.flashsale.ordersystem.common.exception.InfrastructureException;
import com.flashsale.ordersystem.order.application.port.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RedisStockService implements StockService {
    private final StringRedisTemplate redisTemplate;
    private static final String LUA_SCRIPT = """
        local stock = tonumber(redis.call('GET', KEYS[1]))
        local qty = tonumber(ARGV[1])

        if qty == nil or qty <= 0 then
           return -3
        end

        if stock == nil then
           return -2
        end

        if stock >= qty then
           return redis.call('DECRBY', KEYS[1], qty)
        else
           return -1
        end
 """;

    private static final long OUT_OF_STOCK = -1;
    private static final long NOT_INITIALIZED = -2;
    private static final long INVALID_QTY = -3;
    private static final DefaultRedisScript<Long> SCRIPT;

    static {
        SCRIPT = new DefaultRedisScript<>();
        SCRIPT.setScriptText(LUA_SCRIPT);
        SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean decrement(Long saleId, Long productId, int quantity) {
        String key = "stock:%d:%d".formatted(saleId, productId);
        Long result =  redisTemplate.execute(
                SCRIPT,
                Collections.singletonList(key),
                String.valueOf(quantity)
        );
        if(result == null){
            throw  new InfrastructureException(ErrorCode.REDIS_EXECUTION_FAILED);
        }
        if( result == OUT_OF_STOCK){
            return false;
        }
        if(result==NOT_INITIALIZED){
            throw new InfrastructureException(ErrorCode.STOCK_NOT_INITIALIZED);
        }
        if (result==INVALID_QTY){
            throw new CustomException(ErrorCode.INVALID_QUANTITY);
        }
        return true;
    }
}
