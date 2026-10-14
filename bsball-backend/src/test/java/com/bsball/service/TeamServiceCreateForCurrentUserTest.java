/*
 * 账号权限重构（批次 3a，Task 3.5；Issue #318 起扩展为「创建即负责」）：TeamService.create 落职矩阵测试。
 *
 * 铁律：非超管 / 非租管且已登录（uid != null）创建球队时 → 落 bs_team_manager active 行并后置失效范围缓存；
 * 入参 leagueId 一律忽略（球队不隶属联盟，防旧前端回写脏数据）；超管 / 租管 / 未登录（uid == null）→
 * 不落 team_manager、不 evict。
 *
 * evict 语义：create 整体 @Transactional，evict 经 accountScopeService.evictUserScopeCacheAfterCommit 后置提交。
 * 本测试用 mock AccountScopeService 仅验证「接线调用」；无事务立即失效 / 提交后失效 / 回滚不失效的真实
 * 语义由 AccountScopeServiceTest 直接覆盖（I1 盲区补齐）。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；TeamService 手工 new（@Generated 构造器）。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.model.entity.Team;
import com.bsball.model.entity.TeamManager;
import com.bsball.repository.GameRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.TeamManagerRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamService：create 门户创建即授职分派矩阵（#154 / Task 3.5）")
class TeamServiceCreateForCurrentUserTest {

    private static final long TENANT_ID = 10L;
    private static final long PORTAL_UID = 7L;
    private static final long TEAM_ID = 88L;
    private static final long LEAGUE_ID = 50L;

    @Mock
    private TeamRepository teamRepository;

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
    private TeamManagerRepository teamManagerRepository;

    @Mock
    private ApiPermissionService apiPermissionService;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private PlayerTeamRepository playerTeamRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerTeamService playerTeamService;

    private TeamService service;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        service = new TeamService(teamRepository, accountScopeService, scopeQuerySupport,
                resourceGuard, personnelHistoryRecorder, tenantQueryPolicyService, teamManagerRepository,
                apiPermissionService, gameRepository, playerTeamRepository, playerRepository, playerTeamService);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    // ------------------------------------------------------------ 门户账号：授职

    @Test
    @DisplayName("门户用户建队（leagueId=null）→ 落 team_manager active + 即时 evict")
    void create_portalUser_noLeague_provisionsManagerAndEvicts() {
        CurrentUserHolder.set(PORTAL_UID, TENANT_ID);
        stubPortalUser();
        stubTeamSaved();
        stubNoExistingManager();

        Team out = service.create(payload(null));

        assertEquals(TEAM_ID, out.getId());
        TeamManager tm = captureManager();
        assertEquals(TENANT_ID, tm.getTenantId());
        assertEquals(TEAM_ID, tm.getTeamId());
        assertEquals(PORTAL_UID, tm.getUserId());
        assertEquals(TeamManager.STATUS_ACTIVE, tm.getStatus());
        verify(accountScopeService).evictUserScopeCacheAfterCommit(PORTAL_UID);
        verifyNoInteractions(resourceGuard);
    }

    @Test
    @DisplayName("门户用户建队带 leagueId 入参 → 入参忽略（落库 leagueId=null）+ 落 team_manager + evict")
    void create_portalUser_leagueIdInput_ignored() {
        CurrentUserHolder.set(PORTAL_UID, TENANT_ID);
        stubPortalUser();
        stubTeamSaved();
        stubNoExistingManager();

        Team out = service.create(payload(LEAGUE_ID));

        assertEquals(TEAM_ID, out.getId());
        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(teamCaptor.capture());
        assertEquals(null, teamCaptor.getValue().getLeagueId(), "入参 leagueId 一律忽略");
        TeamManager tm = captureManager();
        assertEquals(TEAM_ID, tm.getTeamId());
        assertEquals(PORTAL_UID, tm.getUserId());
        assertEquals(TeamManager.STATUS_ACTIVE, tm.getStatus());
        verify(accountScopeService).evictUserScopeCacheAfterCommit(PORTAL_UID);
    }

    @Test
    @DisplayName("防御分支桩测：门户用户建队已有 team_manager 行 → 不重复落库但仍 evict")
    void create_portalUser_existingManager_idempotentNoSaveButEvicts() {
        CurrentUserHolder.set(PORTAL_UID, TENANT_ID);
        stubPortalUser();
        stubTeamSaved();
        // 防御分支桩测：create 路径球队为新 id 判存必空，此处刻意构造「已存在」以覆盖防御分支。
        when(teamManagerRepository.findByTeamIdAndUserIdAndDeletedAtIsNull(TEAM_ID, PORTAL_UID))
                .thenReturn(Optional.of(new TeamManager()));

        Team out = service.create(payload(null));

        assertEquals(TEAM_ID, out.getId());
        verify(teamManagerRepository, never()).save(any());
        verify(accountScopeService).evictUserScopeCacheAfterCommit(PORTAL_UID);
    }

    @Test
    @DisplayName("门户用户建队忽略入参 id（防 merge 覆盖既有行）")
    void create_inputIdIgnored() {
        CurrentUserHolder.set(PORTAL_UID, TENANT_ID);
        stubPortalUser();
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));
        when(teamManagerRepository.findByTeamIdAndUserIdAndDeletedAtIsNull(any(), eq(PORTAL_UID)))
                .thenReturn(Optional.empty());

        Team payload = payload(null);
        payload.setId(999L);
        Team out = service.create(payload);

        assertEquals(null, out.getId(), "入参 id 一律忽略");
        verify(teamManagerRepository).save(any(TeamManager.class));
    }

    // ------------------------------------------------------------ 管理员 / 匿名：跳过授职

    @Test
    @DisplayName("超管建队 → 不落 team_manager、不 evict")
    void create_superAdmin_skipsProvision() {
        CurrentUserHolder.set(1L, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(1L)).thenReturn(true);
        stubTeamSaved();

        Team out = service.create(payload(null));

        assertEquals(TEAM_ID, out.getId());
        verifyNoInteractions(teamManagerRepository);
        verify(accountScopeService, never()).evictUserScopeCacheAfterCommit(any());
        verifyNoInteractions(resourceGuard);
    }

    @Test
    @DisplayName("租管建队 → 不落 team_manager、不 evict")
    void create_tenantAdmin_skipsProvision() {
        CurrentUserHolder.set(2L, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(2L)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(2L)).thenReturn(true);
        stubTeamSaved();

        Team out = service.create(payload(null));

        assertEquals(TEAM_ID, out.getId());
        verifyNoInteractions(teamManagerRepository);
        verify(accountScopeService, never()).evictUserScopeCacheAfterCommit(any());
        verifyNoInteractions(resourceGuard);
    }

    @Test
    @DisplayName("未登录（uid==null）建队 → 跳过授职（不触达权限判定）")
    void create_anonymous_skipsProvision() {
        // 不设置 CurrentUserHolder → uid == null
        stubTeamSaved();

        Team out = service.create(payload(null));

        assertEquals(TEAM_ID, out.getId());
        verifyNoInteractions(apiPermissionService);
        verifyNoInteractions(teamManagerRepository);
        verify(accountScopeService, never()).evictUserScopeCacheAfterCommit(any());
    }

    // ------------------------------------------------------------- helpers

    private void stubPortalUser() {
        when(apiPermissionService.isSuperAdmin(PORTAL_UID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(PORTAL_UID)).thenReturn(false);
    }

    private void stubTeamSaved() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> {
            Team t = inv.getArgument(0);
            t.setId(TEAM_ID);
            return t;
        });
    }

    private void stubNoExistingManager() {
        when(teamManagerRepository.findByTeamIdAndUserIdAndDeletedAtIsNull(TEAM_ID, PORTAL_UID))
                .thenReturn(Optional.empty());
    }

    private TeamManager captureManager() {
        ArgumentCaptor<TeamManager> captor = ArgumentCaptor.forClass(TeamManager.class);
        verify(teamManagerRepository).save(captor.capture());
        return captor.getValue();
    }

    private static Team payload(Long leagueId) {
        Team t = new Team();
        t.setName("门户自建球队");
        t.setLeagueId(leagueId);
        return t;
    }
}
