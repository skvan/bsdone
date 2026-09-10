/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.utils.Ipv4CidrMatch
 */
package com.bsball.utils;

/*
 * Exception performing whole class analysis ignored.
 */
public final class Ipv4CidrMatch {
    private Ipv4CidrMatch() {
    }

    public static boolean matches(String cidrOrSingle, String clientIp) {
        if (cidrOrSingle == null || clientIp == null) {
            return false;
        }
        String rule = cidrOrSingle.trim();
        String ip = clientIp.trim();
        if (rule.isEmpty() || ip.isEmpty()) {
            return false;
        }
        try {
            if (!rule.contains("/")) {
                return rule.equals(ip);
            }
            String[] parts = rule.split("/", 2);
            int prefix = Integer.parseInt(parts[1].trim());
            if (prefix < 0 || prefix > 32) {
                return false;
            }
            long network = Ipv4CidrMatch.parseIpv4ToLong((String)parts[0].trim());
            long host = Ipv4CidrMatch.parseIpv4ToLong((String)ip);
            long mask = Ipv4CidrMatch.cidrMask32((int)prefix);
            return (network & mask) == (host & mask);
        }
        catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isValidRule(String cidrOrSingle) {
        if (cidrOrSingle == null || cidrOrSingle.isBlank()) {
            return false;
        }
        String s = cidrOrSingle.trim();
        try {
            if (!s.contains("/")) {
                Ipv4CidrMatch.parseIpv4ToLong((String)s);
                return true;
            }
            String[] parts = s.split("/", 2);
            Ipv4CidrMatch.parseIpv4ToLong((String)parts[0].trim());
            int p = Integer.parseInt(parts[1].trim());
            return p >= 0 && p <= 32;
        }
        catch (Exception e) {
            return false;
        }
    }

    private static long cidrMask32(int prefixLen) {
        if (prefixLen <= 0) {
            return 0L;
        }
        if (prefixLen >= 32) {
            return 0xFFFFFFFFL;
        }
        return 0xFFFFFFFFL << 32 - prefixLen;
    }

    private static long parseIpv4ToLong(String dotted) {
        String[] oct = dotted.split("\\.");
        if (oct.length != 4) {
            throw new IllegalArgumentException();
        }
        int a = Integer.parseInt(oct[0]);
        int b = Integer.parseInt(oct[1]);
        int c = Integer.parseInt(oct[2]);
        int d = Integer.parseInt(oct[3]);
        if (a < 0 || a > 255 || b < 0 || b > 255 || c < 0 || c > 255 || d < 0 || d > 255) {
            throw new IllegalArgumentException();
        }
        return (long)(a & 0xFF) << 24 | (long)(b & 0xFF) << 16 | (long)(c & 0xFF) << 8 | (long)(d & 0xFF);
    }
}

