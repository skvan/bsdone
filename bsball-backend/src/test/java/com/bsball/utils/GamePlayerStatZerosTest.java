package com.bsball.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bsball.model.entity.GamePlayerStat;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GamePlayerStatZeros：统计行全零判定")
class GamePlayerStatZerosTest {

    @Test
    @DisplayName("null 行不算全零")
    void nullRow_isNotZero() {
        assertFalse(GamePlayerStatZeros.isZero(null));
    }

    @Test
    @DisplayName("只有身份、排序与展示字段的行视为全零")
    void identityFieldsOnly_isZero() {
        assertTrue(GamePlayerStatZeros.isZero(baseRow()));
    }

    @Test
    @DisplayName("仅残留隐藏 pitchPa 仍视为全零（结果页不暴露该字段）")
    void pitchPaOnly_isZero() {
        assertTrue(GamePlayerStatZeros.isZero(row(stat -> stat.setPitchPa(Integer.valueOf(1)))));
    }

    @Test
    @DisplayName("统计字段显式写 0 / 0.0 仍视为全零")
    void explicitZeroValues_isZero() {
        assertTrue(GamePlayerStatZeros.isZero(row(stat -> {
            stat.setPa(Integer.valueOf(0));
            stat.setAb(Integer.valueOf(0));
            stat.setIp(Double.valueOf(0D));
            stat.setNp(Integer.valueOf(0));
            stat.setPitchBf(Integer.valueOf(0));
            stat.setPitchBb(Integer.valueOf(0));
            stat.setDefInn(Double.valueOf(0D));
            stat.setFieldingGs(Integer.valueOf(0));
        })));
    }

    @Test
    @DisplayName("任一打者、投手或守备统计非零即不可删")
    void anyStatNonZero_isNotZero() {
        assertNotZero(stat -> stat.setPitchBb(Integer.valueOf(1)));
        assertNotZero(stat -> stat.setPitchBf(Integer.valueOf(1)));
        assertNotZero(stat -> stat.setNp(Integer.valueOf(4)));
        assertNotZero(stat -> stat.setIp(Double.valueOf(0.1D)));
        assertNotZero(stat -> stat.setPa(Integer.valueOf(1)));
        assertNotZero(stat -> stat.setH(Integer.valueOf(1)));
        assertNotZero(stat -> stat.setPo(Integer.valueOf(1)));
        assertNotZero(stat -> stat.setDefInn(Double.valueOf(0.1D)));
        assertNotZero(stat -> stat.setPitchR(Integer.valueOf(1)));
        assertNotZero(stat -> stat.setFieldingGs(Integer.valueOf(1)));
        assertNotZero(stat -> stat.setPitchDoubles(Integer.valueOf(1)));
        assertNotZero(stat -> stat.setW(Integer.valueOf(1)));
    }

    private static void assertNotZero(Consumer<GamePlayerStat> customizer) {
        assertFalse(GamePlayerStatZeros.isZero(row(customizer)));
    }

    private static GamePlayerStat row(Consumer<GamePlayerStat> customizer) {
        GamePlayerStat row = baseRow();
        customizer.accept(row);
        return row;
    }

    private static GamePlayerStat baseRow() {
        GamePlayerStat row = new GamePlayerStat();
        row.setId(Long.valueOf(5184L));
        row.setGameId(Long.valueOf(245L));
        row.setTeamId(Long.valueOf(57L));
        row.setPlayerId(Long.valueOf(1324L));
        row.setIsPitcher(Integer.valueOf(1));
        row.setPitcherOrder(Integer.valueOf(3));
        row.setListOrder(Integer.valueOf(2));
        row.setPosition("P");
        row.setNumber("21");
        return row;
    }
}
