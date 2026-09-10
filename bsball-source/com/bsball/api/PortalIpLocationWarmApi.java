/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.PortalIpLocationWarmApi
 *  com.bsball.common.Result
 *  com.bsball.service.PortalIpLocationWarmService
 *  com.bsball.utils.HttpClientIpUtil
 *  jakarta.servlet.http.HttpServletRequest
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.service.PortalIpLocationWarmService;
import com.bsball.utils.HttpClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * Exception performing whole class analysis ignored.
 */
@RestController
@RequestMapping(value={"/portal/ip-location"})
public class PortalIpLocationWarmApi {
    private static final int IP_MAX = 128;
    private final PortalIpLocationWarmService portalIpLocationWarmService;

    @PostMapping(value={"/warm"})
    public Result<Map<String, Object>> warm(HttpServletRequest request) {
        String ip = PortalIpLocationWarmApi.truncate((String)HttpClientIpUtil.getClientIp((HttpServletRequest)request), (int)128);
        this.portalIpLocationWarmService.warmFromClientIpAsync(ip);
        return Result.ok((Object)Map.of((Object)"accepted", (Object)true));
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    @Generated
    public PortalIpLocationWarmApi(PortalIpLocationWarmService portalIpLocationWarmService) {
        this.portalIpLocationWarmService = portalIpLocationWarmService;
    }
}

