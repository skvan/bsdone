/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.TenantProperties
 *  com.bsball.model.entity.SysConfig
 *  com.bsball.model.entity.SysConfigId
 *  com.bsball.repository.SysConfigRepository
 *  com.bsball.service.SysConfigService
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  lombok.Generated
 *  org.springframework.cache.annotation.CacheEvict
 *  org.springframework.cache.annotation.Cacheable
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.config.TenantProperties;
import com.bsball.model.entity.SysConfig;
import com.bsball.model.entity.SysConfigId;
import com.bsball.repository.SysConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class SysConfigService {
    private static final String LEGACY_JWT_SECRET_KEY = "jwtSecret";
    private final SysConfigRepository sysConfigRepository;
    private final TenantProperties tenantProperties;
    private final ObjectMapper objectMapper;

    @Cacheable(value={"config"}, key="'t:' + #tenantId", unless="#result == null || #result.isEmpty()")
    public Map<String, Object> getConfig(long tenantId) {
        List all = this.sysConfigRepository.findByTenantId(Long.valueOf(tenantId));
        HashMap<String, Object> map = new HashMap<String, Object>();
        for (SysConfig c : all) {
            if (LEGACY_JWT_SECRET_KEY.equals(c.getConfigKey())) continue;
            map.put(c.getConfigKey(), this.parseValue(c.getConfigValue()));
        }
        return map;
    }

    @CacheEvict(value={"config"}, key="'t:' + #tenantId")
    public void updateConfig(long tenantId, Map<String, Object> updates) {
        for (Map.Entry<String, Object> e : updates.entrySet()) {
            Object val;
            String key = e.getKey();
            if (LEGACY_JWT_SECRET_KEY.equals(key) || (val = e.getValue()) == null) continue;
            SysConfigId id = new SysConfigId();
            id.setTenantId(Long.valueOf(tenantId));
            id.setConfigKey(key);
            SysConfig c = this.sysConfigRepository.findById((Object)id).orElse(new SysConfig());
            c.setTenantId(Long.valueOf(tenantId));
            c.setConfigKey(key);
            c.setConfigValue(this.serializeValue(val));
            this.sysConfigRepository.save((Object)c);
        }
    }

    private String serializeValue(Object val) {
        if (val instanceof String) {
            return (String)val;
        }
        if (val instanceof Boolean || val instanceof Number) {
            return val.toString();
        }
        try {
            return this.objectMapper.writeValueAsString(val);
        }
        catch (JsonProcessingException ex) {
            return val.toString();
        }
    }

    public long effectiveTenantId(Long tenantIdFromContext) {
        return tenantIdFromContext != null ? tenantIdFromContext.longValue() : this.tenantProperties.getDefaultId();
    }

    private Object parseValue(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        if ("true".equalsIgnoreCase(t)) {
            return true;
        }
        if ("false".equalsIgnoreCase(t)) {
            return false;
        }
        if (t.startsWith("[") || t.startsWith("{")) {
            try {
                return this.objectMapper.readValue(t, Object.class);
            }
            catch (JsonProcessingException ignored) {
                return v;
            }
        }
        if (t.matches("-?\\d+")) {
            try {
                return Integer.parseInt(t);
            }
            catch (NumberFormatException ignored) {
                return v;
            }
        }
        return v;
    }

    @Generated
    public SysConfigService(SysConfigRepository sysConfigRepository, TenantProperties tenantProperties, ObjectMapper objectMapper) {
        this.sysConfigRepository = sysConfigRepository;
        this.tenantProperties = tenantProperties;
        this.objectMapper = objectMapper;
    }
}

