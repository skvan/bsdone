/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.DataSourceSchemaConfigurer
 *  com.zaxxer.hikari.HikariDataSource
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.BeansException
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.beans.factory.config.BeanPostProcessor
 *  org.springframework.stereotype.Component
 */
package com.bsball.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class DataSourceSchemaConfigurer
implements BeanPostProcessor {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(DataSourceSchemaConfigurer.class);
    @Value(value="${spring.jpa.properties.hibernate.default_schema:}")
    private String defaultSchema;

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        HikariDataSource hikari;
        block5: {
            block4: {
                if (!(bean instanceof HikariDataSource)) break block4;
                hikari = (HikariDataSource)bean;
                if (this.defaultSchema != null && !this.defaultSchema.isBlank()) break block5;
            }
            return bean;
        }
        String url = hikari.getJdbcUrl();
        if (url == null || !url.contains("postgresql")) {
            return bean;
        }
        String escaped = this.defaultSchema.replace("\"", "\"\"");
        String initSql = "SET search_path TO \"" + escaped + "\"";
        hikari.setConnectionInitSql(initSql);
        log.info("\u5df2\u4e3a PostgreSQL \u8fde\u63a5\u8bbe\u7f6e search_path: {}", (Object)this.defaultSchema);
        return bean;
    }
}

