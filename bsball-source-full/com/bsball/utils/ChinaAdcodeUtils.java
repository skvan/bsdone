/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.utils.ChinaAdcodeUtils
 */
package com.bsball.utils;

public final class ChinaAdcodeUtils {
    private static final int ADCODE_MAX_LEN = 12;

    private ChinaAdcodeUtils() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        int len = s.length();
        if (len == 2) {
            return s + "0000";
        }
        if (len == 4) {
            return s + "00";
        }
        if (len == 6) {
            return s;
        }
        if (len > 6) {
            return len <= 12 ? s : s.substring(0, 12);
        }
        return String.format("%6s", s).replace(' ', '0');
    }
}

