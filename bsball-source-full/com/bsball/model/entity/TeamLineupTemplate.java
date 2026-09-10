/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.TeamLineupTemplate
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.Size
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="bs_team_lineup_template")
@Comment(value="\u7403\u961f\u9635\u5bb9\u6a21\u677f")
public class TeamLineupTemplate
extends BaseEntity {
    @Comment(value="\u79df\u6237ID")
    @Column(nullable=false)
    private Long tenantId;
    @Comment(value="\u7403\u961fID")
    @Column(nullable=false)
    private Long teamId;
    @NotBlank
    @Size(max=120)
    @Comment(value="\u6a21\u677f\u540d\u79f0")
    private @NotBlank @Size(max=120) String name;
    @Size(max=500)
    @Comment(value="\u7528\u9014\u8bf4\u660e\u7b49")
    private @Size(max=500) String description;
    @Column(name="slots_json", nullable=false, columnDefinition="TEXT")
    @Comment(value="9 \u6761\u5148\u53d1 JSON")
    private String slotsJson;

    @Generated
    public TeamLineupTemplate() {
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
    }

    @Generated
    public Long getTeamId() {
        return this.teamId;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getDescription() {
        return this.description;
    }

    @Generated
    public String getSlotsJson() {
        return this.slotsJson;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setDescription(String description) {
        this.description = description;
    }

    @Generated
    public void setSlotsJson(String slotsJson) {
        this.slotsJson = slotsJson;
    }

    @Generated
    public String toString() {
        return "TeamLineupTemplate(tenantId=" + this.getTenantId() + ", teamId=" + this.getTeamId() + ", name=" + this.getName() + ", description=" + this.getDescription() + ", slotsJson=" + this.getSlotsJson() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TeamLineupTemplate)) {
            return false;
        }
        TeamLineupTemplate other = (TeamLineupTemplate)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        Long this$teamId = this.getTeamId();
        Long other$teamId = other.getTeamId();
        if (this$teamId == null ? other$teamId != null : !((Object)this$teamId).equals(other$teamId)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$slotsJson = this.getSlotsJson();
        String other$slotsJson = other.getSlotsJson();
        return !(this$slotsJson == null ? other$slotsJson != null : !this$slotsJson.equals(other$slotsJson));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TeamLineupTemplate;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Long $teamId = this.getTeamId();
        result = result * 59 + ($teamId == null ? 43 : ((Object)$teamId).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $slotsJson = this.getSlotsJson();
        result = result * 59 + ($slotsJson == null ? 43 : $slotsJson.hashCode());
        return result;
    }
}

