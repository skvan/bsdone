/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.IdsConverter
 *  com.bsball.common.json.IdsDeserializer
 *  com.fasterxml.jackson.core.JsonParser
 *  com.fasterxml.jackson.core.JsonToken
 *  com.fasterxml.jackson.databind.DeserializationContext
 *  com.fasterxml.jackson.databind.JsonDeserializer
 */
package com.bsball.common.json;

import com.bsball.common.IdsConverter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.ArrayList;

public class IdsDeserializer
extends JsonDeserializer<String> {
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ArrayList<Long> ids = new ArrayList<Long>();
        if (p.isExpectedStartArrayToken()) {
            while (p.nextToken() != JsonToken.END_ARRAY) {
                ids.add(p.getLongValue());
            }
        } else {
            if (p.hasToken(JsonToken.VALUE_STRING)) {
                return p.getText();
            }
            if (p.hasToken(JsonToken.VALUE_NULL)) {
                return "";
            }
            if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                ids.add(p.getLongValue());
            }
        }
        return IdsConverter.toIdsString(ids);
    }
}

