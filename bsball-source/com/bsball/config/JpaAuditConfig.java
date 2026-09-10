/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.JpaAuditConfig
 *  com.bsball.core.CurrentUserHolder
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.data.domain.AuditorAware
 *  org.springframework.data.jpa.repository.config.EnableJpaAuditing
 */
package com.bsball.config;

import com.bsball.core.CurrentUserHolder;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef="auditorProvider")
public class JpaAuditConfig {
    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> Optional.ofNullable(CurrentUserHolder.get());
    }
}

