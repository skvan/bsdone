/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.TeamPlayerOptionDto
 */
package com.bsball.model.dto;

import java.util.List;

public record TeamPlayerOptionDto(Long id, String name, String number, List<String> positions, String batHand, String throwHand, String status) {
    private final Long id;
    private final String name;
    private final String number;
    private final List<String> positions;
    private final String batHand;
    private final String throwHand;
    private final String status;

    public TeamPlayerOptionDto(Long id, String name, String number, List<String> positions, String batHand, String throwHand, String status) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.positions = positions;
        this.batHand = batHand;
        this.throwHand = throwHand;
        this.status = status;
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

    public List<String> positions() {
        return this.positions;
    }

    public String batHand() {
        return this.batHand;
    }

    public String throwHand() {
        return this.throwHand;
    }

    public String status() {
        return this.status;
    }
}

