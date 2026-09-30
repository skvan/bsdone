/*
 * 账号权限重构（批次 3b，Task 3.13 / spec §6.9）：批量代建与权属转移规则接线测试。
 *
 * 覆盖（真实 ResourceGuard 接线，端到端验证「守卫分档 + 批量入口规则」）：
 *  - 批量代建（PlayerService.batchImport）：球队管理员本队 ✓ / 他队 ✗；联盟管理员域内 ✓ / 域外 ✗；
 *    去重口径同自助建档（§6.9）：同租户 name+birthDate 撞既有档案 / 文件内同键行 → 跳过；birthDate 空不参与去重；
 *  - 批量建队（TeamService.batchCreate）：联盟管理员域内 ✓（初始无主，不授职）/ 域外 ✗；管理员直通；
 *  - 球队权属转移：无主球队联盟可编辑/解散删除（§6.7 语义）✓；有主即只读 403 ✗；
 *  - 球员档案分档：已认领 → 上级 ROSTER 编辑/删除 403；未认领 → 本队 ROSTER 可写 ✓；
 *  - 前置④（saveResult 原域校验）：受限改域外比赛 / 改 eventId 到域外赛事 → 403 且无写；
 *  - 前置⑤（get null league 收口）：管理上下文 + leagueId 为空 → 403；宽读不变；
 *  - 越权矩阵补充：空域账号批量入口一律 403。
 *
 * 风格：Mockito mock 外部依赖，不启动 Spring、不连库；服务手工 new，ResourceGuard 以真实实现接线
 * （守卫内部语义另见 ResourceGuardTest）；CurrentUserHolder 为 ThreadLocal 需逐用例清理。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.dto.SaveGameResultDTO;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.League;
import com.bsball.model.entity.Player;
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
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("批次代建与权属转移：批量入口与守卫分档接线（spec §6.9）")
class BatchOnboardingRuleTest {

    private static final long TENANT_ID = 10L;

    // ---- ResourceGuard 依赖（共享） ----
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

    // ---- TeamService 依赖 ----
    @Mock
    private LeagueRepository leagueRepository;
    @Mock
    private ScopeQuerySupport scopeQuerySupport;
    @Mock
    private PersonnelHistoryRecorder personnelHistoryRecorder;
    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;
    @Mock
    private ApiPermissionService apiPermissionService;
    @Mock
    private PlayerTeamRepository playerTeamRepository;

    // ---- PlayerService 依赖 ----
    @Mock
    private StatsService statsService;
    @Mock
    private SysConfigService sysConfigService;
    @Mock
    private PlayerClaimRepository playerClaimRepository;
    @Mock
    private GamePlayerStatRepository gamePlayerStatRepository;

    // ---- GameService 依赖 ----
    @Mock
    private StadiumRepository stadiumRepository;
    @Mock
    private EarnedRunReconstructionService earnedRunReconstructionService;

    private ResourceGuard guard;
    private TeamService teamService;
    private PlayerService playerService;
    private GameService gameService;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        guard = new ResourceGuard(accountScopeService, eventRepository, gameRepository, playerRepository,
                playerTeamService, teamRepository, teamManagerRepository, apiPermissionService);
        teamService = new TeamService(teamRepository, leagueRepository, accountScopeService, scopeQuerySupport,
                guard, personnelHistoryRecorder, tenantQueryPolicyService, teamManagerRepository,
                apiPermissionService, gameRepository, playerTeamRepository, playerRepository, playerTeamService);
        playerService = new PlayerService(playerRepository, teamRepository, playerTeamRepository, statsService,
                accountScopeService, scopeQuerySupport, guard, personnelHistoryRecorder, playerTeamService,
                tenantQueryPolicyService, sysConfigService, playerClaimRepository, gamePlayerStatRepository);
        gameService = new GameService(gameRepository, gamePlayerStatRepository, eventRepository, stadiumRepository,
                accountScopeService, scopeQuerySupport, guard, tenantQueryPolicyService, earnedRunReconstructionService);
        eventService = new EventService(eventRepository, leagueRepository, accountScopeService, scopeQuerySupport,
                guard, tenantQueryPolicyService);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    // ------------------------------------------------------------------ 批量代建（PlayerService.batchImport）

    @Test
    @DisplayName("批量代建：球队管理员本队 ✓（建后未认领，不写 userId）")
    void batchImport_teamManager_ownTeam_ok() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team(100L, TENANT_ID, 10L)));
        when(playerRepository.findByDeletedAtIsNullAndTenantId(TENANT_ID)).thenReturn(List.of());
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        Player row = playerRow("甲", 100L);
        Map<String, Object> result = playerService.batchImport(List.of(row), "skip");

        assertEquals(Integer.valueOf(1), result.get("created"));
        assertEquals(null, row.getUserId());
        verify(playerTeamService).syncLegacyEntry(row);
    }

    @Test
    @DisplayName("批量代建：球队管理员他队 ✗（403 且无写）")
    void batchImport_teamManager_otherTeam_forbidden() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        when(teamRepository.findById(101L)).thenReturn(Optional.of(team(101L, TENANT_ID, 20L)));
        when(playerRepository.findByDeletedAtIsNullAndTenantId(TENANT_ID)).thenReturn(List.of());

        Player row = playerRow("乙", 101L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> playerService.batchImport(List.of(row), "skip"));

        assertEquals(403, ex.getCode());
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("批量代建去重：与既有自助档案同 name+birthDate → 跳过该行、不重复建档（spec §6.9）")
    void batchImport_dedup_existingSelfProfile_sameNameBirthDate_skipped() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team(100L, TENANT_ID, 10L)));
        // 既有自助档案：number 为 null（自助建档未入队），name+birthDate 与导入行相同
        Player selfArchive = playerRow("张三", null, "1990-01-01", null);
        selfArchive.setId(900L);
        when(playerRepository.findByDeletedAtIsNullAndTenantId(TENANT_ID)).thenReturn(List.of(selfArchive));

        Player row = playerRow("张三", 100L, "1990-01-01", "7");
        Map<String, Object> result = playerService.batchImport(List.of(row), "skip");

        assertEquals(Integer.valueOf(0), result.get("created"));
        assertEquals(Integer.valueOf(1), result.get("skipped"));
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("批量代建去重：文件内两行同 name+birthDate → 仅建一条")
    void batchImport_dedup_inFile_sameNameBirthDate() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team(100L, TENANT_ID, 10L)));
        when(playerRepository.findByDeletedAtIsNullAndTenantId(TENANT_ID)).thenReturn(List.of());
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        Player row1 = playerRow("李四", 100L, "1991-02-02", "8");
        Player row2 = playerRow("李四", 100L, "1991-02-02", "9");
        Map<String, Object> result = playerService.batchImport(List.of(row1, row2), "skip");

        assertEquals(Integer.valueOf(1), result.get("created"));
        assertEquals(Integer.valueOf(1), result.get("skipped"));
        verify(playerRepository, times(1)).save(any(Player.class));
    }

    @Test
    @DisplayName("批量代建去重：birthDate 为空的导入行不参与去重 → 正常建档（与自助口径一致）")
    void batchImport_dedup_blankBirthDate_notDeduped() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team(100L, TENANT_ID, 10L)));
        // 既有同名档案（生日非空）+ 两行 birthDate 均为空 → 键为空，既不撞既有、文件内也不互相去重
        Player existingSameName = playerRow("王五", null, "1988-08-08", null);
        existingSameName.setId(901L);
        when(playerRepository.findByDeletedAtIsNullAndTenantId(TENANT_ID)).thenReturn(List.of(existingSameName));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        Player row1 = playerRow("王五", 100L, null, null);
        Player row2 = playerRow("王五", 100L, "  ", null);
        Map<String, Object> result = playerService.batchImport(List.of(row1, row2), "skip");

        assertEquals(Integer.valueOf(2), result.get("created"));
        assertEquals(Integer.valueOf(0), result.get("skipped"));
        verify(playerRepository, times(2)).save(any(Player.class));
    }

    @Test
    @DisplayName("批量代建：联盟管理员域内球队 ✓")
    void batchImport_leagueOrganizer_domainTeam_ok() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team(100L, TENANT_ID, 10L)));
        when(playerRepository.findByDeletedAtIsNullAndTenantId(TENANT_ID)).thenReturn(List.of());
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        Player row = playerRow("丙", 100L);
        Map<String, Object> result = playerService.batchImport(List.of(row), "skip");

        assertEquals(Integer.valueOf(1), result.get("created"));
    }

    @Test
    @DisplayName("批量代建：联盟管理员域外球队 ✗（403 且无写）")
    void batchImport_leagueOrganizer_outsideDomain_forbidden() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(101L)).thenReturn(Optional.of(team(101L, TENANT_ID, 20L)));
        when(playerRepository.findByDeletedAtIsNullAndTenantId(TENANT_ID)).thenReturn(List.of());

        Player row = playerRow("丁", 101L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> playerService.batchImport(List.of(row), "skip"));

        assertEquals(403, ex.getCode());
        verify(playerRepository, never()).save(any(Player.class));
    }

    // ------------------------------------------------------------------ 批量建队（TeamService.batchCreate）

    @Test
    @DisplayName("批量建队：联盟管理员域内 ✓（初始无主，不授职）")
    void batchCreate_leagueOrganizer_domainLeague_ok_noProvision() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(leagueRepository.findById(10L)).thenReturn(Optional.of(league(10L, TENANT_ID)));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));
        CurrentUserHolder.set(555L, TENANT_ID);

        Team t = new Team();
        t.setName("新队");
        t.setLeagueId(10L);
        List<Team> created = teamService.batchCreate(List.of(t));

        assertEquals(1, created.size());
        verify(teamManagerRepository, never()).save(any(TeamManager.class));
    }

    @Test
    @DisplayName("批量建队：联盟管理员域外 ✗（403 且无写）")
    void batchCreate_leagueOrganizer_outsideDomain_forbidden() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(leagueRepository.findById(20L)).thenReturn(Optional.of(league(20L, TENANT_ID)));
        CurrentUserHolder.set(555L, TENANT_ID);

        Team t = new Team();
        t.setName("域外队");
        t.setLeagueId(20L);
        BusinessException ex = assertThrows(BusinessException.class, () -> teamService.batchCreate(List.of(t)));

        assertEquals(403, ex.getCode());
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName("批量建队：平台/租户管理员完整权限直通（不解析作用域）")
    void batchCreate_admin_passthrough() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(leagueRepository.findById(20L)).thenReturn(Optional.of(league(20L, TENANT_ID)));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));
        CurrentUserHolder.set(1L, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(1L)).thenReturn(true);

        Team t = new Team();
        t.setName("管理员建队");
        t.setLeagueId(20L);
        List<Team> created = teamService.batchCreate(List.of(t));

        assertEquals(1, created.size());
        verify(accountScopeService, never()).resolveCurrent();
    }

    // ------------------------------------------------------------------ 球队权属转移（update / delete）

    @Test
    @DisplayName("球队：无主 → 联盟管理员可编辑 ✓")
    void updateTeam_noManager_leagueSteward_ok() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team(100L, TENANT_ID, 10L)));
        when(teamManagerRepository.existsByTeamIdAndStatusAndDeletedAtIsNull(100L, TeamManager.STATUS_ACTIVE))
                .thenReturn(false);
        Team updated = new Team();
        updated.setName("改名");
        when(teamRepository.save(updated)).thenReturn(updated);

        Team saved = teamService.update(100L, updated);

        assertEquals(updated, saved);
        verify(teamRepository).save(updated);
    }

    @Test
    @DisplayName("球队：有主 → 联盟只读，编辑 403 且无写")
    void updateTeam_hasManager_leagueReadonly_forbidden() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team(100L, TENANT_ID, 10L)));
        when(teamManagerRepository.existsByTeamIdAndStatusAndDeletedAtIsNull(100L, TeamManager.STATUS_ACTIVE))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> teamService.update(100L, new Team()));

        assertEquals(403, ex.getCode());
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName("球队：无主 → 联盟可解散删除（§6.7 语义）✓")
    void deleteTeam_noManager_leagueSteward_ok() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        Team existing = team(100L, TENANT_ID, 10L);
        when(teamRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(teamManagerRepository.existsByTeamIdAndStatusAndDeletedAtIsNull(100L, TeamManager.STATUS_ACTIVE))
                .thenReturn(false);
        when(gameRepository.countPendingGamesByTeamId(100L)).thenReturn(0L);
        when(playerTeamRepository.findCurrentEntriesByTeamId(100L)).thenReturn(List.of());
        when(teamManagerRepository.findByTeamIdAndStatusAndDeletedAtIsNull(100L, TeamManager.STATUS_ACTIVE))
                .thenReturn(List.of());
        CurrentUserHolder.set(9L, TENANT_ID);

        teamService.delete(100L);

        verify(teamRepository).save(existing);
        verify(personnelHistoryRecorder).afterTeamDissolve(existing);
        assertNotNull(existing.getDeletedAt());
    }

    @Test
    @DisplayName("球队：有主 → 联盟不可解散删除（403，不进解散流程）")
    void deleteTeam_hasManager_forbidden() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team(100L, TENANT_ID, 10L)));
        when(teamManagerRepository.existsByTeamIdAndStatusAndDeletedAtIsNull(100L, TeamManager.STATUS_ACTIVE))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> teamService.delete(100L));

        assertEquals(403, ex.getCode());
        verify(gameRepository, never()).countPendingGamesByTeamId(any());
        verify(teamRepository, never()).save(any(Team.class));
    }

    // ------------------------------------------------------------------ 球员档案分档（已认领 / 未认领）

    @Test
    @DisplayName("球员档案：已认领 → 上级 ROSTER 编辑 403 且无写")
    void updatePlayer_claimed_superiorForbidden() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        when(playerRepository.findById(5L)).thenReturn(Optional.of(player(5L, TENANT_ID, 7L)));

        BusinessException ex = assertThrows(BusinessException.class, () -> playerService.update(5L, new Player()));

        assertEquals(403, ex.getCode());
        assertTrue(ex.getMessage().contains("已认领"));
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("球员档案：已认领 → 上级 ROSTER 删除 403 且无写")
    void deletePlayer_claimed_superiorForbidden() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        when(playerRepository.findById(5L)).thenReturn(Optional.of(player(5L, TENANT_ID, 7L)));

        BusinessException ex = assertThrows(BusinessException.class, () -> playerService.delete(5L));

        assertEquals(403, ex.getCode());
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("球员档案：未认领 → 本队 ROSTER 删除 ✓（软删）")
    void deletePlayer_unclaimed_ownTeam_ok() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        Player existing = player(5L, TENANT_ID, null);
        when(playerRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(playerTeamService.currentTeamIds(5L)).thenReturn(Set.of(100L));
        CurrentUserHolder.set(9L, TENANT_ID);

        playerService.delete(5L);

        verify(playerRepository).save(existing);
        assertNotNull(existing.getDeletedAt());
    }

    // ------------------------------------------------------------------ 前置④：saveResult 原域校验

    @Test
    @DisplayName("前置④：受限修改域外比赛 → 首位守卫 403 且无写")
    void saveResult_outDomainGame_forbiddenNoWrite() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        Game g = game(1L, TENANT_ID, 5L);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(g));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event(5L, TENANT_ID, 20L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> gameService.saveResult(1L, new SaveGameResultDTO()));

        assertEquals(403, ex.getCode());
        verify(gameRepository, never()).save(any(Game.class));
        verify(gamePlayerStatRepository, never()).findByGameId(any());
    }

    @Test
    @DisplayName("前置④：受限把比赛 eventId 改到域外赛事 → 403 且无写")
    void saveResult_changeEventToOutDomain_forbiddenNoWrite() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        Game g = game(1L, TENANT_ID, 5L);
        g.setIsSpecialResult(Boolean.TRUE); // 跳先发守位检查，聚焦 eventId 变更守卫
        when(gameRepository.findById(1L)).thenReturn(Optional.of(g));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event(5L, TENANT_ID, 10L)));
        when(eventRepository.findById(7L)).thenReturn(Optional.of(event(7L, TENANT_ID, 20L)));

        SaveGameResultDTO dto = new SaveGameResultDTO();
        SaveGameResultDTO.GamePart gp = new SaveGameResultDTO.GamePart();
        gp.setEventId(7L);
        dto.setGame(gp);

        BusinessException ex = assertThrows(BusinessException.class, () -> gameService.saveResult(1L, dto));

        assertEquals(403, ex.getCode());
        verify(gameRepository, never()).save(any(Game.class));
    }

    // ------------------------------------------------------------------ 前置⑤：get null league 收口

    @Test
    @DisplayName("前置⑤：赛事 get —— 管理上下文且 leagueId 为空 → 403")
    void eventGet_manageContext_nullLeague_forbidden() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(scopeQuerySupport.visibleLeagueIds(any())).thenReturn(List.of(10L));
        Event e = event(5L, TENANT_ID, null);
        when(eventRepository.findById(5L)).thenReturn(Optional.of(e));

        BusinessException ex = assertThrows(BusinessException.class, () -> eventService.get(5L));

        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("前置⑤：赛事 get —— 宽读（visibleLeagueIds=null）leagueId 为空不受影响")
    void eventGet_wideRead_nullLeague_ok() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(scopeQuerySupport.visibleLeagueIds(any())).thenReturn(null);
        Event e = event(5L, TENANT_ID, null);
        when(eventRepository.findById(5L)).thenReturn(Optional.of(e));

        assertEquals(e, eventService.get(5L));
    }

    @Test
    @DisplayName("前置⑤：比赛 get —— 管理上下文且 leagueId 为空 → 403")
    void gameGet_manageContext_nullLeague_forbidden() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(10L), Set.of()));
        when(scopeQuerySupport.visibleLeagueIds(any())).thenReturn(List.of(10L));
        Game g = game(1L, TENANT_ID, 5L);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(g));
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event(5L, TENANT_ID, null)));

        BusinessException ex = assertThrows(BusinessException.class, () -> gameService.get(1L));

        assertEquals(403, ex.getCode());
    }

    // ------------------------------------------------------------------ 越权矩阵补充

    @Test
    @DisplayName("越权矩阵：空域账号批量代建 / 批量建队一律 403")
    void emptyScope_batchEntries_forbidden() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.empty());
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team(100L, TENANT_ID, 10L)));
        when(playerRepository.findByDeletedAtIsNullAndTenantId(TENANT_ID)).thenReturn(List.of());
        when(leagueRepository.findById(10L)).thenReturn(Optional.of(league(10L, TENANT_ID)));
        CurrentUserHolder.set(555L, TENANT_ID);

        Player row = playerRow("戊", 100L);
        assertEquals(403, assertThrows(BusinessException.class,
                () -> playerService.batchImport(List.of(row), "skip")).getCode());

        Team t = new Team();
        t.setName("空域队");
        t.setLeagueId(10L);
        assertEquals(403, assertThrows(BusinessException.class,
                () -> teamService.batchCreate(List.of(t))).getCode());
    }

    // ------------------------------------------------------------------ 辅助

    private static Team team(Long id, Long tenantId, Long leagueId) {
        Team t = new Team();
        t.setId(id);
        t.setTenantId(tenantId);
        t.setLeagueId(leagueId);
        return t;
    }

    private static Event event(Long id, Long tenantId, Long leagueId) {
        Event e = new Event();
        e.setId(id);
        e.setTenantId(tenantId);
        e.setLeagueId(leagueId);
        return e;
    }

    private static Game game(Long id, Long tenantId, Long eventId) {
        Game g = new Game();
        g.setId(id);
        g.setTenantId(tenantId);
        g.setEventId(eventId);
        return g;
    }

    private static Player player(Long id, Long tenantId, Long userId) {
        Player p = new Player();
        p.setId(id);
        p.setTenantId(tenantId);
        p.setUserId(userId);
        return p;
    }

    private static League league(Long id, Long tenantId) {
        League l = new League();
        l.setId(id);
        l.setTenantId(tenantId);
        return l;
    }

    private static Player playerRow(String name, Long teamId) {
        Player p = new Player();
        p.setName(name);
        p.setTeamId(teamId);
        return p;
    }

    private static Player playerRow(String name, Long teamId, String birthDate, String number) {
        Player p = playerRow(name, teamId);
        p.setBirthDate(birthDate);
        p.setNumber(number);
        return p;
    }
}
