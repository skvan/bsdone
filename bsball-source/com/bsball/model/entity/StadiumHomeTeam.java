/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.Stadium
 *  com.bsball.model.entity.StadiumHomeTeam
 *  jakarta.persistence.Entity
 *  jakarta.persistence.FetchType
 *  jakarta.persistence.JoinColumn
 *  jakarta.persistence.ManyToOne
 *  jakarta.persistence.Table
 *  jakarta.validation.constraints.NotNull
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import com.bsball.model.entity.Stadium;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="bs_stadium_home_team")
@Comment(value="\u7403\u573a\u4e3b\u573a\u7403\u961f\u5173\u8054")
public class StadiumHomeTeam
extends BaseEntity {
    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="stadium_id", nullable=false)
    private Stadium stadium;
    @NotNull
    @Comment(value="\u7403\u961fID")
    private Long teamId;
    @Comment(value="\u751f\u6548\u8d77")
    private LocalDate effectiveFrom;
    @Comment(value="\u751f\u6548\u6b62\uff08\u7a7a=\u4ecd\u6709\u6548\uff09")
    private LocalDate effectiveTo;
    @Comment(value="\u6392\u5e8f")
    private Integer sortOrder = 0;

    @Generated
    public StadiumHomeTeam() {
    }

    @Generated
    public Stadium getStadium() {
        return this.stadium;
    }

    @Generated
    public Long getTeamId() {
        return this.teamId;
    }

    @Generated
    public LocalDate getEffectiveFrom() {
        return this.effectiveFrom;
    }

    @Generated
    public LocalDate getEffectiveTo() {
        return this.effectiveTo;
    }

    @Generated
    public Integer getSortOrder() {
        return this.sortOrder;
    }

    @Generated
    public void setStadium(Stadium stadium) {
        this.stadium = stadium;
    }

    @Generated
    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    @Generated
    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    @Generated
    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    @Generated
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Generated
    public String toString() {
        return "StadiumHomeTeam(stadium=" + String.valueOf(this.getStadium()) + ", teamId=" + this.getTeamId() + ", effectiveFrom=" + String.valueOf(this.getEffectiveFrom()) + ", effectiveTo=" + String.valueOf(this.getEffectiveTo()) + ", sortOrder=" + this.getSortOrder() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StadiumHomeTeam)) {
            return false;
        }
        StadiumHomeTeam other = (StadiumHomeTeam)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$teamId = this.getTeamId();
        Long other$teamId = other.getTeamId();
        if (this$teamId == null ? other$teamId != null : !((Object)this$teamId).equals(other$teamId)) {
            return false;
        }
        Integer this$sortOrder = this.getSortOrder();
        Integer other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !((Object)this$sortOrder).equals(other$sortOrder)) {
            return false;
        }
        Stadium this$stadium = this.getStadium();
        Stadium other$stadium = other.getStadium();
        if (this$stadium == null ? other$stadium != null : !this$stadium.equals(other$stadium)) {
            return false;
        }
        LocalDate this$effectiveFrom = this.getEffectiveFrom();
        LocalDate other$effectiveFrom = other.getEffectiveFrom();
        if (this$effectiveFrom == null ? other$effectiveFrom != null : !((Object)this$effectiveFrom).equals(other$effectiveFrom)) {
            return false;
        }
        LocalDate this$effectiveTo = this.getEffectiveTo();
        LocalDate other$effectiveTo = other.getEffectiveTo();
        return !(this$effectiveTo == null ? other$effectiveTo != null : !((Object)this$effectiveTo).equals(other$effectiveTo));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof StadiumHomeTeam;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $teamId = this.getTeamId();
        result = result * 59 + ($teamId == null ? 43 : ((Object)$teamId).hashCode());
        Integer $sortOrder = this.getSortOrder();
        result = result * 59 + ($sortOrder == null ? 43 : ((Object)$sortOrder).hashCode());
        Stadium $stadium = this.getStadium();
        result = result * 59 + ($stadium == null ? 43 : $stadium.hashCode());
        LocalDate $effectiveFrom = this.getEffectiveFrom();
        result = result * 59 + ($effectiveFrom == null ? 43 : ((Object)$effectiveFrom).hashCode());
        LocalDate $effectiveTo = this.getEffectiveTo();
        result = result * 59 + ($effectiveTo == null ? 43 : ((Object)$effectiveTo).hashCode());
        return result;
    }
}

