package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.common.PageResult;
import com.bsball.config.AccountProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerClaimInvite;
import com.bsball.model.entity.Team;
import com.bsball.repository.PlayerClaimInviteRepository;
import com.bsball.repository.PlayerClaimRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.SysUserRepository;
import com.bsball.repository.TeamManagerRepository;
import com.bsball.repository.TeamRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * 电子秩序册（Issue #310）批 4 规则测试：入队邀请管理与全量球员候选。
 * 覆盖：候选搜索（守卫/装配/认领状态/现属球队）、createInvite 守卫（可管理该球队）、邀请列表/撤销（跨队 404/幂等）。
 */
@ExtendWith(MockitoExtension.class)
class PlayerInviteAdminTest {
    private static final long TENANT_ID = 9L;
    private static final long TEAM_ID = 5L;
    private static final long CREATOR_ID = 7L;

    @Mock
    private AccountProperties accountProperties;
    @Mock
    private ApiPermissionService apiPermissionService;
    @Mock
    private PlayerClaimRepository playerClaimRepository;
    @Mock
    private PlayerClaimInviteRepository playerClaimInviteRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamManagerRepository teamManagerRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private PlayerTeamService playerTeamService;
    @Mock
    private AccountScopeService accountScopeService;
    @Mock
    private ResourceGuard resourceGuard;
    @Mock
    private PlayerService playerService;
    @Mock
    private PersonnelHistoryRecorder personnelHistoryRecorder;
    @Mock
    private PlayerTeamRepository playerTeamRepository;

    private PlayerClaimService service;

    @BeforeEach
    void setUp() {
        service = new PlayerClaimService(accountProperties, apiPermissionService, playerClaimRepository,
                playerClaimInviteRepository, playerRepository, teamRepository, teamManagerRepository,
                sysUserRepository, playerTeamService, accountScopeService, resourceGuard, playerService,
                personnelHistoryRecorder, playerTeamRepository);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    private static Team team(String name) {
        Team t = new Team();
        t.setId(Long.valueOf(TEAM_ID));
        t.setTenantId(Long.valueOf(TENANT_ID));
        t.setName(name);
        return t;
    }

    private static Player player(long id, String name, Long userId) {
        Player p = new Player();
        p.setId(Long.valueOf(id));
        p.setTenantId(Long.valueOf(TENANT_ID));
        p.setName(name);
        p.setNumber("7");
        p.setUserId(userId);
        return p;
    }

    private static PlayerClaimInvite invite(long id, long teamId, String status, Long playerId) {
        PlayerClaimInvite i = new PlayerClaimInvite();
        i.setId(Long.valueOf(id));
        i.setTenantId(Long.valueOf(TENANT_ID));
        i.setTeamId(Long.valueOf(teamId));
        i.setPlayerId(playerId);
        i.setToken("tok-" + id);
        i.setStatus(status);
        i.setMaxUses(Integer.valueOf(1));
        i.setUsedCount(Integer.valueOf(0));
        return i;
    }

    @Test
    @DisplayName("候选搜索：本租户全量球员装配认领状态/现属球队/已在本队，并过球队守卫")
    void candidates_assemblesRows() {
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team("雷霆队")));
        when(playerRepository.findAll(ArgumentMatchers.<Specification<Player>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<Player>(List.of(player(1L, "张三", null), player(2L, "李四", 88L)), Pageable.ofSize(20), 2L));
        when(playerTeamService.currentTeamIdsByPlayerIds(List.of(1L, 2L)))
                .thenReturn(Map.of(1L, Set.of(TEAM_ID), 2L, Set.of()));
        when(teamRepository.findAllById(any())).thenReturn(List.of(team("雷霆队")));

        PageResult<Map<String, Object>> out = service.inviteCandidates(TEAM_ID, "张", 1, 20);

        assertEquals(2L, out.getTotal());
        Map<String, Object> row1 = out.getList().get(0);
        assertEquals("张三", row1.get("name"));
        assertEquals(Boolean.FALSE, row1.get("claimed"));
        assertEquals(Boolean.TRUE, row1.get("inThisTeam"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> teams = (List<Map<String, Object>>) row1.get("currentTeams");
        assertEquals("雷霆队", teams.get(0).get("teamName"));
        Map<String, Object> row2 = out.getList().get(1);
        assertEquals(Boolean.TRUE, row2.get("claimed"));
        assertEquals(Boolean.FALSE, row2.get("inThisTeam"));
        verify(resourceGuard).assertCanManageTeam(TEAM_ID);
    }

    @Test
    @DisplayName("候选搜索：分页参数钳制（page<=0→1、pageSize<=0→20、上限 100）且按 id 稳定排序")
    void candidates_clampsPageParams() {
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team("雷霆队")));
        when(playerRepository.findAll(ArgumentMatchers.<Specification<Player>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<Player>(List.of()));

        service.inviteCandidates(TEAM_ID, null, 0, 0);
        service.inviteCandidates(TEAM_ID, null, 2, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(playerRepository, org.mockito.Mockito.times(2))
                .findAll(ArgumentMatchers.<Specification<Player>>any(), captor.capture());
        List<Pageable> pages = captor.getAllValues();
        assertEquals(0, pages.get(0).getPageNumber());
        assertEquals(20, pages.get(0).getPageSize());
        assertEquals(1, pages.get(1).getPageNumber());
        assertEquals(100, pages.get(1).getPageSize());
        assertEquals("id", pages.get(0).getSort().iterator().next().getProperty());
    }

    @Test
    @DisplayName("候选搜索：跨租户球队（非超管）→ 404，零查询")
    void candidates_crossTenant_notFound() {
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team("雷霆队")));
        CurrentUserHolder.set(77L, 5L);
        when(apiPermissionService.isSuperAdmin(77L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.inviteCandidates(TEAM_ID, null, 1, 20));

        assertEquals(404, ex.getCode());
        verify(playerRepository, never()).findAll(ArgumentMatchers.<Specification<Player>>any(), any(Pageable.class));
    }

    @Test
    @DisplayName("候选搜索：守卫拒绝（403）时零查询")
    void candidates_guardForbidden_noQuery() {
        doThrow(new BusinessException(403, "无权管理该球队")).when(resourceGuard).assertCanManageTeam(TEAM_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.inviteCandidates(TEAM_ID, null, 1, 20));

        assertEquals(403, ex.getCode());
        verify(playerRepository, never()).findAll(ArgumentMatchers.<Specification<Player>>any(), any(Pageable.class));
    }

    @Test
    @DisplayName("创建邀请：守卫=可管理该球队（Issue #318 起无联盟代管分支）")
    void createInvite_usesManageGuard() {
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team("雷霆队")));
        when(accountProperties.getInviteDefaultExpireHours()).thenReturn(72);
        when(accountProperties.getInviteDefaultMaxUses()).thenReturn(1);

        Map<String, Object> out = service.createInvite(CREATOR_ID, TEAM_ID, null, null, null, "欢迎加入");

        assertNotNull(out.get("token"));
        assertEquals(TEAM_ID, out.get("teamId"));
        verify(resourceGuard).assertCanManageTeam(TEAM_ID);
        verify(playerClaimInviteRepository).save(any(PlayerClaimInvite.class));
    }

    @Test
    @DisplayName("撤销：跨队/不存在 → 404")
    void revoke_crossTeam_notFound() {
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team("雷霆队")));
        when(playerClaimInviteRepository.findById(7L))
                .thenReturn(Optional.of(invite(7L, 99L, PlayerClaimInvite.STATUS_ACTIVE, null)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.revokeInvite(TEAM_ID, 7L));

        assertEquals(404, ex.getCode());
        verify(playerClaimInviteRepository, never()).save(any(PlayerClaimInvite.class));
    }

    @Test
    @DisplayName("撤销：已撤销幂等返回（无写）")
    void revoke_idempotent() {
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team("雷霆队")));
        PlayerClaimInvite revoked = invite(8L, TEAM_ID, PlayerClaimInvite.STATUS_REVOKED, null);
        when(playerClaimInviteRepository.findById(8L)).thenReturn(Optional.of(revoked));

        Map<String, Object> out = service.revokeInvite(TEAM_ID, 8L);

        assertEquals(PlayerClaimInvite.STATUS_REVOKED, out.get("status"));
        verify(playerClaimInviteRepository, never()).save(any(PlayerClaimInvite.class));
    }

    @Test
    @DisplayName("撤销：生效邀请置 revoked 并落库（旧链接随之失效）")
    void revoke_setsRevoked() {
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team("雷霆队")));
        PlayerClaimInvite active = invite(9L, TEAM_ID, PlayerClaimInvite.STATUS_ACTIVE, null);
        when(playerClaimInviteRepository.findById(9L)).thenReturn(Optional.of(active));

        Map<String, Object> out = service.revokeInvite(TEAM_ID, 9L);

        assertEquals(PlayerClaimInvite.STATUS_REVOKED, out.get("status"));
        assertEquals(PlayerClaimInvite.STATUS_REVOKED, active.getStatus());
        verify(playerClaimInviteRepository).save(active);
    }

    @Test
    @DisplayName("邀请列表：按队倒序 + 绑定球员名批量解析 + 守卫生效")
    void listInvites_assemblesPlayerName() {
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team("雷霆队")));
        PlayerClaimInvite bound = invite(11L, TEAM_ID, PlayerClaimInvite.STATUS_ACTIVE, 1L);
        PlayerClaimInvite open = invite(10L, TEAM_ID, PlayerClaimInvite.STATUS_ACTIVE, null);
        when(playerClaimInviteRepository.findByTeamIdAndDeletedAtIsNullOrderByIdDesc(TEAM_ID))
                .thenReturn(List.of(bound, open));
        when(playerRepository.findAllById(List.of(1L))).thenReturn(List.of(player(1L, "张三", null)));

        List<Map<String, Object>> out = service.listInvites(TEAM_ID);

        assertEquals(2, out.size());
        assertEquals("张三", out.get(0).get("playerName"));
        assertTrue(out.get(1).get("playerName") == null);
        verify(resourceGuard).assertCanManageTeam(TEAM_ID);
    }

    @Test
    @DisplayName("邀请列表/撤销：守卫拒绝（403）时零查询")
    void listAndRevoke_guardForbidden() {
        doThrow(new BusinessException(403, "无权管理该球队")).when(resourceGuard).assertCanManageTeam(TEAM_ID);

        assertEquals(403, assertThrows(BusinessException.class, () -> service.listInvites(TEAM_ID)).getCode());
        assertEquals(403, assertThrows(BusinessException.class, () -> service.revokeInvite(TEAM_ID, 9L)).getCode());

        verify(playerClaimInviteRepository, never()).findByTeamIdAndDeletedAtIsNullOrderByIdDesc(TEAM_ID);
        verify(playerClaimInviteRepository, never()).findById(9L);
    }

    @Test
    @DisplayName("撤销后旧链接失效：存量令牌经 getInvitePublic 被拒（邀请链接已失效）")
    void revokedToken_rejectedOnLinkUse() {
        PlayerClaimInvite revoked = invite(9L, TEAM_ID, PlayerClaimInvite.STATUS_REVOKED, null);
        when(playerClaimInviteRepository.findByTokenAndDeletedAtIsNull("tok-9")).thenReturn(Optional.of(revoked));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getInvitePublic("tok-9"));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("失效"));
    }
}
