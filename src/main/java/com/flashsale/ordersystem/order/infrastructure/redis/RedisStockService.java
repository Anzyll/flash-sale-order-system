package com.flashsale.ordersystem.order.infrastructure.redis;

import com.flashsale.ordersystem.common.exception.CustomException;
import com.flashsale.ordersystem.common.exception.ErrorCode;
import com.flashsale.ordersystem.common.exception.InfrastructureException;
import com.flashsale.ordersystem.order.application.port.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisStockService implements StockService {
    private final StringRedisTemplate redisTemplate;
    private static final String LUA_SCRIPT = """
                   local stock = tonumber(redis.call('GET', KEYS[1]))
                   local purchased = tonumber(redis.call('EXISTS', KEYS[2]))
                   local qty = tonumber(ARGV[1])
                   local ttl = tonumber(ARGV[2])
            
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
                   redis.call('SET',KEYS[2],"1",'EX',ttl)
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
    public boolean processPurchase(String  userId, Long saleId, Long productId, int quantity,long ttl) {
        String stock_key = "stock:%d:%d".formatted(saleId, productId);
        String purchase_done_key = "purchase_done:%s:%d:%d".formatted(userId, saleId, productId);
        Long result = redisTemplate.execute(
                SCRIPT,
                List.of(stock_key, purchase_done_key),
                String.valueOf(quantity),
                String.valueOf(ttl)
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
}
