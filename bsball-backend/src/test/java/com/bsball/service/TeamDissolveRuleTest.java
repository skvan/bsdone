/*
 * 账号权限重构（批次 3b，Task 3.11）：球队解散规则落地测试（spec §6.7）。
 *
 * 覆盖：
 *  - 守卫：存在“未开打”比赛 → 400「存在未开打的比赛，请先处理赛程」，不级联、不软删；
 *  - 级联全链：球员批量离队（current=false + 镜像重算为自由球员 + leave 沿革）+ 负责人指派失效
 *    （inactive + 软删 + 记 manager_removed 沿革 + evict）+ 软删球队 + 解散沿革（afterTeamDissolve）；
 *  - 幂等：已解散（deletedAt 非空）直接返回，不触达任何守卫/级联；
 *  - 积分榜历史口径：解散队仍计入且带 dissolved 标记、对手战绩不变；队名走“含已解散”查询。
 *
 * 风格：外部依赖 Mockito mock；PlayerTeamService 用真实实例（playerTeamRepository/teamRepository mock 驱动），
 * 以真实执行 plan/applyMirror/persistPlan 从而验证“离队 + 镜像重算”。不启动 Spring、不连库。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.common.PageResult;
import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.mapper.PlayerStatsMapper;
import com.bsball.mapper.StatsLeadersMapper;
import com.bsball.model.dto.StandingGameRowDTO;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.model.entity.Team;
import com.bsball.model.entity.TeamManager;
import com.bsball.repository.GameRepository;
import com.bsball.repository.LeagueRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.TeamManagerRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
@DisplayName("球队解散规则（批次3b Task3.11 / spec §6.7）")
class TeamDissolveRuleTest {

    private static final long TENANT_ID = 10L;
    private static final long TEAM_ID = 100L;

    @Mock
    private TeamRepository teamRepository;

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
    private PlayerStatsMapper playerStatsMapper;

    @Mock
    private StatsLeadersMapper statsLeadersMapper;

    @Mock
    private TenantProperties tenantProperties;

    private TeamService teamService;

    private StatsService statsService;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        PlayerTeamService realPlayerTeamService = new PlayerTeamService(playerTeamRepository, teamRepository);
        teamService = new TeamService(teamRepository, leagueRepository, accountScopeService, scopeQuerySupport,
                resourceGuard, personnelHistoryRecorder, tenantQueryPolicyService, teamManagerRepository,
                apiPermissionService, gameRepository, playerTeamRepository, playerRepository, realPlayerTeamService);
        statsService = new StatsService(playerStatsMapper, statsLeadersMapper, tenantProperties, teamRepository);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    // ---------------------------------------------------------------- 守卫

    @Test
    @DisplayName("守卫：存在未开打比赛 → 400「存在未开打的比赛，请先处理赛程」，不级联不软删")
    void delete_pendingGame_rejected() {
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, TENANT_ID)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(gameRepository.countPendingGamesByTeamId(TEAM_ID)).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> teamService.delete(TEAM_ID));

        assertEquals(400, ex.getCode());
        assertEquals("存在未开打的比赛，请先处理赛程", ex.getMessage());
        verify(playerTeamRepository, never()).findCurrentEntriesByTeamId(any());
        verify(teamRepository, never()).save(any());
        verify(personnelHistoryRecorder, never()).afterTeamDissolve(any());
    }

    // ---------------------------------------------------------------- 级联全链

    @Test
    @DisplayName("级联：球员离队（current=false+镜像+leave沿革）+ 负责人失效(+evict) + 软删 + 解散沿革")
    void delete_cascade_fullChain() {
        Team team = team(TEAM_ID, TENANT_ID);
        Player player = player(7L, TENANT_ID);
        PlayerTeam entry = entry(500L, 7L, TEAM_ID, true, "9", List.of("P"));
        TeamManager manager = manager(300L, TEAM_ID, 42L, TeamManager.STATUS_ACTIVE);

        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(gameRepository.countPendingGamesByTeamId(TEAM_ID)).thenReturn(0L);
        when(playerTeamRepository.findCurrentEntriesByTeamId(TEAM_ID)).thenReturn(List.of(entry));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(7L)).thenReturn(List.of(entry));
        when(playerTeamRepository.save(any(PlayerTeam.class))).thenAnswer(inv -> inv.getArgument(0));
        when(playerRepository.findByDeletedAtIsNullAndIdIn(List.of(7L))).thenReturn(List.of(player));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        when(teamManagerRepository.findByTeamIdAndStatusAndDeletedAtIsNull(TEAM_ID, TeamManager.STATUS_ACTIVE))
                .thenReturn(List.of(manager));
        when(teamManagerRepository.save(any(TeamManager.class))).thenAnswer(inv -> inv.getArgument(0));
        when(personnelHistoryRecorder.recordPlayerTeamTransitions(any(), any(), any())).thenReturn(null);

        teamService.delete(TEAM_ID);

        // b. 离队 + 镜像重算：唯一当前队被解散 → 自由球员；该队经历置 current=false
        assertNull(player.getTeamId());
        assertNull(player.getNumber());
        assertNull(player.getPositions());
        assertFalse(Boolean.TRUE.equals(entry.getCurrent()));
        verify(playerTeamRepository).save(entry);
        verify(playerRepository).save(player);
        // leave 沿革：before={100} → after={}
        verify(personnelHistoryRecorder).recordPlayerTeamTransitions(eq(player), eq(Set.of(TEAM_ID)), eq(Set.of()));
        // c. 负责人指派失效 + 记失效沿革 + evict
        assertEquals(TeamManager.STATUS_INACTIVE, manager.getStatus());
        assertNotNull(manager.getDeletedAt());
        verify(teamManagerRepository).save(manager);
        verify(personnelHistoryRecorder).recordTeamManagerRemoved(eq(TEAM_ID), eq(TENANT_ID), eq(42L));
        verify(accountScopeService).evictUserScopeCacheAfterCommit(42L);
        // d. 软删球队 + 解散沿革
        assertNotNull(team.getDeletedAt());
        verify(teamRepository).save(team);
        verify(personnelHistoryRecorder).afterTeamDissolve(team);
    }

    @Test
    @DisplayName("级联：沿革返回 join/transfer ID 时回写 currentJoinRecordId（防御分支）")
    void delete_writebackJoinRecordIdWhenRecorderReturnsId() {
        Team team = team(TEAM_ID, TENANT_ID);
        Player player = player(7L, TENANT_ID);
        PlayerTeam entry = entry(500L, 7L, TEAM_ID, true, null, null);

        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(gameRepository.countPendingGamesByTeamId(TEAM_ID)).thenReturn(0L);
        when(playerTeamRepository.findCurrentEntriesByTeamId(TEAM_ID)).thenReturn(List.of(entry));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(7L)).thenReturn(List.of(entry));
        when(playerTeamRepository.save(any(PlayerTeam.class))).thenAnswer(inv -> inv.getArgument(0));
        when(playerRepository.findByDeletedAtIsNullAndIdIn(List.of(7L))).thenReturn(List.of(player));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        when(teamManagerRepository.findByTeamIdAndStatusAndDeletedAtIsNull(TEAM_ID, TeamManager.STATUS_ACTIVE))
                .thenReturn(List.of());
        when(personnelHistoryRecorder.recordPlayerTeamTransitions(any(), any(), any())).thenReturn(999L);

        teamService.delete(TEAM_ID);

        assertEquals(999L, player.getCurrentJoinRecordId());
        // 镜像 save + 回写 save
        verify(playerRepository, times(2)).save(player);
    }

    // ---------------------------------------------------------------- 幂等

    @Test
    @DisplayName("幂等：已解散（deletedAt 非空）直接返回，不触达守卫/级联")
    void delete_alreadyDissolved_idempotent() {
        Team team = team(TEAM_ID, TENANT_ID);
        team.setDeletedAt(LocalDateTime.now());
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));

        teamService.delete(TEAM_ID);

        verify(tenantQueryPolicyService, never()).requiredTenantId();
        verifyNoInteractions(gameRepository);
        verify(playerTeamRepository, never()).findCurrentEntriesByTeamId(any());
        verify(teamRepository, never()).save(any());
        verify(personnelHistoryRecorder, never()).afterTeamDissolve(any());
    }

    // ---------------------------------------------------------------- 积分榜历史口径

    @Test
    @DisplayName("积分榜：解散队仍计入且带 dissolved 标记、对手战绩不变；队名走含已解散查询")
    void getStandings_includesDissolvedTeam_opponentRecordUnchanged() {
        when(tenantProperties.getDefaultId()).thenReturn(TENANT_ID);
        StandingGameRowDTO g1 = game(1L, "2026-01-01", 1L, 2L, 5, 3); // 甲 胜 乙
        StandingGameRowDTO g2 = game(2L, "2026-01-02", 2L, 3L, 1, 4); // 丙(解散) 胜 乙
        when(statsLeadersMapper.selectStandingGames(eq(TENANT_ID), any(), any(), any(), any()))
                .thenReturn(List.of(g1, g2));

        Team t1 = team(1L, TENANT_ID);
        t1.setName("甲队");
        Team t2 = team(2L, TENANT_ID);
        t2.setName("乙队");
        Team t3 = team(3L, TENANT_ID);
        t3.setName("丙队");
        t3.setDeletedAt(LocalDateTime.now());
        when(teamRepository.findByIdInIncludingDissolved(any())).thenReturn(List.of(t1, t2, t3));

        PageResult<Map<String, Object>> result = statsService.getStandings(null, null, null, null, 1, 10);

        assertEquals(3L, result.getTotal());
        Map<Long, Map<String, Object>> byTeam = new LinkedHashMap<>();
        for (Map<String, Object> row : result.getList()) {
            byTeam.put(((Number) row.get("teamId")).longValue(), row);
        }
        // (a) 原队行保留且带标记
        Map<String, Object> row3 = byTeam.get(3L);
        assertNotNull(row3);
        assertEquals("丙队", row3.get("teamName"));
        assertEquals(Boolean.TRUE, row3.get("dissolved"));
        assertEquals(1, row3.get("win"));
        assertEquals(0, row3.get("loss"));
        // (b) 对手（乙）战绩不变：仍含对解散队的负场
        Map<String, Object> row2 = byTeam.get(2L);
        assertEquals(0, row2.get("win"));
        assertEquals(2, row2.get("loss"));
        Map<String, Object> row1 = byTeam.get(1L);
        assertEquals(1, row1.get("win"));
        assertEquals(0, row1.get("loss"));
        assertEquals(Boolean.FALSE, row1.get("dissolved"));
        // 含已解散名称查询被调用
        verify(teamRepository).findByIdInIncludingDissolved(any());
    }

    // ---------------------------------------------------------------- helpers

    private static Team team(long id, long tenantId) {
        Team t = new Team();
        t.setId(id);
        t.setTenantId(tenantId);
        t.setName("球队" + id);
        return t;
    }

    private static Player player(long id, long tenantId) {
        Player p = new Player();
        p.setId(id);
        p.setTenantId(tenantId);
        return p;
    }

    private static PlayerTeam entry(long id, long playerId, long teamId, boolean current, String number,
            List<String> positions) {
        PlayerTeam e = new PlayerTeam();
        e.setId(id);
        e.setPlayerId(playerId);
        e.setTeamId(teamId);
        e.setTenantId(TENANT_ID);
        e.setNumber(number);
        e.setPositionsList(positions);
        e.setCurrent(current);
        e.setSort(0);
        return e;
    }

    private static TeamManager manager(long id, long teamId, long userId, String status) {
        TeamManager m = new TeamManager();
        m.setId(id);
        m.setTenantId(TENANT_ID);
        m.setTeamId(teamId);
        m.setUserId(userId);
        m.setStatus(status);
        return m;
    }

    private static StandingGameRowDTO game(long id, String gameday, long home, long away, int hs, int as) {
        StandingGameRowDTO g = new StandingGameRowDTO();
        g.setId(id);
        g.setGameday(gameday);
        g.setHomeTeamId(home);
        g.setAwayTeamId(away);
        g.setHomeScore(hs);
        g.setAwayScore(as);
        return g;
    }
}
