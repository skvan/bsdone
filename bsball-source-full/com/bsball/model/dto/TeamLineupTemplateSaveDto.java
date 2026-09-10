/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.TeamLineupTemplateSaveDto
 *  com.bsball.model.dto.TeamLineupTemplateSlotDto
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.Size
 *  lombok.Generated
 */
package com.bsball.model.dto;

import com.bsball.model.dto.TeamLineupTemplateSlotDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Generated;

public class TeamLineupTemplateSaveDto {
    @NotBlank
    @Size(max=120)
    private @NotBlank @Size(max=120) String name;
    @Size(max=500)
    private @Size(max=500) String description;
    private List<TeamLineupTemplateSlotDto> slots;
    private List<Long> benchPlayerIds;
    private Long startingPitcherPlayerId;

    @Generated
    public TeamLineupTemplateSaveDto() {
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
    public List<TeamLineupTemplateSlotDto> getSlots() {
        return this.slots;
    }

    @Generated
    public List<Long> getBenchPlayerIds() {
        return this.benchPlayerIds;
    }

    @Generated
    public Long getStartingPitcherPlayerId() {
        return this.startingPitcherPlayerId;
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
    public void setSlots(List<TeamLineupTemplateSlotDto> slots) {
        this.slots = slots;
    }

    @Generated
    public void setBenchPlayerIds(List<Long> benchPlayerIds) {
        this.benchPlayerIds = benchPlayerIds;
    }

    @Generated
    public void setStartingPitcherPlayerId(Long startingPitcherPlayerId) {
        this.startingPitcherPlayerId = startingPitcherPlayerId;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TeamLineupTemplateSaveDto)) {
            return false;
        }
        TeamLineupTemplateSaveDto other = (TeamLineupTemplateSaveDto)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$startingPitcherPlayerId = this.getStartingPitcherPlayerId();
        Long other$startingPitcherPlayerId = other.getStartingPitcherPlayerId();
        if (this$startingPitcherPlayerId == null ? other$startingPitcherPlayerId != null : !((Object)this$startingPitcherPlayerId).equals(other$startingPitcherPlayerId)) {
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
        List this$slots = this.getSlots();
        List other$slots = other.getSlots();
        if (this$slots == null ? other$slots != null : !((Object)this$slots).equals(other$slots)) {
            return false;
        }
        List this$benchPlayerIds = this.getBenchPlayerIds();
        List other$benchPlayerIds = other.getBenchPlayerIds();
        return !(this$benchPlayerIds == null ? other$benchPlayerIds != null : !((Object)this$benchPlayerIds).equals(other$benchPlayerIds));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TeamLineupTemplateSaveDto;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $startingPitcherPlayerId = this.getStartingPitcherPlayerId();
        result = result * 59 + ($startingPitcherPlayerId == null ? 43 : ((Object)$startingPitcherPlayerId).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        List $slots = this.getSlots();
        result = result * 59 + ($slots == null ? 43 : ((Object)$slots).hashCode());
        List $benchPlayerIds = this.getBenchPlayerIds();
        result = result * 59 + ($benchPlayerIds == null ? 43 : ((Object)$benchPlayerIds).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TeamLineupTemplateSaveDto(name=" + this.getName() + ", description=" + this.getDescription() + ", slots=" + String.valueOf(this.getSlots()) + ", benchPlayerIds=" + String.valueOf(this.getBenchPlayerIds()) + ", startingPitcherPlayerId=" + this.getStartingPitcherPlayerId() + ")";
    }
}

