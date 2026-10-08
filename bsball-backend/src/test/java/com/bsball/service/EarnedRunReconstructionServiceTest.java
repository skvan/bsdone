package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        GamePlayerStat tenantTenPitcher = pitcherStat(10L, 901L, 0, 0);
        lenient().when(gamePlayerStatRepository.findByGameId(20L))
                .thenReturn(List.of(tenantOnePitcher, tenantTenPitcher));
        lenient().when(playRepository.save(any(EarnedRunPlayEntity.class)))
                .thenAnswer(invocation -> {
                    EarnedRunPlayEntity entity = invocation.getArgument(0);
                    entity.setId(900L);
                    return entity;
                });
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

        EarnedRunReconstructionResult result = service.replaceHalfInning(
                1L, 20L, 3, "top", 9L, List.of(play), 500L);

        assertEquals(1, result.pitcherTotals().get(700L).earnedRuns());
        ArgumentCaptor<EarnedRunDecisionEntity> captor =
                ArgumentCaptor.forClass(EarnedRunDecisionEntity.class);
        verify(decisionRepository).save(captor.capture());
        assertEquals(700L, captor.getValue().getResponsiblePitcherId());
        assertEquals("EARNED", captor.getValue().getEarnedStatus());
        assertEquals("NORMAL", captor.getValue().getRunnerOrigin());
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
                com.bsball.stats.earnedrun.RunnerOrigin.NORMAL,
                com.bsball.stats.earnedrun.EarnedRunStatus.UNEARNED,
                com.bsball.stats.earnedrun.UnearnedRunReason.MANUAL_OVERRIDE);

        service.replaceHalfInning(1L, 20L, 3, "bottom", 9L, List.of(
                new EarnedRunPlay(102L, 1, 0, 0, false, List.of(runner))), 500L);

        ArgumentCaptor<EarnedRunDecisionEntity> captor =
                ArgumentCaptor.forClass(EarnedRunDecisionEntity.class);
        verify(decisionRepository).save(captor.capture());
        assertEquals(9L, captor.getValue().getOverriddenBy());
        assertNotNull(captor.getValue().getOverriddenAt());
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
    @DisplayName("重判半局时只以新旧差额更新投手 R 与 ER")
    void appliesPitcherRunAndEarnedRunDeltas() {
        EarnedRunPlayEntity oldPlay = new EarnedRunPlayEntity();
        oldPlay.setId(88L);
        EarnedRunDecisionEntity oldDecision = new EarnedRunDecisionEntity();
        oldDecision.setResponsiblePitcherId(901L);
        oldDecision.setEarnedStatus("EARNED");
        GamePlayerStat pitcher = pitcherStat(10L, 901L, 5, 4);
        when(playRepository.findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
                10L, 20L, 3, "top")).thenReturn(List.of(oldPlay));
        when(decisionRepository.findByTenantIdAndGameIdAndPlayIdIn(
                10L, 20L, List.of(88L))).thenReturn(List.of(oldDecision));
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of(pitcher));

        service.replaceHalfInning(10L, 20L, 3, "top", 30L, List.of(
                play(101L, 1, 0, 0, false,
                        ScoringRunner.reachedOnError(501L, 901L)),
                play(102L, 2, 0, 0, false,
                        ScoringRunner.normal(502L, 901L))), 500L);

        assertEquals(6, pitcher.getPitchR());
        assertEquals(4, pitcher.getEr());
        verify(gamePlayerStatRepository).save(pitcher);
    }

    @Test
    @DisplayName("责任投手统计行缺失时自动补建（teamId 取该半局守备方）并累计失分")
    void createsMissingPitcherStatForResponsiblePitcher() {
        when(gamePlayerStatRepository.findByGameId(20L)).thenReturn(List.of());
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
