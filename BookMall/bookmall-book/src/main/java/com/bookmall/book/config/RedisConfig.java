package com.bookmall.book.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * Redis缓存配置类
 * SpringCache默认使用JDK序列化，会出现二进制乱码；
 * 这里改成JSON序列化，Redis客户端能看懂存的数据，不需要实体实现Serializable接口
 */
@Configuration // 标记这是配置类，项目启动会执行这个类，向Spring注册Bean
public class RedisConfig {

    /**
     * 自定义缓存管理器cacheManager，覆盖SpringCache默认配置
     * @param factory Redis连接工厂，Spring自动注入，包含redis地址、端口等连接信息
     * @return RedisCacheManager 缓存管理器对象，SpringCache底层靠它操作Redis
     */
    @Bean // 将该方法返回的对象交给Spring容器管理，替换默认的CacheManager
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // 获取默认缓存配置
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 设置统一过期时间，避免缓存永久占用 Redis
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                // 设置value值使用Jackson JSON序列化器
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // 使用连接工厂 + 自定义配置，构建RedisCacheManager
        return RedisCacheManager.builder(factory).cacheDefaults(config).build();
    }
}
