/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.ResolvedTenantHolder
 */
package com.bsball.core;

public final class ResolvedTenantHolder {
    private static final ThreadLocal<Long> TENANT = new ThreadLocal();

    public static void set(Long tenantId) {
        if (tenantId != null) {
            TENANT.set(tenantId);
        } else {
            TENANT.remove();
        }
    }

    public static Long get() {
        return (Long)TENANT.get();
    }

    public static void clear() {
        TENANT.remove();
    }

    private ResolvedTenantHolder() {
    }
}

