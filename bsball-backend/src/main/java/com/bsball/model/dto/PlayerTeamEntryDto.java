package com.bsball.model.dto;

import java.util.List;

/**
 * 球员球队经历条目（接口输出）：一段“简历式”经历 = 球队 + 该队背号 + 该队守备位置 + 是否当前球队。
 */
public record PlayerTeamEntryDto(Long id, Long teamId, String teamName, String number, List<String> positions,
        Boolean current, Integer sort) {
}
