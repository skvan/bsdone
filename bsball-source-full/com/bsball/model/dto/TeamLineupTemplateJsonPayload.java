/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.TeamLineupTemplateJsonPayload
 *  com.bsball.model.dto.TeamLineupTemplateSlotDto
 *  lombok.Generated
 */
package com.bsball.model.dto;

import com.bsball.model.dto.TeamLineupTemplateSlotDto;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;

public class TeamLineupTemplateJsonPayload {
    private List<TeamLineupTemplateSlotDto> slots = new ArrayList();
    private List<Long> benchPlayerIds = new ArrayList();
    private Long startingPitcherPlayerId;

    @Generated
    public TeamLineupTemplateJsonPayload() {
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
        if (!(o instanceof TeamLineupTemplateJsonPayload)) {
            return false;
        }
        TeamLineupTemplateJsonPayload other = (TeamLineupTemplateJsonPayload)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$startingPitcherPlayerId = this.getStartingPitcherPlayerId();
        Long other$startingPitcherPlayerId = other.getStartingPitcherPlayerId();
        if (this$startingPitcherPlayerId == null ? other$startingPitcherPlayerId != null : !((Object)this$startingPitcherPlayerId).equals(other$startingPitcherPlayerId)) {
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
        return other instanceof TeamLineupTemplateJsonPayload;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $startingPitcherPlayerId = this.getStartingPitcherPlayerId();
        result = result * 59 + ($startingPitcherPlayerId == null ? 43 : ((Object)$startingPitcherPlayerId).hashCode());
        List $slots = this.getSlots();
        result = result * 59 + ($slots == null ? 43 : ((Object)$slots).hashCode());
        List $benchPlayerIds = this.getBenchPlayerIds();
        result = result * 59 + ($benchPlayerIds == null ? 43 : ((Object)$benchPlayerIds).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TeamLineupTemplateJsonPayload(slots=" + String.valueOf(this.getSlots()) + ", benchPlayerIds=" + String.valueOf(this.getBenchPlayerIds()) + ", startingPitcherPlayerId=" + this.getStartingPitcherPlayerId() + ")";
    }
}

