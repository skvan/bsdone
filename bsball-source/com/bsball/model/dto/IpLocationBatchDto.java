/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.IpLocationBatchDto
 */
package com.bsball.model.dto;

import java.util.Map;

public record IpLocationBatchDto(Map<String, String> locations) {
    private final Map<String, String> locations;

    public IpLocationBatchDto(Map<String, String> locations) {
        this.locations = locations;
    }

    public Map<String, String> locations() {
        return this.locations;
    }
}

