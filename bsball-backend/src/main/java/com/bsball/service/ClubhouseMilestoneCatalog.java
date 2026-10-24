package com.bsball.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 生涯里程碑的累计指标白名单。
 * <p>阈值是面向业余棒球的整数里程碑，服务端不接受任意阈值。</p>
 */
public final class ClubhouseMilestoneCatalog {
    public static final int MAX_SELECTED = 6;

    public record MetricDef(
            String key,
            String label,
            String shortLabel,
            String domain,
            String unit,
            List<Integer> thresholds,
            boolean defaultSelected) {
    }

    private static final List<MetricDef> METRICS = List.of(
            new MetricDef("batting.h", "安打", "H", "batting", "支", List.of(1, 25, 50, 100, 200), true),
            new MetricDef("batting.hr", "本垒打", "HR", "batting", "支", List.of(1, 5, 10, 20, 50), false),
            new MetricDef("batting.rbi", "打点", "RBI", "batting", "分", List.of(1, 25, 50, 100), true),
            new MetricDef("batting.r", "得分", "R", "batting", "分", List.of(10, 25, 50, 100), false),
            new MetricDef("batting.sb", "盗垒", "SB", "batting", "次", List.of(10, 25, 50, 100), true),
            new MetricDef("batting.gp", "出赛", "G", "batting", "场", List.of(10, 25, 50, 100), true),
            new MetricDef("pitching.ip", "投球局数", "IP", "pitching", "局", List.of(10, 25, 50, 100, 200), true),
            new MetricDef("pitching.so", "夺三振", "SO", "pitching", "次", List.of(10, 25, 50, 100, 200), true),
            new MetricDef("pitching.w", "胜投", "W", "pitching", "胜", List.of(1, 5, 10, 20), false),
            new MetricDef("pitching.sv", "救援成功", "SV", "pitching", "次", List.of(1, 5, 10, 20), false),
            new MetricDef("pitching.np", "投球数", "NP", "pitching", "球", List.of(500, 1000, 2000, 5000), false));

    private static final Map<String, MetricDef> BY_KEY = METRICS.stream()
            .collect(Collectors.toUnmodifiableMap(MetricDef::key, Function.identity()));

    private ClubhouseMilestoneCatalog() {
    }

    /** 按 key 查找里程碑指标；未知返回 null。 */
    public static MetricDef find(String key) {
        return key == null ? null : BY_KEY.get(key.trim());
    }

    /** 返回全部里程碑指标定义。 */
    public static List<MetricDef> all() {
        return METRICS;
    }

    /** 返回默认选中的指标 key，顺序与目录一致。 */
    public static List<String> defaultKeys() {
        return METRICS.stream()
                .filter(MetricDef::defaultSelected)
                .map(MetricDef::key)
                .toList();
    }

    /** 返回前端设置与展示所需的指标元数据。 */
    public static List<Map<String, Object>> catalog() {
        List<Map<String, Object>> rows = new ArrayList<>(METRICS.size());
        for (MetricDef def : METRICS) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("key", def.key());
            row.put("label", def.label());
            row.put("shortLabel", def.shortLabel());
            row.put("domain", def.domain());
            row.put("unit", def.unit());
            row.put("defaultSelected", def.defaultSelected());
            row.put("thresholds", def.thresholds());
            rows.add(row);
        }
        return rows;
    }
}
