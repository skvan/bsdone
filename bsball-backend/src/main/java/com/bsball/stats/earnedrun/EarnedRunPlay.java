package com.bsball.stats.earnedrun;

import java.util.List;

public record EarnedRunPlay(
        long playId,
        int sequence,
        int actualOutsAdded,
        int reconstructedOutsAdded,
        boolean rulingPending,
        List<ScoringRunner> scoringRunners) {

    public EarnedRunPlay {
        if (playId <= 0 || sequence <= 0) {
            throw new IllegalArgumentException("playId and sequence must be positive");
        }
        if (actualOutsAdded < 0 || reconstructedOutsAdded < 0) {
            throw new IllegalArgumentException("Out increments cannot be negative");
        }
        if (actualOutsAdded > 3 || reconstructedOutsAdded > 3) {
            throw new IllegalArgumentException("A play cannot add more than three outs");
        }
        scoringRunners = scoringRunners == null ? List.of() : List.copyOf(scoringRunners);
    }
}
