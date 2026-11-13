package com.bsball.model.dto;

import jakarta.validation.constraints.NotBlank;

/** 球队端赛前阵容保存请求：lineupJson 为本队阵容 JSON 字符串。 */
public record LineupSubmissionSaveRequest(@NotBlank String lineupJson, Boolean complete) {
}
