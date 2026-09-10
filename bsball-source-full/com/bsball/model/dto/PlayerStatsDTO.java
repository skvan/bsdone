/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.PlayerStatsDTO
 *  com.bsball.model.dto.PlayerStatsDTO$Batting
 *  com.bsball.model.dto.PlayerStatsDTO$Fielding
 *  com.bsball.model.dto.PlayerStatsDTO$Pitching
 *  lombok.Generated
 */
package com.bsball.model.dto;

import com.bsball.model.dto.PlayerStatsDTO;
import lombok.Generated;

public class PlayerStatsDTO {
    private Batting batting;
    private Pitching pitching;
    private Fielding fielding;

    @Generated
    public PlayerStatsDTO() {
    }

    @Generated
    public Batting getBatting() {
        return this.batting;
    }

    @Generated
    public Pitching getPitching() {
        return this.pitching;
    }

    @Generated
    public Fielding getFielding() {
        return this.fielding;
    }

    @Generated
    public void setBatting(Batting batting) {
        this.batting = batting;
    }

    @Generated
    public void setPitching(Pitching pitching) {
        this.pitching = pitching;
    }

    @Generated
    public void setFielding(Fielding fielding) {
        this.fielding = fielding;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PlayerStatsDTO)) {
            return false;
        }
        PlayerStatsDTO other = (PlayerStatsDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Batting this$batting = this.getBatting();
        Batting other$batting = other.getBatting();
        if (this$batting == null ? other$batting != null : !this$batting.equals(other$batting)) {
            return false;
        }
        Pitching this$pitching = this.getPitching();
        Pitching other$pitching = other.getPitching();
        if (this$pitching == null ? other$pitching != null : !this$pitching.equals(other$pitching)) {
            return false;
        }
        Fielding this$fielding = this.getFielding();
        Fielding other$fielding = other.getFielding();
        return !(this$fielding == null ? other$fielding != null : !this$fielding.equals(other$fielding));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof PlayerStatsDTO;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Batting $batting = this.getBatting();
        result = result * 59 + ($batting == null ? 43 : $batting.hashCode());
        Pitching $pitching = this.getPitching();
        result = result * 59 + ($pitching == null ? 43 : $pitching.hashCode());
        Fielding $fielding = this.getFielding();
        result = result * 59 + ($fielding == null ? 43 : $fielding.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "PlayerStatsDTO(batting=" + String.valueOf(this.getBatting()) + ", pitching=" + String.valueOf(this.getPitching()) + ", fielding=" + String.valueOf(this.getFielding()) + ")";
    }
}

