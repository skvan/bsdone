/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.HistoryRecord
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  jakarta.validation.constraints.Size
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="bs_personnel_change")
@Comment(value="\u6cbf\u9769\u8bb0\u5f55")
public class HistoryRecord
extends BaseEntity {
    @Comment(value="\u8bb0\u5f55\u7c7b\u578b\uff1aevent/snapshot")
    private String recordType = "event";
    @Comment(value="\u4e8b\u4ef6\u7c7b\u578b")
    private String type = "join";
    @Comment(value="\u4e3b\u4f53\u7c7b\u578b\uff1aleague/team/coach/player")
    private String targetType = "player";
    @Comment(value="\u4e3b\u4f53ID")
    private Long targetId;
    @Comment(value="\u5173\u8054\u5bf9\u8c61\u7c7b\u578b\uff1aleague/team/coach/player")
    private String relatedObjectType;
    @Comment(value="\u5173\u8054\u5bf9\u8c61ID")
    private Long relatedObjectId;
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @Comment(value="\u53d8\u52a8\u65e5\u671f")
    private String changeDate;
    @Column(length=6000)
    @Size(max=6000, message="\u5feb\u7167\u6570\u636e\u4e0d\u80fd\u8d85\u8fc76000\u5b57")
    @Comment(value="\u5feb\u7167JSON\uff08recordType=snapshot\uff09")
    private @Size(max=6000, message="\u5feb\u7167\u6570\u636e\u4e0d\u80fd\u8d85\u8fc76000\u5b57") String snapshotJson;
    @Column(length=12000)
    @Size(max=12000, message="\u53d8\u66f4\u5185\u5bb9\u4e0d\u80fd\u8d85\u8fc712000\u5b57")
    @Comment(value="\u53d8\u66f4\u5185\u5bb9JSON\uff08\u5efa\u8bae\u5b58 before/after\uff09")
    private @Size(max=12000, message="\u53d8\u66f4\u5185\u5bb9\u4e0d\u80fd\u8d85\u8fc712000\u5b57") String changePayloadJson;
    @Column(length=2000)
    @Size(max=2000, message="\u5907\u6ce8\u4e0d\u80fd\u8d85\u8fc72000\u5b57")
    @Comment(value="\u5907\u6ce8")
    private @Size(max=2000, message="\u5907\u6ce8\u4e0d\u80fd\u8d85\u8fc72000\u5b57") String remark;

    @Generated
    public HistoryRecord() {
    }

    @Generated
    public String getRecordType() {
        return this.recordType;
    }

    @Generated
    public String getType() {
        return this.type;
    }

    @Generated
    public String getTargetType() {
        return this.targetType;
    }

    @Generated
    public Long getTargetId() {
        return this.targetId;
    }

    @Generated
    public String getRelatedObjectType() {
        return this.relatedObjectType;
    }

    @Generated
    public Long getRelatedObjectId() {
        return this.relatedObjectId;
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
    }

    @Generated
    public String getChangeDate() {
        return this.changeDate;
    }

    @Generated
    public String getSnapshotJson() {
        return this.snapshotJson;
    }

    @Generated
    public String getChangePayloadJson() {
        return this.changePayloadJson;
    }

    @Generated
    public String getRemark() {
        return this.remark;
    }

    @Generated
    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    @Generated
    public void setType(String type) {
        this.type = type;
    }

    @Generated
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    @Generated
    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    @Generated
    public void setRelatedObjectType(String relatedObjectType) {
        this.relatedObjectType = relatedObjectType;
    }

    @Generated
    public void setRelatedObjectId(Long relatedObjectId) {
        this.relatedObjectId = relatedObjectId;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setChangeDate(String changeDate) {
        this.changeDate = changeDate;
    }

    @Generated
    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    @Generated
    public void setChangePayloadJson(String changePayloadJson) {
        this.changePayloadJson = changePayloadJson;
    }

    @Generated
    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Generated
    public String toString() {
        return "HistoryRecord(recordType=" + this.getRecordType() + ", type=" + this.getType() + ", targetType=" + this.getTargetType() + ", targetId=" + this.getTargetId() + ", relatedObjectType=" + this.getRelatedObjectType() + ", relatedObjectId=" + this.getRelatedObjectId() + ", tenantId=" + this.getTenantId() + ", changeDate=" + this.getChangeDate() + ", snapshotJson=" + this.getSnapshotJson() + ", changePayloadJson=" + this.getChangePayloadJson() + ", remark=" + this.getRemark() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof HistoryRecord)) {
            return false;
        }
        HistoryRecord other = (HistoryRecord)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$targetId = this.getTargetId();
        Long other$targetId = other.getTargetId();
        if (this$targetId == null ? other$targetId != null : !((Object)this$targetId).equals(other$targetId)) {
            return false;
        }
        Long this$relatedObjectId = this.getRelatedObjectId();
        Long other$relatedObjectId = other.getRelatedObjectId();
        if (this$relatedObjectId == null ? other$relatedObjectId != null : !((Object)this$relatedObjectId).equals(other$relatedObjectId)) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        String this$recordType = this.getRecordType();
        String other$recordType = other.getRecordType();
        if (this$recordType == null ? other$recordType != null : !this$recordType.equals(other$recordType)) {
            return false;
        }
        String this$type = this.getType();
        String other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
            return false;
        }
        String this$targetType = this.getTargetType();
        String other$targetType = other.getTargetType();
        if (this$targetType == null ? other$targetType != null : !this$targetType.equals(other$targetType)) {
            return false;
        }
        String this$relatedObjectType = this.getRelatedObjectType();
        String other$relatedObjectType = other.getRelatedObjectType();
        if (this$relatedObjectType == null ? other$relatedObjectType != null : !this$relatedObjectType.equals(other$relatedObjectType)) {
            return false;
        }
        String this$changeDate = this.getChangeDate();
        String other$changeDate = other.getChangeDate();
        if (this$changeDate == null ? other$changeDate != null : !this$changeDate.equals(other$changeDate)) {
            return false;
        }
        String this$snapshotJson = this.getSnapshotJson();
        String other$snapshotJson = other.getSnapshotJson();
        if (this$snapshotJson == null ? other$snapshotJson != null : !this$snapshotJson.equals(other$snapshotJson)) {
            return false;
        }
        String this$changePayloadJson = this.getChangePayloadJson();
        String other$changePayloadJson = other.getChangePayloadJson();
        if (this$changePayloadJson == null ? other$changePayloadJson != null : !this$changePayloadJson.equals(other$changePayloadJson)) {
            return false;
        }
        String this$remark = this.getRemark();
        String other$remark = other.getRemark();
        return !(this$remark == null ? other$remark != null : !this$remark.equals(other$remark));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof HistoryRecord;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $targetId = this.getTargetId();
        result = result * 59 + ($targetId == null ? 43 : ((Object)$targetId).hashCode());
        Long $relatedObjectId = this.getRelatedObjectId();
        result = result * 59 + ($relatedObjectId == null ? 43 : ((Object)$relatedObjectId).hashCode());
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        String $recordType = this.getRecordType();
        result = result * 59 + ($recordType == null ? 43 : $recordType.hashCode());
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        String $targetType = this.getTargetType();
        result = result * 59 + ($targetType == null ? 43 : $targetType.hashCode());
        String $relatedObjectType = this.getRelatedObjectType();
        result = result * 59 + ($relatedObjectType == null ? 43 : $relatedObjectType.hashCode());
        String $changeDate = this.getChangeDate();
        result = result * 59 + ($changeDate == null ? 43 : $changeDate.hashCode());
        String $snapshotJson = this.getSnapshotJson();
        result = result * 59 + ($snapshotJson == null ? 43 : $snapshotJson.hashCode());
        String $changePayloadJson = this.getChangePayloadJson();
        result = result * 59 + ($changePayloadJson == null ? 43 : $changePayloadJson.hashCode());
        String $remark = this.getRemark();
        result = result * 59 + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }
}

