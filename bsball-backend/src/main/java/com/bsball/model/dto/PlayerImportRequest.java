/*
 * Decompiled with CFR 0.152.
 */
package com.bsball.model.dto;

import com.bsball.model.entity.Player;
import java.util.List;

public record PlayerImportRequest(List<Player> items, String duplicateStrategy) {
}
