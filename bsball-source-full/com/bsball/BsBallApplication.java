/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.BsBallApplication
 *  com.bsball.config.InitSeedProperties
 *  com.bsball.config.JwtProperties
 *  com.bsball.config.OpenPlatformProperties
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.MysqlDatabaseInitializer
 *  com.bsball.core.PostgresDatabaseInitializer
 *  com.bsball.core.WorkspaceInitializer
 *  org.mybatis.spring.annotation.MapperScan
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.boot.context.properties.EnableConfigurationProperties
 *  org.springframework.context.ApplicationContextInitializer
 *  org.springframework.scheduling.annotation.EnableAsync
 *  org.springframework.scheduling.annotation.EnableScheduling
 */
package com.bsball;

import com.bsball.config.InitSeedProperties;
import com.bsball.config.JwtProperties;
import com.bsball.config.OpenPlatformProperties;
import com.bsball.config.TenantProperties;
import com.bsball.core.MysqlDatabaseInitializer;
import com.bsball.core.PostgresDatabaseInitializer;
import com.bsball.core.WorkspaceInitializer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(value={JwtProperties.class, TenantProperties.class, OpenPlatformProperties.class, InitSeedProperties.class})
@MapperScan(value={"com.bsball.mapper"})
public class BsBallApplication {
    public static void main(String[] args) {
        for (String arg : args) {
            if (arg == null || !arg.startsWith("--spring.profiles.active=")) continue;
            String profile = arg.substring("--spring.profiles.active=".length()).trim();
            if (profile.isEmpty()) break;
            System.setProperty("spring.profiles.active", profile);
            break;
        }
        PostgresDatabaseInitializer.runBeforeSpring((String[])args);
        MysqlDatabaseInitializer.runBeforeSpring((String[])args);
        SpringApplication app = new SpringApplication(new Class[]{BsBallApplication.class});
        app.addInitializers(new ApplicationContextInitializer[]{new WorkspaceInitializer()});
        app.run(args);
    }
}

