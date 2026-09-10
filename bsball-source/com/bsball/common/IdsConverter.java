/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.IdsConverter
 */
package com.bsball.common;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class IdsConverter {
    private IdsConverter() {
    }

    public static List<Long> parseIds(String s) {
        if (s == null || s.isBlank()) {
            return List.of();
        }
        try {
            String normalized = s.trim();
            if (normalized.startsWith("[")) {
                normalized = normalized.replaceAll("[\\[\\]\"]", "");
            }
            if (normalized.isEmpty()) {
                return List.of();
            }
            return Arrays.stream(normalized.split("[,\uff0c]")).map(String::trim).filter(x -> !x.isEmpty()).map(Long::parseLong).collect(Collectors.toList());
        }
        catch (Exception e) {
            return List.of();
        }
    }

    public static String toIdsString(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return ids.stream().filter(id -> id != null).map(String::valueOf).collect(Collectors.joining(","));
    }
}

