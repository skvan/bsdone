/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.json.PositionsSerializer
 *  com.fasterxml.jackson.core.JsonGenerator
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.JsonSerializer
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.SerializerProvider
 */
package com.bsball.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.List;

public class PositionsSerializer
extends JsonSerializer<String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.isBlank()) {
            gen.writeNull();
            return;
        }
        try {
            List list = (List)MAPPER.readValue(value, (TypeReference)new /* Unavailable Anonymous Inner Class!! */);
            gen.writeStartArray();
            for (String s : list) {
                gen.writeString(s);
            }
            gen.writeEndArray();
        }
        catch (Exception e) {
            gen.writeString(value);
        }
    }
}

