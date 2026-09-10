/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.json.PositionsJsonUtil
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.bsball.common.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;

public final class PositionsJsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PositionsJsonUtil() {
    }

    public static List<String> parseList(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<String> list = (List<String>)MAPPER.readValue(raw.trim(), (TypeReference)new /* Unavailable Anonymous Inner Class!! */);
            return list != null ? list : Collections.emptyList();
        }
        catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static String toStorage(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(list);
        }
        catch (Exception e) {
            return null;
        }
    }
}

