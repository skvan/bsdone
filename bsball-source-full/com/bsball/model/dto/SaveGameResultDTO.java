/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.SaveGameResultDTO
 *  com.bsball.model.dto.SaveGameResultDTO$GamePart
 *  com.bsball.model.dto.SaveGameResultDTO$StatPart
 *  lombok.Generated
 */
package com.bsball.model.dto;

import com.bsball.model.dto.SaveGameResultDTO;
import java.util.List;
import lombok.Generated;

public class SaveGameResultDTO {
    private GamePart game;
    private List<StatPart> stats;

    @Generated
    public SaveGameResultDTO() {
    }

    @Generated
    public GamePart getGame() {
        return this.game;
    }

    @Generated
    public List<StatPart> getStats() {
        return this.stats;
    }

    @Generated
    public void setGame(GamePart game) {
        this.game = game;
    }

    @Generated
    public void setStats(List<StatPart> stats) {
        this.stats = stats;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SaveGameResultDTO)) {
            return false;
        }
        SaveGameResultDTO other = (SaveGameResultDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        GamePart this$game = this.getGame();
        GamePart other$game = other.getGame();
        if (this$game == null ? other$game != null : !this$game.equals(other$game)) {
            return false;
        }
        List this$stats = this.getStats();
        List other$stats = other.getStats();
        return !(this$stats == null ? other$stats != null : !((Object)this$stats).equals(other$stats));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SaveGameResultDTO;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        GamePart $game = this.getGame();
        result = result * 59 + ($game == null ? 43 : $game.hashCode());
        List $stats = this.getStats();
        result = result * 59 + ($stats == null ? 43 : ((Object)$stats).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "SaveGameResultDTO(game=" + String.valueOf(this.getGame()) + ", stats=" + String.valueOf(this.getStats()) + ")";
    }
}

