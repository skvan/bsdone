/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.CoachOptionDto
 */
package com.bsball.model.dto;

public record CoachOptionDto(Long id, String name, Long teamId) {
    private final Long id;
    private final String name;
    private final Long teamId;

    public CoachOptionDto(Long id, String name, Long teamId) {
        this.id = id;
        this.name = name;
        this.teamId = teamId;
    }

    public Long id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public Long teamId() {
        return this.teamId;
    }
}

