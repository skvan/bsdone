/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.json.HideMenuDeserializer
 *  com.fasterxml.jackson.core.JsonParser
 *  com.fasterxml.jackson.databind.DeserializationContext
 *  com.fasterxml.jackson.databind.JsonDeserializer
 *  com.fasterxml.jackson.databind.JsonNode
 */
package com.bsball.common.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public class HideMenuDeserializer
extends JsonDeserializer<Integer> {
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = (JsonNode)p.getCodec().readTree(p);
        if (node == null || node.isNull()) {
            return 0;
        }
        if (node.isBoolean()) {
            return node.booleanValue() ? 1 : 0;
        }
        if (node.isNumber()) {
            return node.intValue() != 0 ? 1 : 0;
        }
        String s = node.asText("");
        if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
            return 1;
        }
        return 0;
    }
}

