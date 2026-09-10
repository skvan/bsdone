/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.Event
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
@Table(name="bs_event")
@Comment(value="\u8d5b\u4e8b")
public class Event
extends BaseEntity {
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @Comment(value="\u8d5b\u4e8b\u540d\u79f0")
    private String name;
    @Comment(value="\u8054\u76dfID")
    private Long leagueId;
    @Comment(value="\u8d5b\u5b63")
    private String season;
    @Comment(value="\u5f00\u59cb\u65e5\u671f")
    private String startDate;
    @Comment(value="\u7ed3\u675f\u65e5\u671f")
    private String endDate;
    @Comment(value="\u72b6\u6001")
    private String status = "draft";
    @Comment(value="\u6bd4\u8d5b\u6a21\u5f0f: BASEBALL \u68d2\u7403 / SOFTBALL \u5792\u7403")
    private String gameMode = "BASEBALL";
    @Column(length=2000)
    @Size(max=2000, message="\u63cf\u8ff0\u4e0d\u80fd\u8d85\u8fc72000\u5b57")
    @Comment(value="\u63cf\u8ff0")
    private @Size(max=2000, message="\u63cf\u8ff0\u4e0d\u80fd\u8d85\u8fc72000\u5b57") String description;
    @Comment(value="\u6392\u5e8f")
    private Integer sort = 0;

    @Generated
    public Event() {
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public Long getLeagueId() {
        return this.leagueId;
    }

    @Generated
    public String getSeason() {
        return this.season;
    }

    @Generated
    public String getStartDate() {
        return this.startDate;
    }

    @Generated
    public String getEndDate() {
        return this.endDate;
    }

    @Generated
    public String getStatus() {
        return this.status;
    }

    @Generated
    public String getGameMode() {
        return this.gameMode;
    }

    @Generated
    public String getDescription() {
        return this.description;
    }

    @Generated
    public Integer getSort() {
        return this.sort;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setLeagueId(Long leagueId) {
        this.leagueId = leagueId;
    }

    @Generated
    public void setSeason(String season) {
        this.season = season;
    }

    @Generated
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    @Generated
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    @Generated
    public void setStatus(String status) {
        this.status = status;
    }

    @Generated
    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    @Generated
    public void setDescription(String description) {
        this.description = description;
    }

    @Generated
    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Generated
    public String toString() {
        return "Event(tenantId=" + this.getTenantId() + ", name=" + this.getName() + ", leagueId=" + this.getLeagueId() + ", season=" + this.getSeason() + ", startDate=" + this.getStartDate() + ", endDate=" + this.getEndDate() + ", status=" + this.getStatus() + ", gameMode=" + this.getGameMode() + ", description=" + this.getDescription() + ", sort=" + this.getSort() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Event)) {
            return false;
        }
        Event other = (Event)o;
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
        Long this$leagueId = this.getLeagueId();
        Long other$leagueId = other.getLeagueId();
        if (this$leagueId == null ? other$leagueId != null : !((Object)this$leagueId).equals(other$leagueId)) {
            return false;
        }
        Integer this$sort = this.getSort();
        Integer other$sort = other.getSort();
        if (this$sort == null ? other$sort != null : !((Object)this$sort).equals(other$sort)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$season = this.getSeason();
        String other$season = other.getSeason();
        if (this$season == null ? other$season != null : !this$season.equals(other$season)) {
            return false;
        }
        String this$startDate = this.getStartDate();
        String other$startDate = other.getStartDate();
        if (this$startDate == null ? other$startDate != null : !this$startDate.equals(other$startDate)) {
            return false;
        }
        String this$endDate = this.getEndDate();
        String other$endDate = other.getEndDate();
        if (this$endDate == null ? other$endDate != null : !this$endDate.equals(other$endDate)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$gameMode = this.getGameMode();
        String other$gameMode = other.getGameMode();
        if (this$gameMode == null ? other$gameMode != null : !this$gameMode.equals(other$gameMode)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        return !(this$description == null ? other$description != null : !this$description.equals(other$description));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof Event;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Long $leagueId = this.getLeagueId();
        result = result * 59 + ($leagueId == null ? 43 : ((Object)$leagueId).hashCode());
        Integer $sort = this.getSort();
        result = result * 59 + ($sort == null ? 43 : ((Object)$sort).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $season = this.getSeason();
        result = result * 59 + ($season == null ? 43 : $season.hashCode());
        String $startDate = this.getStartDate();
        result = result * 59 + ($startDate == null ? 43 : $startDate.hashCode());
        String $endDate = this.getEndDate();
        result = result * 59 + ($endDate == null ? 43 : $endDate.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $gameMode = this.getGameMode();
        result = result * 59 + ($gameMode == null ? 43 : $gameMode.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        return result;
    }
}

