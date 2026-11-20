package com.bsball.stats.earnedrun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * “局中先推一次、半局结束后再推一次”到底会不会对同一分做出两种判定？
 * 引擎裁决第 k 棒时只读前 k-1 棒累积的 reconstructedOuts 与 actualOuts（都是单调累加），
 * 所以同一跑者的判定必须是“该半局前缀”的函数：早推与晚推只能得到同一个答案。
 * 这条测试把该不变性钉住，用来界定“只在半局结束后判定”的真正价值在哪里。
 */
class EarnedRunProvisionalPushTest {

    private final EarnedRunReconstructionEngine engine = new EarnedRunReconstructionEngine();

    @Test
    @DisplayName("失误延长半局：局中重建与半局结束后重建，对同一跑者判定逐项相同")
    void provisionalPushMatchesFinalPushOnCommonPrefix() {
        EarnedRunPlay firstOut = play(1, 1, 1, 1, false);
        EarnedRunPlay errorOnGround = play(2, 2, 0, 1, false);   // 失误：实际 0 出局、理论 1 出局
        EarnedRunPlay runBeforeTheoreticalThirdOut = play(3, 3, 0, 0, false,
                ScoringRunner.normal(11, 101));
        EarnedRunPlay secondError = play(4, 4, 0, 1, false);      // 理论出局数就此满 3
        EarnedRunPlay runAfterTheoreticalThirdOut = play(5, 5, 0, 0, false,
                ScoringRunner.normal(12, 101));
        EarnedRunPlay oneMoreRun = play(6, 6, 0, 0, false, ScoringRunner.normal(13, 101));

        // 局中：第 3 棒刚落定就推前 3 棒（现行 Qr() 的行为）
        List<EarnedRunDecision> provisional = engine.reconstruct(List.of(
                firstOut, errorOnGround, runBeforeTheoreticalThirdOut)).decisions();
        // 半局结束后：整半局 6 棒重推
        List<EarnedRunDecision> finalPush = engine.reconstruct(List.of(
                firstOut, errorOnGround, runBeforeTheoreticalThirdOut,
                secondError, runAfterTheoreticalThirdOut, oneMoreRun)).decisions();

        assertEquals(1, provisional.size());
        EarnedRunDecision shared = provisional.get(0);
        EarnedRunDecision sameRunLater = finalPush.get(0);
        assertEquals(shared.runnerId(), sameRunLater.runnerId());
        assertEquals(shared.status(), sameRunLater.status(), "理论第三出局前得分不该被晚推改判");
        assertEquals(shared.reason(), sameRunLater.reason());
        assertEquals(EarnedRunStatus.EARNED, sameRunLater.status());

        // 局中推到第 5 棒时，那笔分已经能被正确判成失误延局非自责
        List<EarnedRunDecision> midHalf = engine.reconstruct(List.of(
                firstOut, errorOnGround, runBeforeTheoreticalThirdOut,
                secondError, runAfterTheoreticalThirdOut)).decisions();
        EarnedRunDecision extended = midHalf.get(1);
        assertEquals(EarnedRunStatus.UNEARNED, extended.status());
        assertEquals(UnearnedRunReason.ERROR_EXTENDED_INNING, extended.reason());
        assertEquals(extended.status(), finalPush.get(1).status(), "局中与结束后必须同判");
        assertEquals(extended.reason(), finalPush.get(1).reason());
    }

    @Test
    @DisplayName("多记一个出局时：三出局前的前缀可重建，整半局重推却整批抛错、判定无从更新")
    void finalPushCanBeAbortedByOneBadPlaySoAutoHealIsNotGuaranteed() {
        EarnedRunPlay out1 = play(1, 1, 1, 1, false);
        EarnedRunPlay run = play(2, 2, 0, 0, false, ScoringRunner.normal(11, 101));
        EarnedRunPlay out2 = play(3, 3, 1, 1, false);
        EarnedRunPlay out3 = play(4, 4, 1, 1, false);       // 实际第三出局：半局正常结束
        EarnedRunPlay fourthOut = play(5, 5, 1, 1, false);  // 记录失误：多出一个实际出局

        // 局中推到第 2 棒、以及推到正常结束的 4 棒，都能重建
        assertEquals(EarnedRunStatus.EARNED,
                engine.reconstruct(List.of(out1, run)).decisions().get(0).status());
        assertEquals(1, engine.reconstruct(List.of(out1, run, out2, out3)).decisions().size());

        // 一旦推送里出现第 4 个实际出局，引擎拒收整个半局：“等下一次重推自然修正”并不可靠
        assertThrows(IllegalArgumentException.class, () -> engine.reconstruct(
                List.of(out1, run, out2, out3, fourthOut)));
    }

    private static EarnedRunPlay play(long playId, int sequence, int actualOuts,
            int reconstructedOuts, boolean pending, ScoringRunner... runners) {
        return new EarnedRunPlay(playId, sequence, actualOuts, reconstructedOuts, pending,
                List.of(runners));
    }
}
