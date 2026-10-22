package com.bsball.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 个人驾驶舱可配置模块白名单。
 * <p>数组顺序即默认展示顺序；宽度由目录固定，第一阶段不开放尺寸调整。</p>
 */
public final class ClubhouseModuleCatalog {
    public record ModuleDef(String key, String label, int span, boolean enabledByDefault) {
    }

    private static final List<ModuleDef> MODULES = List.of(
            new ModuleDef("ranks", "队内坐标", 12, true),
            new ModuleDef("evaluation", "总体评价", 12, true),
            new ModuleDef("milestones", "生涯里程碑", 12, true),
            new ModuleDef("performance", "数据表现", 8, true),
            new ModuleDef("teams", "代表球队", 4, true),
            new ModuleDef("schedule", "接下来的比赛", 8, true),
            new ModuleDef("about", "关于我", 4, true),
            new ModuleDef("recentGames", "最近战报", 8, true),
            new ModuleDef("quickLinks", "快捷入口", 4, true));

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

    /** 默认模块配置；每次返回独立结构。 */
    public static List<Map<String, Object>> defaultModules() {
        List<Map<String, Object>> rows = new ArrayList<>(MODULES.size());
        for (ModuleDef def : MODULES) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("key", def.key());
            row.put("enabled", def.enabledByDefault());
            rows.add(row);
        }
        return rows;
    }

    /** 前端渲染所需的模块元数据。 */
    public static List<Map<String, Object>> catalog() {
        List<Map<String, Object>> rows = new ArrayList<>(MODULES.size());
        for (ModuleDef def : MODULES) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("key", def.key());
            row.put("label", def.label());
            row.put("span", def.span());
            rows.add(row);
        }
        return rows;
    }
}
