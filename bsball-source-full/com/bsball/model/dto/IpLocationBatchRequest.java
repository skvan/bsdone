/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.IpLocationBatchRequest
 */
package com.bsball.model.dto;

import java.util.List;

public record IpLocationBatchRequest(List<String> ips) {
    private final List<String> ips;

    public IpLocationBatchRequest(List<String> ips) {
        this.ips = ips;
    }

    public List<String> ips() {
        return this.ips;
    }
}

