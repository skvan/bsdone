/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.json.PositionsDeserializer
 *  com.fasterxml.jackson.core.JsonParser
 *  com.fasterxml.jackson.core.JsonToken
 *  com.fasterxml.jackson.databind.DeserializationContext
 *  com.fasterxml.jackson.databind.JsonDeserializer
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.bsball.common.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;

public class PositionsDeserializer
extends JsonDeserializer<String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_STRING) {
            return p.getText();
        }
        if (t == JsonToken.START_ARRAY) {
            ArrayList<String> list = new ArrayList<String>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                if (p.getCurrentToken() != JsonToken.VALUE_STRING) continue;
                list.add(p.getText());
            }
            if (list.isEmpty()) {
                return null;
            }
            return MAPPER.writeValueAsString(list);
        }
        return null;
    }
}

