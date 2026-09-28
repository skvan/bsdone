package com.bsball.stats.earnedrun;

import java.util.List;
import java.util.Map;

public record EarnedRunReconstructionResult(
        int actualOuts,
        int reconstructedOuts,
        List<EarnedRunDecision> decisions,
        Map<Long, PitcherRunTotals> pitcherTotals) {

    public record PitcherRunTotals(int runs, int earnedRuns, int unearnedRuns, int pendingRuns) {
    }
}
