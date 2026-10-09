package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.model.entity.Team;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.PlayerClaimRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
@DisplayName("球员删除/恢复/移除 规则（2026-10-09 收窄批次）")
class PlayerRemoveRestoreRuleTest {

    private static final long TENANT_ID = 10L;
    private static final long UID = 9L;

    @Mock private PlayerRepository playerRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private PlayerTeamRepository playerTeamRepository;
    @Mock private StatsService statsService;
    @Mock private AccountScopeService accountScopeService;
    @Mock private ScopeQuerySupport scopeQuerySupport;
    @Mock private PersonnelHistoryRecorder personnelHistoryRecorder;
    @Mock private TenantQueryPolicyService tenantQueryPolicyService;
    @Mock private SysConfigService sysConfigService;
    @Mock private PlayerClaimRepository playerClaimRepository;
    @Mock private GamePlayerStatRepository gamePlayerStatRepository;
    @Mock private ApiPermissionService apiPermissionService;

    private ResourceGuard guard;
    private PlayerTeamService playerTeamService;
    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        guard = new ResourceGuard(accountScopeService, null, null, playerRepository, null,
                teamRepository, null, apiPermissionService);
        playerTeamService = new PlayerTeamService(playerTeamRepository, teamRepository);
        playerService = new PlayerService(playerRepository, teamRepository, playerTeamRepository, statsService,
                accountScopeService, scopeQuerySupport, guard, personnelHistoryRecorder, playerTeamService,
                tenantQueryPolicyService, sysConfigService, playerClaimRepository, gamePlayerStatRepository);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    // ------------------------------------------------------------------ restore

    @Test
    @DisplayName("恢复：超管跨租户放行，字段还原（deletedAt/deletedBy 清空、platformOwned 还原）")
    void restore_superAdmin_ok_clearsFlags() {
        Player p = deletedPlayer(5L, TENANT_ID);
        when(playerRepository.findById(5L)).thenReturn(Optional.of(p));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        CurrentUserHolder.set(1L, 0L);
        when(apiPermissionService.isSuperAdmin(1L)).thenReturn(true);

        playerService.restore(5L);

        assertNull(p.getDeletedAt());
        assertNull(p.getDeletedBy());
        assertEquals(Boolean.FALSE, p.getPlatformOwned());
        verify(playerRepository).save(p);
    }

    @Test
    @DisplayName("恢复：租管本租户放行")
    void restore_tenantAdmin_sameTenant_ok() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        Player p = deletedPlayer(5L, TENANT_ID);
        when(playerRepository.findById(5L)).thenReturn(Optional.of(p));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        CurrentUserHolder.set(UID, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(UID)).thenReturn(true);

        playerService.restore(5L);

        assertNull(p.getDeletedAt());
    }

    @Test
    @DisplayName("恢复：受限角色（球队管理员）→ 403 且无写")
    void restore_restrictedRole_forbidden() {
        CurrentUserHolder.set(UID, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(UID)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> playerService.restore(5L));

        assertEquals(403, ex.getCode());
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("恢复：租管跨租户 → 403 且无写")
    void restore_crossTenant_forbidden() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        Player p = deletedPlayer(5L, 99L);
        when(playerRepository.findById(5L)).thenReturn(Optional.of(p));
        CurrentUserHolder.set(UID, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(UID)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> playerService.restore(5L));

        assertEquals(403, ex.getCode());
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("恢复：未处于删除态 → 400「该球员未被删除」")
    void restore_notDeleted_badRequest() {
        Player p = new Player();
        p.setId(5L);
        p.setTenantId(TENANT_ID);
        when(playerRepository.findById(5L)).thenReturn(Optional.of(p));
        CurrentUserHolder.set(UID, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(UID)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> playerService.restore(5L));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("未被删除"));
    }

    // ------------------------------------------------------------------ removeFromTeam

    @Test
    @DisplayName("移除出球队：无比赛记录 → 删经历（软删+removal 沿革+主档镜像清空）")
    void removeFromTeam_noRecords_deletesEntry() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        Player p = player(5L, TENANT_ID, null);
        Team t = team(100L, TENANT_ID, 10L);
        when(playerRepository.findById(5L)).thenReturn(Optional.of(p));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(t));
        PlayerTeam entry = entry(50L, 5L, 100L, TENANT_ID, true, "7");
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(5L)).thenReturn(new ArrayList<>(List.of(entry)));
        when(gamePlayerStatRepository.countValidByPlayerIdAndTeamId(5L, 100L)).thenReturn(0L);
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        CurrentUserHolder.set(9L, TENANT_ID);

        playerService.removeFromTeam(5L, 100L);

        assertNotNull(entry.getDeletedAt(), "无记录经历应软删");
        verify(playerTeamRepository).save(entry);
        verify(personnelHistoryRecorder).recordPlayerTeamEntryRemoval(eq(5L), eq(TENANT_ID), eq(entry));
        verify(personnelHistoryRecorder).recordPlayerTeamTransitions(any(Player.class), eq(Set.of(100L)), eq(Set.<Long>of()));
        assertNull(p.getTeamId(), "主档镜像 teamId 应清空");
    }

    @Test
    @DisplayName("移除出球队：有比赛记录 → 取消当前球队（current=false + leave 沿革），经历保留")
    void removeFromTeam_withRecords_leaveEntry() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        Player p = player(5L, TENANT_ID, null);
        Team t = team(100L, TENANT_ID, 10L);
        when(playerRepository.findById(5L)).thenReturn(Optional.of(p));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(t));
        PlayerTeam entry = entry(50L, 5L, 100L, TENANT_ID, true, "7");
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(5L)).thenReturn(new ArrayList<>(List.of(entry)));
        when(gamePlayerStatRepository.countValidByPlayerIdAndTeamId(5L, 100L)).thenReturn(2L);
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        CurrentUserHolder.set(9L, TENANT_ID);

        playerService.removeFromTeam(5L, 100L);

        assertEquals(Boolean.FALSE, entry.getCurrent(), "有记录应改为离队（current=false）");
        assertNull(entry.getDeletedAt(), "经历不得删除");
        verify(personnelHistoryRecorder, never()).recordPlayerTeamEntryRemoval(anyLong(), anyLong(), any(PlayerTeam.class));
        verify(personnelHistoryRecorder).recordPlayerTeamTransitions(any(Player.class), eq(Set.of(100L)), eq(Set.<Long>of()));
    }

    @Test
    @DisplayName("移除出球队：已认领球员放行（不沿用档案编辑的认领保护）")
    void removeFromTeam_claimedPlayer_allowed() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        Player p = player(5L, TENANT_ID, 77L);
        Team t = team(100L, TENANT_ID, 10L);
        when(playerRepository.findById(5L)).thenReturn(Optional.of(p));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(t));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(5L))
                .thenReturn(new ArrayList<>(List.of(entry(50L, 5L, 100L, TENANT_ID, true, "7"))));
        when(gamePlayerStatRepository.countValidByPlayerIdAndTeamId(5L, 100L)).thenReturn(0L);
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        CurrentUserHolder.set(9L, TENANT_ID);

        assertDoesNotThrow(() -> playerService.removeFromTeam(5L, 100L));
    }

    @Test
    @DisplayName("移除出球队：非管辖球队 → 403 且无写")
    void removeFromTeam_notManaged_forbidden() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(101L)));
        Player p = player(5L, TENANT_ID, null);
        Team t = team(100L, TENANT_ID, 10L);
        when(playerRepository.findById(5L)).thenReturn(Optional.of(p));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(t));
        CurrentUserHolder.set(9L, TENANT_ID);

        BusinessException ex = assertThrows(BusinessException.class, () -> playerService.removeFromTeam(5L, 100L));

        assertEquals(403, ex.getCode());
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("移除出球队：球员当前不在该队（无 current 经历）→ 400")
    void removeFromTeam_notCurrent_badRequest() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        Player p = player(5L, TENANT_ID, null);
        Team t = team(100L, TENANT_ID, 10L);
        when(playerRepository.findById(5L)).thenReturn(Optional.of(p));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(t));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(5L)).thenReturn(new ArrayList<>());
        CurrentUserHolder.set(9L, TENANT_ID);

        BusinessException ex = assertThrows(BusinessException.class, () -> playerService.removeFromTeam(5L, 100L));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不在该球队"));
    }

    // ------------------------------------------------------------------ listDeleted

    @Test
    @DisplayName("已删除列表：受限角色 → 403 且不查库")
    void listDeleted_restrictedRole_forbidden() {
        CurrentUserHolder.set(UID, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(UID)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> playerService.listDeleted(1, 20, null));

        assertEquals(403, ex.getCode());
        verify(playerRepository, never()).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("已删除列表：租管本租户可见（含删除标记字段）")
    void listDeleted_tenantAdmin_ok() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(tenantQueryPolicyService.isGlobalQueryMode()).thenReturn(false);
        Player p = deletedPlayer(5L, TENANT_ID);
        when(playerRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(p)));
        when(playerTeamRepository.findByPlayerIdInAndDeletedAtIsNullOrderBySortAscIdAsc(any())).thenReturn(List.of());
        CurrentUserHolder.set(UID, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(UID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(UID)).thenReturn(true);

        var result = playerService.listDeleted(1, 20, null);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
    }

    @Test
    @DisplayName("移除出球队：超管全局（跨租户）放行")
    void removeFromTeam_superAdmin_global_ok() {
        Player p = player(5L, TENANT_ID, null);
        Team t = team(100L, TENANT_ID, 10L);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.unrestricted());
        when(playerRepository.findById(5L)).thenReturn(Optional.of(p));
        when(teamRepository.findById(100L)).thenReturn(Optional.of(t));
        when(playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(5L))
                .thenReturn(new ArrayList<>(List.of(entry(50L, 5L, 100L, TENANT_ID, true, "7"))));
        when(gamePlayerStatRepository.countValidByPlayerIdAndTeamId(5L, 100L)).thenReturn(0L);
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        CurrentUserHolder.set(1L, 0L);
        when(apiPermissionService.isSuperAdmin(1L)).thenReturn(true);

        assertDoesNotThrow(() -> playerService.removeFromTeam(5L, 100L));
    }

    private static Player player(long id, long tenantId, Long userId) {
        Player p = new Player();
        p.setId(id);
        p.setTenantId(tenantId);
        p.setUserId(userId);
        return p;
    }

    private static Team team(long id, long tenantId, Long leagueId) {
        Team t = new Team();
        t.setId(id);
        t.setTenantId(tenantId);
        t.setLeagueId(leagueId);
        return t;
    }

    private static PlayerTeam entry(long id, long playerId, long teamId, long tenantId, boolean current, String number) {
        PlayerTeam e = new PlayerTeam();
        e.setId(id);
        e.setPlayerId(playerId);
        e.setTeamId(teamId);
        e.setTenantId(tenantId);
        e.setCurrent(current);
        e.setNumber(number);
        e.setSort(0);
        return e;
    }

    private static Player deletedPlayer(long id, long tenantId) {
        Player p = new Player();
        p.setId(id);
        p.setTenantId(tenantId);
        p.setPlatformOwned(Boolean.TRUE);
        p.setDeletedAt(LocalDateTime.now());
        p.setDeletedBy(UID);
        return p;
    }
}
