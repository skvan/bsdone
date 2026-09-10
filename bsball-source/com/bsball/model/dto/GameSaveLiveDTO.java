/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.GameSaveLiveDTO
 *  com.bsball.model.dto.GameSaveLiveDTO$GamePlayerStatPart
 *  com.bsball.model.dto.GameSaveLiveDTO$GameUpdatePart
 *  lombok.Generated
 */
package com.bsball.model.dto;

import com.bsball.model.dto.GameSaveLiveDTO;
import java.util.List;
import lombok.Generated;

public class GameSaveLiveDTO {
    private GameUpdatePart game;
    private List<GamePlayerStatPart> stats;

    @Generated
    public GameSaveLiveDTO() {
    }

    @Generated
    public GameUpdatePart getGame() {
        return this.game;
    }

    @Generated
    public List<GamePlayerStatPart> getStats() {
        return this.stats;
    }

    @Generated
    public void setGame(GameUpdatePart game) {
        this.game = game;
    }

    @Generated
    public void setStats(List<GamePlayerStatPart> stats) {
        this.stats = stats;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GameSaveLiveDTO)) {
            return false;
        }
        GameSaveLiveDTO other = (GameSaveLiveDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        GameUpdatePart this$game = this.getGame();
        GameUpdatePart other$game = other.getGame();
        if (this$game == null ? other$game != null : !this$game.equals(other$game)) {
            return false;
        }
        List this$stats = this.getStats();
        List other$stats = other.getStats();
        return !(this$stats == null ? other$stats != null : !((Object)this$stats).equals(other$stats));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof GameSaveLiveDTO;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        GameUpdatePart $game = this.getGame();
        result = result * 59 + ($game == null ? 43 : $game.hashCode());
        List $stats = this.getStats();
        result = result * 59 + ($stats == null ? 43 : ((Object)$stats).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "GameSaveLiveDTO(game=" + String.valueOf(this.getGame()) + ", stats=" + String.valueOf(this.getStats()) + ")";
    }
}

