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
    /**
     * 本次保存来自撤销/重做后的现场恢复，明确允许逐棒记录清零。
     * 缺省 false：旧设备/旧标签页把本地归零的存档推上来仍按空壳拒收（#713 拍板）。
     */
    private Boolean allowEvidenceShrink;

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

    public Boolean getAllowEvidenceShrink() {
        return allowEvidenceShrink;
    }

    public void setAllowEvidenceShrink(Boolean allowEvidenceShrink) {
        this.allowEvidenceShrink = allowEvidenceShrink;
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

