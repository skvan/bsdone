package com.bsball.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 个人驾驶舱可配置模块白名单。
 * <p>数组顺序即默认展示顺序；宽度由默认列宽与最小列宽约束，用户可在桌面端调整。
 * 高度由用户按 10px 行调整，0 表示随内容自适应。</p>
 */
public final class ClubhouseModuleCatalog {
    public static final int MAX_SPAN = 12;
    public static final int MAX_HEIGHT_ROWS = 80;

    public record ModuleDef(String key, String label, int span, int minSpan, boolean enabledByDefault) {
    }

    private static final List<ModuleDef> MODULES = List.of(
            new ModuleDef("ranks", "队内坐标", 12, 6, true),
            new ModuleDef("evaluation", "总体评价", 12, 8, true),
            new ModuleDef("milestones", "生涯里程碑", 12, 8, true),
            new ModuleDef("performance", "数据表现", 8, 6, true),
            new ModuleDef("teams", "代表球队", 4, 4, true),
            new ModuleDef("schedule", "接下来的比赛", 8, 6, true),
            new ModuleDef("about", "关于我", 4, 4, true),
            new ModuleDef("recentGames", "最近战报", 8, 6, true),
            new ModuleDef("quickLinks", "快捷入口", 4, 4, true));

    private static final Map<String, ModuleDef> BY_KEY = MODULES.stream()
            .collect(Collectors.toUnmodifiableMap(ModuleDef::key, Function.identity()));

    private ClubhouseModuleCatalog() {
    }

    public static ModuleDef find(String key) {
        return key == null ? null : BY_KEY.get(key.trim());
    }

    public static List<ModuleDef> all() {
        return MODULES;
    }

    /** 默认模块配置；每次返回独立结构，宽度取目录默认值，高度自适应。 */
    public static List<Map<String, Object>> defaultModules() {
        List<Map<String, Object>> rows = new ArrayList<>(MODULES.size());
        for (ModuleDef def : MODULES) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("key", def.key());
            row.put("enabled", def.enabledByDefault());
            row.put("width", def.span());
            row.put("height", 0);
            rows.add(row);
        }
        return rows;
    }

    /** 前端渲染与尺寸手柄所需的模块元数据。 */
    public static List<Map<String, Object>> catalog() {
        List<Map<String, Object>> rows = new ArrayList<>(MODULES.size());
        for (ModuleDef def : MODULES) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("key", def.key());
            row.put("label", def.label());
            row.put("span", def.span());
            row.put("minSpan", def.minSpan());
            row.put("maxSpan", MAX_SPAN);
            rows.add(row);
        }
        return rows;
    }
}
