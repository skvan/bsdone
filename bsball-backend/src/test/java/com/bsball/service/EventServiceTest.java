/*
 * 赛事级特殊规则 + 联赛收费能力（Issue #275，2026-10-24 口径调整）。
 *
 * 契约：GET /event/{id} 的 specialRules 返回
 *   {specialRulesEnabled, hasEventRules, extraBatter, forceHalf}；
 *   - 联赛能力或赛事既有规则决定能否配置；已配置赛事在能力关闭后仍用自身规则；
 *   - 未配置且能力关闭 → 默认 extraBatter=false、forceHalf=true；
 *   - create/update 的 specialRules 由 EventService 在同一事务写库；
 *     create 必须联赛能力开启；update 仅在赛事已有规则（grandfather）或联赛能力开启时允许。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；EventService 手工 new。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.League;
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
@DisplayName("EventService：赛事级特殊规则与联赛收费能力（Issue #275）")
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
    @Mock
    private EventSpecialRuleService eventSpecialRuleService;

    private EventService service;

    @BeforeEach
    void setUp() {
        service = new EventService(eventRepository, leagueRepository, accountScopeService, scopeQuerySupport,
                resourceGuard, tenantQueryPolicyService, gameRepository, leagueSpecialRuleService,
                eventSpecialRuleService);
    }

    @Test
    @DisplayName("已配置赛事 + 联赛能力关闭：保留赛事规则并标记 hasEventRules")
    void get_eventWithRules_capabilityOff_keepsRules() {
        stubWideRead();
        stubEvent(LEAGUE_ID, null);
        when(eventSpecialRuleService.hasRules(EVENT_ID)).thenReturn(true);
        when(eventSpecialRuleService.getEffective(EVENT_ID)).thenReturn(rules(true, false));
        when(leagueSpecialRuleService.isSpecialRulesEnabled(LEAGUE_ID)).thenReturn(false);

        Event result = service.get(EVENT_ID);

        assertEquals(Boolean.FALSE, result.getSpecialRules().get("specialRulesEnabled"));
        assertEquals(Boolean.TRUE, result.getSpecialRules().get("hasEventRules"));
        assertEquals(Boolean.TRUE, result.getSpecialRules().get("extraBatter"));
        assertEquals(Boolean.FALSE, result.getSpecialRules().get("forceHalf"));
    }

    @Test
    @DisplayName("未配置赛事 + 联赛能力开启：返回默认规则并标记可配置")
    void get_eventWithoutRules_capabilityOn_attachesDefaults() {
        stubWideRead();
        stubEvent(LEAGUE_ID, null);
        when(eventSpecialRuleService.hasRules(EVENT_ID)).thenReturn(false);
        when(leagueSpecialRuleService.isSpecialRulesEnabled(LEAGUE_ID)).thenReturn(true);

        Event result = service.get(EVENT_ID);

        assertEquals(Boolean.TRUE, result.getSpecialRules().get("specialRulesEnabled"));
        assertEquals(Boolean.FALSE, result.getSpecialRules().get("hasEventRules"));
        assertEquals(Boolean.FALSE, result.getSpecialRules().get("extraBatter"));
        assertEquals(Boolean.TRUE, result.getSpecialRules().get("forceHalf"));
        verify(eventSpecialRuleService, never()).getEffective(EVENT_ID);
    }

    @Test
    @DisplayName("未配置赛事 + 联赛能力关闭：默认规则，不可配置")
    void get_eventWithoutRules_capabilityOff_attachesDefaults() {
        stubWideRead();
        stubEvent(null, null);

        Event result = service.get(EVENT_ID);

        assertEquals(Boolean.FALSE, result.getSpecialRules().get("specialRulesEnabled"));
        assertEquals(Boolean.FALSE, result.getSpecialRules().get("hasEventRules"));
        assertEquals(Boolean.FALSE, result.getSpecialRules().get("extraBatter"));
        assertEquals(Boolean.TRUE, result.getSpecialRules().get("forceHalf"));
    }

    @Test
    @DisplayName("回归：跨租户 → null，不咨询规则服务")
    void get_crossTenant_returnsNullWithoutRuleLookup() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID + 1);
        Event entity = new Event();
        entity.setId(EVENT_ID);
        entity.setTenantId(TENANT_ID);
        entity.setLeagueId(LEAGUE_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(entity));

        assertNull(service.get(EVENT_ID));

        verifyNoInteractions(eventSpecialRuleService, leagueSpecialRuleService);
    }

    @Test
    @DisplayName("回归：已软删 → null，不咨询规则服务")
    void get_softDeleted_returnsNullWithoutRuleLookup() {
        stubEvent(LEAGUE_ID, LocalDateTime.now());

        assertNull(service.get(EVENT_ID));

        verifyNoInteractions(eventSpecialRuleService, leagueSpecialRuleService);
    }

    @Test
    @DisplayName("回归：受限身份可见域外 → 403，不咨询规则服务")
    void get_restrictedScope_outOfVisibleLeague_forbiddenWithoutRuleLookup() {
        stubRestrictedRead(List.of(10L));
        stubEvent(LEAGUE_ID, null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.get(EVENT_ID));

        assertEquals(403, ex.getCode());
        verifyNoInteractions(eventSpecialRuleService, leagueSpecialRuleService);
    }

    @Test
    @DisplayName("回归：管理上下文且未挂联赛 → 403，不咨询规则服务")
    void get_manageContext_nullLeague_forbiddenWithoutRuleLookup() {
        stubRestrictedRead(List.of(10L));
        stubEvent(null, null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.get(EVENT_ID));

        assertEquals(403, ex.getCode());
        verifyNoInteractions(eventSpecialRuleService, leagueSpecialRuleService);
    }

    @Test
    @DisplayName("回归：受限身份可见域内 → 正常返回并装配规则")
    void get_restrictedScope_inVisibleLeague_okWithRules() {
        stubRestrictedRead(List.of(LEAGUE_ID));
        stubEvent(LEAGUE_ID, null);
        when(eventSpecialRuleService.hasRules(EVENT_ID)).thenReturn(false);
        when(leagueSpecialRuleService.isSpecialRulesEnabled(LEAGUE_ID)).thenReturn(true);

        Event result = service.get(EVENT_ID);

        assertEquals(Boolean.TRUE, result.getSpecialRules().get("specialRulesEnabled"));
        assertEquals(Boolean.FALSE, result.getSpecialRules().get("hasEventRules"));
    }

    @Test
    @DisplayName("更新赛事携带规则：赛事已有规则时，联赛能力关闭仍允许（保留）")
    void update_withExistingRules_capabilityOff_writesEventRules() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        Event existing = event(null);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existing));
        when(eventSpecialRuleService.hasRules(EVENT_ID)).thenReturn(true);
        when(eventRepository.save(any(Event.class))).thenAnswer((invocation) -> invocation.getArgument(0));

        Event body = event(null);
        body.setSpecialRules(rules(true, false));

        Event saved = service.update(EVENT_ID, body);

        assertEquals(EVENT_ID, saved.getId());
        verify(eventSpecialRuleService).updateRules(EVENT_ID, TENANT_ID, rules(true, false));
        verify(resourceGuard).assertCanManageEvent(EVENT_ID);
    }

    @Test
    @DisplayName("更新赛事携带规则：无既有规则且联赛能力关闭 → 400 且不写规则")
    void update_withoutExistingRules_capabilityOff_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        Event existing = event(null);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existing));
        when(eventSpecialRuleService.hasRules(EVENT_ID)).thenReturn(false);

        Event body = event(null);
        body.setSpecialRules(rules(true, false));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(EVENT_ID, body));

        assertEquals(400, ex.getCode());
        verify(eventSpecialRuleService, never()).updateRules(any(), any(), any());
    }

    @Test
    @DisplayName("更新赛事携带规则：无既有规则但联赛能力开启 → 允许写入")
    void update_withoutExistingRules_capabilityOn_writesEventRules() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        Event existing = event(LEAGUE_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existing));
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(league(LEAGUE_ID)));
        when(eventSpecialRuleService.hasRules(EVENT_ID)).thenReturn(false);
        when(leagueSpecialRuleService.isSpecialRulesEnabled(LEAGUE_ID)).thenReturn(true);
        when(eventRepository.save(any(Event.class))).thenAnswer((invocation) -> invocation.getArgument(0));

        Event body = event(LEAGUE_ID);
        body.setSpecialRules(rules(false, true));

        service.update(EVENT_ID, body);

        verify(eventSpecialRuleService).updateRules(EVENT_ID, TENANT_ID, rules(false, true));
    }

    @Test
    @DisplayName("更新赛事未携带规则：不改动赛事规则")
    void update_withoutSpecialRules_keepsRulesUnchanged() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        Event existing = event(null);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer((invocation) -> invocation.getArgument(0));

        Event body = event(null);

        service.update(EVENT_ID, body);

        verifyNoInteractions(eventSpecialRuleService);
    }

    @Test
    @DisplayName("更新赛事：已有规则迁到能力关闭联赛仍允许，且不查目标联赛能力")
    void update_withExistingRules_movedToCapabilityOffLeague_allowedWithoutCapabilityLookup() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        Event existing = event(LEAGUE_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existing));
        when(leagueRepository.findById(2L)).thenReturn(Optional.of(league(2L)));
        when(eventSpecialRuleService.hasRules(EVENT_ID)).thenReturn(true);
        when(eventRepository.save(any(Event.class))).thenAnswer((invocation) -> invocation.getArgument(0));

        Event body = event(2L);
        body.setSpecialRules(rules(true, false));

        service.update(EVENT_ID, body);

        verify(eventSpecialRuleService).updateRules(EVENT_ID, TENANT_ID, rules(true, false));
        verify(leagueSpecialRuleService, never()).isSpecialRulesEnabled(any());
    }

    @Test
    @DisplayName("更新赛事：无既有规则迁到能力关闭联赛 → 400，按目标联赛判断")
    void update_withoutRules_movedToCapabilityOffLeague_rejectedByTargetLeague() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        Event existing = event(LEAGUE_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existing));
        when(leagueRepository.findById(2L)).thenReturn(Optional.of(league(2L)));
        when(eventSpecialRuleService.hasRules(EVENT_ID)).thenReturn(false);
        when(leagueSpecialRuleService.isSpecialRulesEnabled(2L)).thenReturn(false);

        Event body = event(2L);
        body.setSpecialRules(rules(true, false));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(EVENT_ID, body));

        assertEquals(400, ex.getCode());
        verify(leagueSpecialRuleService).isSpecialRulesEnabled(2L);
        verify(leagueSpecialRuleService, never()).isSpecialRulesEnabled(LEAGUE_ID);
        verify(eventSpecialRuleService, never()).updateRules(any(), any(), any());
    }

    @Test
    @DisplayName("更新赛事：空规则映射视为未携带，零写入")
    void update_withEmptyRules_treatedAsNoRules() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        Event existing = event(null);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer((invocation) -> invocation.getArgument(0));

        Event body = event(null);
        body.setSpecialRules(Map.of());

        service.update(EVENT_ID, body);

        verifyNoInteractions(eventSpecialRuleService);
    }

    @Test
    @DisplayName("创建赛事携带规则：未挂联赛 → 400 且不保存")
    void create_withSpecialRules_withoutLeague_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);

        Event body = event(null);
        body.setSpecialRules(rules(true, false));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(body));

        assertEquals(400, ex.getCode());
        verify(eventRepository, never()).save(any(Event.class));
        verify(eventSpecialRuleService, never()).updateRules(any(), any(), any());
    }

    @Test
    @DisplayName("创建赛事携带规则：联赛能力开启 → 保存后写入")
    void create_withSpecialRules_capabilityOn_writesEventRules() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(league(LEAGUE_ID)));
        when(leagueSpecialRuleService.isSpecialRulesEnabled(LEAGUE_ID)).thenReturn(true);
        when(eventRepository.save(any(Event.class))).thenAnswer((invocation) -> {
            Event saved = invocation.getArgument(0);
            saved.setId(EVENT_ID);
            return saved;
        });

        Event body = event(LEAGUE_ID);
        body.setSpecialRules(rules(false, true));

        Event saved = service.create(body);

        assertEquals(EVENT_ID, saved.getId());
        verify(eventSpecialRuleService).updateRules(EVENT_ID, TENANT_ID, rules(false, true));
    }

    @Test
    @DisplayName("创建赛事携带规则：联赛能力关闭 → 400 且不保存")
    void create_withSpecialRules_capabilityOff_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(league(LEAGUE_ID)));
        when(leagueSpecialRuleService.isSpecialRulesEnabled(LEAGUE_ID)).thenReturn(false);

        Event body = event(LEAGUE_ID);
        body.setSpecialRules(rules(true, false));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(body));

        assertEquals(400, ex.getCode());
        verify(eventRepository, never()).save(any(Event.class));
        verify(eventSpecialRuleService, never()).updateRules(any(), any(), any());
    }

    @Test
    @DisplayName("序列化：specialRules 非空时随 JSON 输出")
    void json_specialRulesSet_isSerialized() throws Exception {
        Event event = new Event();
        Map<String, Boolean> value = new LinkedHashMap<>();
        value.put("specialRulesEnabled", true);
        value.put("hasEventRules", false);
        value.put("extraBatter", false);
        value.put("forceHalf", true);
        event.setSpecialRules(value);

        String json = new ObjectMapper().writeValueAsString(event);

        assertTrue(json.contains("\"specialRulesEnabled\":true"));
        assertTrue(json.contains("\"hasEventRules\":false"));
        assertTrue(json.contains("\"extraBatter\":false"));
        assertTrue(json.contains("\"forceHalf\":true"));
    }

    @Test
    @DisplayName("序列化：specialRules 为 null 时从 JSON 省略")
    void json_specialRulesNull_isOmitted() throws Exception {
        String json = new ObjectMapper().writeValueAsString(new Event());

        assertFalse(json.contains("specialRules"));
    }

    private Event event(Long leagueId) {
        Event event = new Event();
        event.setId(EVENT_ID);
        event.setTenantId(TENANT_ID);
        event.setLeagueId(leagueId);
        event.setStatus("draft");
        event.setRequireRoster(Boolean.TRUE);
        return event;
    }

    private League league(Long id) {
        League league = new League();
        league.setId(id);
        league.setTenantId(TENANT_ID);
        return league;
    }

    private void stubWideRead() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(scopeQuerySupport.visibleLeagueIds(any())).thenReturn(null);
    }

    private void stubRestrictedRead(List<Long> visibleLeagueIds) {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.copyOf(visibleLeagueIds), Set.of()));
        when(scopeQuerySupport.visibleLeagueIds(any())).thenReturn(visibleLeagueIds);
    }

    private void stubEvent(Long leagueId, LocalDateTime deletedAt) {
        Event entity = event(leagueId);
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
