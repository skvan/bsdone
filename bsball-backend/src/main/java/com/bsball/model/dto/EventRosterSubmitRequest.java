package com.bsball.model.dto;

import java.util.List;

/** 提交报名名单请求（电子秩序册，Issue #310）：playerIds 按展示顺序。 */
public record EventRosterSubmitRequest(List<Long> playerIds) {
}
