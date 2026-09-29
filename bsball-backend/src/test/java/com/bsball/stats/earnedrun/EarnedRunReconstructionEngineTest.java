package com.bsball.stats.earnedrun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EarnedRunReconstructionEngineTest {

    private final EarnedRunReconstructionEngine engine = new EarnedRunReconstructionEngine();

    @Test
    @DisplayName("正常上垒跑者在理论第三出局前得分应为自责分")
    void marksNormalRunEarnedBeforeReconstructedThirdOut() {
        EarnedRunPlay play = play(1, 1, 0, 0, false, ScoringRunner.normal(11, 101));

        EarnedRunDecision decision = engine.reconstruct(List.of(play)).decisions().get(0);

        assertEquals(EarnedRunStatus.EARNED, decision.status());
        assertEquals(1, engine.reconstruct(List.of(play)).pitcherTotals().get(101L).earnedRuns());
    }

    @Test
    @DisplayName("失误上垒跑者得分为非自责分但同一全垒打的打者可为自责分")
    void doesNotMarkEveryRunAfterAnErrorUnearned() {
        EarnedRunPlay error = play(1, 1, 0, 1, false);
        EarnedRunPlay homeRun = play(2, 2, 0, 0, false,
                ScoringRunner.reachedOnError(11, 101),
                ScoringRunner.normal(12, 101));

        List<EarnedRunDecision> decisions = engine.reconstruct(List.of(error, homeRun)).decisions();

        assertEquals(EarnedRunStatus.UNEARNED, decisions.get(0).status());
        assertEquals(UnearnedRunReason.ERROR_REACHED_BASE, decisions.get(0).reason());
        assertEquals(EarnedRunStatus.EARNED, decisions.get(1).status());
    }

    @Test
    @DisplayName("理论第三出局后的正常跑者得分也应为非自责分")
    void marksRunsAfterReconstructedThirdOutUnearned() {
        EarnedRunPlay firstOut = play(1, 1, 1, 1, false);
        EarnedRunPlay errorOut = play(2, 2, 0, 1, false);
        EarnedRunPlay strikeout = play(3, 3, 1, 1, false);
        EarnedRunPlay doubleAfterThirdOut = play(4, 4, 0, 0, false, ScoringRunner.normal(13, 101));

        EarnedRunDecision decision = engine.reconstruct(
                List.of(firstOut, errorOut, strikeout, doubleAfterThirdOut)).decisions().get(0);

        assertEquals(EarnedRunStatus.UNEARNED, decision.status());
        assertEquals(UnearnedRunReason.ERROR_EXTENDED_INNING, decision.reason());
    }

    @Test
    @DisplayName("承继跑者得分应归属原责任投手")
    void chargesInheritedRunnerToResponsiblePitcher() {
        EarnedRunPlay play = play(1, 1, 0, 0, false, ScoringRunner.normal(11, 101));

        EarnedRunReconstructionResult result = engine.reconstruct(List.of(play));

        assertEquals(1, result.pitcherTotals().get(101L).runs());
        assertEquals(null, result.pitcherTotals().get(202L));
    }

    @Test
    @DisplayName("同一 play 可將承繼跑者與新投手責任分開統計")
    void keepsInheritedAndCurrentPitcherTotalsSeparate() {
        EarnedRunPlay play = play(1, 1, 0, 0, false,
                ScoringRunner.normal(11, 101),
                ScoringRunner.normal(12, 202));

        EarnedRunReconstructionResult result = engine.reconstruct(List.of(play));

        assertEquals(1, result.pitcherTotals().get(101L).runs());
        assertEquals(1, result.pitcherTotals().get(202L).runs());
        assertEquals(1, result.pitcherTotals().get(101L).earnedRuns());
        assertEquals(1, result.pitcherTotals().get(202L).earnedRuns());
    }

    @Test
    @DisplayName("改判尚未完成时得分保持待覆核")
    void keepsRunPendingWhileRulingIsPending() {
        EarnedRunPlay play = play(1, 1, 0, 1, true, ScoringRunner.normal(11, 101));

        EarnedRunDecision decision = engine.reconstruct(List.of(play)).decisions().get(0);

        assertEquals(EarnedRunStatus.PENDING, decision.status());
        assertEquals(1, engine.reconstruct(List.of(play)).pitcherTotals().get(101L).pendingRuns());
    }

    @Test
    @DisplayName("突破僵局预置跑者得分为非自责分")
    void marksTieBreakRunnerUnearned() {
        EarnedRunPlay play = play(1, 1, 0, 0, false, ScoringRunner.tieBreak(11, 101));

        EarnedRunDecision decision = engine.reconstruct(List.of(play)).decisions().get(0);

        assertEquals(EarnedRunStatus.UNEARNED, decision.status());
        assertEquals(UnearnedRunReason.TIE_BREAK_RUNNER, decision.reason());
    }

    @Test
    @DisplayName("捕逸造成的得分保留非自責原因")
    void marksPassedBallRunUnearned() {
        EarnedRunDecision decision = engine.reconstruct(List.of(
                play(1, 1, 0, 0, false, ScoringRunner.passedBall(11, 101))))
                .decisions().get(0);

        assertEquals(EarnedRunStatus.UNEARNED, decision.status());
        assertEquals(UnearnedRunReason.PASSED_BALL, decision.reason());
    }

    @Test
    @DisplayName("暴投與捕逸使用不同的非自責原因")
    void distinguishesWildPitchFromPassedBall() {
        EarnedRunDecision decision = engine.reconstruct(List.of(
                play(1, 1, 0, 0, false, ScoringRunner.wildPitch(11, 101))))
                .decisions().get(0);

        assertEquals(EarnedRunStatus.UNEARNED, decision.status());
        assertEquals(UnearnedRunReason.WILD_PITCH, decision.reason());
    }

    @Test
    @DisplayName("妨礙造成的得分保留非自責原因")
    void marksInterferenceRunUnearned() {
        EarnedRunDecision decision = engine.reconstruct(List.of(
                play(1, 1, 0, 0, false, ScoringRunner.interference(11, 101))))
                .decisions().get(0);

        assertEquals(EarnedRunStatus.UNEARNED, decision.status());
        assertEquals(UnearnedRunReason.INTERFERENCE, decision.reason());
    }

    @Test
    @DisplayName("暴投、犧牲打與野手選擇不應自動一律判為非自責分")
    void keepsNormalPitchingSequencesEarned() {
        List<EarnedRunPlay> plays = List.of(
                play(1, 1, 0, 0, false),
                play(2, 2, 0, 0, false, ScoringRunner.normal(11, 101)),
                play(3, 3, 0, 0, false, ScoringRunner.normal(12, 101)));

        List<EarnedRunDecision> decisions = engine.reconstruct(plays).decisions();

        assertEquals(EarnedRunStatus.EARNED, decisions.get(0).status());
        assertEquals(EarnedRunStatus.EARNED, decisions.get(1).status());
    }

    @Test
    @DisplayName("暴傳失誤推進造成的得分，應在失誤延長半局時判為非自責分")
    void marksErrorAdvanceRunUnearnedAfterReconstructedThirdOut() {
        List<EarnedRunPlay> plays = List.of(
                play(1, 1, 1, 1, false),
                play(2, 2, 0, 1, false),
                play(3, 3, 1, 1, false),
                play(4, 4, 0, 0, false, ScoringRunner.normal(31, 101)));

        EarnedRunDecision decision = engine.reconstruct(plays).decisions().get(0);

        assertEquals(EarnedRunStatus.UNEARNED, decision.status());
        assertEquals(UnearnedRunReason.ERROR_EXTENDED_INNING, decision.reason());
    }

    @Test
    @DisplayName("換投後承繼跑者與新投手得分，應分別歸屬責任投手")
    void separatesInheritedRunnerFromNewPitcherRun() {
        EarnedRunPlay play = play(1, 1, 0, 0, false,
                ScoringRunner.normal(41, 101),
                ScoringRunner.normal(42, 202));

        EarnedRunReconstructionResult result = engine.reconstruct(List.of(play));

        assertEquals(1, result.pitcherTotals().get(101L).runs());
        assertEquals(1, result.pitcherTotals().get(202L).runs());
        assertEquals(1, result.pitcherTotals().get(101L).earnedRuns());
        assertEquals(1, result.pitcherTotals().get(202L).earnedRuns());
    }

    @Test
    @DisplayName("野手選擇、犧牲飛球與犧牲觸擊的正常得分應維持自責分")
    void keepsFielderChoiceAndSacrificeRunsEarned() {
        List<EarnedRunPlay> plays = List.of(
                play(1, 1, 1, 1, false), // FC
                play(2, 2, 1, 1, false, ScoringRunner.normal(51, 101)), // SF
                play(3, 3, 1, 1, false, ScoringRunner.normal(52, 101))); // SH

        List<EarnedRunDecision> decisions = engine.reconstruct(plays).decisions();

        assertEquals(2, decisions.size());
        assertEquals(EarnedRunStatus.EARNED, decisions.get(0).status());
        assertEquals(EarnedRunStatus.EARNED, decisions.get(1).status());
    }

    @Test
    @DisplayName("第三好球捕逸造成的得分，應保留捕逸的非自責原因")
    void marksDroppedThirdStrikeRunUnearned() {
        EarnedRunDecision decision = engine.reconstruct(List.of(
                play(1, 1, 0, 0, false,
                        ScoringRunner.passedBall(61, 101)))).decisions().get(0);

        assertEquals(EarnedRunStatus.UNEARNED, decision.status());
        assertEquals(UnearnedRunReason.PASSED_BALL, decision.reason());
    }

    @Test
    @DisplayName("人工覆核可明确指定非自责原因")
    void acceptsAuditableManualOverride() {
        ScoringRunner runner = new ScoringRunner(
                11, 101, RunnerOrigin.NORMAL,
                EarnedRunStatus.UNEARNED, UnearnedRunReason.MANUAL_OVERRIDE);

        EarnedRunDecision decision = engine.reconstruct(
                List.of(play(1, 1, 0, 0, false, runner))).decisions().get(0);

        assertEquals(EarnedRunStatus.UNEARNED, decision.status());
        assertEquals(UnearnedRunReason.MANUAL_OVERRIDE, decision.reason());
    }

    @Test
    @DisplayName("事件顺序必须严格递增")
    void rejectsOutOfOrderPlays() {
        List<EarnedRunPlay> plays = List.of(
                play(1, 2, 0, 0, false),
                play(2, 1, 0, 0, false));

        assertThrows(IllegalArgumentException.class, () -> engine.reconstruct(plays));
    }

    private static EarnedRunPlay play(
            long playId,
            int sequence,
            int actualOuts,
            int reconstructedOuts,
            boolean pending,
            ScoringRunner... runners) {
        return new EarnedRunPlay(
                playId, sequence, actualOuts, reconstructedOuts, pending, List.of(runners));
    }
}
