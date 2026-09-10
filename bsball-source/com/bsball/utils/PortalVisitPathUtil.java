/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.utils.PortalVisitPathUtil
 */
package com.bsball.utils;

public final class PortalVisitPathUtil {
    public static final int PV_PATH_MAX_LEN = 512;

    private PortalVisitPathUtil() {
    }

    public static String normalizeForPv(String path) {
        int q;
        if (path == null) {
            return "";
        }
        String p = path.trim();
        if (p.isEmpty()) {
            return "";
        }
        int h = p.indexOf(35);
        if (h >= 0) {
            p = p.substring(0, h).trim();
        }
        if ((q = p.indexOf(63)) >= 0) {
            p = p.substring(0, q).trim();
        }
        if (p.isEmpty()) {
            return "/";
        }
        return p.length() > 512 ? p.substring(0, 512) : p;
    }
}

