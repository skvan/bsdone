/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.TeamOptionDto
 */
package com.bsball.model.dto;

public record TeamOptionDto(Long id, String name, String shortName, String logo) {
    private final Long id;
    private final String name;
    private final String shortName;
    private final String logo;

    public TeamOptionDto(Long id, String name, String shortName, String logo) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.logo = logo;
    }

    public Long id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public String shortName() {
        return this.shortName;
    }

    public String logo() {
        return this.logo;
    }
}

