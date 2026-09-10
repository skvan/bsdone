/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.exception.BusinessException
 *  com.bsball.util.TenantCodeValidator
 */
package com.bsball.util;

import com.bsball.exception.BusinessException;
import java.util.Locale;
import java.util.Set;

public final class TenantCodeValidator {
    private static final Set<String> RESERVED = Set.of((Object[])new String[]{"admin", "user", "users", "default", "api", "assets", "src", "files", "www", "static", "public", "server", "health", "sys", "system", "favicon.ico", "root", "null", "undefined", "portal", "docs", "login", "logout", "auth", "oauth", "index", "404"});

    private TenantCodeValidator() {
    }

    public static void validateNewCode(String trimmedCode) {
        String key = trimmedCode.toLowerCase(Locale.ROOT);
        if (RESERVED.contains(key)) {
            throw new BusinessException(400, "\u79df\u6237\u7f16\u7801\u4e0d\u80fd\u4f7f\u7528\u4fdd\u7559\u5b57\uff1a" + trimmedCode);
        }
    }
}

