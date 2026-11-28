package com.bsball.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.ibatis.io.Resources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Issue #702 回填迁移护栏：只改“可判定为自动推算”的历史值（旧公式 + 0 值），人工覆写保留；
 * 公式与 PlayerStatsMapper.pitchAbFromBfCol / pitchAbLegacyFromBfCol 完全一致。
 */
@DisplayName("历史 pitch_ab 回填迁移（#702）")
class PitchAbBackfillMigrationTest {

    private static final String MIGRATION =
            "db/migration/postgresql/V20261125150000__fix_pitch_ab_sh_sf_702.sql";
    private static final String OLD_MIGRATION =
            "db/migration/postgresql/V20260501100000__game_player_stat_pitch_ab.sql";
    private static final Pattern STAT_UPDATE = Pattern.compile("(?s)UPDATE bs_game_player_stat.*?;");

    private static final String NEW_FORMULA =
            "GREATEST(0, COALESCE(pitch_bf,0) - (CASE WHEN COALESCE(pitch_bb,0) + COALESCE(pitch_hbp,0) > 0 "
            + "THEN COALESCE(pitch_bb,0) ELSE GREATEST(COALESCE(pitch_bb_hp,0) - COALESCE(pitch_hbp,0), 0) END) "
            + "- COALESCE(pitch_hbp,0) - COALESCE(pitch_sh,0) - COALESCE(pitch_sf,0))";
    private static final String LEGACY_FORMULA =
            "GREATEST(0, COALESCE(pitch_bf,0) - (CASE WHEN COALESCE(pitch_bb,0) + COALESCE(pitch_hbp,0) > 0 "
            + "THEN COALESCE(pitch_bb,0) ELSE GREATEST(COALESCE(pitch_bb_hp,0) - COALESCE(pitch_hbp,0), 0) END) "
            + "- COALESCE(pitch_hbp,0))";

    @Test
    @DisplayName("只有一条 UPDATE，且写新公式（含 SH/SF 与 BBHP 回退）")
    void updateUsesNewFormula() throws Exception {
        String sql = read(MIGRATION);
        Matcher matcher = STAT_UPDATE.matcher(sql);
        int statements = 0;
        String update = null;
        while (matcher.find()) {
            statements++;
            update = matcher.group();
        }
        assertEquals(1, statements, "应恰好一条 bs_game_player_stat 更新");
        assertTrue(update.contains("SET pitch_ab = " + NEW_FORMULA), "SET 未使用与 Mapper 相同的新公式");
        assertTrue(update.contains("COALESCE(pitch_bb_hp,0)"), "缺少旧数据 BB+HBP 合计回退");
        assertTrue(update.contains("COALESCE(pitch_sh,0)") && update.contains("COALESCE(pitch_sf,0)"),
                "新公式必须扣除 SH/SF");
        assertFalse(sql.contains("ALTER TABLE"), "不得改表结构");
    }

    @Test
    @DisplayName("只回填旧公式自动值（SH/SF 存在）与 0 值默认，NULL 与他人值不动")
    void whereLimitsToAutoValues() throws Exception {
        String sql = read(MIGRATION);
        assertTrue(sql.contains("WHERE pitch_ab IS NOT NULL"), "整体必须排除 NULL（查询期推算，不强制落库）");

        String update = STAT_UPDATE.matcher(sql).results().reduce((a, b) -> b).orElseThrow().group();
        String where = update.substring(update.indexOf("WHERE"));
        assertTrue(where.contains("pitch_ab = " + LEGACY_FORMULA),
                "缺少“库值=旧公式”判定（未减 SH/SF 的历史自动值）");
        assertTrue(where.contains("(COALESCE(pitch_sh,0) <> 0 OR COALESCE(pitch_sf,0) <> 0)"),
                "旧公式分支必须限定本行存在 SH/SF，否则无 SH/SF 场次无需回填");
        assertTrue(where.contains("pitch_ab = 0"), "缺少 0 值默认兜底分支");
        assertTrue(where.contains("> 0"), "0 值分支必须限定新公式 > 0");
        assertTrue(where.contains("OR ("), "两个回填条件应为 OR 合并");
    }

    @Test
    @DisplayName("审计 SQL 全部注释掉，不随迁移执行")
    void auditSqlIsCommentedOut() throws Exception {
        String sql = read(MIGRATION);
        for (String line : sql.split("\\R")) {
            if (line.contains("SELECT")) {
                assertTrue(line.trim().startsWith("--"), "审计 SELECT 未注释：" + line.trim());
            }
        }
        assertTrue(sql.contains("SELECT COUNT(*)"), "缺少只读审计计数 SQL");
        assertTrue(sql.contains("LIMIT 50"), "缺少抽样审计 SQL");
    }

    @Test
    @DisplayName("不修改旧迁移文件 V20260501100000")
    void oldMigrationUntouched() throws Exception {
        String old = read(OLD_MIGRATION);
        assertTrue(old.contains("ADD COLUMN IF NOT EXISTS pitch_ab INTEGER"), "旧迁移建列语句被改动");
        assertTrue(old.contains("BF−BB−IBB−HBP"), "旧迁移原始注释被改动（Flyway 校验和会变）");
        assertFalse(old.contains("pitch_sh"), "旧迁移不应包含 AB 回填逻辑");
    }

    private static String read(String path) throws Exception {
        try (InputStream in = Resources.getResourceAsStream(path)) {
            assertNotNull(in, "文件不存在: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
