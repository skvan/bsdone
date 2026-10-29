package com.bsball.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.ibatis.io.Resources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 历史首个守位回填迁移护栏：只填空值、按最早有效事件还原、旧场次不臆造。
 */
@DisplayName("历史 first_position 回填迁移")
class FirstPositionBackfillMigrationTest {

    private static final String MIGRATION =
            "db/migration/postgresql/V20261029100000__backfill_game_player_stat_first_position.sql";
    private static final Pattern STAT_UPDATE = Pattern.compile("(?s)UPDATE bs_game_player_stat.*?;");
    private static final String VALID_POSITIONS =
            "('P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF', 'XF')";

    @Test
    @DisplayName("四条回填都只填空值，不覆盖实时写入")
    void everyUpdateOnlyFillsNulls() throws Exception {
        String sql = read();
        Matcher matcher = STAT_UPDATE.matcher(sql);
        int statements = 0;
        while (matcher.find()) {
            statements++;
            assertTrue(matcher.group().contains("first_position IS NULL"),
                    "第 " + statements + " 条回填未限定 first_position IS NULL");
        }
        assertEquals(4, statements, "应恰好四条回填：事件、当前守位、投手复制、纯投手");
        assertFalse(sql.replaceAll("\\s+", " ").contains("SET first_position = position"),
                "不得无条件用 position 覆盖历史 first_position");
    }

    @Test
    @DisplayName("按最早有效换人事件还原，兼容 DH/PH/EP 进场与软删")
    void resolvesEarliestFieldingEvent() throws Exception {
        String sql = read();
        assertTrue(sql.contains("bs_game_substitution_event"), "缺少换人事件来源");
        assertTrue(sql.contains("e.deleted_at IS NULL"), "换人事件必须过滤软删除");
        assertTrue(sql.contains("SUBSTITUTION"), "换人事件未参与还原");
        assertTrue(sql.contains("POSITION_CHANGE"), "位置调整事件未参与还原");
        assertTrue(sql.contains("PITCHING_CHANGE"), "换投事件未参与还原");
        assertTrue(sql.contains("DH_CANCEL"), "DH 取消事件未参与还原");
        assertTrue(sql.contains("ORDER BY stat_id, sequence, change_seq, event_id"),
                "未按全场事件顺序取最早一条");
        assertTrue(sql.contains("UPPER(BTRIM(s.position)) NOT IN ('DH', 'PH', 'EP')"),
                "DH/PH/EP 不是守位");
        assertEquals(3, countOccurrences(sql, "IN " + VALID_POSITIONS),
                "事件守位、当前守位与并行行复制都必须限制为合法守位");

        int firstEvent = sql.indexOf("first_fielding_event AS (");
        int whitelist = sql.indexOf("WHERE pos IN " + VALID_POSITIONS);
        int firstUpdate = sql.indexOf("UPDATE bs_game_player_stat");
        assertTrue(firstEvent > 0 && whitelist > firstEvent && whitelist < firstUpdate,
                "必须先过滤非法守位再取最早事件，否则会被代打上场的 PH 挡住");

        int currentPosition = sql.indexOf("SET first_position = UPPER(BTRIM(s.position))");
        int gameEvents = sql.indexOf("SELECT 1 FROM bs_game_substitution_event e");
        assertTrue(currentPosition > 0 && gameEvents > currentPosition,
                "当前守位兜底必须限定同场（同队）存在未删除事件");
        assertTrue(sql.contains("s.deleted_at IS NULL") && sql.contains("p.deleted_at IS NULL"),
                "回填必须过滤软删除的统计行");
    }

    @Test
    @DisplayName("投手并行行只复制球员行，纯投手才补 P")
    void pitcherRowsFollowPlayerRow() throws Exception {
        String sql = read();
        assertTrue(sql.contains("COALESCE(p.is_pitcher, 0) = 1"), "投手行识别条件丢失");
        assertTrue(sql.contains("COALESCE(b.is_pitcher, 0) = 0"), "并行行不得从另一投手行复制");
        assertTrue(sql.contains("b.player_id = p.player_id"), "并行行未按同场同球员复制");
        assertTrue(sql.contains("b.first_position IS NOT NULL"), "缺少复制来源非空判断");
        assertTrue(sql.contains("first_position = 'P'"), "纯投手行未补 P");
        assertTrue(sql.contains("COALESCE(p.ip, 0) > 0"), "纯投手缺少投球证据门槛");

        String purePitcher = sql.substring(sql.indexOf("first_position = 'P'"));
        assertTrue(purePitcher.contains("NOT EXISTS"), "纯投手补 P 缺少球员行去重（二刀流双计）");
        assertTrue(purePitcher.contains("COALESCE(b.is_pitcher, 0) = 0"),
                "纯投手去重未按球员行判断");
    }

    @Test
    @DisplayName("攻方半局换上的代打/代跑不继承被换下者守位")
    void offensiveHalfSubstitutionDoesNotInheritPosition() throws Exception {
        String sql = read();
        assertTrue(sql.contains("JOIN bs_game g ON g.id = s.game_id"), "缺少主客队方向判断");
        assertTrue(sql.contains("e.top_bottom = 'top' AND e.team_id = g.away_team_id"),
                "上半局客队为攻方，未排除其换上的代打/代跑");
        assertTrue(sql.contains("e.top_bottom = 'bottom' AND e.team_id = g.home_team_id"),
                "下半局主队为攻方，未排除其换上的代打/代跑");
        int substitution = sql.indexOf("e.change_type = 'SUBSTITUTION'");
        int topGuard = sql.indexOf("e.top_bottom = 'top'", substitution);
        int thenIn = sql.indexOf("THEN e.in_position", substitution);
        assertTrue(substitution > 0 && topGuard > substitution && topGuard < thenIn,
                "SUBSTITUTION 分支必须先判断攻守方向，再取 in_position");
    }

    private static String read() throws Exception {
        try (InputStream in = Resources.getResourceAsStream(MIGRATION)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
