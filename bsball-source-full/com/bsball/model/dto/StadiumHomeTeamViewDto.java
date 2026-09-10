/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.StadiumHomeTeamViewDto
 *  lombok.Generated
 */
package com.bsball.model.dto;

import java.time.LocalDate;
import lombok.Generated;

public class StadiumHomeTeamViewDto {
    private Long id;
    private Long teamId;
    private String teamName;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer sortOrder;

    @Generated
    public StadiumHomeTeamViewDto() {
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public Long getTeamId() {
        return this.teamId;
    }

    @Generated
    public String getTeamName() {
        return this.teamName;
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
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    @Generated
    public void setTeamName(String teamName) {
        this.teamName = teamName;
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StadiumHomeTeamViewDto)) {
            return false;
        }
        StadiumHomeTeamViewDto other = (StadiumHomeTeamViewDto)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
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
        String this$teamName = this.getTeamName();
        String other$teamName = other.getTeamName();
        if (this$teamName == null ? other$teamName != null : !this$teamName.equals(other$teamName)) {
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
        return other instanceof StadiumHomeTeamViewDto;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $teamId = this.getTeamId();
        result = result * 59 + ($teamId == null ? 43 : ((Object)$teamId).hashCode());
        Integer $sortOrder = this.getSortOrder();
        result = result * 59 + ($sortOrder == null ? 43 : ((Object)$sortOrder).hashCode());
        String $teamName = this.getTeamName();
        result = result * 59 + ($teamName == null ? 43 : $teamName.hashCode());
        LocalDate $effectiveFrom = this.getEffectiveFrom();
        result = result * 59 + ($effectiveFrom == null ? 43 : ((Object)$effectiveFrom).hashCode());
        LocalDate $effectiveTo = this.getEffectiveTo();
        result = result * 59 + ($effectiveTo == null ? 43 : ((Object)$effectiveTo).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "StadiumHomeTeamViewDto(id=" + this.getId() + ", teamId=" + this.getTeamId() + ", teamName=" + this.getTeamName() + ", effectiveFrom=" + String.valueOf(this.getEffectiveFrom()) + ", effectiveTo=" + String.valueOf(this.getEffectiveTo()) + ", sortOrder=" + this.getSortOrder() + ")";
    }
}

