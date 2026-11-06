package com.bsball.model.dto;

/** 身份转记结果。 */
public record IdentityTransferResultDTO(
        Long transferId,
        Long sourcePlayerId,
        Long targetPlayerId,
        String status) {
}
