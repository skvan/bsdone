/*
 * isPitcher 反序列化回归测试：
 * 前端「实时比赛录入 / 赛程结果」保存时发送 isPitcher: 1/0（LiveGame 产物 Eo() 中 isPitcherRow?1:0），
 * 两个 DTO 的 isPitcher 必须声明 @JsonDeserialize(using = BoolToIntDeserializer.class)。
 * 历史回归：后端源码重建时曾误写为 @JsonDeserializeAs(BoolToIntDeserializer.class)，
 * 该注解语义是"把 Integer 收窄为 BoolToIntDeserializer 类型"，Jackson 构建设置器时抛
 * IllegalArgumentException: Failed to narrow type ... not subtype of java.lang.Integer，
 * 导致 /api/game/{id}/save-live 与 /save-result 所有请求整体 400。
 */
package com.bsball.model.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("isPitcher 字段：兼容前端 1/0 数字与 true/false 布尔值")
class IsPitcherDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("实时比赛录入 DTO 接受数字与布尔值")
    void liveDtoAcceptsNumericAndBooleanIsPitcher() throws Exception {
        GameSaveLiveDTO.GamePlayerStatPart fromNumber = objectMapper.readValue(
                "{\"isPitcher\":1}", GameSaveLiveDTO.GamePlayerStatPart.class);
        assertEquals(Integer.valueOf(1), fromNumber.getIsPitcher());

        GameSaveLiveDTO.GamePlayerStatPart fromZero = objectMapper.readValue(
                "{\"isPitcher\":0}", GameSaveLiveDTO.GamePlayerStatPart.class);
        assertEquals(Integer.valueOf(0), fromZero.getIsPitcher());

        GameSaveLiveDTO.GamePlayerStatPart fromBoolean = objectMapper.readValue(
                "{\"isPitcher\":true}", GameSaveLiveDTO.GamePlayerStatPart.class);
        assertEquals(Integer.valueOf(1), fromBoolean.getIsPitcher());
    }

    @Test
    @DisplayName("赛程结果 DTO 接受数字/布尔值/字符串/缺省")
    void resultDtoAcceptsNumericBooleanStringAndMissingIsPitcher() throws Exception {
        SaveGameResultDTO.StatPart fromBoolean = objectMapper.readValue(
                "{\"isPitcher\":false}", SaveGameResultDTO.StatPart.class);
        assertEquals(Integer.valueOf(0), fromBoolean.getIsPitcher());

        SaveGameResultDTO.StatPart fromString = objectMapper.readValue(
                "{\"isPitcher\":\"true\"}", SaveGameResultDTO.StatPart.class);
        assertEquals(Integer.valueOf(1), fromString.getIsPitcher());

        SaveGameResultDTO.StatPart missing = objectMapper.readValue(
                "{}", SaveGameResultDTO.StatPart.class);
        assertNull(missing.getIsPitcher());
    }
}
