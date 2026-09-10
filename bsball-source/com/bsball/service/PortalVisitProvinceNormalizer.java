/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.service.PortalVisitProvinceNormalizer
 */
package com.bsball.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/*
 * Exception performing whole class analysis ignored.
 */
public final class PortalVisitProvinceNormalizer {
    private static final Set<String> CANONICAL = Set.of((Object[])new String[]{"\u5317\u4eac\u5e02", "\u5929\u6d25\u5e02", "\u6cb3\u5317\u7701", "\u5c71\u897f\u7701", "\u5185\u8499\u53e4\u81ea\u6cbb\u533a", "\u8fbd\u5b81\u7701", "\u5409\u6797\u7701", "\u9ed1\u9f99\u6c5f\u7701", "\u4e0a\u6d77\u5e02", "\u6c5f\u82cf\u7701", "\u6d59\u6c5f\u7701", "\u5b89\u5fbd\u7701", "\u798f\u5efa\u7701", "\u6c5f\u897f\u7701", "\u5c71\u4e1c\u7701", "\u6cb3\u5357\u7701", "\u6e56\u5317\u7701", "\u6e56\u5357\u7701", "\u5e7f\u4e1c\u7701", "\u5e7f\u897f\u58ee\u65cf\u81ea\u6cbb\u533a", "\u6d77\u5357\u7701", "\u91cd\u5e86\u5e02", "\u56db\u5ddd\u7701", "\u8d35\u5dde\u7701", "\u4e91\u5357\u7701", "\u897f\u85cf\u81ea\u6cbb\u533a", "\u9655\u897f\u7701", "\u7518\u8083\u7701", "\u9752\u6d77\u7701", "\u5b81\u590f\u56de\u65cf\u81ea\u6cbb\u533a", "\u65b0\u7586\u7ef4\u543e\u5c14\u81ea\u6cbb\u533a", "\u53f0\u6e7e\u7701", "\u9999\u6e2f\u7279\u522b\u884c\u653f\u533a", "\u6fb3\u95e8\u7279\u522b\u884c\u653f\u533a"});
    private static final Map<String, String> ALIAS = new HashMap();

    private PortalVisitProvinceNormalizer() {
    }

    public static String normalizeForChinaMap(String regionText) {
        if (regionText == null) {
            return null;
        }
        String extracted = PortalVisitProvinceNormalizer.extractProvince((String)regionText.trim());
        if (extracted == null || extracted.isEmpty()) {
            return null;
        }
        if (CANONICAL.contains(extracted)) {
            return extracted;
        }
        String aliased = (String)ALIAS.get(extracted);
        if (aliased != null) {
            return aliased;
        }
        if (CANONICAL.contains(regionText.trim())) {
            return regionText.trim();
        }
        return null;
    }

    private static String extractProvince(String s) {
        if (s.contains("\u9999\u6e2f")) {
            return "\u9999\u6e2f\u7279\u522b\u884c\u653f\u533a";
        }
        if (s.contains("\u6fb3\u95e8")) {
            return "\u6fb3\u95e8\u7279\u522b\u884c\u653f\u533a";
        }
        int i = s.indexOf("\u7279\u522b\u884c\u653f\u533a");
        if (i >= 0) {
            return s.substring(0, Math.min(i + "\u7279\u522b\u884c\u653f\u533a".length(), s.length()));
        }
        i = s.indexOf("\u81ea\u6cbb\u533a");
        if (i >= 0) {
            return s.substring(0, i + "\u81ea\u6cbb\u533a".length());
        }
        i = s.indexOf(30465);
        if (i > 0) {
            return s.substring(0, i + 1);
        }
        if (s.startsWith("\u5317\u4eac")) {
            return "\u5317\u4eac\u5e02";
        }
        if (s.startsWith("\u4e0a\u6d77")) {
            return "\u4e0a\u6d77\u5e02";
        }
        if (s.startsWith("\u5929\u6d25")) {
            return "\u5929\u6d25\u5e02";
        }
        if (s.startsWith("\u91cd\u5e86")) {
            return "\u91cd\u5e86\u5e02";
        }
        if (s.startsWith("\u5185\u8499\u53e4")) {
            return "\u5185\u8499\u53e4\u81ea\u6cbb\u533a";
        }
        if (s.startsWith("\u5e7f\u897f")) {
            return "\u5e7f\u897f\u58ee\u65cf\u81ea\u6cbb\u533a";
        }
        if (s.startsWith("\u5b81\u590f")) {
            return "\u5b81\u590f\u56de\u65cf\u81ea\u6cbb\u533a";
        }
        if (s.startsWith("\u65b0\u7586")) {
            return "\u65b0\u7586\u7ef4\u543e\u5c14\u81ea\u6cbb\u533a";
        }
        if (s.startsWith("\u897f\u85cf")) {
            return "\u897f\u85cf\u81ea\u6cbb\u533a";
        }
        return null;
    }

    static {
        ALIAS.put("\u5317\u4eac", "\u5317\u4eac\u5e02");
        ALIAS.put("\u4e0a\u6d77", "\u4e0a\u6d77\u5e02");
        ALIAS.put("\u5929\u6d25", "\u5929\u6d25\u5e02");
        ALIAS.put("\u91cd\u5e86", "\u91cd\u5e86\u5e02");
        ALIAS.put("\u5185\u8499\u53e4", "\u5185\u8499\u53e4\u81ea\u6cbb\u533a");
        ALIAS.put("\u5e7f\u897f", "\u5e7f\u897f\u58ee\u65cf\u81ea\u6cbb\u533a");
        ALIAS.put("\u897f\u85cf", "\u897f\u85cf\u81ea\u6cbb\u533a");
        ALIAS.put("\u5b81\u590f", "\u5b81\u590f\u56de\u65cf\u81ea\u6cbb\u533a");
        ALIAS.put("\u65b0\u7586", "\u65b0\u7586\u7ef4\u543e\u5c14\u81ea\u6cbb\u533a");
        ALIAS.put("\u9999\u6e2f", "\u9999\u6e2f\u7279\u522b\u884c\u653f\u533a");
        ALIAS.put("\u6fb3\u95e8", "\u6fb3\u95e8\u7279\u522b\u884c\u653f\u533a");
    }
}

