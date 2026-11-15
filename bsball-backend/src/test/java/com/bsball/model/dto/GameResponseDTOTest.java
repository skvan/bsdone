package com.bsball.model.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bsball.model.entity.Game;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 比赛列表 DTO 映射（2026-11-15 生产 OOM 修复，Issue #686）：
 * 列表不再读取快照文本，hasLiveSnapshot 改由实体上的 SQL 计算字段提供。
 */
class GameResponseDTOTest {

    @Test
    @DisplayName("列表 DTO：实体标记有快照时 hasLiveSnapshot 为 true")
    void from_snapshotPresent_true() throws Exception {
        Game g = new Game();
        setHasLiveSnapshot(g, Boolean.TRUE);

        assertEquals(Boolean.TRUE, GameResponseDTO.from(g).getHasLiveSnapshot());
    }

    @Test
    @DisplayName("列表 DTO：实体未标记快照时 hasLiveSnapshot 为 false")
    void from_snapshotAbsent_false() throws Exception {
        Game g = new Game();
        assertEquals(Boolean.FALSE, GameResponseDTO.from(g).getHasLiveSnapshot());

        setHasLiveSnapshot(g, Boolean.FALSE);
        assertEquals(Boolean.FALSE, GameResponseDTO.from(g).getHasLiveSnapshot());
    }

    private static void setHasLiveSnapshot(Game g, Boolean value) throws Exception {
        Field field = Game.class.getDeclaredField("hasLiveSnapshot");
        field.setAccessible(true);
        field.set(g, value);
    }
}
