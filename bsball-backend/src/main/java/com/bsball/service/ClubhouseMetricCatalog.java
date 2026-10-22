package com.bsball.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 个人驾驶舱的指标白名单与图表兼容矩阵。
 * <p>只有这里登记的指标可以被前端选择或写入配置，服务端不接受任意字段名。</p>
 */
public final class ClubhouseMetricCatalog {
    public static final String DEFAULT_PERFORMANCE_KEY = "batting.avg";
    public static final String DEFAULT_VIZ = "line";
    public static final List<String> DEFAULT_RANK_KEYS = List.of(
            "batting.avg", "batting.h", "batting.obp", "batting.rbi");
    public static final List<String> SCALAR_VIZ = List.of("kpi", "line", "bar");
    public static final List<String> COMPOSITION_VIZ = List.of("donut");

    public record MetricDef(
            String key,
            String label,
            String domain,
            String field,
            List<String> viz,
            boolean rankable,
            String sortProp,
            String sortOrder) {
    }

    private static final List<MetricDef> METRICS = List.of(
            new MetricDef("batting.avg", "打击率", "batting", "avg", SCALAR_VIZ, true, "avg", "desc"),
            new MetricDef("batting.obp", "上垒率", "batting", "obp", SCALAR_VIZ, true, "obp", "desc"),
            new MetricDef("batting.slg", "长打率", "batting", "slg", SCALAR_VIZ, true, "slg", "desc"),
            new MetricDef("batting.ops", "OPS", "batting", "ops", SCALAR_VIZ, true, "ops", "desc"),
            new MetricDef("batting.h", "安打", "batting", "h", SCALAR_VIZ, true, "h", "desc"),
            new MetricDef("batting.r", "得分", "batting", "r", SCALAR_VIZ, true, "r", "desc"),
            new MetricDef("batting.rbi", "打点", "batting", "rbi", SCALAR_VIZ, true, "rbi", "desc"),
            new MetricDef("batting.sb", "盗垒", "batting", "sb", SCALAR_VIZ, true, "sb", "desc"),
            new MetricDef("batting.hr", "全垒打", "batting", "hr", SCALAR_VIZ, false, "hr", "desc"),
            new MetricDef("batting.so", "三振", "batting", "so", SCALAR_VIZ, false, "so", "asc"),
            new MetricDef("batting.hitTypes", "安打构成", "batting", null, COMPOSITION_VIZ, false, null, null),
            new MetricDef("pitching.era", "防御率", "pitching", "era", SCALAR_VIZ, true, "era", "asc"),
            new MetricDef("pitching.whip", "WHIP", "pitching", "whip", SCALAR_VIZ, true, "whip", "asc"),
            new MetricDef("pitching.w", "胜投", "pitching", "w", SCALAR_VIZ, true, "w", "desc"),
            new MetricDef("pitching.sv", "救援成功", "pitching", "sv", SCALAR_VIZ, true, "sv", "desc"),
            new MetricDef("pitching.so", "夺三振", "pitching", "pitchSo", SCALAR_VIZ, true, "pitchSo", "desc"),
            new MetricDef("pitching.ip", "投球局数", "pitching", "ip", SCALAR_VIZ, false, "ip", "desc"));

    private static final Map<String, MetricDef> BY_KEY = METRICS.stream()
            .collect(Collectors.toUnmodifiableMap(MetricDef::key, Function.identity()));

    private ClubhouseMetricCatalog() {
    }

    public static MetricDef find(String key) {
        return key == null ? null : BY_KEY.get(key.trim());
    }

    public static boolean supportsViz(String key, String viz) {
        MetricDef def = find(key);
        return def != null && viz != null && def.viz().contains(viz.trim());
    }

    public static boolean isRankable(String key) {
        MetricDef def = find(key);
        return def != null && def.rankable();
    }

    public static List<MetricDef> all() {
        return METRICS;
    }

    public static List<String> allowedViz(String key) {
        MetricDef def = find(key);
        return def == null ? List.of() : def.viz();
    }

    public static Map<String, Object> defaultConfig() {
        LinkedHashMap<String, Object> performance = new LinkedHashMap<>();
        performance.put("metricKey", DEFAULT_PERFORMANCE_KEY);
        performance.put("viz", DEFAULT_VIZ);

        LinkedHashMap<String, Object> milestones = new LinkedHashMap<>();
        milestones.put("metricKeys", ClubhouseMilestoneCatalog.defaultKeys());

        LinkedHashMap<String, Object> config = new LinkedHashMap<>();
        config.put("version", 2);
        config.put("layout", "v3-modular");
        config.put("modules", ClubhouseModuleCatalog.defaultModules());
        config.put("milestones", milestones);
        config.put("performance", performance);
        config.put("ranks", defaultRankEntries());
        return config;
    }

    /** 默认四个排名位；每次返回独立的可变结构，避免调用方共享状态。 */
    public static List<Map<String, Object>> defaultRankEntries() {
        List<Map<String, Object>> ranks = new ArrayList<>(DEFAULT_RANK_KEYS.size());
        for (String key : DEFAULT_RANK_KEYS) {
            LinkedHashMap<String, Object> rank = new LinkedHashMap<>();
            rank.put("metricKey", key);
            ranks.add(rank);
        }
        return ranks;
    }

    public static List<Map<String, Object>> catalog(boolean battingAvailable, boolean pitchingAvailable) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MetricDef def : METRICS) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("key", def.key());
            row.put("label", def.label());
            row.put("domain", def.domain());
            row.put("viz", def.viz());
            row.put("rankable", def.rankable());
            boolean available = "batting".equals(def.domain()) ? battingAvailable : pitchingAvailable;
            row.put("available", available);
            rows.add(row);
        }
        return rows;
    }

    public static Set<String> allKeys() {
        return BY_KEY.keySet();
    }
}
