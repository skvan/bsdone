/*
 * 账号权限重构（批次 3b，Task 3.14 / spec §6.10）：历史数据处置权落地规则测试。
 *
 * 覆盖：
 *  - 销毁守卫 ResourceGuard.assertCanPurgeHistoricalData：非超管 403（文案逐字）/ 超管放行；
 *  - 归还标记（平台资产）：5 条删除路径（League/Team/Player(+deleteBatch)/Event/Game）非超管删除 →
 *    实体 platformOwned=true 且软删；超管删除 → 不置标记（保持 false）；
 *  - 超管资产视图 PlatformAssetService.summary：仅超管（非超管 403）；计数正确（mock 仓储）；
 *  - 守卫复核：既有业务守卫对超管同样生效——超管 + 有比赛经历删除经历 → 400；超管 + 解散有未开打比赛 → 400。
 *
 * 风格：Mockito mock 外部依赖，不启动 Spring、不连库；服务手工 new，ResourceGuard 以真实实现接线，
 * 通过 mock 的 ApiPermissionService.isSuperAdmin 切换调用者身份。CurrentUserHolder 为 ThreadLocal 需逐用例清理。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.api.PlatformAssetApi;
import com.bsball.common.Result;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.League;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.model.entity.Team;
import com.bsball.model.entity.TeamManager;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.LeagueRepository;
import com.bsball.repository.PlayerClaimRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.StadiumRepository;
import com.bsball.repository.TeamManagerRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.query.ScopeQuerySupport;
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
@DisplayName("历史数据处置权：归还标记与销毁守卫（spec §6.10）")
class DataSovereigntyRuleTest {

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
    private LeagueRepository leagueRepository;
    @Mock
    private ScopeQuerySupport scopeQuerySupport;
    @Mock
    private PersonnelHistoryRecorder personnelHistoryRecorder;
    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;
    @Mock
    private LeagueProvisionService leagueProvisionService;
    @Mock
    private PlayerTeamRepository playerTeamRepository;
    @Mock
    private StatsService statsService;
    @Mock
    private SysConfigService sysConfigService;
    @Mock
    private PlayerClaimRepository playerClaimRepository;
    @Mock
    private GamePlayerStatRepository gamePlayerStatRepository;
    @Mock
    private StadiumRepository stadiumRepository;
    @Mock
    private EarnedRunReconstructionService earnedRunReconstructionService;

    private ResourceGuard guard;
    private LeagueService leagueService;
    private TeamService teamService;
    private PlayerService playerService;
    private EventService eventService;
    private GameService gameService;
    private PlatformAssetService platformAssetService;
    private PlatformAssetApi platformAssetApi;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        guard = new ResourceGuard(accountScopeService, eventRepository, gameRepository, playerRepository,
                playerTeamService, teamRepository, teamManagerRepository, apiPermissionService);
        leagueService = new LeagueService(leagueRepository, accountScopeService, scopeQuerySupport, guard,
                personnelHistoryRecorder, tenantQueryPolicyService, leagueProvisionService, apiPermissionService,
                teamRepository);
        teamService = new TeamService(teamRepository, leagueRepository, accountScopeService, scopeQuerySupport,
                guard, personnelHistoryRecorder, tenantQueryPolicyService, teamManagerRepository,
                apiPermissionService, gameRepository, playerTeamRepository, playerRepository, playerTeamService);
        playerService = new PlayerService(playerRepository, teamRepository, playerTeamRepository, statsService,
                accountScopeService, scopeQuerySupport, guard, personnelHistoryRecorder, playerTeamService,
                tenantQueryPolicyService, sysConfigService, playerClaimRepository, gamePlayerStatRepository);
        eventService = new EventService(eventRepository, leagueRepository, accountScopeService, scopeQuerySupport,
                guard, tenantQueryPolicyService);
        gameService = new GameService(gameRepository, gamePlayerStatRepository, eventRepository, stadiumRepository,
                accountScopeService, scopeQuerySupport, guard, tenantQueryPolicyService, earnedRunReconstructionService);
        platformAssetService = new PlatformAssetService(leagueRepository, teamRepository, playerRepository,
                eventRepository, gameRepository, apiPermissionService);
        platformAssetApi = new PlatformAssetApi(platformAssetService);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    // ------------------------------------------------------------------ 销毁守卫

    @Test
    @DisplayName("销毁守卫：非超管 → 403（文案逐字）")
    void assertCanPurgeHistoricalData_nonSuper_forbidden() {
        CurrentUserHolder.set(UID, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> guard.assertCanPurgeHistoricalData());

        assertEquals(403, ex.getCode());
        assertEquals("仅系统超管可销毁历史数据", ex.getMessage());
    }

    @Test
    @DisplayName("销毁守卫：超管 → 放行")
    void assertCanPurgeHistoricalData_super_passes() {
        CurrentUserHolder.set(UID, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(true);

        assertDoesNotThrow(() -> guard.assertCanPurgeHistoricalData());
    }

    // ------------------------------------------------------------------ 归还标记：非超管删除 → platformOwned=true

    @Test
    @DisplayName("归还标记：联盟 · 非超管删除 → platformOwned=true 且软删")
    void deleteLeague_nonSuper_marksPlatformOwned() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        League existing = league(1L, TENANT_ID);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(existing));
        CurrentUserHolder.set(UID, TENANT_ID);

        leagueService.delete(1L);

        assertEquals(Boolean.TRUE, existing.getPlatformOwned());
        assertEquals(TENANT_ID, existing.getTenantId());
        assertNotNull(existing.getDeletedAt());
        verify(leagueRepository).save(existing);
    }

    @Test
    @DisplayName("归还标记：球队 · 非超管解散 → platformOwned=true")
    void deleteTeam_nonSuper_marksPlatformOwned() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(gameRepository.countPendingGamesByTeamId(8L)).thenReturn(0L);
        when(playerTeamRepository.findCurrentEntriesByTeamId(8L)).thenReturn(List.of());
        when(teamManagerRepository.findByTeamIdAndStatusAndDeletedAtIsNull(8L, TeamManager.STATUS_ACTIVE))
                .thenReturn(List.of());
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        Team existing = team(8L, TENANT_ID, 5L);
        when(teamRepository.findById(8L)).thenReturn(Optional.of(existing));
        CurrentUserHolder.set(UID, TENANT_ID);

        teamService.delete(8L);

        assertEquals(Boolean.TRUE, existing.getPlatformOwned());
        assertEquals(TENANT_ID, existing.getTenantId());
        assertNotNull(existing.getDeletedAt());
        verify(teamRepository).save(existing);
    }

    @Test
    @DisplayName("归还标记：球员 · 非超管删除 → platformOwned=true")
    void deletePlayer_nonSuper_marksPlatformOwned() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(UID)).thenReturn(true);
        Player existing = player(5L, TENANT_ID);
        when(playerRepository.findById(5L)).thenReturn(Optional.of(existing));
        CurrentUserHolder.set(UID, TENANT_ID);

        playerService.delete(5L);

        assertEquals(Boolean.TRUE, existing.getPlatformOwned());
        assertEquals(TENANT_ID, existing.getTenantId());
        assertNotNull(existing.getDeletedAt());
        verify(playerRepository).save(existing);
    }

    @Test
    @DisplayName("归还标记：球员批量删除 · 非超管 → 逐条 platformOwned=true")
    void deletePlayerBatch_nonSuper_marksPlatformOwned() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(UID)).thenReturn(true);
        Player a = player(6L, TENANT_ID);
        Player b = player(7L, TENANT_ID);
        when(playerRepository.findAllById(any())).thenReturn(List.of(a, b));
        CurrentUserHolder.set(UID, TENANT_ID);

        playerService.deleteBatch(List.of(6L, 7L));

        assertEquals(Boolean.TRUE, a.getPlatformOwned());
        assertEquals(Boolean.TRUE, b.getPlatformOwned());
        assertEquals(TENANT_ID, a.getTenantId());
        assertEquals(TENANT_ID, b.getTenantId());
        assertNotNull(a.getDeletedAt());
        assertNotNull(b.getDeletedAt());
        verify(playerRepository).saveAll(any());
    }

    @Test
    @DisplayName("归还标记：赛事 · 非超管删除 → platformOwned=true")
    void deleteEvent_nonSuper_marksPlatformOwned() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        Event existing = event(2L, TENANT_ID, 5L);
        when(eventRepository.findById(2L)).thenReturn(Optional.of(existing));
        CurrentUserHolder.set(UID, TENANT_ID);

        eventService.delete(2L);

        assertEquals(Boolean.TRUE, existing.getPlatformOwned());
        assertEquals(TENANT_ID, existing.getTenantId());
        assertNotNull(existing.getDeletedAt());
        verify(eventRepository).save(existing);
    }

    @Test
    @DisplayName("归还标记：比赛 · 非超管删除 → platformOwned=true")
    void deleteGame_nonSuper_marksPlatformOwned() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        Game existing = game(3L, TENANT_ID, 7L);
        Event ev = event(7L, TENANT_ID, 5L);
        when(gameRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(eventRepository.findById(7L)).thenReturn(Optional.of(ev));
        CurrentUserHolder.set(UID, TENANT_ID);

        gameService.delete(3L);

        assertEquals(Boolean.TRUE, existing.getPlatformOwned());
        assertEquals(TENANT_ID, existing.getTenantId());
        assertNotNull(existing.getDeletedAt());
        verify(gameRepository).save(existing);
    }

    // ------------------------------------------------------------------ 归还标记：超管删除 → 不置标记

    @Test
    @DisplayName("归还标记：超管删除联盟 → platformOwned 保持 false（最终处置方，不归还）")
    void deleteLeague_super_doesNotMark() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(true);
        League existing = league(1L, TENANT_ID);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(existing));
        CurrentUserHolder.set(UID, TENANT_ID);

        leagueService.delete(1L);

        assertEquals(Boolean.FALSE, existing.getPlatformOwned());
        assertNotNull(existing.getDeletedAt());
    }

    // ------------------------------------------------------------------ 超管资产视图

    @Test
    @DisplayName("资产汇总：非超管 → 403 且不触达仓储")
    void platformAssetSummary_nonSuper_forbidden() {
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> platformAssetService.summary(UID));

        assertEquals(403, ex.getCode());
        verify(leagueRepository, never()).countByPlatformOwnedTrueAndDeletedAtIsNotNull();
    }

    @Test
    @DisplayName("资产汇总：超管 → 各实体已归还计数正确")
    void platformAssetSummary_super_counts() {
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(true);
        when(leagueRepository.countByPlatformOwnedTrueAndDeletedAtIsNotNull()).thenReturn(1L);
        when(teamRepository.countByPlatformOwnedTrueAndDeletedAtIsNotNull()).thenReturn(2L);
        when(playerRepository.countByPlatformOwnedTrueAndDeletedAtIsNotNull()).thenReturn(3L);
        when(eventRepository.countByPlatformOwnedTrueAndDeletedAtIsNotNull()).thenReturn(4L);
        when(gameRepository.countByPlatformOwnedTrueAndDeletedAtIsNotNull()).thenReturn(5L);

        Map<String, Object> result = platformAssetService.summary(UID);

        assertEquals(1L, result.get("league"));
        assertEquals(2L, result.get("team"));
        assertEquals(3L, result.get("player"));
        assertEquals(4L, result.get("event"));
        assertEquals(5L, result.get("game"));
    }

    @Test
    @DisplayName("资产汇总端点：未登录 → 401")
    void platformAssetApi_noUser_unauthorized() {
        CurrentUserHolder.clear();

        Result<Map<String, Object>> result = platformAssetApi.summary();

        assertEquals(401, result.getCode());
    }

    // ------------------------------------------------------------------ 守卫复核：既有守卫对超管同样生效

    @Test
    @DisplayName("守卫复核：超管 + 有比赛经历删除经历 → 仍 400")
    void superAdmin_deleteExperienceWithGames_stillBlocked() {
        CurrentUserHolder.set(UID, TENANT_ID);
        PlayerTeam entry = new PlayerTeam();
        entry.setTeamId(100L);
        when(gamePlayerStatRepository.countValidByPlayerIdAndTeamId(5L, 100L)).thenReturn(3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> playerService.assertEntriesDeletable(5L, List.of(entry)));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不可删除"));
    }

    @Test
    @DisplayName("守卫复核：超管 + 解散有未开打比赛 → 仍 400")
    void superAdmin_dissolveWithPendingGames_stillBlocked() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(gameRepository.countPendingGamesByTeamId(9L)).thenReturn(2L);
        Team existing = team(9L, TENANT_ID, 5L);
        when(teamRepository.findById(9L)).thenReturn(Optional.of(existing));
        CurrentUserHolder.set(UID, TENANT_ID);

        BusinessException ex = assertThrows(BusinessException.class, () -> teamService.delete(9L));

        assertEquals(400, ex.getCode());
        assertEquals(Boolean.FALSE, existing.getPlatformOwned());
        verify(teamRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ fixtures

    private static League league(long id, long tenantId) {
        League l = new League();
        l.setId(id);
        l.setTenantId(tenantId);
        return l;
    }

    private static Team team(long id, long tenantId, long leagueId) {
        Team t = new Team();
        t.setId(id);
        t.setTenantId(tenantId);
        t.setLeagueId(leagueId);
        return t;
    }

    private static Player player(long id, long tenantId) {
        Player p = new Player();
        p.setId(id);
        p.setTenantId(tenantId);
        return p;
    }

    private static Event event(long id, long tenantId, long leagueId) {
        Event e = new Event();
        e.setId(id);
        e.setTenantId(tenantId);
        e.setLeagueId(leagueId);
        return e;
    }

    private static Game game(long id, long tenantId, long eventId) {
        Game g = new Game();
        g.setId(id);
        g.setTenantId(tenantId);
        g.setEventId(eventId);
        return g;
    }
}
