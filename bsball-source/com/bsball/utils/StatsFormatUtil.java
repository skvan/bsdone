/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.utils.StatsFormatUtil
 */
package com.bsball.utils;

/*
 * Exception performing whole class analysis ignored.
 */
public final class StatsFormatUtil {
    public static String fmtAvg(double avg) {
        return String.format("%.3f", avg).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    public static String fmtDecimal(double val, int decimals) {
        if (Double.isNaN(val) || Double.isInfinite(val)) {
            return "0";
        }
        String s = String.format("%." + decimals + "f", val);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    public static String fmtPct(Double pct) {
        if (pct == null || Double.isNaN(pct)) {
            return "-";
        }
        return String.format("%.1f%%", pct);
    }

    public static int baseballIpToOuts(double ip) {
        if (ip < 0.0 || Double.isNaN(ip) || Double.isInfinite(ip)) {
            return 0;
        }
        double ipAdj = ip + 1.0E-10;
        int innings = (int)Math.floor(ipAdj);
        double sub = ipAdj - (double)innings;
        if (sub < 1.0E-10) {
            return innings * 3;
        }
        long digit = Math.round(sub * 10.0);
        if (digit == 3L && sub < 0.31) {
            digit = 2L;
        }
        int inn = innings;
        int d = (int)digit;
        while (d > 2) {
            d -= 3;
            ++inn;
        }
        while (d < 0) {
            d += 3;
            --inn;
        }
        d = Math.min(2, Math.max(0, d));
        return inn * 3 + d;
    }

    public static double baseballOutsToIp(int outs) {
        if (outs <= 0) {
            return 0.0;
        }
        int inn = outs / 3;
        int extra = outs % 3;
        return (double)inn + (double)extra / 10.0;
    }

    public static double normalizeBaseballIp(double ip) {
        return StatsFormatUtil.baseballOutsToIp((int)StatsFormatUtil.baseballIpToOuts((double)ip));
    }

    public static double baseballIpToDecimalInnings(double ip) {
        int outs = StatsFormatUtil.baseballIpToOuts((double)ip);
        return (double)outs / 3.0;
    }
}

