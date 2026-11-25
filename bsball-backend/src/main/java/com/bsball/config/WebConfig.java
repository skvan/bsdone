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
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig
implements WebMvcConfigurer {
    private final Path uploadRootPath;

    /**
     * 报错/回显文案用的请求语言（#713 那一轮）：优先 X-UI-Locale，退回 Accept-Language，
     * 两边都没说时用简体中文。默认值必须显式给：不给时父类会拿 {@code Locale.getDefault()}，
     * 也就是 JVM 的系统语言，同一句报错会随部署机器变。
     */
    @Bean
    public LocaleResolver localeResolver() {
        UiLocaleResolver resolver = new UiLocaleResolver();
        resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        return resolver;
    }

    public WebConfig(Path uploadRootPath) {
        this.uploadRootPath = uploadRootPath;
    }

    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = this.uploadRootPath.toUri().toASCIIString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(new String[]{"/files/**"}).addResourceLocations(new String[]{location}).setCacheControl(CacheControl.maxAge((long)30L, (TimeUnit)TimeUnit.DAYS).cachePublic()).resourceChain(true);
    }

    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOriginPatterns(new String[]{"*"}).allowedMethods(new String[]{"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"}).allowedHeaders(new String[]{"*"}).allowCredentials(true);
    }
}

