/*
 * 账号权限重构（批次 3a，Task 3.7）：PlayerClaimService 邀请改造单元测试（Mockito 手工装配）。
 *
 * 覆盖：
 *  ① createInvite 对“无队伍游离球员”不再因 isCurrentlyInTeam 被拒（Step1）；
 *  ② 无档案者凭邀请 + 草稿 → createSelfProfile 被调 + 入队落库（current=true / 镜像 / join 沿革 / currentJoinRecordId）；
 *  ③ 重复接受 → 幂等（不重复建档 / 不重复入队）；
 *  ④ 已有档案（playerId 非空）→ 原认领关联语义（落一条待审 PlayerClaim）；
 *  ⑤ playerId 非空但跨租户 → 拒绝；
 *  ⑥ 无目标球员且无档案且无草稿 → 400；
 *  ⑦ 存量档案（不在受邀队）凭开放邀请 → 直接入队（不建档，固化 I-2 新语义）；
 *  ⑧ 幂等早返回不消费令牌（usedCount 不变）。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.config.AccountProperties;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.PlayerTeamEntryDto;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerClaim;
import com.bsball.model.entity.PlayerClaimInvite;
import com.bsball.model.entity.Team;
import com.bsball.repository.PlayerClaimInviteRepository;
import com.bsball.repository.PlayerClaimRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.SysUserRepository;
import com.bsball.repository.TeamManagerRepository;
import com.bsball.repository.TeamRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerClaimService：邀请改造（自助建档入队 / 原认领关联 / 幂等）")
@SuppressWarnings("unchecked")
class PlayerClaimServiceTest {

    private static final long CLAIMANT_ID = 3L;
    private static final long CREATOR_ID = 7L;
    private static final long TEAM_ID = 5L;
    private static final long PLAYER_ID = 1L;
    private static final long TENANT_ID = 9L;

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

    private PlayerClaimService service;

    @BeforeEach
    void setUp() {
        service = new PlayerClaimService(accountProperties, apiPermissionService, playerClaimRepository,
                playerClaimInviteRepository, playerRepository, teamRepository, teamManagerRepository,
                sysUserRepository, playerTeamService, accountScopeService, resourceGuard, playerService,
                personnelHistoryRecorder);
    }

    // ------------------------------------------------------------------ Step1：createInvite

    @Test
    @DisplayName("① 创建邀请：无队伍游离球员（同租户）不再因 isCurrentlyInTeam 被拒 → 正常创建邀请")
    void createInvite_playerWithoutTeam_createsInvite() {
        when(apiPermissionService.isSuperAdmin(CREATOR_ID)).thenReturn(true);
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, TENANT_ID)));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player(PLAYER_ID, TENANT_ID, null)));
        when(accountProperties.getInviteDefaultExpireHours()).thenReturn(72);
        when(accountProperties.getInviteDefaultMaxUses()).thenReturn(1);
        when(playerClaimInviteRepository.save(any(PlayerClaimInvite.class))).thenAnswer(invocation -> {
            PlayerClaimInvite invite = invocation.getArgument(0);
            if (invite.getId() == null) {
                invite.setId(50L);
            }
            return invite;
        });

        Map<String, Object> out = service.createInvite(CREATOR_ID, TEAM_ID, PLAYER_ID, null, null, "备注");

        assertNotNull(out.get("token"));
        assertEquals(50L, ((Number) out.get("id")).longValue());
        assertEquals(TEAM_ID, ((Number) out.get("teamId")).longValue());
        assertEquals(PLAYER_ID, ((Number) out.get("playerId")).longValue());
        // 硬校验已删除：不再查询“球员是否在当前队”
        verify(playerTeamService, never()).isCurrentlyInTeam(any(), any());
    }

    @Test
    @DisplayName("创建邀请：playerId 非空但跨租户 → 400")
    void createInvite_crossTenantPlayer_rejected() {
        when(apiPermissionService.isSuperAdmin(CREATOR_ID)).thenReturn(true);
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team(TEAM_ID, TENANT_ID)));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player(PLAYER_ID, 8L, null)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createInvite(CREATOR_ID, TEAM_ID, PLAYER_ID, null, null, null));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不属于同一租户"));
    }

    // ------------------------------------------------------------------ Step2：注册入队

    @Test
    @DisplayName("② 无档案者凭邀请 + 草稿 → 建档 + 入队落库（current=true / 镜像 / join 沿革 / currentJoinRecordId）")
    void claimOrRegisterViaInvite_noProfileWithDraft_registersAndJoins() {
        PlayerClaimInvite invite = activeInvite(TEAM_ID, null, 10);
        when(playerClaimInviteRepository.findByTokenAndDeletedAtIsNull("tok")).thenReturn(Optional.of(invite));
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(CLAIMANT_ID)).thenReturn(Optional.empty());
        Player draft = new Player();
        draft.setName("新秀");
        Player created = player(PLAYER_ID, TENANT_ID, CLAIMANT_ID);
        when(playerService.createSelfProfile(CLAIMANT_ID, draft)).thenReturn(created);
        PlayerTeamService.PlayerTeamSyncPlan plan = new PlayerTeamService.PlayerTeamSyncPlan(
                List.of(), List.of(), List.of(), null, Set.of(), Set.of(TEAM_ID), List.of());
        when(playerTeamService.plan(any(), any(), eq(true))).thenReturn(plan);
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(personnelHistoryRecorder.recordPlayerTeamTransitions(any(), any(), any())).thenReturn(77L);

        Map<String, Object> out = service.claimOrRegisterViaInvite(CLAIMANT_ID, "tok", null, draft);

        assertEquals("registered", out.get("mode"));
        assertEquals(PLAYER_ID, ((Number) out.get("playerId")).longValue());
        assertEquals(TEAM_ID, ((Number) out.get("teamId")).longValue());
        // 建档链复用
        verify(playerService).createSelfProfile(CLAIMANT_ID, draft);
        // 入队经历 current=true 且指向邀请球队
        ArgumentCaptor<List<PlayerTeamEntryDto>> entriesCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerTeamService).plan(eq(created), entriesCaptor.capture(), eq(true));
        List<PlayerTeamEntryDto> entries = entriesCaptor.getValue();
        assertEquals(1, entries.size());
        assertEquals(TEAM_ID, entries.get(0).teamId().longValue());
        assertEquals(Boolean.TRUE, entries.get(0).current());
        // 镜像回写 + 落库
        verify(playerTeamService).applyMirror(created, plan);
        verify(playerTeamService).persistPlan(PLAYER_ID, plan);
        // join 沿革 + currentJoinRecordId 回写
        verify(personnelHistoryRecorder).recordPlayerTeamTransitions(created, Set.of(), Set.of(TEAM_ID));
        assertEquals(77L, created.getCurrentJoinRecordId());
        // 入队后失效范围缓存
        verify(accountScopeService).evictUserScopeCacheAfterCommit(CLAIMANT_ID);
        // 写入路径（建档+入队）→ 消费邀请：usedCount +1
        assertEquals(1, invite.getUsedCount());
        verify(playerClaimInviteRepository).save(invite);
    }

    @Test
    @DisplayName("③ 重复接受 → 幂等：已在队者不重复建档 / 不重复入队")
    void claimOrRegisterViaInvite_repeatAccept_isIdempotent() {
        PlayerClaimInvite invite = activeInvite(TEAM_ID, null, 10);
        when(playerClaimInviteRepository.findByTokenAndDeletedAtIsNull("tok")).thenReturn(Optional.of(invite));
        Player existing = player(PLAYER_ID, TENANT_ID, CLAIMANT_ID);
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(CLAIMANT_ID)).thenReturn(Optional.of(existing));
        when(playerTeamService.currentTeamIds(PLAYER_ID)).thenReturn(Set.of(TEAM_ID));

        Map<String, Object> out = service.claimOrRegisterViaInvite(CLAIMANT_ID, "tok", null, new Player());

        assertEquals("registered", out.get("mode"));
        assertEquals(PLAYER_ID, ((Number) out.get("playerId")).longValue());
        assertEquals(TEAM_ID, ((Number) out.get("teamId")).longValue());
        // 不重复建档 / 不重复入队 / 不重复写沿革
        verify(playerService, never()).createSelfProfile(any(), any());
        verify(playerTeamService, never()).plan(any(), any(), eq(true));
        verify(playerTeamService, never()).persistPlan(any(), any());
        verify(personnelHistoryRecorder, never()).recordPlayerTeamTransitions(any(), any(), any());
    }

    // ------------------------------------------------------------------ Step2：原认领关联

    @Test
    @DisplayName("④ 已有档案（playerId 非空）→ 原认领关联语义：落一条待审 PlayerClaim")
    void claimOrRegisterViaInvite_existingPlayer_createsClaim() {
        PlayerClaimInvite invite = activeInvite(TEAM_ID, PLAYER_ID, 10);
        when(playerClaimInviteRepository.findByTokenAndDeletedAtIsNull("tok")).thenReturn(Optional.of(invite));
        Player target = player(PLAYER_ID, TENANT_ID, null);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(target));
        when(playerClaimRepository.existsByUserIdAndStatusAndDeletedAtIsNull(CLAIMANT_ID, "approved")).thenReturn(false);
        when(playerClaimRepository.existsByUserIdAndStatusAndDeletedAtIsNull(CLAIMANT_ID, "pending")).thenReturn(false);
        when(playerClaimRepository.findTopByUserIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(CLAIMANT_ID, "cancelled"))
                .thenReturn(Optional.empty());
        when(playerClaimRepository.existsByPlayerIdAndStatusAndDeletedAtIsNull(PLAYER_ID, "pending")).thenReturn(false);
        when(playerTeamService.currentTeamIds(PLAYER_ID)).thenReturn(Set.of());
        when(playerClaimRepository.save(any(PlayerClaim.class))).thenAnswer(invocation -> {
            PlayerClaim claim = invocation.getArgument(0);
            if (claim.getId() == null) {
                claim.setId(88L);
            }
            return claim;
        });

        Map<String, Object> out = service.claimOrRegisterViaInvite(CLAIMANT_ID, "tok", null, null);

        assertEquals("claim", out.get("mode"));
        assertEquals(88L, ((Number) out.get("claimId")).longValue());
        assertEquals("pending", out.get("status"));
        verify(playerClaimRepository).save(any(PlayerClaim.class));
        verify(playerService, never()).createSelfProfile(any(), any());
    }

    @Test
    @DisplayName("⑤ playerId 非空但跨租户 → 400")
    void claimOrRegisterViaInvite_crossTenantPlayer_rejected() {
        PlayerClaimInvite invite = activeInvite(TEAM_ID, PLAYER_ID, 10);
        when(playerClaimInviteRepository.findByTokenAndDeletedAtIsNull("tok")).thenReturn(Optional.of(invite));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player(PLAYER_ID, 8L, null)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.claimOrRegisterViaInvite(CLAIMANT_ID, "tok", null, null));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不属于同一租户"));
        verify(playerClaimRepository, never()).save(any(PlayerClaim.class));
        // 校验失败早抛，无写入 → 不消费令牌
        assertEquals(0, invite.getUsedCount());
    }

    @Test
    @DisplayName("⑥ targetPlayerId 与 draft 均空 → 400 请指定要认领的球员或提供建档信息")
    void claimOrRegisterViaInvite_noTargetNoDraft_rejected() {
        PlayerClaimInvite invite = activeInvite(TEAM_ID, null, 10);
        when(playerClaimInviteRepository.findByTokenAndDeletedAtIsNull("tok")).thenReturn(Optional.of(invite));
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(CLAIMANT_ID)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.claimOrRegisterViaInvite(CLAIMANT_ID, "tok", null, null));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("请指定要认领的球员或提供建档信息"));
        verify(playerService, never()).createSelfProfile(any(), any());
        // 抛错前无写入 → 不消费令牌
        assertEquals(0, invite.getUsedCount());
    }

    @Test
    @DisplayName("⑦ 存量档案（不在受邀队）凭开放邀请 → 直接入队（applyInviteJoin 生效、不建档）")
    void claimOrRegisterViaInvite_existingProfileNotInTeam_joinsExistingProfile() {
        PlayerClaimInvite invite = activeInvite(TEAM_ID, null, 10);
        when(playerClaimInviteRepository.findByTokenAndDeletedAtIsNull("tok")).thenReturn(Optional.of(invite));
        Player existing = player(PLAYER_ID, TENANT_ID, CLAIMANT_ID);
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(CLAIMANT_ID)).thenReturn(Optional.of(existing));
        when(playerTeamService.currentTeamIds(PLAYER_ID)).thenReturn(Set.of()); // 不在受邀队
        PlayerTeamService.PlayerTeamSyncPlan plan = new PlayerTeamService.PlayerTeamSyncPlan(
                List.of(), List.of(), List.of(), null, Set.of(), Set.of(TEAM_ID), List.of());
        when(playerTeamService.plan(any(), any(), eq(true))).thenReturn(plan);
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(personnelHistoryRecorder.recordPlayerTeamTransitions(any(), any(), any())).thenReturn(88L);

        Map<String, Object> out = service.claimOrRegisterViaInvite(CLAIMANT_ID, "tok", null, null);

        assertEquals("registered", out.get("mode"));
        assertEquals(PLAYER_ID, ((Number) out.get("playerId")).longValue());
        assertEquals(TEAM_ID, ((Number) out.get("teamId")).longValue());
        // 存量档案直接入队：绝不建档
        verify(playerService, never()).createSelfProfile(any(), any());
        // 入队落库：plan(current=true) + 镜像 + persistPlan + join 沿革 + currentJoinRecordId
        ArgumentCaptor<List<PlayerTeamEntryDto>> entriesCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerTeamService).plan(eq(existing), entriesCaptor.capture(), eq(true));
        assertEquals(TEAM_ID, entriesCaptor.getValue().get(0).teamId().longValue());
        assertEquals(Boolean.TRUE, entriesCaptor.getValue().get(0).current());
        verify(playerTeamService).applyMirror(existing, plan);
        verify(playerTeamService).persistPlan(PLAYER_ID, plan);
        verify(personnelHistoryRecorder).recordPlayerTeamTransitions(existing, Set.of(), Set.of(TEAM_ID));
        assertEquals(88L, existing.getCurrentJoinRecordId());
        verify(accountScopeService).evictUserScopeCacheAfterCommit(CLAIMANT_ID);
        // 写入路径（存量入队）→ 消费邀请
        assertEquals(1, invite.getUsedCount());
        verify(playerClaimInviteRepository).save(invite);
    }

    @Test
    @DisplayName("⑧ 幂等早返回（已在受邀队）→ 不消费令牌（usedCount 不变）")
    void claimOrRegisterViaInvite_idempotentEarlyReturn_doesNotConsumeInvite() {
        PlayerClaimInvite invite = activeInvite(TEAM_ID, null, 10);
        when(playerClaimInviteRepository.findByTokenAndDeletedAtIsNull("tok")).thenReturn(Optional.of(invite));
        Player existing = player(PLAYER_ID, TENANT_ID, CLAIMANT_ID);
        when(playerRepository.findFirstByUserIdAndDeletedAtIsNull(CLAIMANT_ID)).thenReturn(Optional.of(existing));
        when(playerTeamService.currentTeamIds(PLAYER_ID)).thenReturn(Set.of(TEAM_ID));

        Map<String, Object> out = service.claimOrRegisterViaInvite(CLAIMANT_ID, "tok", null, new Player());

        assertEquals("registered", out.get("mode"));
        assertEquals(PLAYER_ID, ((Number) out.get("playerId")).longValue());
        // 无写入 → 不消费令牌、不落库邀请
        assertEquals(0, invite.getUsedCount());
        verify(playerClaimInviteRepository, never()).save(any(PlayerClaimInvite.class));
        verify(playerService, never()).createSelfProfile(any(), any());
        verify(playerTeamService, never()).plan(any(), any(), eq(true));
    }

    // ------------------------------------------------------------------ 辅助

    private PlayerClaimInvite activeInvite(Long teamId, Long playerId, int maxUses) {
        PlayerClaimInvite invite = new PlayerClaimInvite();
        invite.setId(9L);
        invite.setStatus("active");
        invite.setTeamId(teamId);
        invite.setTenantId(TENANT_ID);
        invite.setPlayerId(playerId);
        invite.setExpiresAt(LocalDateTime.now().plusDays(1));
        invite.setUsedCount(0);
        invite.setMaxUses(maxUses);
        return invite;
    }

    private static Player player(Long id, Long tenantId, Long userId) {
        Player p = new Player();
        p.setId(id);
        p.setTenantId(tenantId);
        p.setUserId(userId);
        return p;
    }

    private static Team team(long id, Long tenantId) {
        Team t = new Team();
        t.setId(id);
        t.setTenantId(tenantId);
        t.setName("球队" + id);
        return t;
    }
}
