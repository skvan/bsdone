/*
 * 账号权限重构（批次 3a，评审修复 I6）：LeagueService.createForCurrentUser 分派矩阵测试。
 *
 * 铁律：门户角色（member / team_manager / league_organizer）且非超管 / 租管 → 走审批链路
 * （leagueProvisionService.submitOrCreate）；其余（超管 / 租管 / 未登录 uid==null）→ 直建
 * （leagueService.createInternal）。本类逐一 verify 分支调用（Mockito 手工装配）。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.model.entity.League;
import com.bsball.repository.LeagueRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeagueService：createForCurrentUser 分派矩阵（#154）")
class LeagueServiceCreateForCurrentUserTest {

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private AccountScopeService accountScopeService;

    @Mock
    private ScopeQuerySupport scopeQuerySupport;

    @Mock
    private ResourceGuard resourceGuard;

    @Mock
    private PersonnelHistoryRecorder personnelHistoryRecorder;

    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;

    @Mock
    private LeagueProvisionService leagueProvisionService;

    @Mock
    private ApiPermissionService apiPermissionService;

    @Mock
    private TeamRepository teamRepository;

    private LeagueService service;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        service = new LeagueService(leagueRepository, accountScopeService, scopeQuerySupport, resourceGuard,
                personnelHistoryRecorder, tenantQueryPolicyService, leagueProvisionService, apiPermissionService,
                teamRepository);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("门户角色（member / team_manager / league_organizer）→ submitOrCreate")
    void createForCurrentUser_portalRole_dispatchesSubmitOrCreate() {
        CurrentUserHolder.set(7L, 10L);
        when(apiPermissionService.isSuperAdmin(7L)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(7L)).thenReturn(false);
        when(apiPermissionService.hasAnyRoleCode(7L, "member", "team_manager", "league_organizer"))
                .thenReturn(true);
        when(leagueProvisionService.submitOrCreate(eq(7L), any(League.class)))
                .thenReturn(Map.of("pending", true, "requestId", 500L));

        Map<String, Object> out = service.createForCurrentUser(new League());

        assertEquals(Boolean.TRUE, out.get("pending"));
        verify(leagueProvisionService).submitOrCreate(eq(7L), any(League.class));
        verify(leagueRepository, never()).save(any());
    }

    @Test
    @DisplayName("超管 → createInternal（直建，不走审批）")
    void createForCurrentUser_superAdmin_directCreate() {
        CurrentUserHolder.set(1L, 10L);
        when(apiPermissionService.isSuperAdmin(1L)).thenReturn(true);
        stubDirectCreate("超管直建");

        Map<String, Object> out = service.createForCurrentUser(payload("超管直建"));

        assertEquals(Boolean.FALSE, out.get("pending"));
        assertEquals(88L, ((Number) out.get("id")).longValue());
        verify(leagueProvisionService, never()).submitOrCreate(any(), any());
        verify(leagueRepository).save(any(League.class));
    }

    @Test
    @DisplayName("租管 → createInternal（直建，不走审批）")
    void createForCurrentUser_tenantAdmin_directCreate() {
        CurrentUserHolder.set(2L, 10L);
        when(apiPermissionService.isSuperAdmin(2L)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(2L)).thenReturn(true);
        stubDirectCreate("租管直建");

        Map<String, Object> out = service.createForCurrentUser(payload("租管直建"));

        assertEquals(Boolean.FALSE, out.get("pending"));
        assertEquals(88L, ((Number) out.get("id")).longValue());
        verify(leagueProvisionService, never()).submitOrCreate(any(), any());
    }

    @Test
    @DisplayName("未登录（uid==null）→ createInternal（直建）")
    void createForCurrentUser_anonymous_directCreate() {
        // 不设置 CurrentUserHolder → uid == null
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(10L);
        when(leagueRepository.existsByTenantIdAndNameIgnoreCaseAndDeletedAtIsNull(10L, "匿名直建"))
                .thenReturn(false);
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> {
            League l = inv.getArgument(0);
            l.setId(88L);
            return l;
        });

        Map<String, Object> out = service.createForCurrentUser(payload("匿名直建"));

        assertEquals(Boolean.FALSE, out.get("pending"));
        assertEquals(88L, ((Number) out.get("id")).longValue());
        verify(leagueProvisionService, never()).submitOrCreate(any(), any());
    }

    // ------------------------------------------------------------- helpers

    private void stubDirectCreate(String name) {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(10L);
        when(leagueRepository.existsByTenantIdAndNameIgnoreCaseAndDeletedAtIsNull(10L, name)).thenReturn(false);
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> {
            League l = inv.getArgument(0);
            l.setId(88L);
            return l;
        });
    }

    private static League payload(String name) {
        League l = new League();
        l.setName(name);
        return l;
    }
}
