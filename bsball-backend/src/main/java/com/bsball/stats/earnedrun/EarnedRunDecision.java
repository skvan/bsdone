package com.bsball.stats.earnedrun;

public record EarnedRunDecision(
        long playId,
        long runnerId,
        long responsiblePitcherId,
        EarnedRunStatus status,
        UnearnedRunReason reason) {
}
