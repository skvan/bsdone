package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.model.dto.EarnedRunDecisionListDTO;
import com.bsball.model.dto.EarnedRunPlayKeyDTO;
import com.bsball.model.dto.PitcherRunTotalsDTO;
import com.bsball.model.dto.PitcherTransferResultDTO;
import com.bsball.model.entity.EarnedRunDecisionEntity;
import com.bsball.model.entity.EarnedRunPlayEntity;
import com.bsball.model.entity.ErDecisionAdjustment;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GameFieldingAdjustment;
import com.bsball.model.entity.GameFieldingPosStat;
import com.bsball.model.entity.GameIdentityTransfer;
import com.bsball.model.entity.GamePitcherTransfer;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.PitcherRunOverride;
import com.bsball.model.entity.Player;
import com.bsball.repository.EarnedRunDecisionRepository;
import com.bsball.repository.EarnedRunPlayRepository;
import com.bsball.repository.ErDecisionAdjustmentRepository;
import com.bsball.repository.GameFieldingAdjustmentRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GameIdentityTransferRepository;
import com.bsball.repository.GamePitcherTransferRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.GameSnapshotRepository;
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
    private GameIdentityTransferRepository gameIdentityTransferRepository;
    @Mock
    private ErDecisionAdjustmentRepository erDecisionAdjustmentRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameSnapshotRepository gameSnapshotRepository;
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
                gameIdentityTransferRepository, erDecisionAdjustmentRepository,
                gameRepository, gameSnapshotRepository, playerRepository, objectMapper);
        lenient().when(gameSnapshotRepository.findSnapshotJsonById(any())).thenReturn(Optional.empty());
        Game game = new Game();
        game.setId(20L);
        game.setTenantId(1L);
        lenient().when(gameRepository.findById(20L)).thenReturn(Optional.of(game));
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
        lenient().when(gameIdentityTransferRepository.save(any(GameIdentityTransfer.class)))
                .thenAnswer(invocation -> {
                    GameIdentityTransfer row = invocation.getArgument(0);
                    if (row.getId() == null) {
                        row.setId(77L);
                    }
                    return row;
                });
        lenient().when(erDecisionAdjustmentRepository.save(any(ErDecisionAdjustment.class)))
                .thenAnswer(invocation -> {
                    ErDecisionAdjustment row = invocation.getArgument(0);
                    if (row.getId() == null) {
                        row.setId(88L);
                    }
                    return row;
                });
        lenient().when(gamePlayerStatRepository.save(any(GamePlayerStat.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("删除投手行前按有效责任投手查询自责分判定")
    void hasResponsiblePitcherDecisions_usesEffectivePitcher() {
        EarnedRunPlayEntity play = play(900L, "play-1");
        EarnedRunDecisionEntity decision = decision(700L, "EARNED");
        decision.setId(11L);
        decision.setPlayId(900L);
        decision.setRunnerId(55L);
        when(decisionRepository.findByGameId(20L)).thenReturn(List.of(decision));
        when(playRepository.findByGameId(20L)).thenReturn(List.of(play));
        when(erDecisionAdjustmentRepository.findByGameIdAndStatus(20L, "ACTIVE"))
                .thenReturn(List.of());

        assertTrue(service.hasResponsiblePitcherDecisions(20L, 700L));
        assertFalse(service.hasResponsiblePitcherDecisions(20L, 701L));
        assertFalse(service.hasResponsiblePitcherDecisions(0L, 700L));
        assertFalse(service.hasResponsiblePitcherDecisions(20L, 0L));
    }

    @Test
    @DisplayName("判定改派后：统计归目标投手，原投手不再被删除守卫拦截")
    void reassignDecision_movesEffectivePitcherAndStats() {
        GamePlayerStat original = pitcherStat(1L, 700L, 1, 1);
        original.setTeamId(1L);
        GamePlayerStat target = pitcherStat(1L, 701L, 0, 0);
        target.setTeamId(1L);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(original, target));
        EarnedRunPlayEntity play = play(900L, "play-1");
        EarnedRunDecisionEntity decision = decision(700L, "EARNED");
        decision.setId(11L);
        decision.setPlayId(900L);
        decision.setRunnerId(55L);
        when(playRepository.findById(900L)).thenReturn(Optional.of(play));
        when(decisionRepository.findByIdAndTenantIdAndGameId(11L, 1L, 20L))
                .thenReturn(Optional.of(decision));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L))
                .thenReturn(List.of(decision));
        when(playRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(play));
        List<ErDecisionAdjustment> activeRows = new ArrayList<>();
        ErDecisionAdjustment previous = adjustment(87L, "VOID", null);
        activeRows.add(previous);
        stubActiveAdjustmentStore(activeRows);
        when(erDecisionAdjustmentRepository
                .findByTenantIdAndGameIdAndSourceEventIdAndRunnerIdAndStatus(
                        1L, 20L, "play-1", 55L, "ACTIVE"))
                .thenReturn(List.of(previous));
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(55L, "跑者甲"), player(700L, "投手乙"), player(701L, "投手丙")));

        EarnedRunDecisionListDTO result = service.reassignDecision(
                1L, 20L, 11L, 701L, "现场更正", 9L);

        assertEquals(701L, result.effectivePitcherId());
        assertEquals("投手丙", result.effectivePitcherName());
        assertEquals("REASSIGN", result.adjustmentAction());
        assertEquals(1, target.getPitchR());
        assertEquals(1, target.getEr());
        assertEquals(0, original.getPitchR());
        assertEquals("SUPERSEDED", previous.getStatus());
        verify(erDecisionAdjustmentRepository).saveAndFlush(previous);
        ArgumentCaptor<ErDecisionAdjustment> captor =
                ArgumentCaptor.forClass(ErDecisionAdjustment.class);
        verify(erDecisionAdjustmentRepository).save(captor.capture());
        assertEquals("ACTIVE", captor.getValue().getStatus());
        assertEquals(701L, captor.getValue().getTargetPitcherId());
    }

    @Test
    @DisplayName("判定作废后：不再计入任何投手 R/ER")
    void voidDecision_excludesDecisionFromTotals() {
        GamePlayerStat original = pitcherStat(1L, 700L, 1, 1);
        original.setTeamId(1L);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(original));
        EarnedRunPlayEntity play = play(900L, "play-1");
        EarnedRunDecisionEntity decision = decision(700L, "EARNED");
        decision.setId(11L);
        decision.setPlayId(900L);
        decision.setRunnerId(55L);
        when(playRepository.findById(900L)).thenReturn(Optional.of(play));
        when(decisionRepository.findByIdAndTenantIdAndGameId(11L, 1L, 20L))
                .thenReturn(Optional.of(decision));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L))
                .thenReturn(List.of(decision));
        when(playRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(play));
        List<ErDecisionAdjustment> activeRows = new ArrayList<>();
        stubActiveAdjustmentStore(activeRows);
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(55L, "跑者甲"), player(700L, "投手乙")));

        EarnedRunDecisionListDTO result = service.voidDecision(
                1L, 20L, 11L, "误录判定", 9L);

        assertTrue(result.voided());
        assertNull(result.effectivePitcherId());
        assertEquals(0, original.getPitchR());
        assertEquals(0, original.getEr());
    }

    /**
     * 作废只改「这分怎么算」，不改「这分有没有发生」：分账口径照旧把这笔剔掉，
     * 但保存守门的分母必须仍算它，否则记录员作废完再保存下一个半局，会被
     * 「重建数 < 记分栏总分」永久顶回 409（线上就是这么卡住过）。
     */
    @Test
    @DisplayName("判定作废后：不再计入投手 R/ER，但仍计入守门分母")
    void voidDecision_keepsDecisionInGuardDenominator() {
        GamePlayerStat original = pitcherStat(1L, 700L, 1, 1);
        original.setTeamId(1L);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(original));
        EarnedRunPlayEntity play = play(900L, "play-1");
        EarnedRunDecisionEntity decision = decision(700L, "EARNED");
        decision.setId(11L);
        decision.setPlayId(900L);
        decision.setRunnerId(55L);
        when(playRepository.findById(900L)).thenReturn(Optional.of(play));
        when(decisionRepository.findByIdAndTenantIdAndGameId(11L, 1L, 20L))
                .thenReturn(Optional.of(decision));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L))
                .thenReturn(List.of(decision));
        when(playRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(play));
        when(playRepository.findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(1L, 20L, 3, "top"))
                .thenReturn(List.of(play));
        List<ErDecisionAdjustment> activeRows = new ArrayList<>();
        stubActiveAdjustmentStore(activeRows);
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(55L, "跑者甲"), player(700L, "投手乙")));

        service.voidDecision(1L, 20L, 11L, "误录判定", 9L);

        assertEquals(0, original.getPitchR());
        assertEquals(0, original.getEr());
        assertEquals(1, service.countDecisionsForCoverage(1L, 20L));
        assertEquals(1, service.countDecisionsForCoverageOfHalf(1L, 20L, 3, "top"));
    }

    @Test
    @DisplayName("撤销判定改派后：恢复原责任投手与统计")
    void revokeDecisionAdjustment_restoresOriginalPitcher() {
        GamePlayerStat original = pitcherStat(1L, 700L, 0, 0);
        original.setTeamId(1L);
        GamePlayerStat target = pitcherStat(1L, 701L, 1, 1);
        target.setTeamId(1L);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(original, target));
        EarnedRunPlayEntity play = play(900L, "play-1");
        EarnedRunDecisionEntity decision = decision(700L, "EARNED");
        decision.setId(11L);
        decision.setPlayId(900L);
        decision.setRunnerId(55L);
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L))
                .thenReturn(List.of(decision));
        when(playRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(play));
        ErDecisionAdjustment adjustment = new ErDecisionAdjustment();
        adjustment.setId(88L);
        adjustment.setTenantId(1L);
        adjustment.setGameId(20L);
        adjustment.setDecisionId(11L);
        adjustment.setSourceEventId("play-1");
        adjustment.setRunnerId(55L);
        adjustment.setAction("REASSIGN");
        adjustment.setTargetPitcherId(701L);
        adjustment.setStatus("ACTIVE");
        when(erDecisionAdjustmentRepository
                .findByIdAndTenantIdAndGameId(88L, 1L, 20L))
                .thenReturn(Optional.of(adjustment));
        List<ErDecisionAdjustment> activeRows = new ArrayList<>();
        activeRows.add(adjustment);
        stubActiveAdjustmentStore(activeRows);
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(55L, "跑者甲"), player(700L, "投手乙"), player(701L, "投手丙")));

        EarnedRunDecisionListDTO result = service.revokeDecisionAdjustment(1L, 20L, 88L, 9L);

        assertEquals("REVOKED", adjustment.getStatus());
        assertEquals(700L, result.effectivePitcherId());
        assertEquals(1, original.getPitchR());
        assertEquals(0, target.getPitchR());
    }

    @Test
    @DisplayName("删除守卫：改派后按目标投手判断，原投手放行")
    void hasResponsiblePitcherDecisions_usesReassignedTarget() {
        EarnedRunPlayEntity play = play(900L, "play-1");
        EarnedRunDecisionEntity decision = decision(700L, "EARNED");
        decision.setId(11L);
        decision.setPlayId(900L);
        decision.setRunnerId(55L);
        ErDecisionAdjustment adjustment = new ErDecisionAdjustment();
        adjustment.setId(88L);
        adjustment.setSourceEventId("play-1");
        adjustment.setRunnerId(55L);
        adjustment.setAction("REASSIGN");
        adjustment.setTargetPitcherId(701L);
        adjustment.setStatus("ACTIVE");
        when(decisionRepository.findByGameId(20L)).thenReturn(List.of(decision));
        when(playRepository.findByGameId(20L)).thenReturn(List.of(play));
        when(erDecisionAdjustmentRepository.findByGameIdAndStatus(20L, "ACTIVE"))
                .thenReturn(List.of(adjustment));

        assertFalse(service.hasResponsiblePitcherDecisions(20L, 700L));
        assertTrue(service.hasResponsiblePitcherDecisions(20L, 701L));
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
    @DisplayName("§11.6 责任投手在场内没有投手统计行时进入违规清单（幻影投手，R/ER 无处落账）")
    void validateSettlement_reportsResponsiblePitcherWithoutStatRow() {
        when(gamePlayerStatRepository.findByGameId(20L))
                .thenReturn(List.of(pitcherStat(1L, 700L, 1, 1)));
        EarnedRunDecisionEntity normal = decision(700L, "EARNED");
        normal.setId(6L);
        normal.setPlayId(904L);
        normal.setRunnerId(59L);
        EarnedRunDecisionEntity phantom = decision(888L, "EARNED");
        phantom.setId(5L);
        phantom.setPlayId(903L);
        phantom.setRunnerId(58L);
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L))
                .thenReturn(List.of(normal, phantom));

        List<String> violations = service.validateSettlement(1L, 20L);

        assertEquals(1, violations.stream().filter(v -> v.contains("§11.6")).count(),
                "只在场内缺投手统计行的责任投手才报幻影投手");
        assertTrue(violations.stream().anyMatch(v -> v.contains("§11.6") && v.contains("888")),
                "违规信息要带出该投手 id 供人工复核");
    }

    @Test
    @DisplayName("§11.7 判定笔数多于总分时进入违规清单（线上实测：255 判定 14、总分 13）")
    void validateSettlement_reportsJudgedRunCountAgainstScore() {
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of());
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                scoredDecision(1L, 900L, 55L), scoredDecision(2L, 901L, 56L),
                scoredDecision(3L, 902L, 57L), scoredDecision(4L, 903L, 58L)));
        // 总分 2:1 = 3，判定却有 4 笔
        when(gameRepository.findById(20L)).thenReturn(Optional.of(
                scoreGame(2, 1, null, null, 7L, 8L)));

        List<String> violations = service.validateSettlement(1L, 20L);

        assertTrue(violations.stream().anyMatch(v -> v.contains("§11.7") && v.contains("+1")),
                "判定多于比分时一定是判定层有比分层没记到的分数，必须报出并带差额方向");
    }

    @Test
    @DisplayName("§11.7 判定少于比分不报警：混合来源场次（结果录入补分）的常態，线上实测 260")
    void validateSettlement_staysQuietWhenJudgedRunsBelowScore() {
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of());
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                scoredDecision(1L, 900L, 55L)));
        // 总分 1:17 = 18，live 只判定过 1 笔：不能当成记录链路的缺陷
        when(gameRepository.findById(20L)).thenReturn(Optional.of(
                scoreGame(1, 17, null, null, 7L, 8L)));

        List<String> violations = service.validateSettlement(1L, 20L);

        assertTrue(violations.stream().noneMatch(v -> v.contains("§11.7")),
                "判定少于总分不该报警：" + violations);
    }

    @Test
    @DisplayName("§11.8 某队投手群 R 合计不等于对手得分时进入违规清单")
    void validateSettlement_reportsPitcherStaffRunsAgainstOpponentScore() {
        GamePlayerStat away1 = pitcherStat(1L, 700L, 5, 4);
        away1.setTeamId(7L);
        GamePlayerStat away2 = pitcherStat(1L, 701L, 3, 3);
        away2.setTeamId(7L);
        GamePlayerStat home = pitcherStat(1L, 702L, 3, 3);
        home.setTeamId(8L);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(away1, away2, home));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of());
        // 客队（teamId 7）投手合计 8 分，但主队只得 7 分；主队投手 3 = 客队得分 3
        when(gameRepository.findById(20L)).thenReturn(Optional.of(
                scoreGame(3, 7, "[3,4]", "[2,4,2]", 7L, 8L)));

        List<String> violations = service.validateSettlement(1L, 20L);

        assertEquals(1, violations.stream().filter(v -> v.contains("§11.8")).count(),
                "只报对不上的那一队");
        assertTrue(violations.stream().anyMatch(v -> v.contains("§11.8") && v.contains("teamId=7")
                && v.contains("+1")), "违规信息要点名队伍与差额");
    }

    @Test
    @DisplayName("§11.8 投手群 pitchR 全为 NULL 时不报警：该队从未被落账，比分与 R 走结果录入")
    void validateSettlement_skipsPitcherStaffWhenNeverSettled() {
        GamePlayerStat nullRuns = new GamePlayerStat();
        nullRuns.setTenantId(1L);
        nullRuns.setPlayerId(700L);
        nullRuns.setIsPitcher(1);
        nullRuns.setTeamId(7L);
        // pitchR 保持 null：引擎从没为这队写过四栏（线上实测 235/248 即此形）
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(nullRuns));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of());
        when(gameRepository.findById(20L)).thenReturn(Optional.of(
                scoreGame(4, 2, null, null, 7L, 8L)));

        List<String> violations = service.validateSettlement(1L, 20L);

        assertTrue(violations.stream().noneMatch(v -> v.contains("§11.8")),
                "未落账不等于失 0 分，不该报错：" + violations);
    }

    @Test
    @DisplayName("§11.8 显式归零（pitchR=0）算已落账：要被拖进对账，不能当成 NULL")
    void validateSettlement_treatsZeroedPitcherStaffAsSettled() {
        GamePlayerStat zeroed = pitcherStat(1L, 700L, 0, 0);   // 走 applyPitcherTotals(stat, new int[4]) 的形状
        zeroed.setTeamId(7L);
        zeroed.setUnearnedR(0);
        zeroed.setPendingR(0);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(zeroed));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of());
        when(gameRepository.findById(20L)).thenReturn(Optional.of(
                scoreGame(4, 2, null, null, 7L, 8L)));

        List<String> violations = service.validateSettlement(1L, 20L);

        // 客队（teamId 7）投手群归零为 0，但主队（home）得 2 分 → 必须报错；
        // 这条钉住 NULL/0 的分界：归零路径会把未落账的队变成“已落账且失 0 分”。
        assertTrue(violations.stream().anyMatch(v -> v.contains("§11.8") && v.contains("teamId=7")),
                "归零的队要被看见对不上，而不是静悄悄：" + violations);
    }

    @Test
    @DisplayName("§11.9 总分与逐局比分加总不符（线上实测：278 总分 22、逐局和 23）")
    void validateSettlement_reportsTotalAgainstLineScore() {
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of());
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of());
        when(gameRepository.findById(20L)).thenReturn(Optional.of(
                scoreGame(15, 7, "[7,8]", "[2,4,2]", 7L, 8L)));

        List<String> violations = service.validateSettlement(1L, 20L);

        assertEquals(1, violations.stream().filter(v -> v.contains("§11.9")).count(),
                "客队逐局和 15 = 总分 15 不报，只报主队 7 vs 8");
        assertTrue(violations.stream().anyMatch(v -> v.contains("§11.9") && v.contains("home")),
                "违规信息要分清是哪一队");
    }

    @Test
    @DisplayName("三层加总互相对得上时不得误报 §11.7–§11.9")
    void validateSettlement_staysQuietWhenTotalsCohere() {
        // 客队投手守主队打序：teamId=7 的投手群承 1 分（=主队得分），teamId=8 的投手群承 2 分（=客队得分）
        GamePlayerStat awayPitcher = pitcherStat(1L, 700L, 1, 1);
        awayPitcher.setTeamId(7L);
        GamePlayerStat homePitcher = pitcherStat(1L, 701L, 2, 2);
        homePitcher.setTeamId(8L);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(awayPitcher, homePitcher));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                scoredDecision(1L, 900L, 55L), scoredDecision(2L, 901L, 56L), scoredDecision(3L, 902L, 57L)));
        // 总分 2:1 = 3 = 判定 3 笔；逐局加总也齐（客队 1+1、主队 1）
        when(gameRepository.findById(20L)).thenReturn(Optional.of(
                scoreGame(2, 1, "[1,1]", "[1]", 7L, 8L)));

        List<String> violations = service.validateSettlement(1L, 20L);

        assertTrue(violations.stream().noneMatch(v -> v.contains("§11.7") || v.contains("§11.8")
                        || v.contains("§11.9")),
                "自洽场次不应产生全场级违规：" + violations);
    }

    @Test
    @DisplayName("§11.7–§11.9 跳过裁定/特殊结果场次：比分按规则给出，不参与逐笔加总检查")
    void validateSettlement_skipsTotalsForSpecialResult() {
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of());
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                scoredDecision(1L, 900L, 55L)));
        Game special = scoreGame(7, 0, "[3,4]", null, 7L, 8L);
        special.setIsSpecialResult(Boolean.TRUE);
        when(gameRepository.findById(20L)).thenReturn(Optional.of(special));

        List<String> violations = service.validateSettlement(1L, 20L);

        assertTrue(violations.stream().noneMatch(v -> v.contains("§11.7") || v.contains("§11.8")
                        || v.contains("§11.9")),
                "裁定场次不该产生全场级违规：" + violations);
    }

    @Test
    @DisplayName("§11.10 半局有逐棒事件但判定缺笔：点名哪一半局、差多少（线上 game 261）")
    void validateSettlement_reportsMissingDecisionOfOneHalfInning() {
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of());
        // 全场级完全平：判定 3 笔 = 总分 3:0，逐局加总也对得上——正因为两笔差额互相抵销，缺笔只能靠半局粒度抓
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                scoredDecision(1L, 900L, 55L), scoredDecision(2L, 901L, 56L),
                scoredDecision(3L, 902L, 57L)));
        when(playRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                playOfHalf(900L, 1, "top"), playOfHalf(901L, 3, "bottom"),
                playOfHalf(902L, 3, "bottom")));
        when(gameRepository.findById(20L)).thenReturn(Optional.of(
                scoreGame(0, 3, "[0]", "[0,0,3]", 7L, 8L)));

        List<String> violations = service.validateSettlement(1L, 20L);

        assertTrue(violations.stream().anyMatch(v -> v.contains("§11.10")
                        && v.contains("第 3 局下") && v.contains("-1")),
                "要点名缺笔的那一半局与方向：" + violations);
        assertTrue(violations.stream().anyMatch(v -> v.contains("§11.10")
                        && v.contains("第 1 局上") && v.contains("+1")),
                "抵销另一侧的多判也要看见：" + violations);
        assertTrue(violations.stream().noneMatch(v -> v.contains("§11.7") || v.contains("§11.9")),
                "这形状在全场级是平的，不该报：" + violations);
    }

    @Test
    @DisplayName("§11.10 该半局判定多于记分：差额方向为 +")
    void validateSettlement_reportsExtraDecisionOfOneHalfInning() {
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of());
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                scoredDecision(1L, 900L, 55L), scoredDecision(2L, 901L, 56L)));
        when(playRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                playOfHalf(900L, 2, "bottom"), playOfHalf(901L, 2, "bottom")));
        // 总分 0:2 与判定 2 笔平；主队 2 局下记分 1 分、另有 1 局下 1 分
        when(gameRepository.findById(20L)).thenReturn(Optional.of(
                scoreGame(0, 2, null, "[1,1]", 7L, 8L)));

        List<String> violations = service.validateSettlement(1L, 20L);

        assertTrue(violations.stream().anyMatch(v -> v.contains("§11.10")
                        && v.contains("第 2 局下") && v.contains("+1")),
                "判定层多出记分层没有的分也要按半局点名：" + violations);
    }

    @Test
    @DisplayName("§11.10 不误报：半局判定与记分一致、或根本无逐棒事件（纯结果录入）")
    void validateSettlement_staysQuietOnCohesiveAndLegacyHalves() {
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of());
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                scoredDecision(1L, 900L, 55L), scoredDecision(2L, 901L, 56L)));
        when(playRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                playOfHalf(900L, 1, "top"), playOfHalf(901L, 2, "top")));
        when(gameRepository.findById(20L)).thenReturn(Optional.of(
                scoreGame(2, 0, "[1,1]", null, 7L, 8L)));

        assertTrue(service.validateSettlement(1L, 20L).stream()
                        .noneMatch(v -> v.contains("§11.10")),
                "对得上的半局不该产生违规");

        // 同一比分下拿掉事件列（结果录入场次，线上实测 235/248）：依旧不报
        when(playRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of());
        assertTrue(service.validateSettlement(1L, 20L).stream()
                        .noneMatch(v -> v.contains("§11.10")),
                "无逐棒证据的场次不参与半局对帐");
    }

    @Test
    @DisplayName("§11.10 记分有分、判定层一笔都没有的半局也被点名（强制换局形状）")
    void validateSettlement_reportsHalfWithRunsButNoDecisionAtAll() {
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of());
        // 1局上正常定案（两笔判定）；2局下被强制结束，库里连 play 列都没有，但记分栏已有 2 分
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                scoredDecision(1L, 900L, 55L), scoredDecision(2L, 901L, 56L)));
        when(playRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                playOfHalf(900L, 1, "top"), playOfHalf(901L, 1, "top")));
        when(gameRepository.findById(20L)).thenReturn(Optional.of(
                scoreGame(2, 2, "[2]", "[0,2]", 7L, 8L)));

        List<String> violations = service.validateSettlement(1L, 20L);

        assertTrue(violations.stream().anyMatch(v -> v.contains("§11.10")
                        && v.contains("第 2 局下") && v.contains("判定 0 笔")
                        && v.contains("差额 -2") && v.contains("一笔都没有")),
                "零判定的半局不能因为「没有 play 列」而逃出对帐：" + violations);
        assertTrue(violations.stream().noneMatch(v -> v.contains("§11.7") || v.contains("§11.9")),
                "判定 2 笔 < 总分 4 且逐局加总平，全场级那两条本就该闭嘴：" + violations);
    }

    @Test
    @DisplayName("§11.10 差额里有人工作废：成因指向作废，不得说成事件缺笔")
    void validateSettlement_blamesVoidedDecisionNotMissingEvidence() {
        // 同一个半局里「重建跑过、判定也推上来过、只是被人抹了」与「事件根本没入库」是两种
        // 完全不同的故障。守门分母已经为这个区别改成含作废（countDecisionsForCoverage），
        // 稽核的成因文案要是还写「没跑完或被覆盖」，就与它正面矛盾、把复盘的人带偏。
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of());
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                scoredDecision(1L, 900L, 55L), scoredDecision(2L, 901L, 56L),
                scoredDecision(3L, 902L, 57L)));
        when(playRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                playOfHalf(900L, 1, "top"), playOfHalf(901L, 2, "bottom"),
                playOfHalf(902L, 2, "bottom")));
        ErDecisionAdjustment voided = adjustment(87L, "VOID", null);
        voided.setSourceEventId("901");
        voided.setRunnerId(56L);
        when(erDecisionAdjustmentRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "ACTIVE"))
                .thenReturn(List.of(voided));
        when(gameRepository.findById(20L)).thenReturn(Optional.of(
                scoreGame(1, 3, "[1]", "[0,3]", 7L, 8L)));

        List<String> violations = service.validateSettlement(1L, 20L);
        List<String> half110 = violations.stream()
                .filter(v -> v.contains("§11.10") && v.contains("第 2 局下"))
                .collect(java.util.stream.Collectors.toList());

        assertEquals(1, half110.size(), "2局下判定 1 对记分 3，应且只应点一次名：" + violations);
        assertTrue(half110.get(0).contains("差额 -2") && half110.get(0).contains("作废"),
                "有作废笔数的缺口要说作废：" + half110.get(0));
        assertFalse(half110.get(0).contains("没跑完或事件被覆盖"),
                "该半局的重建明明跑过，不能归成缺笔：" + half110.get(0));
    }

    /** 带半局归属的逐棒事件列（§11.10 要靠它把判定归到正确的半局）。 */
    private EarnedRunPlayEntity playOfHalf(Long id, int inning, String half) {
        EarnedRunPlayEntity play = new EarnedRunPlayEntity();
        play.setId(id);
        play.setTenantId(1L);
        play.setGameId(20L);
        play.setInning(Integer.valueOf(inning));
        play.setHalf(half);
        play.setSourceEventId(String.valueOf(id));
        return play;
    }

    private EarnedRunDecisionEntity scoredDecision(long id, long playId, long runnerId) {
        EarnedRunDecisionEntity decision = decision(700L, "EARNED");
        decision.setId(id);
        decision.setPlayId(playId);
        decision.setRunnerId(runnerId);
        return decision;
    }

    private Game scoreGame(int awayScore, int homeScore, String awayLine, String homeLine,
            long awayTeamId, long homeTeamId) {
        Game game = new Game();
        game.setId(20L);
        game.setTenantId(1L);
        game.setAwayScore(awayScore);
        game.setHomeScore(homeScore);
        game.setAwayScoreByInning(awayLine);
        game.setHomeScoreByInning(homeLine);
        game.setAwayTeamId(awayTeamId);
        game.setHomeTeamId(homeTeamId);
        return game;
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
    @DisplayName("投手合计：返回登板序，覆写球员无统计行时用所属球队兜底")
    void listPitcherTotals_includesPitcherOrderAndTeamFallback() {
        GamePlayerStat starter = pitcherStat(1L, 700L, 2, 1);
        starter.setTeamId(5L);
        starter.setPitcherOrder(2);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(starter));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L))
                .thenReturn(List.of(decision(700L, "EARNED")));
        PitcherRunOverride override = overrideRow(5L, 1L, 20L, 701L, 3, 2);
        when(pitcherRunOverrideRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenReturn(List.of(override));
        Player starterPlayer = player(700L, "投手甲");
        starterPlayer.setTeamId(9L);
        Player overridePlayer = player(701L, "投手乙");
        overridePlayer.setTeamId(5L);
        when(playerRepository.findAllById(any()))
                .thenReturn(List.of(starterPlayer, overridePlayer));

        List<PitcherRunTotalsDTO> rows = service.listPitcherTotals(1L, 20L);
        PitcherRunTotalsDTO starterRow = rows.stream()
                .filter(row -> Long.valueOf(700L).equals(row.playerId()))
                .findFirst().orElseThrow();
        PitcherRunTotalsDTO overrideOnlyRow = rows.stream()
                .filter(row -> Long.valueOf(701L).equals(row.playerId()))
                .findFirst().orElseThrow();

        assertEquals(5L, starterRow.teamId(), "有统计行时以统计行球队为准");
        assertEquals(2, starterRow.pitcherOrder());
        assertEquals(5L, overrideOnlyRow.teamId(), "无统计行时用球员所属球队兜底");
        assertNull(overrideOnlyRow.pitcherOrder());
    }

    @Test
    @DisplayName("投手合计：覆写球员无统计行且球员缺失时球队为空")
    void listPitcherTotals_missingPlayerKeepsTeamNull() {
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of());
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of());
        when(pitcherRunOverrideRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenReturn(List.of(overrideRow(6L, 1L, 20L, 702L, 1, 0)));
        when(playerRepository.findAllById(any())).thenReturn(List.of());

        List<PitcherRunTotalsDTO> rows = service.listPitcherTotals(1L, 20L);

        assertEquals(1, rows.size());
        assertNull(rows.get(0).teamId());
        assertNull(rows.get(0).pitcherOrder());
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
    @DisplayName("投手覆写：引擎待判定不被无声折进非自责（线上实测 game 278 投手 1275）")
    void overridePitcherRuns_keepsEnginePendingOutOfUnearned() {
        GamePlayerStat pitcher = pitcherStat(1L, 700L, 4, 3);
        pitcher.setTeamId(1L);
        pitcher.setUnearnedR(1);
        pitcher.setPendingR(0);
        PitcherRunTotalsDTO row = overrideWithDecisions(pitcher, 700L, List.of(
                decision(700L, "EARNED"), decision(700L, "EARNED"), decision(700L, "PENDING")),
                3, 1);

        // 人工额度 room = 3-1 = 2，引擎还有 1 笔待判 → PEND 保留 1、UER 只剩 1；
        // 旧口径会直接报 UER=2/PEND=0，把「还没判」说成「已判定为非自责」。
        assertEquals(3, row.runsAllowed());
        assertEquals(1, row.earnedRuns());
        assertEquals(1, row.unearnedRuns(), "待判定不得被折进非自责");
        assertEquals(1, row.pendingRuns(), "引擎的待判定必须保留");
        assertEquals(1, pitcher.getUnearnedR());
        assertEquals(1, pitcher.getPendingR());
    }

    @Test
    @DisplayName("投手覆写：人工额度装不下全部待判定时以人工值截断，恒等式仍成立")
    void overridePitcherRuns_truncatesPendingWhenManualRoomIsSmaller() {
        GamePlayerStat pitcher = pitcherStat(1L, 700L, 4, 2);
        pitcher.setTeamId(1L);
        PitcherRunTotalsDTO row = overrideWithDecisions(pitcher, 700L, List.of(
                decision(700L, "EARNED"), decision(700L, "PENDING"), decision(700L, "PENDING")),
                3, 3);

        // room = 0：记录员明确宣告这 3 分全是自责分，人工值为准，PEND 被截到 0。
        assertEquals(3, row.runsAllowed());
        assertEquals(3, row.earnedRuns());
        assertEquals(0, row.unearnedRuns());
        assertEquals(0, row.pendingRuns());
        assertEquals(row.runsAllowed(), row.earnedRuns() + row.unearnedRuns() + row.pendingRuns(),
                "§11.1 恒等式不能被截断破坏");
    }

    /** 赛后覆写的标准桩件：给定引擎判定与统计行，跑一次人工覆写并返回回读的四栏。 */
    private PitcherRunTotalsDTO overrideWithDecisions(GamePlayerStat pitcher, long playerId,
            List<EarnedRunDecisionEntity> decisions, int runs, int earned) {
        when(playRepository.existsByGameId(20L)).thenReturn(true);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(pitcher));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(decisions);
        when(pitcherRunOverrideRepository
                .findFirstByTenantIdAndGameIdAndPlayerIdAndStatusOrderByIdDesc(
                        1L, 20L, playerId, "APPROVED")).thenReturn(Optional.empty());
        AtomicReference<PitcherRunOverride> saved = new AtomicReference<>();
        when(pitcherRunOverrideRepository.save(any(PitcherRunOverride.class)))
                .thenAnswer(invocation -> {
                    PitcherRunOverride row = invocation.getArgument(0);
                    if (row.getId() == null) {
                        row.setId(55L);
                    }
                    saved.set(row);
                    return row;
                });
        when(pitcherRunOverrideRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenAnswer(invocation -> saved.get() == null
                        ? List.of() : List.of(saved.get()));
        when(playerRepository.findAllById(any()))
                .thenReturn(List.of(player(playerId, "投手甲")));
        return service.overridePitcherRuns(1L, 20L, playerId, runs, earned, "复核", 9L);
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
    @DisplayName("结果页覆写：不查判定表，但保留统计行已有的待判定")
    void applyResultPitcherRunOverrides_keepsExistingPending() {
        when(playRepository.existsByGameId(20L)).thenReturn(true);
        GamePlayerStat selected = pitcherStat(1L, 700L, 3, 1);
        selected.setTeamId(1L);
        selected.setUnearnedR(1);
        selected.setPendingR(1);   // 引擎先前落过一笔待判
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(selected));
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

        service.applyResultPitcherRunOverrides(1L, 20L, Map.of(700L, new int[]{3, 2}), 9L);

        // room = 3-2 = 1，已有 PEND=1 → 该分留在待判，不被折成已判定非自责
        assertEquals(3, selected.getPitchR());
        assertEquals(2, selected.getEr());
        assertEquals(0, selected.getUnearnedR(), "待判定不得被折进非自责");
        assertEquals(1, selected.getPendingR());
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
        source.setPickoffAttempts(2);
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
        assertEquals(Integer.valueOf(2), target.getPickoffAttempts());
        assertEquals(Integer.valueOf(3), target.getFo());
        assertEquals(Double.valueOf(1.2d), target.getIp());
        assertEquals(Integer.valueOf(0), source.getNp());
        assertEquals(Integer.valueOf(0), source.getPitchPa());
        assertEquals(Integer.valueOf(0), source.getPickoffAttempts());
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

        PitcherTransferResultDTO result = service.deletePitcherTransferSource(1L, 20L, 55L, 9L);

        assertEquals(55L, result.transferId());
        assertEquals("DELETED", result.status());
        assertEquals(700L, result.sourcePlayerId());
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

    @Test
    @DisplayName("半局未完成的重建：不删旧判定、不写新判定、不作废覆写、不改统计")
    void halfIncompleteRebuildPreviewsWithoutWriting() {
        EarnedRunPlayEntity oldPlay = new EarnedRunPlayEntity();
        oldPlay.setId(88L);
        when(playRepository.findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
                1L, 20L, 3, "top")).thenReturn(List.of(oldPlay));
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L))
                .thenReturn(List.of(decisionForPlay(700L, 88L)));

        EarnedRunReconstructionResult result = service.replaceHalfInning(
                1L, 20L, 3, "top", 9L,
                List.of(play(101L, 1, 0, 0, false, ScoringRunner.normal(55L, 701L))),
                500L, false);

        assertEquals(1, result.pitcherAbsoluteTotals().get(701L).runs());
        assertEquals(1, result.pitcherAbsoluteTotals().get(701L).earnedRuns());
        verify(playRepository, never()).save(any(EarnedRunPlayEntity.class));
        verify(playRepository, never())
                .deleteByTenantIdAndGameIdAndInningAndHalf(1L, 20L, 3, "top");
        verify(decisionRepository, never()).save(any(EarnedRunDecisionEntity.class));
        verify(decisionRepository, never())
                .deleteByTenantIdAndGameIdAndPlayIdIn(1L, 20L, List.of(88L));
        verify(pitcherRunOverrideRepository, never()).save(any(PitcherRunOverride.class));
        verify(gamePlayerStatRepository, never()).save(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("预览分母：排除本半局即将被替换的旧判定，保留其他半局，再叠上本次逐棒判定")
    void previewDenominatorReplacesOnlyTheCurrentHalf() {
        EarnedRunPlayEntity oldPlay = new EarnedRunPlayEntity();
        oldPlay.setId(88L);
        when(playRepository.findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
                1L, 20L, 3, "top")).thenReturn(List.of(oldPlay));
        EarnedRunDecisionEntity otherHalf = decisionForPlay(700L, 999L);
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L))
                .thenReturn(List.of(decisionForPlay(700L, 88L), otherHalf));

        EarnedRunReconstructionResult result = service.replaceHalfInning(
                1L, 20L, 3, "top", 9L,
                List.of(play(101L, 1, 0, 0, false, ScoringRunner.normal(55L, 701L))),
                500L, false);

        assertEquals(1, result.pitcherAbsoluteTotals().get(700L).runs(),
                "其他半局那笔保留、本半局即将被替换的那笔不能双计");
        assertEquals(1, result.pitcherAbsoluteTotals().get(701L).runs());
    }

    @Test
    @DisplayName("预览不叠「定案时会被作废」的覆写：给引擎口径，并把该名单回传")
    void previewExcludesOverrideThatSettlementWouldInvalidate() {
        when(playRepository.findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
                1L, 20L, 3, "top")).thenReturn(List.of());
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                decisionForPlay(701L, 999L), decision(701L, "PENDING")));
        when(pitcherRunOverrideRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenReturn(List.of(overrideRow(1L, 1L, 20L, 701L, 3, 1)));

        EarnedRunReconstructionResult result = service.replaceHalfInning(
                1L, 20L, 3, "top", 9L,
                List.of(play(101L, 1, 0, 0, false, ScoringRunner.normal(55L, 701L))),
                500L, false);

        EarnedRunReconstructionResult.PitcherRunTotals totals =
                result.pitcherAbsoluteTotals().get(701L);
        // 701 是本半局新判定的责任投手，定案时他的 APPROVED 覆写会被作废；
        // 预览若照叠覆写值，就是在给记录员看一个马上会被推翻的数字。
        assertEquals(3, totals.runs());
        assertEquals(2, totals.earnedRuns(), "该给引擎口径而不是覆写值：" + totals);
        assertEquals(1, totals.pendingRuns());
        assertEquals(List.of(701L), result.overrideInvalidationPitcherIds(),
                "哪些人的覆写会在定案时失效，必须随预览回传");
        verify(pitcherRunOverrideRepository, never()).save(any(PitcherRunOverride.class));
        verify(gamePlayerStatRepository, never()).save(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("预览仍保留「本半局碰不到」的投手覆写：不被作废的人工决断继续生效")
    void previewKeepsOverrideOfPitcherNotTouchedByThisHalf() {
        when(playRepository.findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
                1L, 20L, 3, "top")).thenReturn(List.of());
        when(decisionRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                decisionForPlay(701L, 999L), decisionForPlay(702L, 999L)));
        when(pitcherRunOverrideRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenReturn(List.of(overrideRow(2L, 1L, 20L, 702L, 3, 1)));

        EarnedRunReconstructionResult result = service.replaceHalfInning(
                1L, 20L, 3, "top", 9L,
                List.of(play(101L, 1, 0, 0, false, ScoringRunner.normal(55L, 701L))),
                500L, false);

        Map<Long, EarnedRunReconstructionResult.PitcherRunTotals> totals =
                result.pitcherAbsoluteTotals();
        assertEquals(2, totals.get(701L).runs(), "本半局责任投手走引擎口径");
        assertTrue(result.overrideInvalidationPitcherIds().isEmpty(),
                "702 不属受影响投手，定案不会动他的覆写：" + result.overrideInvalidationPitcherIds());
        EarnedRunReconstructionResult.PitcherRunTotals kept = totals.get(702L);
        assertEquals(3, kept.runs(), "不受影的投手仍显示人工 R/ER，与落库同款优先级");
        assertEquals(1, kept.earnedRuns());
        assertEquals(2, kept.unearnedRuns());
        assertEquals(kept.runs(), kept.earnedRuns() + kept.unearnedRuns() + kept.pendingRuns(),
                "四栏恒等式必须成立");
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
        player.setTeamId(1L);
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

    @Test
    @DisplayName("身份转记预览：同队球员返回统计行且可转记")
    void previewIdentityTransferReturnsRows() {
        GamePlayerStat source = batterStat(700L, 3, 2);
        GamePlayerStat target = batterStat(701L, 0, 0);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(source, target));
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L)).thenReturn(List.of());
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(700L, "源球员"), player(701L, "目标球员")));

        var preview = service.previewIdentityTransfer(1L, 20L, 700L, 701L);

        assertTrue(preview.transferable());
        assertEquals("源球员", preview.sourcePlayerName());
        assertEquals("目标球员", preview.targetPlayerName());
        assertEquals(1, preview.rows().size());
        assertTrue(preview.rows().get(0).targetExists());
    }

    @Test
    @DisplayName("身份转记：统计字段目标优先合并并清零源行")
    void transferIdentityMergesStatsAndZeroesSource() {
        GamePlayerStat source = batterStat(700L, 3, 2);
        GamePlayerStat target = batterStat(701L, 0, 0);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(source, target));
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L)).thenReturn(List.of());
        when(decisionRepository.findByGameId(20L)).thenReturn(List.of());
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(700L, "源球员"), player(701L, "目标球员")));

        var result = service.transferIdentity(
                1L, 20L, 700L, 701L, List.of("STATS"), "更正", 9L);

        assertEquals(77L, result.transferId());
        assertEquals("TRANSFERRED", result.status());
        assertEquals(3, target.getPa());
        assertEquals(2, target.getH());
        assertEquals(0, source.getPa());
        assertEquals(0, source.getH());
        ArgumentCaptor<GameIdentityTransfer> captor =
                ArgumentCaptor.forClass(GameIdentityTransfer.class);
        verify(gameIdentityTransferRepository).save(captor.capture());
        assertEquals("STATS", captor.getValue().getScopes());
        assertEquals(700L, captor.getValue().getSourcePlayerId());
        assertEquals(701L, captor.getValue().getTargetPlayerId());
    }

    @Test
    @DisplayName("身份转记预览：目标无本场数据时必须在源球队名单中")
    void previewIdentityTransferRejectsTargetOutsideTeam() {
        GamePlayerStat source = batterStat(700L, 3, 2);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(source));
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L)).thenReturn(List.of());
        Player otherTeamTarget = player(701L, "别队球员");
        otherTeamTarget.setTeamId(2L);
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(700L, "源球员"), otherTeamTarget));

        var preview = service.previewIdentityTransfer(1L, 20L, 700L, 701L);

        assertFalse(preview.transferable());
        assertTrue(preview.blockers().contains("目标球员不在源球队名单中"));
    }

    @Test
    @DisplayName("身份转记预览：源数据全零时拒绝重复转记")
    void previewIdentityTransferRejectsAllZeroSource() {
        GamePlayerStat source = batterStat(700L, 0, 0);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(source));
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L)).thenReturn(List.of());
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(700L, "源球员"), player(701L, "目标球员")));

        var preview = service.previewIdentityTransfer(1L, 20L, 700L, 701L);

        assertFalse(preview.transferable());
        assertTrue(preview.blockers().contains("源球员没有可转记的数据"));
    }

    @Test
    @DisplayName("身份转记：拒绝空范围与未知范围")
    void transferIdentityRejectsInvalidScopes() {
        GamePlayerStat source = batterStat(700L, 3, 2);
        GamePlayerStat target = batterStat(701L, 0, 0);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(source, target));
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L)).thenReturn(List.of());
        when(decisionRepository.findByGameId(20L)).thenReturn(List.of());
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(700L, "源球员"), player(701L, "目标球员")));

        IllegalArgumentException empty = assertThrows(IllegalArgumentException.class,
                () -> service.transferIdentity(1L, 20L, 700L, 701L, List.of(), null, 9L));
        assertEquals("至少选择一个转记范围", empty.getMessage());
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> service.transferIdentity(
                        1L, 20L, 700L, 701L, List.of("STATS", "UNKNOWN"), null, 9L));
        assertTrue(unknown.getMessage().contains("不支持的转记范围"));
    }

    @Test
    @DisplayName("身份转记：删除源数据要求全零并写入 DELETED 审计")
    void deleteIdentityTransferSourceMarksDeleted() {
        GameIdentityTransfer transfer = new GameIdentityTransfer();
        transfer.setId(77L);
        transfer.setTenantId(1L);
        transfer.setGameId(20L);
        transfer.setSourcePlayerId(700L);
        transfer.setTargetPlayerId(701L);
        transfer.setStatus("TRANSFERRED");
        when(gameIdentityTransferRepository.findByIdAndTenantIdAndGameId(77L, 1L, 20L))
                .thenReturn(Optional.of(transfer));
        GamePlayerStat source = batterStat(700L, 0, 0);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(source));
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L)).thenReturn(List.of());
        when(decisionRepository.findByGameId(20L)).thenReturn(List.of());
        when(pitcherRunOverrideRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenReturn(List.of());

        var result = service.deleteIdentityTransferSource(1L, 20L, 77L, 9L);

        assertEquals("DELETED", result.status());
        assertEquals(9L, transfer.getSourceDeletedBy());
        verify(gamePlayerStatRepository).delete(source);
    }

    @Test
    @DisplayName("身份转记：源数据非零时拒绝删除")
    void deleteIdentityTransferSourceRejectsNonZeroSource() {
        GameIdentityTransfer transfer = new GameIdentityTransfer();
        transfer.setId(77L);
        transfer.setSourcePlayerId(700L);
        transfer.setStatus("TRANSFERRED");
        when(gameIdentityTransferRepository.findByIdAndTenantIdAndGameId(77L, 1L, 20L))
                .thenReturn(Optional.of(transfer));
        when(gamePlayerStatRepository.findByGameId(20L))
                .thenReturn(List.of(batterStat(700L, 3, 2)));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.deleteIdentityTransferSource(1L, 20L, 77L, 9L));

        assertTrue(error.getMessage().contains("源球员仍有统计"));
        verify(gamePlayerStatRepository, never()).delete(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("身份转记：FIELDING 范围移动守备补记并阻断同一 Play 冲突")
    void transferIdentityMovesFieldingAdjustments() {
        GamePlayerStat source = batterStat(700L, 3, 2);
        GamePlayerStat target = batterStat(701L, 0, 0);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(source, target));
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L)).thenReturn(List.of());
        when(decisionRepository.findByGameId(20L)).thenReturn(List.of());
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(700L, "源球员"), player(701L, "目标球员")));
        GameFieldingAdjustment adjustment = fieldingAdjustment(700L, "play-1");
        when(gameFieldingAdjustmentRepository
                .findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenReturn(List.of(adjustment));

        var preview = service.previewIdentityTransfer(1L, 20L, 700L, 701L);
        assertEquals(1, preview.fieldingAdjustmentCount());

        service.transferIdentity(1L, 20L, 700L, 701L, List.of("FIELDING"), null, 9L);

        assertEquals(701L, adjustment.getPlayerId());
        verify(gameFieldingAdjustmentRepository).save(adjustment);
    }

    @Test
    @DisplayName("身份转记：目标同一 Play 已有守备补记时阻断")
    void previewIdentityTransferBlocksFieldingAdjustmentConflict() {
        GamePlayerStat source = batterStat(700L, 3, 2);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(source));
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L)).thenReturn(List.of());
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(700L, "源球员"), player(701L, "目标球员")));
        when(gameFieldingAdjustmentRepository
                .findByTenantIdAndGameIdAndStatus(1L, 20L, "APPROVED"))
                .thenReturn(List.of(fieldingAdjustment(700L, "play-1")));
        when(gameFieldingAdjustmentRepository
                .findFirstByTenantIdAndGameIdAndSourceEventIdAndPlayerIdOrderByIdAsc(
                        1L, 20L, "play-1", 701L))
                .thenReturn(Optional.of(fieldingAdjustment(701L, "play-1")));

        var preview = service.previewIdentityTransfer(1L, 20L, 700L, 701L);

        assertFalse(preview.transferable());
        assertTrue(preview.blockers().contains("目标球员在同一失误 Play 已有守备补记"));
    }

    @Test
    @DisplayName("身份转记：LIVE_RECORDS 改写现场记录并保留审计快照")
    void transferIdentityRewritesLiveSnapshot() {
        GamePlayerStat source = batterStat(700L, 3, 2);
        GamePlayerStat target = batterStat(701L, 0, 0);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(source, target));
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L)).thenReturn(List.of());
        when(decisionRepository.findByGameId(20L)).thenReturn(List.of());
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(700L, "源球员"), player(701L, "目标球员")));
        Game game = new Game();
        game.setId(20L);
        game.setTenantId(1L);
        when(gameSnapshotRepository.findSnapshotJsonById(20L)).thenReturn(Optional.of("""
                {"gameId":20,"homeLineup":[{"id":700}],
                 "playEvents":[{"batterPlayerId":700,
                   "scoringRunners":[{"runnerId":700,"responsiblePitcherId":700}]}],
                 "playerStatsById":{"700":{"batting":{"pa":3}}}}
                """));
        when(gameRepository.findById(20L)).thenReturn(Optional.of(game));

        var preview = service.previewIdentityTransfer(1L, 20L, 700L, 701L);
        assertTrue(preview.liveSnapshotAvailable());
        assertTrue(preview.liveReferenceCount() >= 4);

        service.transferIdentity(
                1L, 20L, 700L, 701L, List.of("LIVE_RECORDS"), "现场更正", 9L);

        ArgumentCaptor<String> snapshotCaptor = ArgumentCaptor.forClass(String.class);
        verify(gameSnapshotRepository).updateSnapshotJson(eq(20L), snapshotCaptor.capture(), eq(9L));
        String rewritten = snapshotCaptor.getValue();
        assertTrue(rewritten.contains("\"id\":701"));
        assertFalse(rewritten.contains("\"id\":700"));
        assertFalse(rewritten.contains("\"batterPlayerId\":700"));
        verify(gameRepository).save(game);
        ArgumentCaptor<GameIdentityTransfer> captor =
                ArgumentCaptor.forClass(GameIdentityTransfer.class);
        verify(gameIdentityTransferRepository).save(captor.capture());
        assertTrue(captor.getValue().getSourceBefore().contains("liveSnapshot"));
        assertTrue(captor.getValue().getSourceBefore().contains("\"batterPlayerId\":700"));
    }

    @Test
    @DisplayName("身份转记：删除源数据时守备补记仍生效则拒绝")
    void deleteIdentityTransferSourceRejectsFieldingAdjustment() {
        GameIdentityTransfer transfer = new GameIdentityTransfer();
        transfer.setId(77L);
        transfer.setSourcePlayerId(700L);
        transfer.setStatus("TRANSFERRED");
        when(gameIdentityTransferRepository.findByIdAndTenantIdAndGameId(77L, 1L, 20L))
                .thenReturn(Optional.of(transfer));
        when(gamePlayerStatRepository.findByGameId(20L))
                .thenReturn(List.of(batterStat(700L, 0, 0)));
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L)).thenReturn(List.of());
        when(decisionRepository.findByGameId(20L)).thenReturn(List.of());
        when(gameFieldingAdjustmentRepository
                .existsByTenantIdAndGameIdAndPlayerIdAndStatus(1L, 20L, 700L, "APPROVED"))
                .thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.deleteIdentityTransferSource(1L, 20L, 77L, 9L));

        assertTrue(error.getMessage().contains("守备补记"));
        verify(gamePlayerStatRepository, never()).delete(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("身份转记：删除源数据时现场记录仍引用源球员则拒绝")
    void deleteIdentityTransferSourceRejectsLiveReferences() {
        GameIdentityTransfer transfer = new GameIdentityTransfer();
        transfer.setId(77L);
        transfer.setSourcePlayerId(700L);
        transfer.setStatus("TRANSFERRED");
        when(gameIdentityTransferRepository.findByIdAndTenantIdAndGameId(77L, 1L, 20L))
                .thenReturn(Optional.of(transfer));
        when(gamePlayerStatRepository.findByGameId(20L))
                .thenReturn(List.of(batterStat(700L, 0, 0)));
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L)).thenReturn(List.of());
        when(decisionRepository.findByGameId(20L)).thenReturn(List.of());
        Game game = new Game();
        game.setId(20L);
        game.setTenantId(1L);
        when(gameSnapshotRepository.findSnapshotJsonById(20L))
                .thenReturn(Optional.of("{\"playEvents\":[{\"batterPlayerId\":700}]}"));
        when(gameRepository.findById(20L)).thenReturn(Optional.of(game));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.deleteIdentityTransferSource(1L, 20L, 77L, 9L));

        assertTrue(error.getMessage().contains("现场记录仍引用源球员"));
        verify(gamePlayerStatRepository, never()).delete(any(GamePlayerStat.class));
    }

    @Test
    @DisplayName("身份转记：统计行已转走后仍可按现场记录范围补转")
    void previewIdentityTransferAllowsLiveOnlyFollowUp() {
        GamePlayerStat target = batterStat(701L, 0, 0);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(target));
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L)).thenReturn(List.of());
        when(decisionRepository.findByGameId(20L)).thenReturn(List.of());
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(700L, "源球员"), player(701L, "目标球员")));
        GameIdentityTransfer previous = new GameIdentityTransfer();
        previous.setSourcePlayerId(700L);
        previous.setTargetPlayerId(701L);
        previous.setSourceTeamId(1L);
        previous.setStatus("TRANSFERRED");
        when(gameIdentityTransferRepository
                .findFirstByTenantIdAndGameIdAndSourcePlayerIdAndTargetPlayerIdAndStatusOrderByIdDesc(
                        1L, 20L, 700L, 701L, "TRANSFERRED"))
                .thenReturn(Optional.of(previous));
        Game game = new Game();
        game.setId(20L);
        game.setTenantId(1L);
        when(gameSnapshotRepository.findSnapshotJsonById(20L))
                .thenReturn(Optional.of("{\"playEvents\":[{\"batterPlayerId\":700}]}"));
        when(gameRepository.findById(20L)).thenReturn(Optional.of(game));

        var preview = service.previewIdentityTransfer(1L, 20L, 700L, 701L);

        assertTrue(preview.transferable());
        assertEquals(1L, preview.teamId());
        assertTrue(preview.liveReferenceCount() > 0);
    }

    @Test
    @DisplayName("身份转记：目标已在同一阵容时按现场记录范围拒绝")
    void transferIdentityRejectsLiveRosterConflict() {
        GamePlayerStat source = batterStat(700L, 3, 2);
        GamePlayerStat target = batterStat(701L, 0, 0);
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(source, target));
        when(gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(20L)).thenReturn(List.of());
        when(decisionRepository.findByGameId(20L)).thenReturn(List.of());
        when(playerRepository.findAllById(any())).thenReturn(List.of(
                player(700L, "源球员"), player(701L, "目标球员")));
        Game game = new Game();
        game.setId(20L);
        game.setTenantId(1L);
        when(gameSnapshotRepository.findSnapshotJsonById(20L))
                .thenReturn(Optional.of("{\"homeLineup\":[{\"id\":700},{\"id\":701}]}"));
        when(gameRepository.findById(20L)).thenReturn(Optional.of(game));

        var preview = service.previewIdentityTransfer(1L, 20L, 700L, 701L);
        assertTrue(preview.transferable());
        assertTrue(preview.liveRosterConflict());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.transferIdentity(
                        1L, 20L, 700L, 701L, List.of("LIVE_RECORDS"), null, 9L));
        assertTrue(error.getMessage().contains("无法转记现场记录"));
    }

    @Test
    @DisplayName("play-keys：只返回非空 sourceEventId，按局/半局/序排序")
    void listPlayKeysFiltersAndSorts() {
        when(playRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                playEntity(3, "bottom", 1, "3002"),
                playEntity(3, "top", 2, "3001"),
                playEntity(3, "top", 1, null),
                playEntity(3, "top", 3, "  ")));

        List<EarnedRunPlayKeyDTO> keys = service.listPlayKeys(1L, 20L);

        assertEquals(List.of("3001", "3002"), keys.stream()
                .map(EarnedRunPlayKeyDTO::sourceEventId).toList());
        assertEquals(Integer.valueOf(3), keys.get(0).inning());
        assertEquals("top", keys.get(0).half());
        assertEquals(Integer.valueOf(2), keys.get(0).sequence());
    }

    @Test
    @DisplayName("play-keys：空集返回空；null 局/序排在最后，不抛 NPE")
    void listPlayKeysHandlesEmptyAndNullOrdering() {
        when(playRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of());
        assertTrue(service.listPlayKeys(1L, 20L).isEmpty());

        when(playRepository.findByTenantIdAndGameId(1L, 20L)).thenReturn(List.of(
                playEntity(null, "top", null, "null-key"),
                playEntity(1, "bottom", 1, "1002"),
                playEntity(1, "top", 2, "1001")));

        List<EarnedRunPlayKeyDTO> keys = service.listPlayKeys(1L, 20L);

        assertEquals(List.of("1001", "1002", "null-key"), keys.stream()
                .map(EarnedRunPlayKeyDTO::sourceEventId).toList());
    }

    private EarnedRunPlayEntity playEntity(Integer inning, String half, Integer sequence, String sourceEventId) {
        EarnedRunPlayEntity entity = new EarnedRunPlayEntity();
        entity.setTenantId(1L);
        entity.setGameId(20L);
        entity.setInning(inning);
        entity.setHalf(half);
        entity.setSequence(sequence);
        entity.setSourceEventId(sourceEventId);
        return entity;
    }

    private GameFieldingAdjustment fieldingAdjustment(long playerId, String sourceEventId) {
        GameFieldingAdjustment row = new GameFieldingAdjustment();
        row.setTenantId(1L);
        row.setGameId(20L);
        row.setPlayerId(playerId);
        row.setSourceEventId(sourceEventId);
        row.setStatus("APPROVED");
        return row;
    }

    private GamePlayerStat batterStat(long playerId, int pa, int h) {
        GamePlayerStat stat = new GamePlayerStat();
        stat.setTenantId(1L);
        stat.setGameId(20L);
        stat.setTeamId(1L);
        stat.setPlayerId(playerId);
        stat.setIsPitcher(0);
        stat.setPa(pa);
        stat.setH(h);
        return stat;
    }

    private ErDecisionAdjustment adjustment(long id, String action, Long targetPitcherId) {
        ErDecisionAdjustment adjustment = new ErDecisionAdjustment();
        adjustment.setId(id);
        adjustment.setTenantId(1L);
        adjustment.setGameId(20L);
        adjustment.setDecisionId(11L);
        adjustment.setSourceEventId("play-1");
        adjustment.setRunnerId(55L);
        adjustment.setAction(action);
        adjustment.setTargetPitcherId(targetPitcherId);
        adjustment.setStatus("ACTIVE");
        return adjustment;
    }

    private EarnedRunPlayEntity play(long id, String sourceEventId) {
        EarnedRunPlayEntity play = new EarnedRunPlayEntity();
        play.setId(id);
        play.setTenantId(1L);
        play.setGameId(20L);
        play.setInning(3);
        play.setHalf("top");
        play.setSequence(2);
        play.setSourceEventId(sourceEventId);
        return play;
    }

    /** 模拟 ACTIVE 调整表的读写：保存时维护内存中的生效行，供重算与列表回查。 */
    private void stubActiveAdjustmentStore(List<ErDecisionAdjustment> activeRows) {
        when(erDecisionAdjustmentRepository.save(any(ErDecisionAdjustment.class)))
                .thenAnswer(invocation -> {
                    ErDecisionAdjustment row = invocation.getArgument(0);
                    if (row.getId() == null) {
                        row.setId(88L);
                    }
                    if ("ACTIVE".equals(row.getStatus())) {
                        activeRows.clear();
                        activeRows.add(row);
                    } else {
                        activeRows.remove(row);
                    }
                    return row;
                });
        when(erDecisionAdjustmentRepository.findByTenantIdAndGameIdAndStatus(1L, 20L, "ACTIVE"))
                .thenAnswer(invocation -> new ArrayList<>(activeRows));
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
