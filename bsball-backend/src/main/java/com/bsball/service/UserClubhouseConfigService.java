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
 * {@link ClubhouseModuleCatalog} 中的模块；模块顺序即展示顺序，尺寸由白名单固定。</p>
 */
@Service
@RequiredArgsConstructor
public class UserClubhouseConfigService {
    private static final int RANK_SLOT_COUNT = 4;

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
        List<Map<String, Object>> modules = normalizeModules(source.get("modules"));
        List<String> milestoneKeys = normalizeMilestones(source.get("milestones"));
        LinkedHashMap<String, Object> config = new LinkedHashMap<>();
        config.put("version", 2);
        config.put("layout", "v3-modular");
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

    private List<Map<String, Object>> normalizeModules(Object raw) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return ClubhouseModuleCatalog.defaultModules();
        }
        List<Map<String, Object>> out = new ArrayList<>();
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
            out.add(moduleEntry(def, enabled(row.get("enabled"))));
        }
        for (ClubhouseModuleCatalog.ModuleDef def : ClubhouseModuleCatalog.all()) {
            if (!seen.contains(def.key())) {
                out.add(moduleEntry(def, def.enabledByDefault()));
            }
        }
        return out;
    }

    private static Map<String, Object> moduleEntry(ClubhouseModuleCatalog.ModuleDef def, boolean enabled) {
        LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", def.key());
        entry.put("enabled", enabled);
        return entry;
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
