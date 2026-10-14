package com.bsball.model.dto;

import java.util.List;

/**
 * 阵容选人池（电子秩序册，Issue #310）：
 * {@code restricted=true} 时 players=该队有效报名名单球员；false 时=本队 current 全量在役。
 */
public record RosterPlayersDto(Boolean restricted, List<TeamPlayerOptionDto> players) {
}
