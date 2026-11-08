package com.bsball.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.Column;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 投手失分分解四列（pitchR/er/unearnedR/pendingR，含遗留的 earnedR）必须把列名显式钉死。
 *
 * <p>PG 生产库里 bs_game_player_stat 同时存在两组同义列：应用实际读写的是无下划线的一组
 * （pitchr/er/unearnedr/pendingr），带下划线的一组（pitch_r/earned_r/unearned_r/pending_r）
 * 是 migration 按 snake_case 建出的退役列，全表被 DEFAULT 灌成 0 且无维护。
 * 一旦实体不写 name，列名就取自 Hibernate 命名策略，策略调整会把读写静默切到退役列，
 * 外在表现是"ER 引擎算对了但表与前台不动"，排查代价极高。此测试用来把这条边界钉住。
 */
@DisplayName("实体：投手失分分解列必须显式钉死为应用在用列名")
class GamePlayerStatErColumnMappingTest {

    private static final Map<String, String> EXPECTED = expected();

    /** 退役列名：任何情况下都不允许成为实体映射目标。 */
    private static final Set<String> RETIRED = Set.of("pitch_r", "earned_r", "unearned_r", "pending_r");

    private static Map<String, String> expected() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("pitchR", "pitchr");
        m.put("er", "er");
        m.put("earnedR", "earnedr");
        m.put("unearnedR", "unearnedr");
        m.put("pendingR", "pendingr");
        return m;
    }

    private static Column columnOf(String fieldName) throws NoSuchFieldException {
        Column column = GamePlayerStat.class.getDeclaredField(fieldName).getAnnotation(Column.class);
        assertNotNull(column, fieldName + " 缺少 @Column：隐式命名会随 Hibernate 命名策略漂移切到同义退役列");
        return column;
    }

    @Test
    @DisplayName("五个失分分解字段都带显式 @Column(name)，且名字等于生产在用的列")
    void erColumnsAreExplicitlyPinned() throws NoSuchFieldException {
        for (Map.Entry<String, String> entry : EXPECTED.entrySet()) {
            Column column = columnOf(entry.getKey());
            assertEquals(entry.getValue(), column.name(),
                    entry.getKey() + " 的列名被改成 " + column.name() + "，会写到无维护的数据");
        }
    }

    @Test
    @DisplayName("不允许映射到带下划线的退役列")
    void retiredColumnsAreNotMapped() throws NoSuchFieldException {
        for (String fieldName : EXPECTED.keySet()) {
            String name = columnOf(fieldName).name();
            assertFalse(RETIRED.contains(name),
                    fieldName + " 指向退役列 " + name + "（应用不写它，前台与表会不一致）");
        }
    }
}
