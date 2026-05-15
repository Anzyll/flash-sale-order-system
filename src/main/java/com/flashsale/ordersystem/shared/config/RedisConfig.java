package com.flashsale.ordersystem.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;
@Configuration
public class RedisConfig {
    @Bean
    public DefaultRedisScript<Long> purchaseScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/stock_purchase.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public ProxyManager<String> proxyManager() {

        RedisClient redisClient =
                RedisClient.create("redis://localhost:6379");

        StatefulRedisConnection<String, byte[]> connection =
                redisClient.connect(
                        io.lettuce.core.codec.RedisCodec.of(
                                StringCodec.UTF8,
                                ByteArrayCodec.INSTANCE
                        )
                );

        return LettuceBasedProxyManager.builderFor(connection)
                .build();
    }
}
