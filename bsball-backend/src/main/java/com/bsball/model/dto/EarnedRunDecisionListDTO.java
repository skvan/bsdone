package com.bsball.model.dto;

import java.time.LocalDateTime;

/** 單筆自責分判定清單行（含關聯跑者/投手名稱、半局資訊）。 */
public record EarnedRunDecisionListDTO(
        Long decisionId,
        Long playId,
        Integer inning,
        String half,
        Integer sequence,
        Long runnerId,
        String runnerName,
        Long responsiblePitcherId,
        String pitcherName,
        String earnedStatus,
        String unearnedReason,
        String runnerOrigin,
        Long overriddenBy,
        LocalDateTime overriddenAt) {}
