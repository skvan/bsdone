package com.bsball.service;

import com.bsball.model.entity.EarnedRunDecisionEntity;
import com.bsball.model.entity.EarnedRunPlayEntity;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.repository.EarnedRunDecisionRepository;
import com.bsball.repository.EarnedRunPlayRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.stats.earnedrun.EarnedRunDecision;
import com.bsball.stats.earnedrun.EarnedRunPlay;
import com.bsball.stats.earnedrun.EarnedRunReconstructionEngine;
import com.bsball.stats.earnedrun.EarnedRunReconstructionResult;
import com.bsball.stats.earnedrun.EarnedRunStatus;
import com.bsball.stats.earnedrun.ScoringRunner;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EarnedRunReconstructionService {
    private final EarnedRunPlayRepository playRepository;
    private final EarnedRunDecisionRepository decisionRepository;
    private final GamePlayerStatRepository gamePlayerStatRepository;
    private final EarnedRunReconstructionEngine engine;

    public EarnedRunReconstructionService(
            EarnedRunPlayRepository playRepository,
            EarnedRunDecisionRepository decisionRepository,
            GamePlayerStatRepository gamePlayerStatRepository) {
        this.playRepository = playRepository;
        this.decisionRepository = decisionRepository;
        this.gamePlayerStatRepository = gamePlayerStatRepository;
        this.engine = new EarnedRunReconstructionEngine();
    }

    /** Replaces and reconstructs all earned-run events for one half-inning. */
    @Transactional
    public EarnedRunReconstructionResult replaceHalfInning(
            long tenantId,
            long gameId,
            int inning,
            String half,
            long operatorId,
            List<EarnedRunPlay> plays) {
        validateIdentity(tenantId, gameId, inning, half, operatorId);
        EarnedRunReconstructionResult result = engine.reconstruct(plays);
        List<EarnedRunPlayEntity> existing = playRepository
                .findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
                        tenantId, gameId, inning, half);
        List<EarnedRunDecisionEntity> oldDecisions = findExistingDecisions(
                tenantId, gameId, existing);
        deleteExistingDecisions(tenantId, gameId, existing);
        playRepository.deleteByTenantIdAndGameIdAndInningAndHalf(
                tenantId, gameId, inning, half);
        persistReconstruction(tenantId, gameId, inning, half, operatorId, plays, result);
        applyPitcherStatDeltas(tenantId, gameId, oldDecisions, result.decisions());
        return result;
    }

    private List<EarnedRunDecisionEntity> findExistingDecisions(
            long tenantId, long gameId, List<EarnedRunPlayEntity> existing) {
        List<Long> playIds = existing.stream().map(EarnedRunPlayEntity::getId).toList();
        if (playIds.isEmpty()) {
            return List.of();
        }
        return decisionRepository.findByTenantIdAndGameIdAndPlayIdIn(
                tenantId, gameId, playIds);
    }

    private void applyPitcherStatDeltas(
            long tenantId,
            long gameId,
            List<EarnedRunDecisionEntity> oldDecisions,
            List<EarnedRunDecision> newDecisions) {
        Map<Long, int[]> deltas = new HashMap<>();
        for (EarnedRunDecisionEntity decision : oldDecisions) {
            addDelta(deltas, decision.getResponsiblePitcherId(), -1,
                    "EARNED".equals(decision.getEarnedStatus()) ? -1 : 0);
        }
        for (EarnedRunDecision decision : newDecisions) {
            addDelta(deltas, decision.responsiblePitcherId(), 1,
                    decision.status() == EarnedRunStatus.EARNED ? 1 : 0);
        }
        if (deltas.isEmpty()) {
            return;
        }

        Map<Long, GamePlayerStat> pitcherStats = new HashMap<>();
        for (GamePlayerStat stat : gamePlayerStatRepository.findByGameId(gameId)) {
            if (stat.getTenantId() != null
                    && stat.getTenantId() == tenantId
                    && stat.getPlayerId() != null
                    && Integer.valueOf(1).equals(stat.getIsPitcher())) {
                pitcherStats.put(stat.getPlayerId(), stat);
            }
        }
        for (Map.Entry<Long, int[]> entry : deltas.entrySet()) {
            GamePlayerStat stat = pitcherStats.get(entry.getKey());
            if (stat == null) {
                throw new IllegalStateException(
                        "Missing pitcher stat row for responsible pitcher " + entry.getKey());
            }
            int[] delta = entry.getValue();
            stat.setPitchR(Math.max(0, valueOrZero(stat.getPitchR()) + delta[0]));
            stat.setEr(Math.max(0, valueOrZero(stat.getEr()) + delta[1]));
            gamePlayerStatRepository.save(stat);
        }
    }

    private void addDelta(Map<Long, int[]> deltas, Long pitcherId, int runDelta, int earnedDelta) {
        int[] value = deltas.computeIfAbsent(pitcherId, ignored -> new int[2]);
        value[0] += runDelta;
        value[1] += earnedDelta;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private void persistReconstruction(
            long tenantId,
            long gameId,
            int inning,
            String half,
            long operatorId,
            List<EarnedRunPlay> plays,
            EarnedRunReconstructionResult result) {
        Map<Long, EarnedRunPlayEntity> savedPlays = new HashMap<>();
        for (EarnedRunPlay play : plays) {
            EarnedRunPlayEntity saved = playRepository.save(toEntity(
                    tenantId, gameId, inning, half, play));
            savedPlays.put(play.playId(), saved);
        }
        Map<String, ScoringRunner> runners = indexRunners(plays);
        for (EarnedRunDecision decision : result.decisions()) {
            ScoringRunner runner = runners.get(decisionKey(decision.playId(), decision.runnerId()));
            EarnedRunPlayEntity play = savedPlays.get(decision.playId());
            decisionRepository.save(toEntity(
                    tenantId, gameId, operatorId, play.getId(), runner, decision));
        }
    }

    private EarnedRunPlayEntity toEntity(
            long tenantId, long gameId, int inning, String half, EarnedRunPlay play) {
        EarnedRunPlayEntity entity = new EarnedRunPlayEntity();
        entity.setTenantId(tenantId);
        entity.setGameId(gameId);
        entity.setInning(inning);
        entity.setHalf(half);
        entity.setSequence(play.sequence());
        entity.setSourceEventId(String.valueOf(play.playId()));
        entity.setActualOutsAdded(play.actualOutsAdded());
        entity.setReconstructedOutsAdded(play.reconstructedOutsAdded());
        entity.setRulingPending(play.rulingPending());
        return entity;
    }

    private EarnedRunDecisionEntity toEntity(
            long tenantId,
            long gameId,
            long operatorId,
            long playId,
            ScoringRunner runner,
            EarnedRunDecision decision) {
        EarnedRunDecisionEntity entity = new EarnedRunDecisionEntity();
        entity.setTenantId(tenantId);
        entity.setGameId(gameId);
        entity.setPlayId(playId);
        entity.setRunnerId(decision.runnerId());
        entity.setResponsiblePitcherId(decision.responsiblePitcherId());
        entity.setRunnerOrigin(runner.origin().name());
        entity.setEarnedStatus(decision.status().name());
        entity.setUnearnedReason(decision.reason() == null ? null : decision.reason().name());
        if (runner.overrideStatus() != null) {
            entity.setOverriddenBy(operatorId);
            entity.setOverriddenAt(LocalDateTime.now());
        }
        return entity;
    }

    private Map<String, ScoringRunner> indexRunners(List<EarnedRunPlay> plays) {
        Map<String, ScoringRunner> runners = new HashMap<>();
        for (EarnedRunPlay play : plays) {
            for (ScoringRunner runner : play.scoringRunners()) {
                runners.put(decisionKey(play.playId(), runner.runnerId()), runner);
            }
        }
        return runners;
    }

    private String decisionKey(long playId, long runnerId) {
        return playId + ":" + runnerId;
    }

    private void deleteExistingDecisions(
            long tenantId, long gameId, List<EarnedRunPlayEntity> existing) {
        List<Long> playIds = existing.stream().map(EarnedRunPlayEntity::getId).toList();
        if (!playIds.isEmpty()) {
            decisionRepository.deleteByTenantIdAndGameIdAndPlayIdIn(tenantId, gameId, playIds);
        }
    }

    private void validateIdentity(
            long tenantId, long gameId, int inning, String half, long operatorId) {
        if (tenantId <= 0 || gameId <= 0 || inning <= 0 || operatorId <= 0) {
            throw new IllegalArgumentException("Tenant, game, inning and operator must be positive");
        }
        if (!"top".equals(half) && !"bottom".equals(half)) {
            throw new IllegalArgumentException("Half must be top or bottom");
        }
    }
}
