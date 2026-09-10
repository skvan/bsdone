/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.BoolToIntDeserializer
 *  com.fasterxml.jackson.core.JsonParser
 *  com.fasterxml.jackson.core.JsonToken
 *  com.fasterxml.jackson.databind.DeserializationContext
 *  com.fasterxml.jackson.databind.JsonDeserializer
 */
package com.bsball.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class BoolToIntDeserializer
extends JsonDeserializer<Integer> {
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_TRUE) {
            return 1;
        }
        if (t == JsonToken.VALUE_FALSE) {
            return 0;
        }
        if (t == JsonToken.VALUE_NULL) {
            return null;
        }
        if (t == JsonToken.VALUE_NUMBER_INT) {
            return p.getIntValue();
        }
        if (t == JsonToken.VALUE_NUMBER_FLOAT) {
            double d = p.getDoubleValue();
            if (d == 0.0) {
                return 0;
            }
            if (d == 1.0) {
                return 1;
            }
            return (int)Math.round(d);
        }
        if (t == JsonToken.VALUE_STRING) {
            String s = p.getValueAsString();
            if (s == null) {
                return null;
            }
            if ((s = s.trim()).isEmpty()) {
                return null;
            }
            if ("0".equals(s) || "false".equalsIgnoreCase(s)) {
                return 0;
            }
            if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
                return 1;
            }
            try {
                return Integer.parseInt(s);
            }
            catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}

