package com.bsball.model.dto;

import java.util.List;

/** 邀请球队参赛请求（电子秩序册，Issue #310）。 */
public record EventTeamInviteRequest(List<Long> teamIds, String remark) {
}
