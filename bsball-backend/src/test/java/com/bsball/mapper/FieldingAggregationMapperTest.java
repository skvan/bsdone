package com.bsball.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 守备聚合 Mapper 护栏：野手改投后（position=P）仍按球员行计入守备统计，且投手并行行不重复累计。
 */
@DisplayName("守备聚合 Mapper：P 行按球员行计一次")
class FieldingAggregationMapperTest {

    private static final String PLAYER_STATS = "mapper/PlayerStatsMapper.xml";
    private static final String STATS_LEADERS = "mapper/StatsLeadersMapper.xml";

    private static Configuration configuration;
    private static String playerStatsXml;
    private static String statsLeadersXml;

    @BeforeAll
    static void parseMappers() throws Exception {
        configuration = new Configuration();
        configuration.setDatabaseId("PostgreSQL");
        parse(PLAYER_STATS);
        parse(STATS_LEADERS);
        playerStatsXml = read(PLAYER_STATS);
        statsLeadersXml = read(STATS_LEADERS);
    }

    @Test
    @DisplayName("个人/球队/赛季/钻取守备统计全部使用统一行过滤")
    void playerFieldingStatementsUseCountedRowFilter() {
        Map<String, Object> params = playerParams();
        for (String id : List.of(
                "selectFieldingStats",
                "selectFieldingStatsByTeamScope",
                "selectFieldingStatsByEvent",
                "countFieldingDrillDown",
                "selectFieldingDrillDownPage")) {
            assertCountedRow(boundSql("com.bsball.mapper.PlayerStatsMapper." + id, params), id);
        }
        assertEquals(4, countOccurrences(playerStatsXml, "refid=\"fieldingCountedPlayerRow\""),
                "PlayerStatsMapper 的 4 处守备聚合都应 include 统一行过滤");
        assertEquals(1, countOccurrences(playerStatsXml, "<sql id=\"fieldingCountedPlayerRow\">"),
                "统一行过滤只应定义一次");
        assertFalse(playerStatsXml.contains("'P', 'DH', 'PH'"),
                "守备聚合不得再按最终守位排除 P 行（野手改投会丢整行守备数据）");
    }

    @Test
    @DisplayName("个人守备榜/球队守备榜的 count 与 page 一致使用统一行过滤")
    void leaderFieldingStatementsUseCountedRowFilter() {
        Map<String, Object> params = leaderParams();
        for (String id : List.of(
                "countFieldingLeaders",
                "selectFieldingLeadersPage",
                "countTeamFieldingLeaders",
                "selectTeamFieldingLeadersPage")) {
            assertCountedRow(boundSql("com.bsball.mapper.StatsLeadersMapper." + id, params), id);
        }
        assertEquals(2, countOccurrences(statsLeadersXml, "refid=\"fieldingCountedPlayerRow\""),
                "个人守备榜与球队守备都应 include 统一行过滤");
        assertEquals(1, countOccurrences(statsLeadersXml, "<sql id=\"fieldingCountedPlayerRow\">"),
                "统一行过滤只应定义一次");
        assertFalse(statsLeadersXml.contains("'P', 'DH', 'PH'"),
                "守备榜不得再按最终守位排除 P 行（野手改投会丢整行守备数据）");
        assertFalse(boundSql("com.bsball.mapper.StatsLeadersMapper.countFieldingLeaders", params)
                        .contains("COALESCE(s.is_pitcher, 0) <> 0"),
                "未按守位筛选时不应放宽投手行的守备动作门槛");
    }

    @Test
    @DisplayName("两个 Mapper 的守备行口径保持同步")
    void countedRowFragmentsStayInSync() {
        assertEquals(normalize(sqlFragment(playerStatsXml)), normalize(sqlFragment(statsLeadersXml)),
                "PlayerStatsMapper 与 StatsLeadersMapper 的守备行口径必须一致");
    }

    @Test
    @DisplayName("位置筛选 P 时补入投手行，同位置已有带守备数据的球员行则不重复")
    void positionCodeFilterKeepsPitcherRowsOnce() {
        Map<String, Object> params = leaderParams();
        params.put("positionCode", "P");

        String countSql = boundSql("com.bsball.mapper.StatsLeadersMapper.countFieldingLeaders", params);
        assertTrue(countSql.contains("s.position = ?"), "位置筛选条件丢失");
        assertTrue(countSql.contains("COALESCE(s.is_pitcher, 0) <> 0"), "P 筛选缺少投手行补入口");
        assertTrue(countSql.contains("b.position = ?"), "补入口缺少同位置球员行去重");
        assertTrue(countSql.contains("NOT EXISTS"), "补入口缺少 NOT EXISTS 去重");

        String pageSql = boundSql("com.bsball.mapper.StatsLeadersMapper.selectFieldingLeadersPage", params);
        assertTrue(pageSql.contains("COALESCE(s.is_pitcher, 0) <> 0"), "page 查询与 count 口径不一致");
    }

    @Test
    @DisplayName("捕手后改投：PB/CS/SB 按行累计，C 筛选按首次守位/捕手动作纳入")
    void catcherStatsCountedByRowAfterPositionChange() {
        String playerSql = boundSql("com.bsball.mapper.PlayerStatsMapper.selectFieldingStats", playerParams());
        assertTrue(playerSql.contains("SUM(COALESCE(s.pb, 0))"), "个人守备 PB 仍按最终守位过滤");
        assertTrue(playerSql.contains("SUM(COALESCE(s.catcher_cs, 0))"), "个人守备 CS 仍按最终守位过滤");
        assertFalse(playerSql.contains("CASE WHEN s.position = 'C'"), "个人守备捕手列仍受 position 限制");

        String teamSql = boundSql("com.bsball.mapper.StatsLeadersMapper.selectTeamFieldingLeadersPage", leaderParams());
        assertTrue(teamSql.contains("SUM(COALESCE(s.pb, 0))"), "球队守备 PB 仍按最终守位过滤");
        assertFalse(teamSql.contains("CASE WHEN s.position = 'C'"), "球队守备捕手列仍受 position 限制");

        Map<String, Object> params = leaderParams();
        params.put("positionCode", "C");
        String countSql = boundSql("com.bsball.mapper.StatsLeadersMapper.countFieldingLeaders", params);
        assertTrue(countSql.contains("COALESCE(s.pb,0) > 0"), "C 筛选未纳入捕手动作");
        assertTrue(countSql.contains("s.first_position = 'C'"), "C 筛选未纳入首次守位");
        String pageSql = boundSql("com.bsball.mapper.StatsLeadersMapper.selectFieldingLeadersPage", params);
        assertTrue(pageSql.contains("s.first_position = 'C'"), "page 查询与 count 的 C 筛选口径不一致");

        String noPosition = boundSql("com.bsball.mapper.StatsLeadersMapper.countFieldingLeaders", leaderParams());
        assertFalse(noPosition.contains("s.first_position = 'C'"), "未筛选守位时不应附加 C 口径");
    }

    private static void assertCountedRow(String sql, String id) {
        assertTrue(sql.contains("s.position IS NOT NULL"), id + " 缺少守位非空过滤");
        assertTrue(sql.contains("s.position NOT IN ('DH', 'PH', 'EP')"), id + " 缺少 DH/PH/EP 排除");
        assertTrue(sql.contains("COALESCE(s.is_pitcher, 0) = 0"), id + " 缺少球员行口径");
        assertTrue(sql.contains("COALESCE(b.is_pitcher, 0) = 0"), id + " 子查询未按球员行判断");
        assertTrue(sql.contains("b.position NOT IN ('DH', 'PH', 'EP')"), id + " 子查询未排除 DH/PH/EP");
        assertTrue(sql.contains("NOT EXISTS"), id + " 缺少投手行补计去重");
        assertTrue(sql.contains("COALESCE(s.catcher_cs,0)"),
                id + " 补计口径应覆盖 PO/A/E/DP/PB/CS/SB 动作");
        assertTrue(sql.contains("COALESCE(s.def_inn,0)"),
                id + " 外层球员行缺少守备证据门槛（旧数据只写投手行时会同场出现两行）");
        assertTrue(sql.contains("COALESCE(b.def_inn,0)"),
                id + " 球员行去重应覆盖守备局数，避免与投手行重复累计");
        int outerEvidence = sql.indexOf("OR COALESCE(s.def_inn,0) > 0");
        int roleBranch = sql.indexOf("COALESCE(s.is_pitcher, 0) = 0");
        assertTrue(outerEvidence > 0 && roleBranch > outerEvidence,
                id + " 守备证据门槛必须先于 is_pitcher 角色分支，零守备球员行不得挡住投手行");
        assertFalse(sql.contains("'P', 'DH', 'PH'"), id + " 仍按最终守位排除 P 行");
    }

    private static String sqlFragment(String xml) {
        var matcher = Pattern.compile("(?s)<sql id=\"fieldingCountedPlayerRow\">(.*?)</sql>").matcher(xml);
        assertTrue(matcher.find(), "缺少 fieldingCountedPlayerRow 定义");
        return matcher.group(1);
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static String boundSql(String statementId, Map<String, Object> params) {
        MappedStatement statement = configuration.getMappedStatement(statementId);
        BoundSql boundSql = statement.getBoundSql(params);
        return boundSql.getSql().replaceAll("\\s+", " ").trim();
    }

    private static Map<String, Object> playerParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("playerId", 1L);
        params.put("tenantId", 1L);
        params.put("teamId", 2L);
        params.put("eventId", null);
        params.put("season", null);
        params.put("gameMode", null);
        params.put("valueSql", "s.tc");
        params.put("limit", 20);
        params.put("offset", 0L);
        return params;
    }

    private static Map<String, Object> leaderParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("tenantId", 1L);
        params.put("eventId", null);
        params.put("eventIds", null);
        params.put("years", null);
        params.put("teamIds", null);
        params.put("playerKeyword", null);
        params.put("positionCode", null);
        params.put("homeAway", null);
        params.put("batterHand", null);
        params.put("pitcherHand", null);
        params.put("gameMode", null);
        params.put("applyQualification", Boolean.FALSE);
        params.put("orderBySql", "tc DESC");
        params.put("limit", 20);
        params.put("offset", 0L);
        return params;
    }

    private static void parse(String resource) throws Exception {
        try (InputStream in = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private static String read(String resource) throws Exception {
        try (InputStream in = Resources.getResourceAsStream(resource)) {
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
