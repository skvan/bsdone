package com.bsball.model.dto;

/** 投手行转记结果：供前端刷新目标行与后续“是否删除源行”确认。 */
public record PitcherTransferResultDTO(
        Long transferId,
        Long sourceStatId,
        Long sourcePlayerId,
        Long targetStatId,
        Long targetPlayerId,
        String status) {}
