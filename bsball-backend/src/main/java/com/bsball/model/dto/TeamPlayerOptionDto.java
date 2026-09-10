/*
 * Decompiled with CFR 0.152.
 */
package com.bsball.model.dto;

import java.util.List;

public record TeamPlayerOptionDto(Long id, String name, String number, List<String> positions, String batHand, String throwHand, String status) {
}
