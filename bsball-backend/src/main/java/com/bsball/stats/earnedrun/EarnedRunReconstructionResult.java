package com.bsball.stats.earnedrun;

import java.util.List;
import java.util.Map;

public record EarnedRunReconstructionResult(
        int actualOuts,
        int reconstructedOuts,
        List<EarnedRunDecision> decisions,
        Map<Long, PitcherRunTotals> pitcherTotals,
        Map<Long, PitcherRunTotals> pitcherAbsoluteTotals) {

    public record PitcherRunTotals(int runs, int earnedRuns, int unearnedRuns, int pendingRuns) {
    }

    /** 兼容既有调用方：旧响应没有绝对总量时默认空 Map，前端据此跳过本地累加。 */
    public EarnedRunReconstructionResult(
            int actualOuts,
            int reconstructedOuts,
            List<EarnedRunDecision> decisions,
            Map<Long, PitcherRunTotals> pitcherTotals) {
        this(actualOuts, reconstructedOuts, decisions, pitcherTotals, Map.of());
    }

    public EarnedRunReconstructionResult withPitcherAbsoluteTotals(
            Map<Long, PitcherRunTotals> absoluteTotals) {
        return new EarnedRunReconstructionResult(
                actualOuts, reconstructedOuts, decisions, pitcherTotals, absoluteTotals);
    }
}
