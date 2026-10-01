/*
 * 账号权限重构（批次 5，Task ⑭）：其它 delete 复刻归还标记落地测试。
 *
 * 覆盖（对照 spec §6.10，与 DataSovereigntyRuleTest 同构）：
 *  - Coach / TeamLineupTemplate / HighlightMoment / Stadium 四条删除路径：
 *    非超管删除 → 实体 platformOwned=true 且软删；超管删除 → 不置标记（保持 false）；
 *  - tenant_id 不动断言一并（归还 ≠ 迁租）；
 *  - Stadium 级联（本批复刻关键裁定）：非超管归还球场时，其主场球队（homeTeams）一并置
 *    platform_owned=true（复用既有 bs_team.platform_owned，不改 tenant_id）；超管不级联。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；服务手工 new（@Generated 构造器），
 * ResourceGuard 以真实实现接线，通过 mock 的 ApiPermissionService.isSuperAdmin 切换调用者身份。
 * CurrentUserHolder 为 ThreadLocal 需逐用例清理。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.entity.Coach;
import com.bsball.model.entity.HighlightMoment;
import com.bsball.model.entity.Stadium;
import com.bsball.model.entity.StadiumHomeTeam;
import com.bsball.model.entity.Team;
import com.bsball.model.entity.TeamLineupTemplate;
import com.bsball.repository.ChinaRegionRepository;
import com.bsball.repository.CoachRepository;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.HighlightMomentRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.StadiumRepository;
import com.bsball.repository.TeamLineupTemplateRepository;
import com.bsball.repository.TeamManagerRepository;
import com.bsball.repository.TeamRepository;
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
@DisplayName("历史数据处置权（批次 5 复刻）：教练/阵容模板/高光/球场归还标记与球场级联（spec §6.10）")
class DataSovereigntyRuleMoreTest {

    private static final long TENANT_ID = 10L;
    private static final long UID = 9L;

    // ---- ResourceGuard 依赖（真实 guard 接线） ----
    @Mock
    private AccountScopeService accountScopeService;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerTeamService playerTeamService;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamManagerRepository teamManagerRepository;
    @Mock
    private ApiPermissionService apiPermissionService;

    // ---- 各服务依赖 ----
    @Mock
    private CoachRepository coachRepository;
    @Mock
    private ScopeQuerySupport scopeQuerySupport;
    @Mock
    private PersonnelHistoryRecorder personnelHistoryRecorder;
    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;
    @Mock
    private TeamService teamService;
    @Mock
    private TeamLineupTemplateRepository templateRepository;
    @Mock
    private GamePlayerStatRepository gamePlayerStatRepository;
    @Mock
    private HighlightMomentRepository highlightMomentRepository;
    @Mock
    private StadiumRepository stadiumRepository;
    @Mock
    private ChinaRegionRepository chinaRegionRepository;

    private ResourceGuard guard;
    private CoachService coachService;
    private TeamLineupTemplateService teamLineupTemplateService;
    private HighlightMomentService highlightMomentService;
    private StadiumService stadiumService;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        guard = new ResourceGuard(accountScopeService, eventRepository, gameRepository, playerRepository,
                playerTeamService, teamRepository, teamManagerRepository, apiPermissionService);
        coachService = new CoachService(coachRepository, teamRepository, accountScopeService, scopeQuerySupport,
                guard, personnelHistoryRecorder, tenantQueryPolicyService);
        teamLineupTemplateService = new TeamLineupTemplateService(templateRepository, teamService, gameRepository,
                gamePlayerStatRepository, accountScopeService, scopeQuerySupport, guard, tenantQueryPolicyService);
        highlightMomentService = new HighlightMomentService(highlightMomentRepository, tenantQueryPolicyService,
                guard);
        stadiumService = new StadiumService(stadiumRepository, teamRepository, chinaRegionRepository,
                tenantQueryPolicyService, guard);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    // ------------------------------------------------------------------ 教练

    @Test
    @DisplayName("归还标记：教练 · 非超管删除 → platformOwned=true 且软删、tenant_id 不动")
    void deleteCoach_nonSuper_marksPlatformOwned() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        Coach existing = coach(1L, TENANT_ID, 100L);
        when(coachRepository.findById(1L)).thenReturn(Optional.of(existing));
        CurrentUserHolder.set(UID, TENANT_ID);

        coachService.delete(1L);

        assertEquals(Boolean.TRUE, existing.getPlatformOwned());
        assertEquals(TENANT_ID, existing.getTenantId());
        assertNotNull(existing.getDeletedAt());
        verify(coachRepository).save(existing);
    }

    @Test
    @DisplayName("归还标记：教练 · 超管删除 → platformOwned 保持 false（最终处置方，不归还）")
    void deleteCoach_super_doesNotMark() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(true);
        Coach existing = coach(1L, TENANT_ID, 100L);
        when(coachRepository.findById(1L)).thenReturn(Optional.of(existing));
        CurrentUserHolder.set(UID, TENANT_ID);

        coachService.delete(1L);

        assertEquals(Boolean.FALSE, existing.getPlatformOwned());
        assertEquals(TENANT_ID, existing.getTenantId());
        assertNotNull(existing.getDeletedAt());
        verify(coachRepository).save(existing);
    }

    // ------------------------------------------------------------------ 阵容模板（仅置标，不收窄名目）

    @Test
    @DisplayName("归还标记：阵容模板 · 非超管删除 → platformOwned=true（名目保持 manage）")
    void deleteTeamLineupTemplate_nonSuper_marksPlatformOwned() {
        when(teamService.get(100L)).thenReturn(team(100L, TENANT_ID));
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        TeamLineupTemplate existing = template(1L, TENANT_ID, 100L);
        when(templateRepository.findByIdAndTeamIdAndDeletedAtIsNull(1L, 100L)).thenReturn(Optional.of(existing));
        CurrentUserHolder.set(UID, TENANT_ID);

        teamLineupTemplateService.delete(100L, 1L);

        assertEquals(Boolean.TRUE, existing.getPlatformOwned());
        assertEquals(TENANT_ID, existing.getTenantId());
        assertNotNull(existing.getDeletedAt());
        verify(templateRepository).save(existing);
    }

    @Test
    @DisplayName("归还标记：阵容模板 · 超管删除 → platformOwned 保持 false")
    void deleteTeamLineupTemplate_super_doesNotMark() {
        when(teamService.get(100L)).thenReturn(team(100L, TENANT_ID));
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(true);
        TeamLineupTemplate existing = template(1L, TENANT_ID, 100L);
        when(templateRepository.findByIdAndTeamIdAndDeletedAtIsNull(1L, 100L)).thenReturn(Optional.of(existing));
        CurrentUserHolder.set(UID, TENANT_ID);

        teamLineupTemplateService.delete(100L, 1L);

        assertEquals(Boolean.FALSE, existing.getPlatformOwned());
        assertNotNull(existing.getDeletedAt());
        verify(templateRepository).save(existing);
    }

    // ------------------------------------------------------------------ 高光时刻

    @Test
    @DisplayName("归还标记：高光时刻 · 非超管删除 → platformOwned=true 且软删")
    void deleteHighlightMoment_nonSuper_marksPlatformOwned() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        HighlightMoment existing = highlightMoment(2L, TENANT_ID);
        when(highlightMomentRepository.findById(2L)).thenReturn(Optional.of(existing));
        CurrentUserHolder.set(UID, TENANT_ID);

        highlightMomentService.delete(2L);

        assertEquals(Boolean.TRUE, existing.getPlatformOwned());
        assertEquals(TENANT_ID, existing.getTenantId());
        assertNotNull(existing.getDeletedAt());
        verify(highlightMomentRepository).save(existing);
    }

    @Test
    @DisplayName("归还标记：高光时刻 · 超管删除 → platformOwned 保持 false")
    void deleteHighlightMoment_super_doesNotMark() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(true);
        HighlightMoment existing = highlightMoment(2L, TENANT_ID);
        when(highlightMomentRepository.findById(2L)).thenReturn(Optional.of(existing));
        CurrentUserHolder.set(UID, TENANT_ID);

        highlightMomentService.delete(2L);

        assertEquals(Boolean.FALSE, existing.getPlatformOwned());
        assertNotNull(existing.getDeletedAt());
        verify(highlightMomentRepository).save(existing);
    }

    // ------------------------------------------------------------------ 球场（含 homeTeams 级联）

    @Test
    @DisplayName("归还标记：球场 · 非超管归还 → 球场平台标记 + 级联 homeTeams 置 team.platform_owned=true")
    void deleteStadium_nonSuper_marksStadiumAndCascadesTeams() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        Stadium s = stadium(3L, TENANT_ID, 300L, 301L);
        when(stadiumRepository.findById(3L)).thenReturn(Optional.of(s));
        Team t300 = team(300L, TENANT_ID);
        Team t301 = team(301L, TENANT_ID);
        when(teamRepository.findById(300L)).thenReturn(Optional.of(t300));
        when(teamRepository.findById(301L)).thenReturn(Optional.of(t301));
        CurrentUserHolder.set(UID, TENANT_ID);

        stadiumService.delete(3L);

        // 球场本体
        assertEquals(Boolean.TRUE, s.getPlatformOwned());
        assertEquals(TENANT_ID, s.getTenantId());
        assertNotNull(s.getDeletedAt());
        verify(stadiumRepository).save(s);
        // 级联：主场球队沉淀（复用既有 bs_team.platform_owned，tenant_id 不动）
        for (StadiumHomeTeam ht : s.getHomeTeams()) {
            assertNotNull(ht.getDeletedAt());
        }
        assertEquals(Boolean.TRUE, t300.getPlatformOwned());
        assertEquals(TENANT_ID, t300.getTenantId());
        assertEquals(Boolean.TRUE, t301.getPlatformOwned());
        assertEquals(TENANT_ID, t301.getTenantId());
        verify(teamRepository).save(t300);
        verify(teamRepository).save(t301);
    }

    @Test
    @DisplayName("归还标记：球场 · 超管归还 → 球场与主场球队均不置标记（最终处置方，不级联）")
    void deleteStadium_super_doesNotMarkNorCascade() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(true);
        Stadium s = stadium(3L, TENANT_ID, 300L);
        when(stadiumRepository.findById(3L)).thenReturn(Optional.of(s));
        CurrentUserHolder.set(UID, TENANT_ID);

        stadiumService.delete(3L);

        assertEquals(Boolean.FALSE, s.getPlatformOwned());
        assertEquals(TENANT_ID, s.getTenantId());
        assertNotNull(s.getDeletedAt());
        verify(stadiumRepository).save(s);
        // 超管不级联：既不改球队标记，也不落库球队
        verify(teamRepository, never()).save(any());
        verify(teamRepository, never()).findById(any());
    }

    // ------------------------------------------------------------------ fixtures

    private static Coach coach(long id, long tenantId, Long teamId) {
        Coach c = new Coach();
        c.setId(id);
        c.setTenantId(tenantId);
        c.setTeamId(teamId);
        return c;
    }

    private static TeamLineupTemplate template(long id, long tenantId, long teamId) {
        TeamLineupTemplate t = new TeamLineupTemplate();
        t.setId(id);
        t.setTenantId(tenantId);
        t.setTeamId(teamId);
        return t;
    }

    private static HighlightMoment highlightMoment(long id, long tenantId) {
        HighlightMoment h = new HighlightMoment();
        h.setId(id);
        h.setTenantId(tenantId);
        return h;
    }

    private static Stadium stadium(long id, long tenantId, Long... homeTeamIds) {
        Stadium s = new Stadium();
        s.setId(id);
        s.setTenantId(tenantId);
        for (Long tid : homeTeamIds) {
            StadiumHomeTeam ht = new StadiumHomeTeam();
            ht.setStadium(s);
            ht.setTeamId(tid);
            s.getHomeTeams().add(ht);
        }
        return s;
    }

    private static Team team(long id, long tenantId) {
        Team t = new Team();
        t.setId(id);
        t.setTenantId(tenantId);
        return t;
    }
}
