package com.bsball.stats.earnedrun;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Replays one half-inning while keeping actual and error-free out counts separate.
 */
public final class EarnedRunReconstructionEngine {

    public EarnedRunReconstructionResult reconstruct(List<EarnedRunPlay> plays) {
        List<EarnedRunDecision> decisions = new ArrayList<>();
        int actualOuts = 0;
        int reconstructedOuts = 0;
        int previousSequence = 0;

        for (EarnedRunPlay play : List.copyOf(plays)) {
            if (play.sequence() <= previousSequence) {
                throw new IllegalArgumentException("Play sequence must be strictly increasing");
            }
            previousSequence = play.sequence();
            boolean inningAlreadyReconstructed = reconstructedOuts >= 3;
            actualOuts += play.actualOutsAdded();
            reconstructedOuts += play.reconstructedOutsAdded();
            if (actualOuts > 3) {
                throw new IllegalArgumentException("Actual outs cannot exceed three in a half-inning");
            }

            boolean inningExtendedByError = inningAlreadyReconstructed
                    || (reconstructedOuts >= 3 && actualOuts < 3);
            for (ScoringRunner runner : play.scoringRunners()) {
                decisions.add(decide(play, runner, inningExtendedByError));
            }
        }

        return new EarnedRunReconstructionResult(
                actualOuts,
                reconstructedOuts,
                List.copyOf(decisions),
                buildPitcherTotals(decisions));
    }

    private EarnedRunDecision decide(
            EarnedRunPlay play,
            ScoringRunner runner,
            boolean inningExtendedByError) {
        if (play.rulingPending() || runner.overrideStatus() == EarnedRunStatus.PENDING) {
            return decision(play, runner, EarnedRunStatus.PENDING, null);
        }
        if (runner.overrideStatus() != null) {
            return decision(play, runner, runner.overrideStatus(), runner.overrideReason());
        }
        if (runner.origin() == RunnerOrigin.ERROR) {
            return decision(play, runner, EarnedRunStatus.UNEARNED, UnearnedRunReason.ERROR_REACHED_BASE);
        }
        if (runner.origin() == RunnerOrigin.TIE_BREAK) {
            return decision(play, runner, EarnedRunStatus.UNEARNED, UnearnedRunReason.TIE_BREAK_RUNNER);
        }
        if (runner.origin() == RunnerOrigin.PASSED_BALL) {
            return decision(play, runner, EarnedRunStatus.UNEARNED, UnearnedRunReason.PASSED_BALL);
        }
        if (runner.origin() == RunnerOrigin.INTERFERENCE) {
            return decision(play, runner, EarnedRunStatus.UNEARNED, UnearnedRunReason.INTERFERENCE);
        }
        if (inningExtendedByError) {
            return decision(play, runner, EarnedRunStatus.UNEARNED, UnearnedRunReason.ERROR_EXTENDED_INNING);
        }
        return decision(play, runner, EarnedRunStatus.EARNED, null);
    }

    private EarnedRunDecision decision(
            EarnedRunPlay play,
            ScoringRunner runner,
            EarnedRunStatus status,
            UnearnedRunReason reason) {
        return new EarnedRunDecision(
                play.playId(), runner.runnerId(), runner.responsiblePitcherId(), status, reason);
    }

    private Map<Long, EarnedRunReconstructionResult.PitcherRunTotals> buildPitcherTotals(
            List<EarnedRunDecision> decisions) {
        Map<Long, int[]> counts = new LinkedHashMap<>();
        for (EarnedRunDecision decision : decisions) {
            int[] values = counts.computeIfAbsent(decision.responsiblePitcherId(), ignored -> new int[4]);
            values[0]++;
            if (decision.status() == EarnedRunStatus.EARNED) {
                values[1]++;
            } else if (decision.status() == EarnedRunStatus.UNEARNED) {
                values[2]++;
            } else {
                values[3]++;
            }
        }
        Map<Long, EarnedRunReconstructionResult.PitcherRunTotals> totals = new LinkedHashMap<>();
        counts.forEach((pitcherId, values) -> totals.put(
                pitcherId,
                new EarnedRunReconstructionResult.PitcherRunTotals(
                        values[0], values[1], values[2], values[3])));
        return Map.copyOf(totals);
    }
}
