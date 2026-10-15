/*
 * 联赛级特殊规则开关服务（Issue #275）：extra_batter（EP 制）/ force_half（强制换局）的读取与写入。
 *
 * 语义铁律（设计稿 §2.1 / §3.1）：
 *  - 无记录 = 默认值（extra_batter=false、force_half=true）；有记录 = 以记录为准（支持显式关闭）；
 *  - DB 中未知 rule_key 读取时忽略；
 *  - 规则键与 API（camelCase）键的映射、默认值集中在服务内，避免散落；
 *  - 写路径：键名 / 值校验（未知键、非布尔值、空映射 → 400）→ 联赛存在性（不存在 / 已软删 → 404）
 *    → 租户 / 身份守卫（null 租户 fail-closed；超管可跨租户；其余限本租户，越界 403；
 *      再经 assertCanManageLeague 拦截「同租户内受限身份」，对齐 LeagueService 同一资源的写守卫）
 *    → 按行 upsert（审计：createdBy / updatedBy = 当前用户）；
 *  - 规则行只写不删（无软删语义）。
 *
 * 消费方：T3 LeagueApi 的两接口、T4 EventService 赛事详情附带生效值。
 */
package com.bsball.service;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.League;
import com.bsball.model.entity.LeagueSpecialRule;
import com.bsball.repository.LeagueRepository;
import com.bsball.repository.LeagueSpecialRuleRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeagueSpecialRuleService {

    /** 规则键（DB 落库值）：额外打击者 / EP 制开关。 */
    public static final String RULE_EXTRA_BATTER = "extra_batter";
    /** 规则键（DB 落库值）：强制换局开关。 */
    public static final String RULE_FORCE_HALF = "force_half";

    /** API 键（camelCase，对外）：额外打击者 / EP 制开关。 */
    private static final String API_EXTRA_BATTER = "extraBatter";
    /** API 键（camelCase，对外）：强制换局开关。 */
    private static final String API_FORCE_HALF = "forceHalf";

    /** API 键（camelCase，对外）→ DB 规则键（snake_case）映射。 */
    private static final Map<String, String> API_TO_DB_KEY = Map.of(
            API_EXTRA_BATTER, RULE_EXTRA_BATTER,
            API_FORCE_HALF, RULE_FORCE_HALF);

    /** 无记录时的默认生效值（设计稿裁决 6：extra_batter 关闭、force_half 开启）。 */
    private static final Map<String, Boolean> DEFAULTS = Map.of(
            RULE_EXTRA_BATTER, Boolean.FALSE,
            RULE_FORCE_HALF, Boolean.TRUE);

    private final LeagueSpecialRuleRepository leagueSpecialRuleRepository;
    private final LeagueRepository leagueRepository;
    private final ResourceGuard resourceGuard;

    /**
     * 读取联赛特殊规则生效值（含默认兜底，供赛事详情等只读消费）。
     * 纯读取：不做联赛存在性 / 权限判定（访问控制由调用入口负责——T4 在 EventService.get
     * 通过租户 + 可见域校验后附带本值；写守卫 assertCanManageLeague 属写语义，不用于读路径）；
     * 无记录或未知 rule_key 均按默认值兜底。
     *
     * @param leagueId 联赛ID
     * @return camelCase 键的生效值：{extraBatter, forceHalf}
     */
    public Map<String, Boolean> getEffective(Long leagueId) {
        Map<String, Boolean> effective = new LinkedHashMap<>();
        effective.put(API_EXTRA_BATTER, DEFAULTS.get(RULE_EXTRA_BATTER));
        effective.put(API_FORCE_HALF, DEFAULTS.get(RULE_FORCE_HALF));
        for (LeagueSpecialRule row : this.leagueSpecialRuleRepository.findByLeagueId(leagueId)) {
            if (row.getEnabled() == null) {
                continue;
            }
            if (RULE_EXTRA_BATTER.equals(row.getRuleKey())) {
                effective.put(API_EXTRA_BATTER, row.getEnabled());
            } else if (RULE_FORCE_HALF.equals(row.getRuleKey())) {
                effective.put(API_FORCE_HALF, row.getEnabled());
            }
            // 未知 rule_key：忽略（保持默认值）
        }
        return effective;
    }

    /**
     * 更新联赛特殊规则（camelCase 子集 / 全集），返回更新后的生效值。
     * 校验顺序：键名 / 值校验（400）→ 联赛存在性（404）→ 租户 / 身份守卫（403）→ 按行 upsert。
     *
     * @param leagueId 联赛ID
     * @param rules    camelCase 规则映射（如 {extraBatter: true}），支持子集更新
     * @return 更新后的生效值：{extraBatter, forceHalf}
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Boolean> updateRules(Long leagueId, Map<String, Boolean> rules) {
        Map<String, Boolean> validated = validateRules(rules);

        League league = this.leagueRepository.findById(leagueId).orElse(null);
        if (league == null || league.getDeletedAt() != null) {
            throw new BusinessException(404, "联赛不存在");
        }
        assertTenantAccess(league);

        Long operatorId = CurrentUserHolder.get();
        for (Map.Entry<String, Boolean> entry : validated.entrySet()) {
            upsertRow(leagueId, league.getTenantId(), entry.getKey(), entry.getValue(), operatorId);
        }
        return getEffective(leagueId);
    }

    /** 键名白名单与值校验：未知键 / null 值 / 空映射一律 400，且先于任何读写。 */
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

    /**
     * 租户 / 身份守卫（评审修复：对齐仓库规范，Issue #275）：
     *  1. 无租户上下文（null）→ 403（fail-closed，防止上下文缺失被静默放行）；
     *  2. 非超管须操作本租户联赛（跨租户 403）；超管可跨租户（设计 §3.1）；
     *  3. 再经仓库规范写守卫 {@link ResourceGuard#assertCanManageLeague(Long)} 拦截「同租户内受限身份」
     *     （与 LeagueService 对同一资源的写守卫口径一致；租户相等校验作为第一道防线保留）。
     */
    private void assertTenantAccess(League league) {
        Long currentTenantId = CurrentUserHolder.getTenantId();
        if (currentTenantId == null) {
            throw new BusinessException(403, "无权修改该联赛的特殊规则");
        }
        if (!this.resourceGuard.isCurrentUserSuperAdmin() && !currentTenantId.equals(league.getTenantId())) {
            throw new BusinessException(403, "无权修改该联赛的特殊规则");
        }
        this.resourceGuard.assertCanManageLeague(league.getId());
    }

    /** 按行 upsert：行存在则原地更新（保留 createdBy），不存在则新增；规则行只写不删。 */
    private void upsertRow(Long leagueId, Long tenantId, String apiKey, Boolean enabled, Long operatorId) {
        String dbKey = API_TO_DB_KEY.get(apiKey);
        LeagueSpecialRule row = this.leagueSpecialRuleRepository
                .findByLeagueIdAndRuleKey(leagueId, dbKey).orElse(null);
        if (row == null) {
            row = new LeagueSpecialRule();
            row.setTenantId(tenantId);
            row.setLeagueId(leagueId);
            row.setRuleKey(dbKey);
            row.setCreatedBy(operatorId);
        }
        row.setEnabled(enabled);
        row.setUpdatedBy(operatorId);
        this.leagueSpecialRuleRepository.save(row);
    }

    public LeagueSpecialRuleService(LeagueSpecialRuleRepository leagueSpecialRuleRepository,
            LeagueRepository leagueRepository, ResourceGuard resourceGuard) {
        this.leagueSpecialRuleRepository = leagueSpecialRuleRepository;
        this.leagueRepository = leagueRepository;
        this.resourceGuard = resourceGuard;
    }
}
