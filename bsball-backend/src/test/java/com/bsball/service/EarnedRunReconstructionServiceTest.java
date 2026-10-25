package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.model.entity.EarnedRunDecisionEntity;
import com.bsball.model.entity.EarnedRunPlayEntity;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.repository.EarnedRunDecisionRepository;
import com.bsball.repository.EarnedRunPlayRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.stats.earnedrun.EarnedRunPlay;
import com.bsball.stats.earnedrun.EarnedRunReconstructionResult;
import com.bsball.stats.earnedrun.ScoringRunner;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EarnedRunReconstructionServiceTest {
    @Mock
    private EarnedRunPlayRepository playRepository;
    @Mock
    private EarnedRunDecisionRepository decisionRepository;
    @Mock
    private GamePlayerStatRepository gamePlayerStatRepository;
    private EarnedRunReconstructionService service;

    @BeforeEach
    void setUp() {
        service = new EarnedRunReconstructionService(
                playRepository, decisionRepository, gamePlayerStatRepository);
        GamePlayerStat tenantOnePitcher = pitcherStat(1L, 700L, 0, 0);
        GamePlayerStat tenantOneReliever = pitcherStat(1L, 701L, 0, 0);
        GamePlayerStat tenantTenPitcher = pitcherStat(10L, 901L, 0, 0);
        lenient().when(gamePlayerStatRepository.findByGameId(20L))
                .thenReturn(List.of(tenantOnePitcher, tenantOneReliever, tenantTenPitcher));
        lenient().when(playRepository.save(any(EarnedRunPlayEntity.class)))
                .thenAnswer(invocation -> {
                    EarnedRunPlayEntity entity = invocation.getArgument(0);
                    entity.setId(900L);
                    return entity;
                });
        lenient().when(decisionRepository.save(any(EarnedRunDecisionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(gamePlayerStatRepository.save(any(GamePlayerStat.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("重建半局时保存逐球事件与责任投手判定")
    void replacesHalfInningAndPersistsDecisions() {
        when(playRepository.findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
                1L, 20L, 3, "top")).thenReturn(List.of());
        when(playRepository.save(any(EarnedRunPlayEntity.class))).thenAnswer(invocation -> {
            EarnedRunPlayEntity entity = invocation.getArgument(0);
            entity.setId(901L);
            return entity;
        });
        EarnedRunPlay play = new EarnedRunPlay(
                101L, 1, 0, 0, false,
                List.of(ScoringRunner.normal(55L, 700L)));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L))
                .thenReturn(List.of(decision(700L, "EARNED")));

        EarnedRunReconstructionResult result = service.replaceHalfInning(
                1L, 20L, 3, "top", 9L, List.of(play), 500L);

        assertEquals(1, result.pitcherTotals().get(700L).earnedRuns());
        assertEquals(1, result.pitcherAbsoluteTotals().get(700L).runs());
        assertEquals(1, result.pitcherAbsoluteTotals().get(700L).earnedRuns());
        assertEquals(result.pitcherAbsoluteTotals().get(700L).runs(),
                result.pitcherAbsoluteTotals().get(700L).earnedRuns()
                        + result.pitcherAbsoluteTotals().get(700L).unearnedRuns()
                        + result.pitcherAbsoluteTotals().get(700L).pendingRuns());
        assertEquals(0, result.pitcherAbsoluteTotals().get(701L).runs(),
                "同租户未触及投手也应返回绝对值");
        // 其他租户的投手不应出现在本次绝对总量里
        assertNull(result.pitcherAbsoluteTotals().get(901L));
        ArgumentCaptor<EarnedRunDecisionEntity> captor =
                ArgumentCaptor.forClass(EarnedRunDecisionEntity.class);
        verify(decisionRepository).save(captor.capture());
        assertEquals(700L, captor.getValue().getResponsiblePitcherId());
        assertEquals("EARNED", captor.getValue().getEarnedStatus());
        assertEquals("NORMAL", captor.getValue().getRunnerOrigin());
        assertEquals(null, captor.getValue().getWouldScoreWithoutMisplay());
    }

    @Test
    @DisplayName("同一半局重复重建：绝对总量不重复累计")
    void repeatedReconstruction_keepsAbsoluteTotalsIdempotent() {
        EarnedRunPlayEntity oldPlay = new EarnedRunPlayEntity();
        oldPlay.setId(901L);
        when(playRepository.findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
                1L, 20L, 5, "top")).thenReturn(List.of(), List.of(oldPlay));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L))
                .thenReturn(List.of(decision(700L, "EARNED")));
        EarnedRunPlay play = new EarnedRunPlay(
                101L, 1, 0, 0, false,
                List.of(ScoringRunner.normal(55L, 700L)));

        EarnedRunReconstructionResult first = service.replaceHalfInning(
                1L, 20L, 5, "top", 9L, List.of(play), 500L);
        EarnedRunReconstructionResult second = service.replaceHalfInning(
                1L, 20L, 5, "top", 9L, List.of(play), 500L);

        assertEquals(1, first.pitcherAbsoluteTotals().get(700L).runs());
        assertEquals(1, second.pitcherAbsoluteTotals().get(700L).runs());
        assertEquals(1, second.pitcherAbsoluteTotals().get(700L).earnedRuns());
    }

    @Test
    @DisplayName("本半局无得分事件时仍返回全投手绝对值")
    void emptyDeltaReturnsAbsoluteTotals() {
        when(playRepository.findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
                1L, 20L, 4, "top")).thenReturn(List.of());

        EarnedRunReconstructionResult result = service.replaceHalfInning(
                1L, 20L, 4, "top", 9L, List.of(), 500L);

        assertEquals(0, result.pitcherTotals().size());
        assertEquals(0, result.pitcherAbsoluteTotals().get(700L).runs());
        assertEquals(0, result.pitcherAbsoluteTotals().get(701L).runs());
        assertNull(result.pitcherAbsoluteTotals().get(901L));
    }

    @Test
    @DisplayName("人工覆核后按全量判定绝对重算投手统计")
    void overrideDecisionRecomputesPitcherTotalsAbsolutely() {
        EarnedRunDecisionEntity decision = decision(700L, "EARNED");
        decision.setId(77L);
        decision.setTenantId(1L);
        decision.setGameId(20L);
        GamePlayerStat pitcher = pitcherStat(1L, 700L, 3, 2);
        when(decisionRepository.findByIdAndTenantIdAndGameId(77L, 1L, 20L))
                .thenReturn(java.util.Optional.of(decision));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L))
                .thenReturn(List.of(decision));
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(pitcher));

        EarnedRunDecisionEntity saved = service.overrideDecision(
                1L, 20L, 77L, 9L, "UNEARNED", "ERROR_REACHED_BASE");

        assertEquals("UNEARNED", saved.getEarnedStatus());
        assertEquals(9L, saved.getOverriddenBy());
        assertNotNull(saved.getOverriddenAt());
        assertEquals(1, pitcher.getPitchR());
        assertEquals(0, pitcher.getEr());
        assertEquals(1, pitcher.getUnearnedR());
        assertEquals(0, pitcher.getPendingR());
    }

    @Test
    @DisplayName("人工覆核必须保存操作者与时间")
    void persistsManualOverrideAuditFields() {
        when(playRepository.findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
                1L, 20L, 3, "bottom")).thenReturn(List.of());
        when(playRepository.save(any(EarnedRunPlayEntity.class))).thenAnswer(invocation -> {
            EarnedRunPlayEntity entity = invocation.getArgument(0);
            entity.setId(902L);
            return entity;
        });
        ScoringRunner runner = new ScoringRunner(
                55L,
                700L,
                com.bsball.stats.earnedrun.RunnerOrigin.PASSED_BALL,
                com.bsball.stats.earnedrun.EarnedRunStatus.UNEARNED,
                com.bsball.stats.earnedrun.UnearnedRunReason.MANUAL_OVERRIDE,
                false);

        service.replaceHalfInning(1L, 20L, 3, "bottom", 9L, List.of(
                new EarnedRunPlay(102L, 1, 0, 0, false, List.of(runner))), 500L);

        ArgumentCaptor<EarnedRunDecisionEntity> captor =
                ArgumentCaptor.forClass(EarnedRunDecisionEntity.class);
        verify(decisionRepository).save(captor.capture());
        assertEquals(9L, captor.getValue().getOverriddenBy());
        assertNotNull(captor.getValue().getOverriddenAt());
        assertEquals(false, captor.getValue().getWouldScoreWithoutMisplay());
    }

    @Test
    @DisplayName("替换半局前删除旧判定与旧事件")
    void deletesOldHalfInningBeforePersistingReplacement() {
        EarnedRunPlayEntity oldPlay = new EarnedRunPlayEntity();
        oldPlay.setId(800L);
        when(playRepository.findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
                1L, 20L, 4, "top")).thenReturn(List.of(oldPlay));

        service.replaceHalfInning(1L, 20L, 4, "top", 9L, List.of(), 500L);

        verify(decisionRepository).deleteByTenantIdAndGameIdAndPlayIdIn(
                1L, 20L, List.of(800L));
        verify(playRepository).deleteByTenantIdAndGameIdAndInningAndHalf(
                1L, 20L, 4, "top");
        verify(decisionRepository, never()).save(any());
    }

    @Test
    @DisplayName("非法半局值不得访问数据库")
    void rejectsInvalidHalfBeforeRepositoryAccess() {
        assertThrows(IllegalArgumentException.class, () -> service.replaceHalfInning(
                1L, 20L, 1, "middle", 9L, List.of(), 500L));

        verifyNoInteractions(playRepository, decisionRepository, gamePlayerStatRepository);
    }

    @Test
    @DisplayName("重判半局时按全量判定绝对覆盖投手 R/ER/UER（可修正历史膨胀）")
    void recomputesPitcherTotalsAbsolutely() {
        EarnedRunPlayEntity oldPlay = new EarnedRunPlayEntity();
        oldPlay.setId(88L);
        GamePlayerStat pitcher = pitcherStat(10L, 901L, 6, 14);
        when(playRepository.findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
                10L, 20L, 3, "top")).thenReturn(List.of(oldPlay));
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(pitcher));
        when(decisionRepository.findByTenantIdAndGameId(10L, 20L)).thenReturn(List.of(
                decision(901L, "EARNED"),
                decision(901L, "UNEARNED")));

        service.replaceHalfInning(10L, 20L, 3, "top", 30L, List.of(
                play(101L, 1, 0, 0, false,
                        ScoringRunner.reachedOnError(501L, 901L)),
                play(102L, 2, 0, 0, false,
                        ScoringRunner.normal(502L, 901L))), 500L);

        assertEquals(2, pitcher.getPitchR());
        assertEquals(1, pitcher.getEr());
        assertEquals(1, pitcher.getUnearnedR());
        assertEquals(0, pitcher.getPendingR());
        verify(gamePlayerStatRepository).save(pitcher);
    }

    @Test
    @DisplayName("责任投手统计行缺失时自动补建（teamId 取该半局守备方）并写入绝对失分")
    void createsMissingPitcherStatForResponsiblePitcher() {
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of());
        when(decisionRepository.findByTenantIdAndGameId(10L, 20L))
                .thenReturn(List.of(decision(901L, "EARNED")));
        ArgumentCaptor<GamePlayerStat> captor = ArgumentCaptor.forClass(GamePlayerStat.class);

        service.replaceHalfInning(10L, 20L, 3, "top", 30L,
                List.of(play(101L, 1, 0, 0, false,
                        ScoringRunner.normal(501L, 901L))),
                333L);

        verify(gamePlayerStatRepository).save(captor.capture());
        GamePlayerStat created = captor.getValue();
        assertEquals(901L, created.getPlayerId());
        assertEquals(333L, created.getTeamId());
        assertEquals(1, created.getIsPitcher());
        assertEquals(1, created.getPitchR());
        assertEquals(1, created.getEr());
    }

    private EarnedRunPlay play(
            long playId,
            int sequence,
            int actualOutsAdded,
            int reconstructedOutsAdded,
            boolean rulingPending,
            ScoringRunner... runners) {
        return new EarnedRunPlay(
                playId,
                sequence,
                actualOutsAdded,
                reconstructedOutsAdded,
                rulingPending,
                List.of(runners));
    }

    private EarnedRunDecisionEntity decision(long pitcherId, String status) {
        EarnedRunDecisionEntity decision = new EarnedRunDecisionEntity();
        decision.setResponsiblePitcherId(pitcherId);
        decision.setEarnedStatus(status);
        return decision;
    }

    private GamePlayerStat pitcherStat(
            long tenantId, long playerId, int pitchR, int er) {
        GamePlayerStat stat = new GamePlayerStat();
        stat.setTenantId(tenantId);
        stat.setGameId(20L);
        stat.setPlayerId(playerId);
        stat.setIsPitcher(1);
        stat.setPitchR(pitchR);
        stat.setEr(er);
        return stat;
    }
}
