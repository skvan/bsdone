/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.LiveSnapshotSaveDTO
 *  jakarta.validation.constraints.Size
 *  lombok.Generated
 */
package com.bsball.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Generated;

public class LiveSnapshotSaveDTO {
    @Size(max=10000000)
    private @Size(max=10000000) String snapshotJson;

    @Generated
    public LiveSnapshotSaveDTO() {
    }

    @Generated
    public String getSnapshotJson() {
        return this.snapshotJson;
    }

    @Generated
    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LiveSnapshotSaveDTO)) {
            return false;
        }
        LiveSnapshotSaveDTO other = (LiveSnapshotSaveDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$snapshotJson = this.getSnapshotJson();
        String other$snapshotJson = other.getSnapshotJson();
        return !(this$snapshotJson == null ? other$snapshotJson != null : !this$snapshotJson.equals(other$snapshotJson));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof LiveSnapshotSaveDTO;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $snapshotJson = this.getSnapshotJson();
        result = result * 59 + ($snapshotJson == null ? 43 : $snapshotJson.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "LiveSnapshotSaveDTO(snapshotJson=" + this.getSnapshotJson() + ")";
    }
}

