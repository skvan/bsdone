package com.bsball.stats.earnedrun;

import java.util.Objects;

public record ScoringRunner(
        long runnerId,
        long responsiblePitcherId,
        RunnerOrigin origin,
        EarnedRunStatus overrideStatus,
        UnearnedRunReason overrideReason) {

    public ScoringRunner {
        if (runnerId <= 0 || responsiblePitcherId <= 0) {
            throw new IllegalArgumentException("runnerId and responsiblePitcherId must be positive");
        }
        Objects.requireNonNull(origin, "origin");
        if (overrideStatus == EarnedRunStatus.UNEARNED && overrideReason == null) {
            throw new IllegalArgumentException("An unearned override requires a reason");
        }
        if (overrideStatus != EarnedRunStatus.UNEARNED && overrideReason != null) {
            throw new IllegalArgumentException("An override reason is only valid for UNEARNED");
        }
    }

    public static ScoringRunner normal(long runnerId, long responsiblePitcherId) {
        return new ScoringRunner(runnerId, responsiblePitcherId, RunnerOrigin.NORMAL, null, null);
    }

    public static ScoringRunner reachedOnError(long runnerId, long responsiblePitcherId) {
        return new ScoringRunner(runnerId, responsiblePitcherId, RunnerOrigin.ERROR, null, null);
    }

    public static ScoringRunner tieBreak(long runnerId, long responsiblePitcherId) {
        return new ScoringRunner(runnerId, responsiblePitcherId, RunnerOrigin.TIE_BREAK, null, null);
    }

    public static ScoringRunner passedBall(long runnerId, long responsiblePitcherId) {
        return new ScoringRunner(runnerId, responsiblePitcherId, RunnerOrigin.PASSED_BALL, null, null);
    }

    public static ScoringRunner interference(long runnerId, long responsiblePitcherId) {
        return new ScoringRunner(runnerId, responsiblePitcherId, RunnerOrigin.INTERFERENCE, null, null);
    }
}
