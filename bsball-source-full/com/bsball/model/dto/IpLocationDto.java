/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.IpLocationDto
 */
package com.bsball.model.dto;

public record IpLocationDto(String formatted) {
    private final String formatted;

    public IpLocationDto(String formatted) {
        this.formatted = formatted;
    }

    public String formatted() {
        return this.formatted;
    }
}

