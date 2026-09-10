/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.PlayerImportRequest
 *  com.bsball.model.entity.Player
 */
package com.bsball.model.dto;

import com.bsball.model.entity.Player;
import java.util.List;

public record PlayerImportRequest(List<Player> items, String duplicateStrategy) {
    private final List<Player> items;
    private final String duplicateStrategy;

    public PlayerImportRequest(List<Player> items, String duplicateStrategy) {
        this.items = items;
        this.duplicateStrategy = duplicateStrategy;
    }

    public String duplicateStrategy() {
        return this.duplicateStrategy != null && !this.duplicateStrategy.isBlank() ? this.duplicateStrategy : "skip";
    }

    public List<Player> items() {
        return this.items;
    }
}

