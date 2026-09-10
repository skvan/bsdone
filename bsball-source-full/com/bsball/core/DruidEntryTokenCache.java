/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.DruidEntryTokenCache
 *  com.github.benmanes.caffeine.cache.Cache
 *  com.github.benmanes.caffeine.cache.Caffeine
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.stereotype.Component
 */
package com.bsball.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name={"spring.datasource.druid.stat-view-servlet.enabled"}, havingValue="true")
public class DruidEntryTokenCache {
    private final Cache<String, Boolean> tokens = Caffeine.newBuilder().expireAfterWrite(60L, TimeUnit.SECONDS).maximumSize(1000L).build();

    public void put(String token) {
        if (token != null && !token.isBlank()) {
            this.tokens.put((Object)token, (Object)Boolean.TRUE);
        }
    }

    public boolean consume(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Boolean present = (Boolean)this.tokens.getIfPresent((Object)token);
        if (Boolean.TRUE.equals(present)) {
            this.tokens.invalidate((Object)token);
            return true;
        }
        return false;
    }
}

