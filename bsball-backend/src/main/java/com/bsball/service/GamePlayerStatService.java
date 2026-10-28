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

import com.bsball.exception.BusinessException;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.utils.GamePlayerStatZeros;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import lombok.Generated;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class GamePlayerStatService {
    private final GamePlayerStatRepository gamePlayerStatRepository;
    private final GameRepository gameRepository;
    private final EventRegistrationService eventRegistrationService;
    private final EarnedRunReconstructionService earnedRunReconstructionService;

    public List<GamePlayerStat> listByGame(Long gameId) {
        List<GamePlayerStat> rows = this.gamePlayerStatRepository.findByGameId(gameId);
        LinkedHashMap<String, GamePlayerStat> dedup = new LinkedHashMap<String, GamePlayerStat>();
        for (GamePlayerStat row : rows) {
            String key = this.buildKey(row);
            GamePlayerStat prev = (GamePlayerStat)dedup.get(key);
            if (prev != null && this.statRichness(row) <= this.statRichness(prev)) continue;
            dedup.put(key, row);
        }
        return List.copyOf(dedup.values());
    }

    @Transactional
    public GamePlayerStat create(GamePlayerStat entity) {
        if (entity != null) {
            // create 只允许新建；带 id 的请求必须走 update，避免 merge 绕过引擎守卫
            entity.setId(null);
            this.assertRosterForWrite(entity.getGameId(), entity.getTeamId(), entity.getPlayerId());
            if (this.isEngineManaged(entity.getGameId())) {
                this.zeroEngineTotals(entity);
            }
        }
        return (GamePlayerStat)this.gamePlayerStatRepository.save(entity);
    }

    @Transactional
    public GamePlayerStat update(Long id, GamePlayerStat entity) {
        GamePlayerStat existing = this.gamePlayerStatRepository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        // 门②（Issue #310）：按"合并后最终 teamId/playerId"校验（载荷缺省字段沿用既有值）
        Long finalGameId = entity.getGameId() != null ? entity.getGameId() : existing.getGameId();
        Long finalTeamId = entity.getTeamId() != null ? entity.getTeamId() : existing.getTeamId();
        Long finalPlayerId = entity.getPlayerId() != null ? entity.getPlayerId() : existing.getPlayerId();
        this.assertRosterForWrite(finalGameId, finalTeamId, finalPlayerId);
        boolean engineManaged = this.isEngineManaged(existing.getGameId())
                || this.isEngineManaged(finalGameId);
        if (engineManaged) {
            if (!Objects.equals(finalGameId, existing.getGameId())) {
                throw new BusinessException(400, "引擎场次不能移动统计行");
            }
            Integer incomingPitcher = entity.getIsPitcher() != null
                    ? entity.getIsPitcher() : existing.getIsPitcher();
            if (!Objects.equals(finalTeamId, existing.getTeamId())
                    || !Objects.equals(finalPlayerId, existing.getPlayerId())
                    || !Objects.equals(incomingPitcher, existing.getIsPitcher())) {
                throw new BusinessException(400, "引擎场次不能改变统计行身份");
            }
            if (Integer.valueOf(1).equals(existing.getIsPitcher())) {
                entity.setIsPitcher(existing.getIsPitcher());
                entity.setPitchR(existing.getPitchR());
                entity.setEr(existing.getEr());
                entity.setUnearnedR(existing.getUnearnedR());
                entity.setPendingR(existing.getPendingR());
            }
        }
        entity.setGameId(finalGameId);
        entity.setTeamId(finalTeamId);
        entity.setPlayerId(finalPlayerId);
        if (entity.getIsPitcher() == null) {
            entity.setIsPitcher(existing.getIsPitcher());
        }
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        return (GamePlayerStat)this.gamePlayerStatRepository.save(entity);
    }

    /** 门②：统计单条写入的秩序册校验（首次引入名单外球员 → 400；未受限赛事跳过）。 */
    private void assertRosterForWrite(Long gameId, Long teamId, Long playerId) {
        if (gameId == null) {
            return;
        }
        Game game = this.gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            return;
        }
        this.eventRegistrationService.assertStatsPartsInRoster(game.getEventId(), gameId,
                List.of(new EventRegistrationService.StatsRef(teamId, playerId)));
    }

    @Transactional
    public void delete(Long id) {
        GamePlayerStat existing = this.gamePlayerStatRepository.findById(id).orElse(null);
        if (existing != null && this.isEngineManaged(existing.getGameId())
                && Integer.valueOf(1).equals(existing.getIsPitcher())) {
            this.assertEnginePitcherRowDeletable(existing);
        }
        this.gamePlayerStatRepository.deleteById(id);
    }

    /** 引擎投手行由 ER 引擎维护，只有“全零且没有自责分判定”时才允许删除（换人错记后的转记/清理）。 */
    private void assertEnginePitcherRowDeletable(GamePlayerStat existing) {
        if (!GamePlayerStatZeros.isZero(existing)) {
            throw new BusinessException(400, "该投手行仍有数据，请先转记后再删除");
        }
        Long gameId = existing.getGameId();
        Long playerId = existing.getPlayerId();
        if (gameId != null && playerId != null
                && this.earnedRunReconstructionService.hasResponsiblePitcherDecisions(gameId, playerId)) {
            throw new BusinessException(400, "该投手仍有自责分判定，请先处理后再删除");
        }
    }

    private boolean isEngineManaged(Long gameId) {
        return gameId != null && this.earnedRunReconstructionService.hasEngineData(gameId);
    }

    private void zeroEngineTotals(GamePlayerStat entity) {
        entity.setPitchR(0);
        entity.setEr(0);
        entity.setUnearnedR(0);
        entity.setPendingR(0);
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
    public GamePlayerStatService(GamePlayerStatRepository gamePlayerStatRepository,
            GameRepository gameRepository,
            EventRegistrationService eventRegistrationService,
            EarnedRunReconstructionService earnedRunReconstructionService) {
        this.gamePlayerStatRepository = gamePlayerStatRepository;
        this.gameRepository = gameRepository;
        this.eventRegistrationService = eventRegistrationService;
        this.earnedRunReconstructionService = earnedRunReconstructionService;
    }
}

