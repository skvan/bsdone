/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.GuestPublicApiHolder
 */
package com.bsball.core;

public final class GuestPublicApiHolder {
    private static final ThreadLocal<Boolean> GUEST_LIKE_READ = new ThreadLocal();

    private GuestPublicApiHolder() {
    }

    public static void setGuestLikeRead(boolean guestLikeRead) {
        if (guestLikeRead) {
            GUEST_LIKE_READ.set(Boolean.TRUE);
        } else {
            GUEST_LIKE_READ.remove();
        }
    }

    public static boolean isGuestLikeRead() {
        return Boolean.TRUE.equals(GUEST_LIKE_READ.get());
    }

    public static void clear() {
        GUEST_LIKE_READ.remove();
    }
}

