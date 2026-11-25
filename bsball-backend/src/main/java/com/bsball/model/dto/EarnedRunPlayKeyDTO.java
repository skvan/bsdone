package com.bsball.model.dto;

/** 已落库定案的一笔打席键：重进时用于校验本地快照是否覆盖云端记录。 */
public record EarnedRunPlayKeyDTO(
        Integer inning,
        String half,
        Integer sequence,
        String sourceEventId) {}
