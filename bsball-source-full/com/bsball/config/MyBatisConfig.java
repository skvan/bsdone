/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.MyBatisConfig
 *  com.bsball.mybatis.CompactSqlLoggingInterceptor
 *  org.apache.ibatis.mapping.DatabaseIdProvider
 *  org.apache.ibatis.mapping.VendorDatabaseIdProvider
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.context.annotation.Profile
 */
package com.bsball.config;

import com.bsball.mybatis.CompactSqlLoggingInterceptor;
import java.util.Properties;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class MyBatisConfig {
    @Bean
    @Profile(value={"dev"})
    public CompactSqlLoggingInterceptor compactSqlLoggingInterceptor() {
        return new CompactSqlLoggingInterceptor();
    }

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        Properties props = new Properties();
        props.setProperty("PostgreSQL", "PostgreSQL");
        props.setProperty("MySQL", "MySQL");
        props.setProperty("SQLite", "SQLite");
        props.setProperty("H2", "H2");
        provider.setProperties(props);
        return provider;
    }
}

