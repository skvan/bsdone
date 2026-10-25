package com.bsball.service;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.EventSpecialRule;
import com.bsball.repository.EventSpecialRuleRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 赛事级特殊规则开关服务（Issue #275）：extra_batter（EP 制）/ force_half（强制换局）的读取与写入。
 *
 * <p>语义：无记录 = 默认值（extraBatter=false、forceHalf=true）；有记录 = 以记录为准。
 * 是否允许配置由 EventService 按「联赛收费能力或赛事既有规则」门禁后调用本服务。</p>
 */
@Service
public class EventSpecialRuleService {

    public static final String RULE_EXTRA_BATTER = "extra_batter";
    public static final String RULE_FORCE_HALF = "force_half";

    private static final String API_EXTRA_BATTER = "extraBatter";
    private static final String API_FORCE_HALF = "forceHalf";

    private static final Map<String, String> API_TO_DB_KEY = Map.of(
            API_EXTRA_BATTER, RULE_EXTRA_BATTER,
            API_FORCE_HALF, RULE_FORCE_HALF);

    private static final Map<String, Boolean> DEFAULTS = Map.of(
            RULE_EXTRA_BATTER, Boolean.FALSE,
            RULE_FORCE_HALF, Boolean.TRUE);

    private final EventSpecialRuleRepository eventSpecialRuleRepository;

    public EventSpecialRuleService(EventSpecialRuleRepository eventSpecialRuleRepository) {
        this.eventSpecialRuleRepository = eventSpecialRuleRepository;
    }

    /**
     * 读取赛事规则生效值（含默认兜底）。
     *
     * @param eventId 赛事ID；可为 null（直接返回默认值）
     * @return camelCase 键的生效值：{extraBatter, forceHalf}
     */
    public Map<String, Boolean> getEffective(Long eventId) {
        if (eventId == null) {
            return defaults();
        }
        Map<String, Boolean> effective = defaults();
        for (EventSpecialRule row : this.eventSpecialRuleRepository.findByEventId(eventId)) {
            if (row.getEnabled() == null) {
                continue;
            }
            if (RULE_EXTRA_BATTER.equals(row.getRuleKey())) {
                effective.put(API_EXTRA_BATTER, row.getEnabled());
            } else if (RULE_FORCE_HALF.equals(row.getRuleKey())) {
                effective.put(API_FORCE_HALF, row.getEnabled());
            }
        }
        return effective;
    }

    /** 赛事是否已有显式规则行（用于「能力关闭后已配置赛事保留」判定）。 */
    public boolean hasRules(Long eventId) {
        return eventId != null && this.eventSpecialRuleRepository.existsByEventId(eventId);
    }

    /**
     * 更新赛事规则（camelCase 子集 / 全集），返回更新后的生效值。
     * 权限与联赛能力门禁由 EventService 的赛事创建/更新入口负责。
     *
     * @param eventId  赛事ID
     * @param tenantId 租户ID
     * @param rules    camelCase 规则映射（如 {extraBatter: true}）
     * @return 更新后的生效值
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Boolean> updateRules(Long eventId, Long tenantId, Map<String, Boolean> rules) {
        Map<String, Boolean> validated = validateRules(rules);
        Long operatorId = CurrentUserHolder.get();
        for (Map.Entry<String, Boolean> entry : validated.entrySet()) {
            upsertRow(eventId, tenantId, entry.getKey(), entry.getValue(), operatorId);
        }
        return getEffective(eventId);
    }

    private Map<String, Boolean> validateRules(Map<String, Boolean> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new BusinessException(400, "规则不能为空");
        }
        Map<String, Boolean> validated = new LinkedHashMap<>();
        for (Map.Entry<String, Boolean> entry : rules.entrySet()) {
            String apiKey = entry.getKey();
            if (apiKey == null || !API_TO_DB_KEY.containsKey(apiKey)) {
                throw new BusinessException(400, "未知的规则键：" + apiKey);
            }
            if (entry.getValue() == null) {
                throw new BusinessException(400, "规则值必须为布尔：" + apiKey);
            }
            validated.put(apiKey, entry.getValue());
        }
        return validated;
    }

    /** 按行 upsert：行存在则原地更新（保留 createdBy），不存在则新增；规则行只写不删。 */
    private void upsertRow(Long eventId, Long tenantId, String apiKey, Boolean enabled, Long operatorId) {
        String dbKey = API_TO_DB_KEY.get(apiKey);
        EventSpecialRule row = this.eventSpecialRuleRepository
                .findByEventIdAndRuleKey(eventId, dbKey).orElse(null);
        if (row == null) {
            row = new EventSpecialRule();
            row.setTenantId(tenantId);
            row.setEventId(eventId);
            row.setRuleKey(dbKey);
            row.setCreatedBy(operatorId);
        }
        row.setEnabled(enabled);
        row.setUpdatedBy(operatorId);
        this.eventSpecialRuleRepository.save(row);
    }

    private static Map<String, Boolean> defaults() {
        Map<String, Boolean> defaults = new LinkedHashMap<>();
        defaults.put(API_EXTRA_BATTER, DEFAULTS.get(RULE_EXTRA_BATTER));
        defaults.put(API_FORCE_HALF, DEFAULTS.get(RULE_FORCE_HALF));
        return defaults;
    }
}
