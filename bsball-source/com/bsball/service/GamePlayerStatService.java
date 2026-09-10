/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.GamePlayerStat
 *  com.bsball.repository.GamePlayerStatRepository
 *  com.bsball.service.GamePlayerStatService
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.model.entity.GamePlayerStat;
import com.bsball.repository.GamePlayerStatRepository;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.Generated;
import org.springframework.stereotype.Service;

@Service
public class GamePlayerStatService {
    private final GamePlayerStatRepository gamePlayerStatRepository;

    public List<GamePlayerStat> listByGame(Long gameId) {
        List rows = this.gamePlayerStatRepository.findByGameId(gameId);
        LinkedHashMap<String, GamePlayerStat> dedup = new LinkedHashMap<String, GamePlayerStat>();
        for (GamePlayerStat row : rows) {
            String key = this.buildKey(row);
            GamePlayerStat prev = (GamePlayerStat)dedup.get(key);
            if (prev != null && this.statRichness(row) <= this.statRichness(prev)) continue;
            dedup.put(key, row);
        }
        return List.copyOf(dedup.values());
    }

    public GamePlayerStat create(GamePlayerStat entity) {
        return (GamePlayerStat)this.gamePlayerStatRepository.save((Object)entity);
    }

    public GamePlayerStat update(Long id, GamePlayerStat entity) {
        GamePlayerStat existing = this.gamePlayerStatRepository.findById((Object)id).orElse(null);
        if (existing == null) {
            return null;
        }
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        return (GamePlayerStat)this.gamePlayerStatRepository.save((Object)entity);
    }

    public void delete(Long id) {
        this.gamePlayerStatRepository.deleteById((Object)id);
    }

    private String buildKey(GamePlayerStat r) {
        return String.join((CharSequence)"|", String.valueOf(r.getGameId()), String.valueOf(r.getTeamId()), String.valueOf(r.getPlayerId()), String.valueOf(r.getIsPitcher()), String.valueOf(r.getBattingOrder()), String.valueOf(r.getPitcherOrder()), String.valueOf(r.getListOrder()), String.valueOf(r.getPosition()));
    }

    private int statRichness(GamePlayerStat r) {
        int score = 0;
        if (r.getAb() != null) {
            ++score;
        }
        if (r.getR() != null) {
            ++score;
        }
        if (r.getH() != null) {
            ++score;
        }
        if (r.getRbi() != null) {
            ++score;
        }
        if (r.getIp() != null) {
            ++score;
        }
        if (r.getPitchPa() != null) {
            ++score;
        }
        if (r.getNp() != null) {
            ++score;
        }
        if (r.getPitchH() != null) {
            ++score;
        }
        if (r.getPitchSo() != null) {
            ++score;
        }
        if (r.getPitchR() != null) {
            ++score;
        }
        return score;
    }

    @Generated
    public GamePlayerStatService(GamePlayerStatRepository gamePlayerStatRepository) {
        this.gamePlayerStatRepository = gamePlayerStatRepository;
    }
}

