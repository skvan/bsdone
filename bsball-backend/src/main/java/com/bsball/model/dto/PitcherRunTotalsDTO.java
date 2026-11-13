package com.bsball.model.dto;

import java.time.LocalDateTime;

/** 赛后复核：单名投手的 R/ER 合计与生效中的人工覆写信息。 */
public record PitcherRunTotalsDTO(
        Long playerId,
        String playerName,
        Long teamId,
        Integer pitcherOrder,
        Integer runsAllowed,
        Integer earnedRuns,
        Integer unearnedRuns,
        Integer pendingRuns,
        Integer engineRunsAllowed,
        Integer engineEarnedRuns,
        Long overrideId,
        String overrideNote,
        Long overriddenBy,
        LocalDateTime overriddenAt) {}
