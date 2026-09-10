/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.AuditColumnOrderingStrategy
 *  com.bsball.config.HibernateConfig
 *  org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package com.bsball.config;

import com.bsball.config.AuditColumnOrderingStrategy;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateConfig {
    @Bean
    public HibernatePropertiesCustomizer columnOrderingStrategyCustomizer() {
        return hibernateProperties -> hibernateProperties.put("hibernate.column_ordering_strategy", new AuditColumnOrderingStrategy());
    }
}

