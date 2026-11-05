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
import com.bsball.model.dto.PitcherRunTotalsDTO;
import com.bsball.model.dto.PitcherTransferResultDTO;
import com.bsball.model.entity.EarnedRunDecisionEntity;
import com.bsball.model.entity.EarnedRunPlayEntity;
import com.bsball.model.entity.GameFieldingPosStat;
import com.bsball.model.entity.GamePitcherTransfer;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.PitcherRunOverride;
import com.bsball.model.entity.Player;
import com.bsball.repository.EarnedRunDecisionRepository;
import com.bsball.repository.EarnedRunPlayRepository;
import com.bsball.repository.GameFieldingAdjustmentRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GamePitcherTransferRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.PitcherRunOverrideRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bsball.stats.earnedrun.EarnedRunPlay;
import com.bsball.stats.earnedrun.EarnedRunReconstructionResult;
import com.bsball.stats.earnedrun.ScoringRunner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
    private PitcherRunOverrideRepository pitcherRunOverrideRepository;
    @Mock
    private GameFieldingPosStatRepository gameFieldingPosStatRepository;
    @Mock
    private GameFieldingAdjustmentRepository gameFieldingAdjustmentRepository;
    @Mock
    private GamePitcherTransferRepository gamePitcherTransferRepository;
    @Mock
    private PlayerRepository playerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private EarnedRunReconstructionService service;

    @BeforeEach
    void setUp() {
        service = new EarnedRunReconstructionService(
                playRepository, decisionRepository, gamePlayerStatRepository,
                pitcherRunOverrideRepository, gameFieldingPosStatRepository,
                gameFieldingAdjustmentRepository, gamePitcherTransferRepository,
                playerRepository, objectMapper);
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
        EarnedRunDecisionEntity orphan = decision(700L, "EARNED");
        orphan.setId(12L);
        orphan.setPlayId(999L);
        orphan.setRunnerId(56L);
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L))
                .thenReturn(List.of(orphan, decision));
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(55L, "跑者甲"), player(700L, "投手乙"), player(701L, "上垒投手丙")));

        List<EarnedRunDecisionListDTO> rows = service.listDecisions(1L, 20L);

        assertEquals(2, rows.size());
        assertEquals(11L, rows.get(0).decisionId(), "有半局信息的行排在前面");
        assertNull(rows.get(1).inning(), "找不到 play 的行排最后");
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

    @Test
    @DisplayName("投手合计：生效覆写优先，未覆写投手保留引擎值")
    void listPitcherTotals_appliesActiveOverride() {
        GamePlayerStat overridden = pitcherStat(1L, 700L, 8, 7);
        overridden.setTeamId(1L);
        GamePlayerStat enginePitcher = pitcherStat(1L, 701L, 0, 0);
        enginePitcher.setTeamId(1L);
        when(gamePlayerStatRepository.findByGameId(20L))
                .thenReturn(List.of(overridden, enginePitcher));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                decision(700L, "EARNED"), decision(700L, "UNEARNED"),
                decision(701L, "EARNED")));
        PitcherRunOverride override = overrideRow(5L, 1L, 20L, 700L, 3, 1);
        override.setNote("现场补正");
        override.setUpdatedBy(9L);
        when(pitcherRunOverrideRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenReturn(List.of(override));
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(700L, "投手甲"), player(701L, "投手乙")));

        List<PitcherRunTotalsDTO> rows = service.listPitcherTotals(1L, 20L);

        assertEquals(2, rows.size());
        PitcherRunTotalsDTO first = rows.get(0);
        assertEquals(700L, first.playerId());
        assertEquals(3, first.runsAllowed());
        assertEquals(1, first.earnedRuns());
        assertEquals(2, first.unearnedRuns());
        assertEquals(0, first.pendingRuns());
        assertEquals(5L, first.overrideId());
        assertEquals("现场补正", first.overrideNote());
        PitcherRunTotalsDTO second = rows.get(1);
        assertEquals(701L, second.playerId());
        assertEquals(1, second.runsAllowed());
        assertEquals(1, second.earnedRuns());
        assertNull(second.overrideId());
    }

    @Test
    @DisplayName("投手覆写：ER 大于 R 或非本场投手时拒绝")
    void overridePitcherRuns_rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () ->
                service.overridePitcherRuns(1L, 20L, 700L, 2, 3, null, 9L));
        assertThrows(IllegalArgumentException.class, () ->
                service.overridePitcherRuns(1L, 20L, 700L, 2, 1, "x".repeat(201), 9L));

        when(playRepository.existsByGameId(20L)).thenReturn(true);
        when(gamePlayerStatRepository.findByGameId(20L))
                .thenReturn(List.of(pitcherStat(1L, 700L, 0, 0)));
        assertThrows(IllegalArgumentException.class, () ->
                service.overridePitcherRuns(1L, 20L, 999L, 2, 1, null, 9L));
    }

    @Test
    @DisplayName("保存投手覆写：重算时按人工值覆盖 R/ER/UER")
    void overridePitcherRuns_appliesAndReturnsTotals() {
        when(playRepository.existsByGameId(20L)).thenReturn(true);
        GamePlayerStat pitcher = pitcherStat(1L, 700L, 8, 7);
        pitcher.setTeamId(1L);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(pitcher));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                decision(700L, "EARNED"), decision(700L, "UNEARNED")));
        when(pitcherRunOverrideRepository
                .findFirstByTenantIdAndGameIdAndPlayerIdAndStatusOrderByIdDesc(
                        1L, 20L, 700L, "APPROVED")).thenReturn(Optional.empty());
        AtomicReference<PitcherRunOverride> saved = new AtomicReference<>();
        when(pitcherRunOverrideRepository.save(any(PitcherRunOverride.class)))
                .thenAnswer(invocation -> {
                    PitcherRunOverride row = invocation.getArgument(0);
                    if (row.getId() == null) {
                        row.setId(55L);
                    }
                    row.setUpdatedBy(9L);
                    row.setUpdatedAt(java.time.LocalDateTime.now());
                    saved.set(row);
                    return row;
                });
        when(pitcherRunOverrideRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenAnswer(invocation -> saved.get() == null
                        ? List.of() : List.of(saved.get()));
        when(playerRepository.findAllById(any())).thenReturn(List.of(player(700L, "投手甲")));

        PitcherRunTotalsDTO row = service.overridePitcherRuns(
                1L, 20L, 700L, 3, 1, " 补正 ", 9L);

        assertEquals(3, row.runsAllowed());
        assertEquals(1, row.earnedRuns());
        assertEquals(2, row.unearnedRuns());
        assertEquals(0, row.pendingRuns());
        assertEquals(55L, row.overrideId());
        assertEquals("补正", row.overrideNote());
        assertEquals(3, pitcher.getPitchR());
        assertEquals(1, pitcher.getEr());
        assertEquals(2, pitcher.getUnearnedR());
        assertEquals(0, pitcher.getPendingR());
        assertEquals("APPROVED", saved.get().getStatus());
    }

    @Test
    @DisplayName("撤销投手覆写：恢复引擎值")
    void revokePitcherOverride_restoresEngineTotals() {
        GamePlayerStat pitcher = pitcherStat(1L, 700L, 3, 1);
        pitcher.setTeamId(1L);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(pitcher));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                decision(700L, "EARNED"), decision(700L, "UNEARNED")));
        PitcherRunOverride row = overrideRow(55L, 1L, 20L, 700L, 3, 1);
        when(pitcherRunOverrideRepository.findByIdAndTenantIdAndGameId(55L, 1L, 20L))
                .thenReturn(Optional.of(row));
        when(pitcherRunOverrideRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenReturn(List.of());
        when(playerRepository.findAllById(any())).thenReturn(List.of(player(700L, "投手甲")));

        PitcherRunTotalsDTO totals = service.revokePitcherOverride(1L, 20L, 55L, 9L);

        assertEquals("REVOKED", row.getStatus());
        assertEquals(9L, row.getRevokedBy());
        assertNotNull(row.getRevokedAt());
        assertEquals(2, totals.runsAllowed());
        assertEquals(1, totals.earnedRuns());
        assertEquals(1, totals.unearnedRuns());
        assertEquals(2, pitcher.getPitchR());
        assertEquals(1, pitcher.getEr());
    }

    @Test
    @DisplayName("撤销覆写：无判定投手归零")
    void revokePitcherOverride_zeroesPitcherWithoutDecisions() {
        GamePlayerStat pitcher = pitcherStat(1L, 700L, 5, 4);
        pitcher.setTeamId(1L);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(pitcher));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of());
        PitcherRunOverride row = overrideRow(55L, 1L, 20L, 700L, 5, 4);
        when(pitcherRunOverrideRepository.findByIdAndTenantIdAndGameId(55L, 1L, 20L))
                .thenReturn(Optional.of(row));
        when(pitcherRunOverrideRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenReturn(List.of());
        when(playerRepository.findAllById(any())).thenReturn(List.of(player(700L, "投手甲")));

        PitcherRunTotalsDTO totals = service.revokePitcherOverride(1L, 20L, 55L, 9L);

        assertEquals(0, pitcher.getPitchR());
        assertEquals(0, pitcher.getEr());
        assertEquals(0, totals.runsAllowed());
        assertEquals(0, totals.earnedRuns());
    }

    @Test
    @DisplayName("重建半局：仅失效受影响投手的覆写")
    void replaceHalfInning_invalidatesAffectedOverridesOnly() {
        EarnedRunPlayEntity oldPlay = new EarnedRunPlayEntity();
        oldPlay.setId(88L);
        when(playRepository.findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
                1L, 20L, 3, "top")).thenReturn(List.of(oldPlay));
        when(decisionRepository.findByTenantIdAndGameIdAndPlayIdIn(
                1L, 20L, List.of(88L)))
                .thenReturn(List.of(decisionForPlay(700L, 88L)));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of());
        PitcherRunOverride affected = overrideRow(1L, 1L, 20L, 700L, 2, 1);
        PitcherRunOverride untouched = overrideRow(2L, 1L, 20L, 701L, 1, 1);
        List<PitcherRunOverride> active = new ArrayList<>(List.of(affected, untouched));
        when(pitcherRunOverrideRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenAnswer(invocation -> active.stream()
                        .filter(row -> "APPROVED".equals(row.getStatus())).toList());
        when(pitcherRunOverrideRepository.save(any(PitcherRunOverride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        GamePlayerStat affectedStat = pitcherStat(1L, 700L, 5, 3);
        affectedStat.setTeamId(1L);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(affectedStat));

        service.replaceHalfInning(1L, 20L, 3, "top", 9L, List.of(), 500L);

        assertEquals("INVALIDATED", affected.getStatus());
        assertEquals("APPROVED", untouched.getStatus());
        assertNotNull(affected.getInvalidReason());
        assertEquals(0, affectedStat.getPitchR(), "失效且无判定的投手 R 应归零");
        assertEquals(0, affectedStat.getEr(), "失效且无判定的投手 ER 应归零");
    }

    @Test
    @DisplayName("结果页覆写：只更新所选投手，不触发全场引擎重算")
    void applyResultPitcherRunOverrides_updatesOnlySelectedPitcher() {
        when(playRepository.existsByGameId(20L)).thenReturn(true);
        GamePlayerStat selected = pitcherStat(1L, 700L, 2, 1);
        selected.setTeamId(1L);
        GamePlayerStat other = pitcherStat(1L, 701L, 5, 4);
        other.setTeamId(1L);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(selected, other));
        when(pitcherRunOverrideRepository
                .findFirstByTenantIdAndGameIdAndPlayerIdAndStatusOrderByIdDesc(
                        1L, 20L, 700L, "APPROVED")).thenReturn(Optional.empty());
        when(pitcherRunOverrideRepository.save(any(PitcherRunOverride.class)))
                .thenAnswer(invocation -> {
                    PitcherRunOverride row = invocation.getArgument(0);
                    if (row.getId() == null) {
                        row.setId(1L);
                    }
                    return row;
                });

        service.applyResultPitcherRunOverrides(1L, 20L, Map.of(700L, new int[]{6, 5}), 9L);

        assertEquals(6, selected.getPitchR());
        assertEquals(5, selected.getEr());
        assertEquals(1, selected.getUnearnedR());
        assertEquals(0, selected.getPendingR());
        assertEquals(5, other.getPitchR(), "未编辑投手不得被引擎重算覆盖");
        assertEquals(4, other.getEr());
        verify(decisionRepository, never()).findByTenantIdAndGameId(1L, 20L);
    }

    @Test
    @DisplayName("投手转记：目标已有值优先，源行可转记字段清零并写审计")
    void transferPitcher_mergesTargetPriorityAndZerosSource() {
        GamePlayerStat source = pitcherStat(1L, 700L, 0, 0);
        source.setId(100L);
        source.setTeamId(1L);
        source.setNp(27);
        source.setPitchPa(11);
        source.setFo(3);
        source.setIp(0d);
        GamePlayerStat target = pitcherStat(1L, 701L, 3, 3);
        target.setId(101L);
        target.setTeamId(1L);
        target.setNp(0);
        target.setPitchPa(0);
        target.setFo(0);
        target.setIp(1.2d);
        target.setPitchBf(11);
        target.setPitchAb(7);
        when(gamePlayerStatRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(source));
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(source, target));
        when(gameFieldingPosStatRepository.findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L))
                .thenReturn(List.of());
        when(gamePitcherTransferRepository.save(any(GamePitcherTransfer.class)))
                .thenAnswer(invocation -> {
                    GamePitcherTransfer transfer = invocation.getArgument(0);
                    transfer.setId(55L);
                    return transfer;
                });
        when(gamePlayerStatRepository.save(any(GamePlayerStat.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PitcherTransferResultDTO result = service.transferPitcher(
                1L, 20L, 100L, 701L, "补正", 9L);

        assertEquals(Integer.valueOf(27), target.getNp());
        assertEquals(Integer.valueOf(11), target.getPitchPa());
        assertEquals(Integer.valueOf(3), target.getFo());
        assertEquals(Double.valueOf(1.2d), target.getIp());
        assertEquals(Integer.valueOf(0), source.getNp());
        assertEquals(Integer.valueOf(0), source.getPitchPa());
        assertEquals(Integer.valueOf(0), source.getFo());
        assertEquals(Long.valueOf(55L), result.transferId());
        assertEquals("TRANSFERRED", result.status());
        verify(gamePitcherTransferRepository).save(any(GamePitcherTransfer.class));
    }

    @Test
    @DisplayName("投手转记：源行仍有失分或自责分判定时拒绝")
    void transferPitcher_rejectsEarnedRunData() {
        GamePlayerStat source = pitcherStat(1L, 700L, 1, 0);
        source.setId(100L);
        source.setTeamId(1L);
        when(gamePlayerStatRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(source));

        assertThrows(IllegalArgumentException.class,
                () -> service.transferPitcher(1L, 20L, 100L, 701L, null, 9L));
        verify(gamePitcherTransferRepository, never()).save(any(GamePitcherTransfer.class));
    }

    @Test
    @DisplayName("投手转记：源行删除必须全零且无判定")
    void deletePitcherTransferSource_deletesZeroSource() {
        GamePitcherTransfer transfer = new GamePitcherTransfer();
        transfer.setId(55L);
        transfer.setTenantId(1L);
        transfer.setGameId(20L);
        transfer.setSourceStatId(100L);
        transfer.setSourcePlayerId(700L);
        transfer.setStatus("TRANSFERRED");
        GamePlayerStat source = pitcherStat(1L, 700L, 0, 0);
        source.setId(100L);
        source.setTeamId(1L);
        source.setNp(0);
        source.setPitchPa(0);
        when(gamePitcherTransferRepository.findByIdAndTenantIdAndGameId(55L, 1L, 20L))
                .thenReturn(Optional.of(transfer));
        when(gamePlayerStatRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(source));
        when(gameFieldingPosStatRepository.findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L))
                .thenReturn(List.of());
        when(gamePitcherTransferRepository.save(any(GamePitcherTransfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.deletePitcherTransferSource(1L, 20L, 55L, 9L);

        assertEquals("DELETED", transfer.getStatus());
        assertEquals(Long.valueOf(9L), transfer.getSourceDeletedBy());
        verify(gamePlayerStatRepository).delete(source);
    }

    @Test
    @DisplayName("投手已人工覆写时拒绝修改其判定")
    void overrideDecision_rejectsWhenPitcherHasActiveOverride() {
        EarnedRunDecisionEntity decision = decision(700L, "EARNED");
        decision.setId(77L);
        decision.setTenantId(1L);
        decision.setGameId(20L);
        when(decisionRepository.findByIdAndTenantIdAndGameId(77L, 1L, 20L))
                .thenReturn(Optional.of(decision));
        when(pitcherRunOverrideRepository
                .existsByTenantIdAndGameIdAndPlayerIdAndStatus(1L, 20L, 700L, "APPROVED"))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.overrideDecision(
                1L, 20L, 77L, 9L, "UNEARNED", "ERROR_REACHED_BASE"));
    }

    private PitcherRunOverride overrideRow(long id, long tenantId, long gameId,
            long playerId, int runs, int earned) {
        PitcherRunOverride row = new PitcherRunOverride();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setGameId(gameId);
        row.setPlayerId(playerId);
        row.setRunsAllowed(runs);
        row.setEarnedRuns(earned);
        row.setStatus("APPROVED");
        return row;
    }

    private EarnedRunDecisionEntity decisionForPlay(Long pitcherId, Long playId) {
        EarnedRunDecisionEntity decision = decision(pitcherId, "EARNED");
        decision.setPlayId(playId);
        return decision;
    }

    private Player player(long id, String name) {
        Player player = new Player();
        player.setId(id);
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
