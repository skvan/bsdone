package com.bsball.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.hibernate.annotations.Formula;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 比赛实体映射边界（2026-11-15 生产 OOM 回归，Issue #686）：
 * 列表查询返回完整 Game 实体。若实体继续映射 live_snapshot_json（生产平均 2.4MB/场），
 * 100 条分页就会把约 240MB 快照文本读入堆内存，在约 894MB 堆上限下触发 OutOfMemoryError。
 * 快照列改由同表轻量实体 GameSnapshot 单独读写，Game 不得再声明该字段。
 */
class GameSnapshotMappingTest {

    @Test
    @DisplayName("Game 实体不得映射 liveSnapshotJson 大字段（防列表查询把快照读入堆）")
    void gameEntity_doesNotDeclareSnapshotField() {
        assertThrows(NoSuchFieldException.class,
                () -> Game.class.getDeclaredField("liveSnapshotJson"));
    }

    @Test
    @DisplayName("快照标记使用仅判 NULL 的 SQL 计算字段（避免 length() 触发 detoast）")
    void gameSnapshotFlag_usesNullCheckOnly() throws Exception {
        Formula formula = Game.class.getDeclaredField("hasLiveSnapshot").getAnnotation(Formula.class);
        assertNotNull(formula);
        assertEquals("(live_snapshot_json is not null)", formula.value());
    }

    @Test
    @DisplayName("快照轻量实体映射同一张 bs_game 表与 live_snapshot_json 列")
    void gameSnapshot_mapsSameTableAndSnapshotColumn() throws Exception {
        Table table = GameSnapshot.class.getAnnotation(Table.class);
        assertEquals("bs_game", table.name());

        Column column = GameSnapshot.class.getDeclaredField("liveSnapshotJson").getAnnotation(Column.class);
        assertEquals("live_snapshot_json", column.name());
    }
}
