/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.PlayerOptionDto
 */
package com.bsball.model.dto;

public record PlayerOptionDto(Long id, String name, String number, Long teamId) {
    private final Long id;
    private final String name;
    private final String number;
    private final Long teamId;

    public PlayerOptionDto(Long id, String name, String number, Long teamId) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.teamId = teamId;
    }

    public Long id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public String number() {
        return this.number;
    }

    public Long teamId() {
        return this.teamId;
    }
}

