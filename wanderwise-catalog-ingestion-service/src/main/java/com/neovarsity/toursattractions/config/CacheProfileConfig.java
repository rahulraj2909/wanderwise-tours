package com.neovarsity.toursattractions.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class CacheProfileConfig {
    // RedisCacheManager provided by RedisConfig when prod profile uses redis
}
