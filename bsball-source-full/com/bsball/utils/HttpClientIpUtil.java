/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.utils.HttpClientIpUtil
 *  jakarta.servlet.http.HttpServletRequest
 */
package com.bsball.utils;

import jakarta.servlet.http.HttpServletRequest;

/*
 * Exception performing whole class analysis ignored.
 */
public final class HttpClientIpUtil {
    private HttpClientIpUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        return HttpClientIpUtil.getClientIp((HttpServletRequest)request, (boolean)true);
    }

    public static String getClientIp(HttpServletRequest request, boolean trustXForwardedFor) {
        String xff;
        if (request == null) {
            return "unknown";
        }
        if (trustXForwardedFor && (xff = request.getHeader("X-Forwarded-For")) != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            return HttpClientIpUtil.truncate((String)first, (int)128);
        }
        String addr = request.getRemoteAddr();
        return HttpClientIpUtil.truncate((String)addr, (int)128);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "unknown";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}

