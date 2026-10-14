/*
 * 联盟解散守卫（Issue #318 决策 6）：由「名下球队计数」改为「名下赛事须全部已结束」。
 *
 * 语义：联盟下存在未删且 status != 'ended' 的赛事 → 400「请先结束联盟内全部赛事」且无写；
 * 全部已结束（或名下无赛事）→ 放行软删（归还标记 / 解散沿革等旁路另有专项覆盖，本类聚焦守卫放行/拦截）。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；LeagueService 手工 new（@Generated 构造器）。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.League;
import com.bsball.repository.EventRepository;
import com.bsball.repository.LeagueRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("联盟解散守卫：赛事全部结束才可解散（Issue #318 决策 6）")
class LeagueDissolveGuardTest {

    private static final long TENANT_ID = 10L;
    private static final long UID = 9L;
    private static final long LEAGUE_ID = 1L;

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
    private EventRepository eventRepository;

    private LeagueService service;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        service = new LeagueService(leagueRepository, accountScopeService, scopeQuerySupport, resourceGuard,
                personnelHistoryRecorder, tenantQueryPolicyService, leagueProvisionService, apiPermissionService,
                eventRepository);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("解散拦截：联盟下存在未结束赛事 → 400「请先结束联盟内全部赛事」且无写")
    void delete_hasNotEndedEvents_400_noWrite() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        League existing = league(LEAGUE_ID, TENANT_ID);
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(existing));
        when(eventRepository.countNotEndedEventsByLeague(LEAGUE_ID)).thenReturn(1L);
        CurrentUserHolder.set(UID, TENANT_ID);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(LEAGUE_ID));

        assertEquals(400, ex.getCode());
        assertEquals("请先结束联盟内全部赛事", ex.getMessage());
        verify(leagueRepository, never()).save(any(League.class));
        verify(personnelHistoryRecorder, never()).afterLeagueDissolve(any());
        assertEquals(null, existing.getDeletedAt());
    }

    @Test
    @DisplayName("解散放行：联盟下赛事全部已结束（计数 0）→ 软删 + 沿革")
    void delete_allEventsEnded_softDeletes() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        League existing = league(LEAGUE_ID, TENANT_ID);
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(existing));
        when(eventRepository.countNotEndedEventsByLeague(LEAGUE_ID)).thenReturn(0L);
        CurrentUserHolder.set(UID, TENANT_ID);

        service.delete(LEAGUE_ID);

        assertNotNull(existing.getDeletedAt());
        assertEquals(UID, existing.getDeletedBy());
        verify(leagueRepository).save(existing);
        verify(personnelHistoryRecorder).afterLeagueDissolve(existing);
    }

    private static League league(long id, long tenantId) {
        League l = new League();
        l.setId(id);
        l.setTenantId(tenantId);
        l.setName("联盟" + id);
        return l;
    }
}
