/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.WebConfig
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.http.CacheControl
 *  org.springframework.web.servlet.config.annotation.CorsRegistry
 *  org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
 *  org.springframework.web.servlet.config.annotation.WebMvcConfigurer
 */
package com.bsball.config;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig
implements WebMvcConfigurer {
    private final Path uploadRootPath;

    public WebConfig(Path uploadRootPath) {
        this.uploadRootPath = uploadRootPath;
    }

    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Object location = this.uploadRootPath.toUri().toASCIIString();
        if (!((String)location).endsWith("/")) {
            location = (String)location + "/";
        }
        registry.addResourceHandler(new String[]{"/files/**"}).addResourceLocations(new String[]{location}).setCacheControl(CacheControl.maxAge((long)30L, (TimeUnit)TimeUnit.DAYS).cachePublic()).resourceChain(true);
    }

    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOriginPatterns(new String[]{"*"}).allowedMethods(new String[]{"GET", "POST", "PUT", "DELETE", "OPTIONS"}).allowedHeaders(new String[]{"*"}).allowCredentials(true);
    }
}

