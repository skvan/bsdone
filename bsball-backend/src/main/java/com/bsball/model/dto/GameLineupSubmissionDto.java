package com.bsball.model.dto;

import java.time.LocalDateTime;

/** 办赛方视角的赛前阵容提交项（一场比赛每队一条）。 */
public record GameLineupSubmissionDto(
        Long teamId,
        String teamName,
        String teamLogo,
        String side,
        String lineupJson,
        LocalDateTime updatedAt,
        Boolean complete) {
}
