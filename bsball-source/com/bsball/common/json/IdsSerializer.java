/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.IdsConverter
 *  com.bsball.common.json.IdsSerializer
 *  com.fasterxml.jackson.core.JsonGenerator
 *  com.fasterxml.jackson.databind.JsonSerializer
 *  com.fasterxml.jackson.databind.SerializerProvider
 */
package com.bsball.common.json;

import com.bsball.common.IdsConverter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.List;

public class IdsSerializer
extends JsonSerializer<String> {
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        List ids = IdsConverter.parseIds((String)value);
        gen.writeObject((Object)ids);
    }
}

