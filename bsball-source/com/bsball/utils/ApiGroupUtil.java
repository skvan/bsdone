/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.utils.ApiGroupUtil
 */
package com.bsball.utils;

public final class ApiGroupUtil {
    private ApiGroupUtil() {
    }

    public static String inferGroup(String path) {
        if (path == null) {
            return "\u672a\u5206\u7ec4";
        }
        if (path.contains("/auth/")) {
            return "\u8ba4\u8bc1";
        }
        if (path.contains("/sys/user")) {
            return "\u7528\u6237";
        }
        if (path.contains("/sys/role")) {
            return "\u89d2\u8272";
        }
        if (path.contains("/sys/menu")) {
            return "\u83dc\u5355";
        }
        if (path.contains("/sys/api")) {
            return "API";
        }
        if (path.contains("/sys/dict")) {
            return "\u5b57\u5178";
        }
        if (path.contains("/sys/article")) {
            return "\u6587\u7ae0";
        }
        if (path.contains("/sys/notice")) {
            return "\u901a\u77e5";
        }
        if (path.contains("/sys/resource")) {
            return "\u8d44\u6e90";
        }
        if (path.contains("/login-log")) {
            return "\u767b\u5f55\u5386\u53f2";
        }
        if (path.contains("/operation-log")) {
            return "\u64cd\u4f5c\u5386\u53f2";
        }
        if (path.contains("/sys/config")) {
            return "\u914d\u7f6e";
        }
        if (path.contains("/sys/ip-location-cache")) {
            return "IP\u5730\u7406\u4fe1\u606f";
        }
        if (path.contains("/sys/portal/devtools-report") || path.contains("/sys/portal/visit-hit") || path.contains("/sys/portal/ip-location")) {
            return "\u7cfb\u7edf\u76d1\u63a7";
        }
        if (path.contains("/monitor")) {
            return "\u7cfb\u7edf\u76d1\u63a7";
        }
        if (path.contains("/league")) {
            return "\u8054\u76df";
        }
        if (path.contains("/team")) {
            return "\u7403\u961f";
        }
        if (path.contains("/event")) {
            return "\u8d5b\u4e8b";
        }
        if (path.contains("/game")) {
            return "\u6bd4\u8d5b";
        }
        if (path.contains("/stats")) {
            return "\u6570\u636e\u6392\u884c";
        }
        if (path.contains("/player")) {
            return "\u7403\u5458";
        }
        if (path.contains("/portal/")) {
            return "\u95e8\u6237";
        }
        if (path.contains("/portal-visit")) {
            return "\u95e8\u6237\u7edf\u8ba1";
        }
        return "\u672a\u5206\u7ec4";
    }
}

