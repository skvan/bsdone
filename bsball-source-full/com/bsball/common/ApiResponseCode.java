/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.ApiResponseCode
 */
package com.bsball.common;

public final class ApiResponseCode {
    public static final int OK = 200;
    public static final int OK_EMPTY = 1000;

    private ApiResponseCode() {
    }

    public static boolean isSuccessFamily(int code) {
        return code == 200 || code >= 1000 && code < 2000;
    }
}

