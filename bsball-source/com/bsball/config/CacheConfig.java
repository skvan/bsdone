/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.CacheConfig
 *  com.github.benmanes.caffeine.cache.Caffeine
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.cache.CacheManager
 *  org.springframework.cache.annotation.EnableCaching
 *  org.springframework.cache.caffeine.CaffeineCacheManager
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package com.bsball.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@ConditionalOnProperty(prefix="app.cache", name={"type"}, havingValue="local", matchIfMissing=true)
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(Arrays.asList("config", "sys_dict"));
        manager.setCaffeine(Caffeine.newBuilder().maximumSize(1000L).expireAfterWrite(10L, TimeUnit.MINUTES).recordStats());
        return manager;
    }
}

