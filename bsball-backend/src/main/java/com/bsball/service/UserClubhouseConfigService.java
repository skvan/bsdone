package com.bsball.service;

import com.bsball.exception.BusinessException;
import com.bsball.model.entity.UserClubhouseConfig;
import com.bsball.repository.UserClubhouseConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 个人驾驶舱配置的读取、校验与保存。
 * <p>服务端只接受 {@link ClubhouseMetricCatalog} 中的指标、兼容图表类型和
 * {@link ClubhouseModuleCatalog} 中的模块；数组顺序用于手机端与设置排序，
 * 桌面端位置与像素尺寸由 x/y/width/height 决定。</p>
 */
@Service
@RequiredArgsConstructor
public class UserClubhouseConfigService {
    private static final int CONFIG_VERSION = 4;
    private static final String CONFIG_LAYOUT = "v4-board";
    private static final int RANK_SLOT_COUNT = 4;
    private static final int LEGACY_MAX_SPAN = 12;
    private static final int LEGACY_MAX_HEIGHT_ROWS = 80;

    private final UserClubhouseConfigRepository configRepository;
    private final ObjectMapper objectMapper;

    public Map<String, Object> getOrCreate(Long userId, Long tenantId) {
        UserClubhouseConfig entity = configRepository
                .findFirstByUserIdAndTenantIdAndDeletedAtIsNull(userId, tenantId)
                .orElse(null);
        if (entity != null) {
            Map<String, Object> parsed = parseOrDefault(entity.getConfigJson());
            if (parsed != null) {
                return parsed;
            }
        }
        // 读接口不写库：避免并发首次访问撞唯一索引；首次保存时再落库。
        return ClubhouseMetricCatalog.defaultConfig();
    }

    @Transactional
    public Map<String, Object> save(Long userId, Long tenantId, Map<String, Object> body) {
        Map<String, Object> normalized = normalize(body);
        configRepository.upsert(userId, tenantId, write(normalized));
        return normalized;
    }

    @Transactional
    public Map<String, Object> reset(Long userId, Long tenantId) {
        return save(userId, tenantId, ClubhouseMetricCatalog.defaultConfig());
    }

    private Map<String, Object> parseOrDefault(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = objectMapper.readValue(json, Map.class);
            return normalize(raw);
        } catch (JsonProcessingException | BusinessException e) {
            return null;
        }
    }

    private String write(Map<String, Object> config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("驾驶舱配置序列化失败", e);
        }
    }

    /**
     * 校验并收敛为固定字段结构；非法指标/图表/排名/模块直接拒绝。
     * <p>v1–v3 旧网格配置在内存中迁移为 v4 像素画布，GET 不会写库。</p>
     */
    Map<String, Object> normalize(Map<String, Object> body) {
        Map<String, Object> source = body == null ? Map.of() : body;
        Map<String, Object> performance = asMap(source.get("performance"));
        String metricKey = asText(performance.get("metricKey"));
        ClubhouseMetricCatalog.MetricDef metric = ClubhouseMetricCatalog.find(metricKey);
        if (metric == null) {
            throw new BusinessException(400, "不支持的驾驶舱指标");
        }
        String viz = asText(performance.get("viz"));
        if (!ClubhouseMetricCatalog.supportsViz(metric.key(), viz)) {
            throw new BusinessException(400, "该指标不支持所选图表类型");
        }

        List<Map<String, Object>> ranks = normalizeRanks(source.get("ranks"));
        List<Map<String, Object>> modules = normalizeModules(source.get("modules"), isBoardConfig(source));
        List<String> milestoneKeys = normalizeMilestones(source.get("milestones"));
        LinkedHashMap<String, Object> config = new LinkedHashMap<>();
        config.put("version", CONFIG_VERSION);
        config.put("layout", CONFIG_LAYOUT);
        config.put("modules", modules);
        LinkedHashMap<String, Object> outMilestones = new LinkedHashMap<>();
        outMilestones.put("metricKeys", milestoneKeys);
        config.put("milestones", outMilestones);
        LinkedHashMap<String, Object> outPerformance = new LinkedHashMap<>();
        outPerformance.put("metricKey", metric.key());
        outPerformance.put("viz", viz.trim());
        config.put("performance", outPerformance);
        config.put("ranks", ranks);
        return config;
    }

    /** version >= 4、v4-board 或任一模块带 x/y 时按自由画布解析，否则按旧网格迁移。 */
    private static boolean isBoardConfig(Map<String, Object> source) {
        if (CONFIG_LAYOUT.equals(asText(source.get("layout")))) {
            return true;
        }
        if (source.get("version") instanceof Number number && number.doubleValue() >= CONFIG_VERSION) {
            return true;
        }
        Object raw = source.get("modules");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> row && (row.containsKey("x") || row.containsKey("y"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Map<String, Object>> normalizeModules(Object raw, boolean boardMode) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return ClubhouseModuleCatalog.defaultModules();
        }
        List<ClubhouseBoardLayout.ModulePlacement> placements = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Object item : list) {
            Map<String, Object> row = asMap(item);
            ClubhouseModuleCatalog.ModuleDef def = ClubhouseModuleCatalog.find(asText(row.get("key")));
            if (def == null) {
                throw new BusinessException(400, "不支持的驾驶舱模块");
            }
            if (!seen.add(def.key())) {
                throw new BusinessException(400, "驾驶舱模块不能重复");
            }
            boolean enabled = enabled(row.get("enabled"));
            placements.add(boardMode
                    ? boardPlacement(def, row, enabled)
                    : legacyPlacement(def, row, enabled));
        }
        List<ClubhouseBoardLayout.ModulePlacement> all = new ArrayList<>(placements);
        for (ClubhouseModuleCatalog.ModuleDef def : ClubhouseModuleCatalog.all()) {
            if (!seen.contains(def.key())) {
                all.add(ClubhouseBoardLayout.placeBelow(defaultPlacement(def, def.enabledByDefault()), all));
            }
        }
        List<ClubhouseBoardLayout.ModulePlacement> resolved = boardMode
                ? ClubhouseBoardLayout.resolveOverlaps(all)
                : ClubhouseBoardLayout.pack(all);
        for (ClubhouseBoardLayout.ModulePlacement placement : resolved) {
            if (placement.enabled() && placement.y() > ClubhouseBoardLayout.MAX_Y) {
                throw new BusinessException(400, "模块坐标超出范围");
            }
        }
        return toRows(resolved);
    }

    /** 解析 v4 模块行；字段缺失时回退默认几何，部分缺失视为非法。 */
    private static ClubhouseBoardLayout.ModulePlacement boardPlacement(
            ClubhouseModuleCatalog.ModuleDef def, Map<String, Object> row, boolean enabled) {
        if (row.get("x") == null && row.get("y") == null
                && row.get("width") == null && row.get("height") == null) {
            return defaultPlacement(def, enabled);
        }
        int x = asInt(row.get("x"), "模块 x 坐标必须为整数");
        int y = asInt(row.get("y"), "模块 y 坐标必须为整数");
        int width = asInt(row.get("width"), "模块宽度必须为整数");
        int height = asInt(row.get("height"), "模块高度必须为整数");
        if (x < 0 || y < 0 || y > ClubhouseBoardLayout.MAX_Y) {
            throw new BusinessException(400, "模块坐标超出范围");
        }
        if (width < def.minWidth() || width > ClubhouseBoardLayout.BOARD_WIDTH) {
            throw new BusinessException(400, def.label() + "宽度需在 " + def.minWidth()
                    + "–" + ClubhouseBoardLayout.BOARD_WIDTH + " 之间");
        }
        if (height < def.minHeight() || height > ClubhouseBoardLayout.MAX_HEIGHT) {
            throw new BusinessException(400, def.label() + "高度需在 " + def.minHeight()
                    + "–" + ClubhouseBoardLayout.MAX_HEIGHT + " 之间");
        }
        if (x + width > ClubhouseBoardLayout.BOARD_WIDTH) {
            throw new BusinessException(400, def.label() + "横向位置超出画布");
        }
        return new ClubhouseBoardLayout.ModulePlacement(def.key(), enabled, x, y, width, height);
    }

    /** 旧网格宽（列）高（10px 行）转像素；高度 0 表示使用目录默认高度。 */
    private static ClubhouseBoardLayout.ModulePlacement legacyPlacement(
            ClubhouseModuleCatalog.ModuleDef def, Map<String, Object> row, boolean enabled) {
        int width = def.defaultWidth();
        if (row.get("width") != null) {
            int span = asInt(row.get("width"), "模块宽度必须为整数");
            if (span < 1 || span > LEGACY_MAX_SPAN) {
                throw new BusinessException(400, def.label() + "宽度需在 1–" + LEGACY_MAX_SPAN + " 列之间");
            }
            width = Math.min(ClubhouseBoardLayout.BOARD_WIDTH,
                    Math.max(def.minWidth(), ClubhouseModuleCatalog.legacyWidth(span)));
        }
        int height = def.defaultHeight();
        if (row.get("height") != null) {
            int rows = asInt(row.get("height"), "模块高度必须为整数");
            if (rows < 0 || rows > LEGACY_MAX_HEIGHT_ROWS) {
                throw new BusinessException(400,
                        "模块高度需在 0–" + LEGACY_MAX_HEIGHT_ROWS + " 行之间");
            }
            if (rows > 0) {
                height = Math.min(ClubhouseBoardLayout.MAX_HEIGHT,
                        Math.max(def.minHeight(), rows * 10));
            }
        }
        return new ClubhouseBoardLayout.ModulePlacement(def.key(), enabled, 0, 0, width, height);
    }

    private static ClubhouseBoardLayout.ModulePlacement defaultPlacement(
            ClubhouseModuleCatalog.ModuleDef def, boolean enabled) {
        return new ClubhouseBoardLayout.ModulePlacement(
                def.key(), enabled, 0, 0, def.defaultWidth(), def.defaultHeight());
    }

    private static List<Map<String, Object>> toRows(List<ClubhouseBoardLayout.ModulePlacement> placements) {
        List<Map<String, Object>> rows = new ArrayList<>(placements.size());
        for (ClubhouseBoardLayout.ModulePlacement placement : placements) {
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

    private static int asInt(Object raw, String message) {
        if (raw instanceof Number number) {
            double value = number.doubleValue();
            if (value == Math.rint(value) && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                return (int) value;
            }
        }
        throw new BusinessException(400, message);
    }

    private boolean enabled(Object raw) {
        if (raw == null) {
            return true;
        }
        if (raw instanceof Boolean value) {
            return value;
        }
        throw new BusinessException(400, "模块 enabled 必须为布尔值");
    }

    private List<String> normalizeMilestones(Object raw) {
        Map<String, Object> milestones = asMap(raw);
        Object rawKeys = milestones.get("metricKeys");
        if (rawKeys == null) {
            return ClubhouseMilestoneCatalog.defaultKeys();
        }
        if (!(rawKeys instanceof List<?> list)) {
            throw new BusinessException(400, "里程碑指标必须为数组");
        }
        if (list.isEmpty()) {
            return ClubhouseMilestoneCatalog.defaultKeys();
        }
        if (list.size() > ClubhouseMilestoneCatalog.MAX_SELECTED) {
            throw new BusinessException(400,
                    "生涯里程碑最多选择 " + ClubhouseMilestoneCatalog.MAX_SELECTED + " 项");
        }
        List<String> out = new ArrayList<>(list.size());
        Set<String> seen = new HashSet<>();
        for (Object item : list) {
            ClubhouseMilestoneCatalog.MetricDef def = ClubhouseMilestoneCatalog.find(asText(item));
            if (def == null) {
                throw new BusinessException(400, "不支持的里程碑指标");
            }
            if (!seen.add(def.key())) {
                throw new BusinessException(400, "里程碑指标不能重复");
            }
            out.add(def.key());
        }
        return out;
    }

    private List<Map<String, Object>> normalizeRanks(Object raw) {
        List<?> list = raw instanceof List<?> values ? values : List.of();
        if (list.size() != RANK_SLOT_COUNT) {
            throw new BusinessException(400, "队内排名必须选择 4 个指标");
        }
        List<Map<String, Object>> out = new ArrayList<>(RANK_SLOT_COUNT);
        Set<String> seen = new HashSet<>();
        for (Object item : list) {
            String key = asText(asMap(item).get("metricKey"));
            if (!ClubhouseMetricCatalog.isRankable(key)) {
                throw new BusinessException(400, "队内排名不支持该指标");
            }
            if (!seen.add(key)) {
                throw new BusinessException(400, "队内排名指标不能重复");
            }
            LinkedHashMap<String, Object> rank = new LinkedHashMap<>();
            rank.put("metricKey", key);
            out.add(rank);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object raw) {
        return raw instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static String asText(Object raw) {
        return raw == null ? null : String.valueOf(raw).trim();
    }
}
