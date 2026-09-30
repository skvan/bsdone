package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.config.AccountProperties;
import com.bsball.exception.BusinessException;
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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerClaimService：多队经历下的邀请/认领/审核适配")
class PlayerClaimServiceTest {

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

    private PlayerClaimService service;

    @BeforeEach
    void setUp() {
        service = new PlayerClaimService(accountProperties, apiPermissionService, playerClaimRepository,
                playerClaimInviteRepository, playerRepository, teamRepository, teamManagerRepository,
                sysUserRepository, playerTeamService, accountScopeService, resourceGuard);
    }

    @Test
    @DisplayName("创建邀请：球员当前未注册在该队 → 400")
    void createInvite_playerNotCurrentlyInTeam_rejected() {
        when(apiPermissionService.isSuperAdmin(7L)).thenReturn(true);
        when(teamRepository.findById(5L)).thenReturn(Optional.of(team(5L)));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L)));
        when(playerTeamService.isCurrentlyInTeam(1L, 5L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createInvite(7L, 5L, 1L, null, null, "备注"));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不属于"));
    }

    @Test
    @DisplayName("创建邀请：球员有该队当前经历 → 正常创建邀请")
    void createInvite_playerCurrentlyInTeam_createsInvite() {
        when(apiPermissionService.isSuperAdmin(7L)).thenReturn(true);
        when(teamRepository.findById(5L)).thenReturn(Optional.of(team(5L)));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L)));
        when(playerTeamService.isCurrentlyInTeam(1L, 5L)).thenReturn(true);
        when(accountProperties.getInviteDefaultExpireHours()).thenReturn(72);
        when(accountProperties.getInviteDefaultMaxUses()).thenReturn(1);
        when(playerClaimInviteRepository.save(any(PlayerClaimInvite.class))).thenAnswer(invocation -> {
            PlayerClaimInvite invite = invocation.getArgument(0);
            if (invite.getId() == null) {
                invite.setId(50L);
            }
            return invite;
        });

        Map<String, Object> out = service.createInvite(7L, 5L, 1L, null, null, "备注");

        assertNotNull(out.get("token"));
        assertEquals(50L, ((Number) out.get("id")).longValue());
        assertEquals(5L, ((Number) out.get("teamId")).longValue());
        assertEquals(1L, ((Number) out.get("playerId")).longValue());
    }

    @Test
    @DisplayName("邀请认领：球员不在邀请球队的当前经历中 → 400")
    void claimViaInvite_playerNotInInviteTeam_rejected() {
        PlayerClaimInvite invite = new PlayerClaimInvite();
        invite.setId(9L);
        invite.setStatus("active");
        invite.setTeamId(5L);
        invite.setPlayerId(1L);
        invite.setExpiresAt(LocalDateTime.now().plusDays(1));
        invite.setUsedCount(0);
        invite.setMaxUses(1);
        when(playerClaimInviteRepository.findByTokenAndDeletedAtIsNull("tok")).thenReturn(Optional.of(invite));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L)));
        when(playerTeamService.isCurrentlyInTeam(1L, 5L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.claimViaInvite(3L, "tok", null, null));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不属于邀请球队"));
    }

    @Test
    @DisplayName("审核：审核守卫通过 → 认领通过并回写球员归属")
    void approve_reviewGuardPasses_succeeds() {
        PlayerClaim claim = new PlayerClaim();
        claim.setId(9L);
        claim.setPlayerId(1L);
        claim.setUserId(3L);
        claim.setStatus("pending");
        claim.setReviewerType("team_manager");
        when(playerClaimRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(claim));
        Player p = player(1L);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(p));
        when(playerClaimRepository.save(any(PlayerClaim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlayerClaim result = service.approve(9L, 7L, "ok");

        assertEquals("approved", result.getStatus());
        assertEquals(3L, p.getUserId().longValue());
        verify(resourceGuard).assertCanReviewClaim(claim);
    }

    private static Player player(Long id) {
        Player p = new Player();
        p.setId(id);
        p.setTenantId(9L);
        return p;
    }

    private static Team team(long id) {
        Team t = new Team();
        t.setId(id);
        t.setTenantId(9L);
        t.setName("球队" + id);
        return t;
    }
}
