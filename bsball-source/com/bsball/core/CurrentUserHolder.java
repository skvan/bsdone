/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.CurrentUserHolder
 */
package com.bsball.core;

public final class CurrentUserHolder {
    private static final ThreadLocal<Long> USER = new ThreadLocal();
    private static final ThreadLocal<Long> TENANT = new ThreadLocal();

    public static void set(Long userId, Long tenantId) {
        if (userId != null) {
            USER.set(userId);
        } else {
            USER.remove();
        }
        if (tenantId != null) {
            TENANT.set(tenantId);
        } else {
            TENANT.remove();
        }
    }

    public static Long get() {
        return (Long)USER.get();
    }

    public static Long getTenantId() {
        return (Long)TENANT.get();
    }

    public static void clear() {
        USER.remove();
        TENANT.remove();
    }

    private CurrentUserHolder() {
    }
}

