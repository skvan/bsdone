/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.HealthApi
 *  com.bsball.common.Result
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.Result;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/health"})
public class HealthApi {
    @Value(value="${spring.application.name}")
    private String serviceName;

    @GetMapping
    public Result<Map<String, Object>> health() {
        return Result.ok((Object)Map.of((Object)"status", (Object)"ok", (Object)"service", (Object)this.serviceName));
    }
}

