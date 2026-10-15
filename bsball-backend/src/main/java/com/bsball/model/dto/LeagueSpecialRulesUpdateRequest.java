package com.bsball.model.dto;

import java.util.Map;

/** 联赛特殊规则更新请求（Issue #275）：外层包裹 {rules:{...}}，规则键名 / 值的校验在服务层。 */
public record LeagueSpecialRulesUpdateRequest(Map<String, Boolean> rules) {
}
