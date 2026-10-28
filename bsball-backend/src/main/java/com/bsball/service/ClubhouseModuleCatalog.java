package com.bsball.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 个人驾驶舱可配置模块白名单与画布默认几何。
 * <p>数组顺序即手机端单列、设置抽屉与键盘 Tab 顺序；桌面自由布局只看
 * x/y/width/height。默认布局按数组顺序装箱，模块间距 16px。</p>
 */
public final class ClubhouseModuleCatalog {
    public record ModuleDef(
            String key,
            String label,
            int defaultWidth,
            int defaultHeight,
            int minWidth,
            int minHeight,
            boolean enabledByDefault) {
    }

    private static final List<ModuleDef> MODULES = List.of(
            new ModuleDef("ranks", "队内坐标", 1216, 220, 600, 160, true),
            new ModuleDef("evaluation", "总体评价", 1216, 700, 720, 360, true),
            new ModuleDef("milestones", "生涯里程碑", 1216, 500, 680, 360, true),
            new ModuleDef("performance", "数据表现", 800, 500, 360, 320, true),
            new ModuleDef("teams", "代表球队", 400, 300, 280, 220, true),
            new ModuleDef("schedule", "接下来的比赛", 800, 560, 420, 300, true),
            new ModuleDef("about", "关于我", 400, 280, 300, 220, true),
            new ModuleDef("recentGames", "最近战报", 800, 320, 360, 260, true),
            new ModuleDef("quickLinks", "快捷入口", 400, 220, 240, 180, true));

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

    /** 默认模块配置：按数组顺序装箱，字段为 key/enabled/x/y/width/height。 */
    public static List<Map<String, Object>> defaultModules() {
        List<ClubhouseBoardLayout.ModulePlacement> placements = new ArrayList<>(MODULES.size());
        for (ModuleDef def : MODULES) {
            placements.add(new ClubhouseBoardLayout.ModulePlacement(
                    def.key(), def.enabledByDefault(), 0, 0, def.defaultWidth(), def.defaultHeight()));
        }
        List<Map<String, Object>> rows = new ArrayList<>(MODULES.size());
        for (ClubhouseBoardLayout.ModulePlacement placement : ClubhouseBoardLayout.pack(placements)) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("key", placement.key());
            row.put("enabled", placement.enabled());
            row.put("x", placement.x());
            row.put("y", placement.y());
            row.put("width", placement.width());
            row.put("height", placement.height());
            rows.add(row);
        }
        return rows;
    }

    /** 前端渲染与尺寸手柄所需的模块元数据，尺寸均为像素。 */
    public static List<Map<String, Object>> catalog() {
        List<Map<String, Object>> rows = new ArrayList<>(MODULES.size());
        for (ModuleDef def : MODULES) {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("key", def.key());
            row.put("label", def.label());
            row.put("defaultWidth", def.defaultWidth());
            row.put("defaultHeight", def.defaultHeight());
            row.put("minWidth", def.minWidth());
            row.put("minHeight", def.minHeight());
            row.put("maxWidth", ClubhouseBoardLayout.BOARD_WIDTH);
            row.put("maxHeight", ClubhouseBoardLayout.MAX_HEIGHT);
            rows.add(row);
        }
        return rows;
    }

    /** 旧 12 列宽度转像素：round(span * (1216 + 16) / 12) - 16。 */
    static int legacyWidth(int span) {
        return (int) Math.round(span * (double) (ClubhouseBoardLayout.BOARD_WIDTH + ClubhouseBoardLayout.GAP) / 12.0)
                - ClubhouseBoardLayout.GAP;
    }
}
