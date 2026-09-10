/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.service.iplocation.IpRegionTextFormatter
 */
package com.bsball.service.iplocation;

public final class IpRegionTextFormatter {
    private IpRegionTextFormatter() {
    }

    public static String format(String province, String city) {
        String c;
        String p = province == null ? "" : province.trim();
        String string = c = city == null ? "" : city.trim();
        if (p.isEmpty() && c.isEmpty()) {
            return "";
        }
        if (p.equals(c)) {
            return p;
        }
        if (p.isEmpty()) {
            return c;
        }
        if (c.isEmpty()) {
            return p;
        }
        if (c.startsWith(p) || p.startsWith(c)) {
            return c.length() >= p.length() ? c : p;
        }
        return p + c;
    }
}

