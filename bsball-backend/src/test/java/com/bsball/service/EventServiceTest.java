/*
 * 赛事详情附带联赛特殊规则（Issue #275，设计稿 §3.3）：EventService.get 装配 specialRules。
 *
 * 契约：GET /event/{id} 响应新增 specialRules:{extraBatter,forceHalf}；
 *  - leagueId 有值 → 取 LeagueSpecialRuleService.getEffective 生效值随响应返回；
 *  - leagueId 为空（未挂联赛）→ 服务直接返回默认值（服务内 null 早退、零查库，
 *    由 LeagueSpecialRuleServiceTest 覆盖），EventService 无条件透传，不重复默认值字面量；
 *  - 租户不匹配 / 已软删 → null；受限身份可见域外（含管理上下文未挂联赛）→ 403；
 *    上述收口路径均不咨询规则服务；
 *  - 装配仅发生在 get 详情路径：list/create/update 不装配（字段保持 null，序列化时省略）。
 *
 * 含 Jackson 序列化契约断言：以 com.fasterxml ObjectMapper 直验（HTTP 响应实际走
 * Boot 4 默认 Jackson 3 JsonMapper，两者共用 com.fasterxml.jackson.annotation 注解包与
 * 默认 getter 序列化语义，本字段契约一致）。
 *
 * 风格：外部依赖（含 LeagueSpecialRuleService）一律 Mockito mock，不启动 Spring、不连库；
 * EventService 手工 new（@Generated 构造器）。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.entity.Event;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.LeagueRepository;
import com.bsball.service.query.ScopeQuerySupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService.get：赛事详情附带联赛特殊规则生效值（Issue #275）")
class EventServiceTest {

    private static final long TENANT_ID = 10L;
    private static final long EVENT_ID = 5L;
    private static final long LEAGUE_ID = 1L;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private AccountScopeService accountScopeService;

    @Mock
    private ScopeQuerySupport scopeQuerySupport;

    @Mock
    private ResourceGuard resourceGuard;

    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private LeagueSpecialRuleService leagueSpecialRuleService;

    private EventService service;

    @BeforeEach
    void setUp() {
        service = new EventService(eventRepository, leagueRepository, accountScopeService, scopeQuerySupport,
                resourceGuard, tenantQueryPolicyService, gameRepository, leagueSpecialRuleService);
    }

    @Test
    @DisplayName("leagueId 有值：按 leagueId 取生效值装配 specialRules")
    void get_leagueEvent_attachesEffectiveRules() {
        stubWideRead();
        stubEvent(LEAGUE_ID, null);
        Map<String, Boolean> effective = rules(true, false);
        when(leagueSpecialRuleService.getEffective(LEAGUE_ID)).thenReturn(effective);

        Event result = service.get(EVENT_ID);

        assertEquals(effective, result.getSpecialRules());
        verify(leagueSpecialRuleService).getEffective(LEAGUE_ID);
        // 读取路径不做联赛存在性校验：不查联赛仓
        verifyNoInteractions(leagueRepository);
    }

    @Test
    @DisplayName("leagueId 为空（未挂联赛）：装配服务默认值 extraBatter=false、forceHalf=true")
    void get_eventWithoutLeague_attachesDefaults() {
        stubWideRead();
        stubEvent(null, null);
        when(leagueSpecialRuleService.getEffective(null)).thenReturn(rules(false, true));

        Event result = service.get(EVENT_ID);

        assertEquals(Boolean.FALSE, result.getSpecialRules().get("extraBatter"));
        assertEquals(Boolean.TRUE, result.getSpecialRules().get("forceHalf"));
        // 无条件透传 null 给服务（默认值语义集中在服务内，此处不查库、不重复字面量）
        verify(leagueSpecialRuleService).getEffective(null);
        verifyNoInteractions(leagueRepository);
    }

    @Test
    @DisplayName("回归：跨租户 → null（既有行为），不咨询规则服务")
    void get_crossTenant_returnsNullWithoutRuleLookup() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID + 1);
        Event entity = new Event();
        entity.setId(EVENT_ID);
        entity.setTenantId(TENANT_ID);
        entity.setLeagueId(LEAGUE_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(entity));

        assertNull(service.get(EVENT_ID));

        verifyNoInteractions(leagueSpecialRuleService);
    }

    @Test
    @DisplayName("回归：已软删 → null（既有行为），不咨询规则服务")
    void get_softDeleted_returnsNullWithoutRuleLookup() {
        stubEvent(LEAGUE_ID, LocalDateTime.now());

        assertNull(service.get(EVENT_ID));

        verifyNoInteractions(leagueSpecialRuleService);
    }

    @Test
    @DisplayName("回归：受限身份可见域外 → 403（既有行为），不咨询规则服务")
    void get_restrictedScope_outOfVisibleLeague_forbiddenWithoutRuleLookup() {
        stubRestrictedRead(List.of(10L));
        stubEvent(LEAGUE_ID, null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.get(EVENT_ID));

        assertEquals(403, ex.getCode());
        verifyNoInteractions(leagueSpecialRuleService);
    }

    @Test
    @DisplayName("回归：管理上下文且未挂联赛 → 403（批次 3b 收窄口径），不咨询规则服务")
    void get_manageContext_nullLeague_forbiddenWithoutRuleLookup() {
        stubRestrictedRead(List.of(10L));
        stubEvent(null, null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.get(EVENT_ID));

        assertEquals(403, ex.getCode());
        verifyNoInteractions(leagueSpecialRuleService);
    }

    @Test
    @DisplayName("回归：受限身份可见域内 → 正常返回并装配规则")
    void get_restrictedScope_inVisibleLeague_okWithRules() {
        stubRestrictedRead(List.of(LEAGUE_ID));
        stubEvent(LEAGUE_ID, null);
        when(leagueSpecialRuleService.getEffective(LEAGUE_ID)).thenReturn(rules(false, true));

        Event result = service.get(EVENT_ID);

        assertEquals(rules(false, true), result.getSpecialRules());
        verify(leagueSpecialRuleService).getEffective(LEAGUE_ID);
    }

    @Test
    @DisplayName("序列化：specialRules 非空时随 JSON 输出（详情响应契约）")
    void json_specialRulesSet_isSerialized() throws Exception {
        Event event = new Event();
        event.setSpecialRules(rules(false, true));

        String json = new ObjectMapper().writeValueAsString(event);

        assertTrue(json.contains("\"specialRules\":{\"extraBatter\":false,\"forceHalf\":true}"));
    }

    @Test
    @DisplayName("序列化：specialRules 为 null 时从 JSON 省略（list/create/update 等其他路径响应不变）")
    void json_specialRulesNull_isOmitted() throws Exception {
        String json = new ObjectMapper().writeValueAsString(new Event());

        assertFalse(json.contains("specialRules"));
    }

    /** 宽读（非管理上下文 / 不受限）：visibleLeagueIds=null，不做可见域收窄。 */
    private void stubWideRead() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(scopeQuerySupport.visibleLeagueIds(any())).thenReturn(null);
    }

    /** 管理上下文 + 受限身份：可见联赛固定为给定集合。 */
    private void stubRestrictedRead(List<Long> visibleLeagueIds) {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.copyOf(visibleLeagueIds), Set.of()));
        when(scopeQuerySupport.visibleLeagueIds(any())).thenReturn(visibleLeagueIds);
    }

    private void stubEvent(Long leagueId, LocalDateTime deletedAt) {
        Event entity = new Event();
        entity.setId(EVENT_ID);
        entity.setTenantId(TENANT_ID);
        entity.setLeagueId(leagueId);
        entity.setDeletedAt(deletedAt);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(entity));
    }

    private static Map<String, Boolean> rules(boolean extraBatter, boolean forceHalf) {
        Map<String, Boolean> rules = new LinkedHashMap<>();
        rules.put("extraBatter", extraBatter);
        rules.put("forceHalf", forceHalf);
        return rules;
    }
}
