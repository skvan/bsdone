package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.common.PageResult;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.PlayerTeamEntryDto;
import com.bsball.model.dto.TeamPlayerOptionDto;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.model.entity.Team;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.PlayerClaimRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.PlayerTeamService.PlayerTeamSyncPlan;
import com.bsball.service.query.ScopeQuerySupport;
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
@DisplayName("PlayerService：多队经历集成（计划/镜像/权限/选择器）")
class PlayerServiceTeamSyncTest {

    private static final long TENANT = 9L;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private PlayerTeamRepository playerTeamRepository;

    @Mock
    private StatsService statsService;

    @Mock
    private AccountScopeService accountScopeService;

    @Mock
    private ScopeQuerySupport scopeQuerySupport;

    @Mock
    private ResourceGuard resourceGuard;

    @Mock
    private PersonnelHistoryRecorder personnelHistoryRecorder;

    @Mock
    private PlayerTeamService playerTeamService;

    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private PlayerClaimRepository playerClaimRepository;

    @Mock
    private GamePlayerStatRepository gamePlayerStatRepository;

    private PlayerService service;

    @BeforeEach
    void setUp() {
        service = new PlayerService(playerRepository, teamRepository, playerTeamRepository, statsService,
                accountScopeService, scopeQuerySupport, resourceGuard, personnelHistoryRecorder,
                playerTeamService, tenantQueryPolicyService, sysConfigService, playerClaimRepository,
                gamePlayerStatRepository);
    }

    @Test
    @DisplayName("创建：按 teamEntries 生成计划、回写镜像、落库并记录流转")
    void create_withTeamEntries_plansMirrorsPersistsAndRecordsTransitions() {
        Player entity = new Player();
        entity.setName("张三");
        entity.setTeamId(5L);
        entity.setTeamEntries(List.of(new PlayerTeamEntryDto(null, 5L, null, "7", List.of("P"), true, null)));
        PlayerTeam entry = new PlayerTeam();
        entry.setTeamId(5L);
        entry.setCurrent(true);
        PlayerTeamSyncPlan plan = new PlayerTeamSyncPlan(List.of(entry), List.of(), List.of(), entry, Set.of(),
                Set.of(5L), List.of(entry));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        when(teamRepository.findById(5L)).thenReturn(Optional.of(team(5L, TENANT)));
        when(teamRepository.existsById(5L)).thenReturn(true);
        when(playerTeamService.plan(entity, entity.getTeamEntries(), true)).thenReturn(plan);
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> {
            Player arg = invocation.getArgument(0);
            if (arg.getId() == null) {
                arg.setId(1L);
            }
            return arg;
        });
        when(personnelHistoryRecorder.recordPlayerTeamTransitions(any(Player.class), any(), any())).thenReturn(100L);

        Player result = service.create(entity);

        assertEquals(1L, result.getId().longValue());
        assertEquals(100L, result.getCurrentJoinRecordId().longValue());
        verify(playerTeamService).applyMirror(entity, plan);
        verify(playerTeamService).persistPlan(1L, plan);
        verify(personnelHistoryRecorder).recordPlayerTeamTransitions(entity, Set.of(), Set.of(5L));
        verify(playerRepository, times(2)).save(entity);
        verify(playerTeamService).attachEntries(entity);
    }

    @Test
    @DisplayName("更新：未提供 teamEntries 时走旧字段兼容语义，档案事件保留")
    void update_legacyPayload_plansLegacyAndKeepsProfileEvents() {
        Player existing = player(1L, TENANT);
        existing.setTeamId(5L);
        existing.setName("张三");
        existing.setStatus("active");
        Player body = new Player();
        body.setName("张三");
        body.setTeamId(5L);
        body.setNumber("7");
        PlayerTeamSyncPlan plan = new PlayerTeamSyncPlan(List.of(), List.of(), List.of(), null, Set.of(5L),
                Set.of(5L), List.of());
        when(playerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        when(teamRepository.findById(5L)).thenReturn(Optional.of(team(5L, TENANT)));
        when(teamRepository.existsById(5L)).thenReturn(true);
        when(playerTeamService.plan(body, null, false)).thenReturn(plan);
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(personnelHistoryRecorder.recordPlayerTeamTransitions(any(Player.class), any(), any())).thenReturn(null);

        Player result = service.update(1L, body);

        assertSame(body, result);
        verify(playerTeamService).applyMirror(body, plan);
        verify(playerTeamService).persistPlan(1L, plan);
        verify(personnelHistoryRecorder).afterPlayerUpdate(any(Player.class), any(Player.class));
        verify(playerRepository).save(body);
        verify(playerTeamService).attachEntries(body);
    }

    @Test
    @DisplayName("详情权限：受限用户且当前经历不在授权球队内 → 403")
    void get_scopedUserOutsideScope_forbidden() {
        Player p = player(1L, TENANT);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(p));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        when(scopeQuerySupport.visibleTeamIds(any(), eq(TENANT))).thenReturn(List.of(5L));
        when(playerTeamService.countCurrentEntriesInTeams(1L, List.of(5L))).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.get(1L));

        assertEquals(403, ex.getCode());
        verify(playerTeamService, never()).attachEntries(any(Player.class));
    }

    @Test
    @DisplayName("详情权限：受限用户且当前经历落在授权球队内 → 返回并填充经历")
    void get_scopedUserInScope_returnsWithEntries() {
        Player p = player(1L, TENANT);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(p));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        when(scopeQuerySupport.visibleTeamIds(any(), eq(TENANT))).thenReturn(List.of(5L));
        when(playerTeamService.countCurrentEntriesInTeams(1L, List.of(5L))).thenReturn(1L);

        Player result = service.get(1L);

        assertSame(p, result);
        verify(playerTeamService).attachEntries(p);
    }

    @Test
    @DisplayName("详情权限：不受限用户不查询经历计数")
    void get_unrestricted_doesNotQueryScopeCount() {
        Player p = player(1L, TENANT);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(p));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        when(scopeQuerySupport.visibleTeamIds(any(), eq(TENANT))).thenReturn(null);

        service.get(1L);

        verify(playerTeamService).attachEntries(p);
        verify(playerTeamService, never()).countCurrentEntriesInTeams(any(), any());
    }

    @Test
    @DisplayName("按ID列表查询：受限用户按“当前经历球队”过滤，无经历球员沿用旧可见性")
    void list_byIds_scopedFiltersUsingCurrentTeams() {
        List<Long> ids = List.of(1L, 2L, 3L);
        Player p1 = player(1L, TENANT);
        Player p2 = player(2L, TENANT);
        Player p3 = player(3L, TENANT);
        when(tenantQueryPolicyService.isGlobalQueryMode()).thenReturn(false);
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        when(scopeQuerySupport.visibleTeamIds(any(), eq(TENANT))).thenReturn(List.of(5L));
        when(playerRepository.findByDeletedAtIsNullAndIdIn(ids)).thenReturn(List.of(p1, p2, p3));
        when(playerTeamService.currentTeamIdsByPlayerIds(List.of(1L, 2L, 3L)))
                .thenReturn(Map.of(2L, Set.of(5L), 3L, Set.of(9L)));

        PageResult<Player> result = service.list(null, null, null, null, null, ids, null, null, null, null, null, null, null, null);

        assertEquals(2L, result.getTotal());
        assertEquals(List.of(1L, 2L), result.getList().stream().map(Player::getId).toList());
        verify(playerTeamService).attachEntries(List.of(p1, p2));
    }

    @Test
    @DisplayName("阵容选择器：返回当前注册球员并按注册段映射背号与守备位置")
    void listTeamPlayerOptions_mapsEntryRows() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        when(scopeQuerySupport.visibleTeamIds(any(), eq(TENANT))).thenReturn(null);
        Object[] row = new Object[]{2L, "李四", "9", "[\"C\",\"1B\"]", "R", "L", "active"};
        when(playerTeamRepository.findTeamPlayerOptionFields(TENANT, 5L)).thenReturn(List.<Object[]>of(row));

        List<TeamPlayerOptionDto> out = service.listTeamPlayerOptions(5L);

        assertEquals(1, out.size());
        TeamPlayerOptionDto dto = out.get(0);
        assertEquals(2L, dto.id().longValue());
        assertEquals("李四", dto.name());
        assertEquals("9", dto.number());
        assertEquals(List.of("C", "1B"), dto.positions());
        assertEquals("R", dto.batHand());
        assertEquals("L", dto.throwHand());
        assertEquals("active", dto.status());
    }

    private static Player player(Long id, long tenantId) {
        Player p = new Player();
        p.setId(id);
        p.setTenantId(tenantId);
        return p;
    }

    private static Team team(long id, long tenantId) {
        Team t = new Team();
        t.setId(id);
        t.setTenantId(tenantId);
        t.setName("球队" + id);
        return t;
    }
}
