/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.TeamLineupTemplateCopyFromGameDto
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.NotNull
 *  lombok.Generated
 */
package com.bsball.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Generated;

public class TeamLineupTemplateCopyFromGameDto {
    @NotNull
    private Long gameId;
    @NotBlank
    private String teamSide;
    private String name;
    private String description;

    @Generated
    public TeamLineupTemplateCopyFromGameDto() {
    }

    @Generated
    public Long getGameId() {
        return this.gameId;
    }

    @Generated
    public String getTeamSide() {
        return this.teamSide;
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
    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    @Generated
    public void setTeamSide(String teamSide) {
        this.teamSide = teamSide;
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TeamLineupTemplateCopyFromGameDto)) {
            return false;
        }
        TeamLineupTemplateCopyFromGameDto other = (TeamLineupTemplateCopyFromGameDto)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$gameId = this.getGameId();
        Long other$gameId = other.getGameId();
        if (this$gameId == null ? other$gameId != null : !((Object)this$gameId).equals(other$gameId)) {
            return false;
        }
        String this$teamSide = this.getTeamSide();
        String other$teamSide = other.getTeamSide();
        if (this$teamSide == null ? other$teamSide != null : !this$teamSide.equals(other$teamSide)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        return !(this$description == null ? other$description != null : !this$description.equals(other$description));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TeamLineupTemplateCopyFromGameDto;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $gameId = this.getGameId();
        result = result * 59 + ($gameId == null ? 43 : ((Object)$gameId).hashCode());
        String $teamSide = this.getTeamSide();
        result = result * 59 + ($teamSide == null ? 43 : $teamSide.hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TeamLineupTemplateCopyFromGameDto(gameId=" + this.getGameId() + ", teamSide=" + this.getTeamSide() + ", name=" + this.getName() + ", description=" + this.getDescription() + ")";
    }
}

