/*
 * 联赛级特殊规则开关（Issue #275）：LeagueSpecialRuleService 单元测试（Mockito 手工装配）。
 *
 * 覆盖：默认值兜底（无记录 / 部分键）、写入并回读生效值（upsert 新增 + 更新既有行）、
 *      非法键名与非法值拒绝（零写入）、租户 / 身份守卫（租管本租户放行并委托规范守卫、
 *      跨租户 403 零写入、超管跨租户放行、null 租户 fail-closed、规范守卫拒绝受限身份零写入）、
 *      联赛不存在 404。
 * 纯单元测试：不启动 Spring 容器、不连接数据库。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.League;
import com.bsball.model.entity.LeagueSpecialRule;
import com.bsball.repository.LeagueRepository;
import com.bsball.repository.LeagueSpecialRuleRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeagueSpecialRuleService：联赛特殊规则读取 / 写入 / 租户守卫（#275）")
class LeagueSpecialRuleServiceTest {

    private static final long LEAGUE_ID = 1L;
    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 9L;

    @Mock
    private LeagueSpecialRuleRepository leagueSpecialRuleRepository;

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private ResourceGuard resourceGuard;

    private LeagueSpecialRuleService service;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        CurrentUserHolder.set(USER_ID, TENANT_ID);
        service = new LeagueSpecialRuleService(leagueSpecialRuleRepository, leagueRepository, resourceGuard);
    }

    @AfterEach
    void tearDown() {
        // CurrentUserHolder 基于 ThreadLocal，测试后必须清理，避免污染其他用例
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("无记录时返回默认值：extraBatter=false、forceHalf=true")
    void getEffective_noRows_returnsDefaults() {
        when(leagueSpecialRuleRepository.findByLeagueId(LEAGUE_ID)).thenReturn(List.of());

        Map<String, Boolean> effective = service.getEffective(LEAGUE_ID);

        assertEquals(2, effective.size());
        assertEquals(Boolean.FALSE, effective.get("extraBatter"));
        assertEquals(Boolean.TRUE, effective.get("forceHalf"));
        // 读取路径不做联赛存在性 / 权限判定（默认兜底，仅供赛事详情与只读消费）
        verifyNoInteractions(leagueRepository, resourceGuard);
    }

    @Test
    @DisplayName("仅有 extra_batter 行时 force_half 回默认开启")
    void getEffective_partialRows_fallbackToDefaults() {
        when(leagueSpecialRuleRepository.findByLeagueId(LEAGUE_ID)).thenReturn(List.of(
                rule(LEAGUE_ID, LeagueSpecialRuleService.RULE_EXTRA_BATTER, true),
                // DB 中未知 rule_key 应被忽略
                rule(LEAGUE_ID, "future_unknown_key", true)));

        Map<String, Boolean> effective = service.getEffective(LEAGUE_ID);

        assertEquals(2, effective.size());
        assertEquals(Boolean.TRUE, effective.get("extraBatter"));
        assertEquals(Boolean.TRUE, effective.get("forceHalf"));
    }

    @Test
    @DisplayName("updateRules 写入并回读生效值（upsert 更新既有行）")
    void updateRules_upsertAndReturnEffective() {
        when(resourceGuard.isCurrentUserSuperAdmin()).thenReturn(false);
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(league(LEAGUE_ID, TENANT_ID)));

        // force_half 既有行：本轮应被原地更新（createdBy 保留、updatedBy 刷新）
        LeagueSpecialRule existing = rule(LEAGUE_ID, LeagueSpecialRuleService.RULE_FORCE_HALF, true);
        existing.setId(5L);
        existing.setCreatedBy(88L);
        when(leagueSpecialRuleRepository.findByLeagueIdAndRuleKey(LEAGUE_ID, LeagueSpecialRuleService.RULE_EXTRA_BATTER))
                .thenReturn(Optional.empty());
        when(leagueSpecialRuleRepository.findByLeagueIdAndRuleKey(LEAGUE_ID, LeagueSpecialRuleService.RULE_FORCE_HALF))
                .thenReturn(Optional.of(existing));

        List<LeagueSpecialRule> savedRows = new ArrayList<>();
        when(leagueSpecialRuleRepository.save(any(LeagueSpecialRule.class))).thenAnswer(inv -> {
            LeagueSpecialRule row = inv.getArgument(0);
            savedRows.add(row);
            return row;
        });
        // 模拟同事务回读（提交后行与写入一致）
        when(leagueSpecialRuleRepository.findByLeagueId(LEAGUE_ID)).thenReturn(savedRows);

        Map<String, Boolean> effective = service.updateRules(LEAGUE_ID, Map.of("extraBatter", true, "forceHalf", false));

        assertEquals(2, effective.size());
        assertEquals(Boolean.TRUE, effective.get("extraBatter"));
        assertEquals(Boolean.FALSE, effective.get("forceHalf"));
        assertEquals(2, savedRows.size());

        LeagueSpecialRule inserted = savedRows.stream()
                .filter(r -> LeagueSpecialRuleService.RULE_EXTRA_BATTER.equals(r.getRuleKey()))
                .findFirst().orElseThrow();
        assertEquals(LEAGUE_ID, inserted.getLeagueId());
        assertEquals(TENANT_ID, inserted.getTenantId());
        assertEquals(Boolean.TRUE, inserted.getEnabled());
        assertEquals(USER_ID, inserted.getCreatedBy());
        assertEquals(USER_ID, inserted.getUpdatedBy());

        LeagueSpecialRule updated = savedRows.stream()
                .filter(r -> LeagueSpecialRuleService.RULE_FORCE_HALF.equals(r.getRuleKey()))
                .findFirst().orElseThrow();
        assertEquals(5L, updated.getId());
        assertEquals(Boolean.FALSE, updated.getEnabled());
        assertEquals(88L, updated.getCreatedBy());
        assertEquals(USER_ID, updated.getUpdatedBy());

        // 租管（本租户）放行路径：委托仓库规范守卫（与 LeagueService 对同一资源的写守卫一致）
        verify(resourceGuard).assertCanManageLeague(LEAGUE_ID);
    }

    @Test
    @DisplayName("非法规则键被拒绝")
    void updateRules_invalidKeyOrValue_rejectedWithoutWrites() {
        // 未知键名
        BusinessException unknownKey = assertThrows(BusinessException.class,
                () -> service.updateRules(LEAGUE_ID, Map.of("unknownKey", true)));
        assertEquals(400, unknownKey.getCode());

        // 已知键但值为 null（非布尔）
        Map<String, Boolean> nullValue = new HashMap<>();
        nullValue.put("extraBatter", null);
        BusinessException nullValued = assertThrows(BusinessException.class,
                () -> service.updateRules(LEAGUE_ID, nullValue));
        assertEquals(400, nullValued.getCode());

        // 空映射
        BusinessException empty = assertThrows(BusinessException.class,
                () -> service.updateRules(LEAGUE_ID, Map.of()));
        assertEquals(400, empty.getCode());

        // 校验先于任何读写：不应触碰仓储与守卫
        verifyNoInteractions(leagueSpecialRuleRepository, leagueRepository, resourceGuard);
    }

    @Test
    @DisplayName("租管操作非本租户联赛被拒（403），超管放行")
    void updateRules_crossTenant_tenantAdminForbiddenSuperAdminAllowed() {
        // 当前上下文租户=1，目标联赛租户=2
        when(leagueRepository.findById(7L)).thenReturn(Optional.of(league(7L, 2L)));
        when(resourceGuard.isCurrentUserSuperAdmin()).thenReturn(false);

        BusinessException forbidden = assertThrows(BusinessException.class,
                () -> service.updateRules(7L, Map.of("forceHalf", false)));
        assertEquals(403, forbidden.getCode());
        verifyNoInteractions(leagueSpecialRuleRepository);

        // 超管放行：同一次调用（同为跨租户）可写入
        when(resourceGuard.isCurrentUserSuperAdmin()).thenReturn(true);
        when(leagueSpecialRuleRepository.findByLeagueIdAndRuleKey(7L, LeagueSpecialRuleService.RULE_FORCE_HALF))
                .thenReturn(Optional.empty());
        List<LeagueSpecialRule> savedRows = new ArrayList<>();
        when(leagueSpecialRuleRepository.save(any(LeagueSpecialRule.class))).thenAnswer(inv -> {
            LeagueSpecialRule row = inv.getArgument(0);
            savedRows.add(row);
            return row;
        });
        when(leagueSpecialRuleRepository.findByLeagueId(7L))
                .thenReturn(List.of(rule(7L, LeagueSpecialRuleService.RULE_FORCE_HALF, false)));

        Map<String, Boolean> effective = service.updateRules(7L, Map.of("forceHalf", false));

        assertEquals(Boolean.FALSE, effective.get("forceHalf"));
        assertEquals(Boolean.FALSE, effective.get("extraBatter"));
        // 子集更新只写目标行；落库租户取「联赛所属租户」（2），而非调用者上下文租户（1）
        assertEquals(1, savedRows.size());
        assertEquals(7L, savedRows.get(0).getLeagueId());
        assertEquals(2L, savedRows.get(0).getTenantId());
        assertEquals(USER_ID, savedRows.get(0).getCreatedBy());
        assertEquals(USER_ID, savedRows.get(0).getUpdatedBy());

        // 超管（跨租户）放行路径同样委托仓库规范守卫
        verify(resourceGuard).assertCanManageLeague(7L);
    }

    @Test
    @DisplayName("无租户上下文（null）被拒（fail-closed），不咨询守卫且零写入")
    void updateRules_nullTenantContext_failClosedWithoutWrites() {
        // 模拟调用方无租户上下文（上下文异常 / 未解析）：不允许静默放行
        CurrentUserHolder.set(USER_ID, null);
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(league(LEAGUE_ID, TENANT_ID)));

        BusinessException forbidden = assertThrows(BusinessException.class,
                () -> service.updateRules(LEAGUE_ID, Map.of("extraBatter", true)));

        assertEquals(403, forbidden.getCode());
        // fail-closed 先于身份 / 守卫判定：不咨询资源守卫、不落库
        verifyNoInteractions(leagueSpecialRuleRepository, resourceGuard);
    }

    @Test
    @DisplayName("规范守卫拒绝同租户受限身份 → 403 且零写入（守卫先于任何落库）")
    void updateRules_guardRejectsRestrictedIdentity_forbiddenWithoutWrites() {
        when(resourceGuard.isCurrentUserSuperAdmin()).thenReturn(false);
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(league(LEAGUE_ID, TENANT_ID)));
        // 同租户但受限身份：仓库规范守卫（自有联盟集合未命中）拒绝
        doThrow(new BusinessException(403, "无权管理该联盟"))
                .when(resourceGuard).assertCanManageLeague(LEAGUE_ID);

        BusinessException forbidden = assertThrows(BusinessException.class,
                () -> service.updateRules(LEAGUE_ID, Map.of("extraBatter", true)));

        assertEquals(403, forbidden.getCode());
        // 守卫在任何落库之前执行：拒绝后规则仓储零写入
        verifyNoInteractions(leagueSpecialRuleRepository);
    }

    @Test
    @DisplayName("联赛不存在返回 404 语义")
    void updateRules_leagueNotFound_returns404() {
        when(leagueRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException notFound = assertThrows(BusinessException.class,
                () -> service.updateRules(99L, Map.of("extraBatter", true)));
        assertEquals(404, notFound.getCode());

        // 已软删联赛同样按不存在处理
        League deleted = league(88L, TENANT_ID);
        deleted.setDeletedAt(LocalDateTime.now());
        when(leagueRepository.findById(88L)).thenReturn(Optional.of(deleted));

        BusinessException deletedNotFound = assertThrows(BusinessException.class,
                () -> service.updateRules(88L, Map.of("extraBatter", true)));
        assertEquals(404, deletedNotFound.getCode());

        verifyNoInteractions(leagueSpecialRuleRepository, resourceGuard);
    }

    private static League league(long leagueId, long tenantId) {
        League league = new League();
        league.setId(leagueId);
        league.setTenantId(tenantId);
        return league;
    }

    private static LeagueSpecialRule rule(long leagueId, String ruleKey, boolean enabled) {
        LeagueSpecialRule row = new LeagueSpecialRule();
        row.setLeagueId(leagueId);
        row.setRuleKey(ruleKey);
        row.setEnabled(enabled);
        return row;
    }
}
