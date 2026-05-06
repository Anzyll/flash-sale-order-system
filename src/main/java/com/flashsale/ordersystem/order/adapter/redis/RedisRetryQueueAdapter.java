package com.flashsale.ordersystem.order.adapter.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.ordersystem.order.port.ProducerRetryQueuePort;
import com.flashsale.ordersystem.shared.exception.ErrorCode;
import com.flashsale.ordersystem.shared.exception.InfrastructureException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisRetryQueueAdapter implements ProducerRetryQueuePort {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String RETRY_KEY = "order:producer:retry";
    private static final String DLQ_KEY = "order:producer:dlq";

    @Override
    public void push(RetryEvent event) {
        try{
            String json = objectMapper.writeValueAsString(event);
            stringRedisTemplate.opsForList().rightPush(RETRY_KEY,json);
        } catch (JsonProcessingException e) {
            throw new InfrastructureException(ErrorCode.REDIS_SERIALIZATION_ERROR);
        }

    }

    @Override
    public RetryEvent pop() {
        String json = stringRedisTemplate.opsForList().leftPop(RETRY_KEY);
        if (json == null) return null;
        try{
          return   objectMapper.readValue(json,RetryEvent.class);
        }
        catch (Exception e){
            throw  new InfrastructureException(ErrorCode.REDIS_DESERIALIZATION_ERROR);
        }

    }

    public void pushToDLQ(RetryEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            stringRedisTemplate.opsForList().rightPush(DLQ_KEY,json);
        }
        catch (Exception e){
            throw  new InfrastructureException(ErrorCode.REDIS_EXECUTION_FAILED);
        }

    }

    @Override
    public RetryEvent popFromDlq() {
        String json = stringRedisTemplate.opsForList().leftPop(DLQ_KEY);
        if (json == null) return null;
        try{
            return   objectMapper.readValue(json,RetryEvent.class);
        }
        catch (Exception e){
            throw  new InfrastructureException(ErrorCode.REDIS_DESERIALIZATION_ERROR);
        }

    }

}
