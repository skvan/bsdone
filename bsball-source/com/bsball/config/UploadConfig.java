/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.UploadConfig
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package com.bsball.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UploadConfig {
    @Value(value="${app.workspace:.}")
    private String workspace;
    @Value(value="${app.upload.dir:uploads}")
    private String uploadDir;
    @Value(value="${app.upload.image-extensions:jpg,jpeg,png,gif,webp}")
    private String imageExtensions;
    @Value(value="${app.upload.max-file-size:5242880}")
    private long maxFileSize;

    @Bean
    public Path uploadRootPath() {
        Path base = Paths.get(this.workspace, new String[0]).toAbsolutePath().normalize();
        return base.resolve(this.uploadDir).normalize();
    }

    @Bean
    public Set<String> allowedImageExtensions() {
        return Arrays.stream(this.imageExtensions.split(",")).map(String::trim).map(String::toLowerCase).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    }

    public long getMaxFileSize() {
        return this.maxFileSize;
    }
}

