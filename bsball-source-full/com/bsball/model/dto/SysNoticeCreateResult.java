/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.SysNoticeCreateResult
 */
package com.bsball.model.dto;

public record SysNoticeCreateResult(long firstId, int createdCount) {
    private final long firstId;
    private final int createdCount;

    public SysNoticeCreateResult(long firstId, int createdCount) {
        this.firstId = firstId;
        this.createdCount = createdCount;
    }

    public long firstId() {
        return this.firstId;
    }

    public int createdCount() {
        return this.createdCount;
    }
}

