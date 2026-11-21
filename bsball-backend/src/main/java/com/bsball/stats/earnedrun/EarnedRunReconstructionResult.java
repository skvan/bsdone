package com.bsball.stats.earnedrun;

import java.util.List;
import java.util.Map;

public record EarnedRunReconstructionResult(
        int actualOuts,
        int reconstructedOuts,
        List<EarnedRunDecision> decisions,
        Map<Long, PitcherRunTotals> pitcherTotals,
        Map<Long, PitcherRunTotals> pitcherAbsoluteTotals,
        List<Long> overrideInvalidationPitcherIds) {

    public EarnedRunReconstructionResult {
        overrideInvalidationPitcherIds = overrideInvalidationPitcherIds == null
                ? List.of()
                : List.copyOf(overrideInvalidationPitcherIds);
    }

    public record PitcherRunTotals(int runs, int earnedRuns, int unearnedRuns, int pendingRuns) {
    }

    /** 兼容既有调用方：旧响应没有绝对总量时默认空 Map，前端据此跳过本地累加。 */
    public EarnedRunReconstructionResult(
            int actualOuts,
            int reconstructedOuts,
            List<EarnedRunDecision> decisions,
            Map<Long, PitcherRunTotals> pitcherTotals) {
        this(actualOuts, reconstructedOuts, decisions, pitcherTotals, Map.of(), List.of());
    }

    public EarnedRunReconstructionResult withPitcherAbsoluteTotals(
            Map<Long, PitcherRunTotals> absoluteTotals) {
        return new EarnedRunReconstructionResult(
                actualOuts, reconstructedOuts, decisions, pitcherTotals, absoluteTotals,
                overrideInvalidationPitcherIds);
    }

    /**
     * 带上「本次让哪些投手的人工 R/ER 覆写失效」的回包。定案路径给的是已经作废的那批，
     * 预览给的是「这个半局一旦定案就会被作废」的那批——两种情况下 {@code pitcherAbsoluteTotals}
     * 都已按覆写不再生效的口径算过，前端拿这两个字段就能解释自己看到的数字。
     */
    public EarnedRunReconstructionResult withPitcherAbsoluteTotals(
            Map<Long, PitcherRunTotals> absoluteTotals, List<Long> invalidatedPitcherIds) {
        return new EarnedRunReconstructionResult(
                actualOuts, reconstructedOuts, decisions, pitcherTotals, absoluteTotals,
                invalidatedPitcherIds);
    }
}
