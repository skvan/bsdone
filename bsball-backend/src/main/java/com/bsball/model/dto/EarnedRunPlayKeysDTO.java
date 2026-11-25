package com.bsball.model.dto;

import java.util.List;

/** 某场比赛已落库的定案打席键集合。空集合表示该场还没有落库定案。 */
public record EarnedRunPlayKeysDTO(List<EarnedRunPlayKeyDTO> keys) {}
