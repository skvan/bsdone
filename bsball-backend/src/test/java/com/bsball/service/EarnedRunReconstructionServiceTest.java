package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.model.dto.EarnedRunDecisionListDTO;
import com.bsball.model.entity.EarnedRunDecisionEntity;
import com.bsball.model.entity.EarnedRunPlayEntity;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.Player;
import com.bsball.repository.EarnedRunDecisionRepository;
import com.bsball.repository.EarnedRunPlayRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.stats.earnedrun.EarnedRunPlay;
import com.bsball.stats.earnedrun.EarnedRunReconstructionResult;
import com.bsball.stats.earnedrun.ScoringRunner;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import com.bsball.repository.PlayerRepository;
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
    @Mock
    private PlayerRepository playerRepository;
    private EarnedRunReconstructionService service;

    @BeforeEach
    void setUp() {
        service = new EarnedRunReconstructionService(
                playRepository, decisionRepository, gamePlayerStatRepository, playerRepository);
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
    @DisplayName("删除投手行前按场次与责任投手查询自责分判定")
    void hasResponsiblePitcherDecisions_delegatesAndGuardsInvalidIds() {
        when(decisionRepository.existsByGameIdAndResponsiblePitcherId(20L, 700L))
                .thenReturn(Boolean.TRUE);

        assertTrue(service.hasResponsiblePitcherDecisions(20L, 700L));
        verify(decisionRepository).existsByGameIdAndResponsiblePitcherId(20L, 700L);
        assertFalse(service.hasResponsiblePitcherDecisions(0L, 700L));
        assertFalse(service.hasResponsiblePitcherDecisions(20L, 0L));
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

    @Test
    @DisplayName("结算验证：R 口径、ER≤R、负 UER、重复判定与责任投手缺失均进入违规清单")
    void validateSettlement_reportsSpecViolations() {
        GamePlayerStat erOverR = pitcherStat(1L, 700L, 1, 2);
        erOverR.setUnearnedR(-1);
        erOverR.setPendingR(0);
        GamePlayerStat sumMismatch = pitcherStat(1L, 701L, 5, 1);
        sumMismatch.setUnearnedR(0);
        sumMismatch.setPendingR(0);
        when(gamePlayerStatRepository.findByGameId(20L))
                .thenReturn(List.of(erOverR, sumMismatch));

        EarnedRunDecisionEntity first = decision(700L, "EARNED");
        first.setId(1L);
        first.setPlayId(900L);
        first.setRunnerId(55L);
        EarnedRunDecisionEntity duplicate = decision(700L, "EARNED");
        duplicate.setId(2L);
        duplicate.setPlayId(900L);
        duplicate.setRunnerId(55L);
        EarnedRunDecisionEntity missingPitcher = decision(null, "UNEARNED");
        missingPitcher.setId(3L);
        missingPitcher.setPlayId(901L);
        missingPitcher.setRunnerId(56L);
        EarnedRunDecisionEntity pendingWithoutPitcher = decision(null, "PENDING");
        pendingWithoutPitcher.setId(4L);
        pendingWithoutPitcher.setPlayId(902L);
        pendingWithoutPitcher.setRunnerId(57L);
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L))
                .thenReturn(List.of(first, duplicate, missingPitcher, pendingWithoutPitcher));

        List<String> violations = service.validateSettlement(1L, 20L);

        assertTrue(violations.stream().anyMatch(v -> v.contains("§11.1")),
                "R != ER+UER+PENDING 必须报出");
        assertTrue(violations.stream().anyMatch(v -> v.contains("§11.2")),
                "ER > R 必须报出");
        assertTrue(violations.stream().anyMatch(v -> v.contains("§11.3")),
                "UER < 0 必须报出");
        assertTrue(violations.stream().anyMatch(v -> v.contains("§11.4")),
                "重复判定必须报出");
        assertEquals(1, violations.stream().filter(v -> v.contains("§11.5")).count(),
                "只有非 PENDING 判定缺责任投手才违规");
    }

    @Test
    @DisplayName("判定清单：带出半局/序号/姓名与覆写信息")
    void listDecisions_mapsPlayAndPlayerNames() {
        EarnedRunPlayEntity play = new EarnedRunPlayEntity();
        play.setId(900L);
        play.setInning(3);
        play.setHalf("top");
        play.setSequence(2);
        when(playRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(play));

        EarnedRunDecisionEntity decision = decision(700L, "UNEARNED");
        decision.setId(11L);
        decision.setPlayId(900L);
        decision.setRunnerId(55L);
        decision.setReachedBasePitcherId(701L);
        decision.setRunnerOrigin("ERROR");
        decision.setOverriddenBy(9L);
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(decision));
        when(playerRepository.findById(55L)).thenReturn(Optional.of(player("跑者甲")));
        when(playerRepository.findById(700L)).thenReturn(Optional.of(player("投手乙")));
        when(playerRepository.findById(701L)).thenReturn(Optional.of(player("上垒投手丙")));

        List<EarnedRunDecisionListDTO> rows = service.listDecisions(1L, 20L);

        assertEquals(1, rows.size());
        EarnedRunDecisionListDTO row = rows.get(0);
        assertEquals(11L, row.decisionId());
        assertEquals(3, row.inning());
        assertEquals("top", row.half());
        assertEquals(2, row.sequence());
        assertEquals("跑者甲", row.runnerName());
        assertEquals("投手乙", row.pitcherName());
        assertEquals("上垒投手丙", row.reachedBasePitcherName());
        assertEquals("ERROR", row.runnerOrigin());
        assertEquals(9L, row.overriddenBy());
    }

    private Player player(String name) {
        Player player = new Player();
        player.setName(name);
        return player;
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

    private EarnedRunDecisionEntity decision(Long pitcherId, String status) {
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
