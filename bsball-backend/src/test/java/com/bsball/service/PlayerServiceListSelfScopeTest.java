package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.PlayerClaimRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.query.ScopeQuerySupport;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
@DisplayName("球员管理：认领档案始终可见（不受球队范围限制）")
class PlayerServiceListSelfScopeTest {

    private static final long TENANT = 1L;
    private static final long USER = 14L;
    private static final long PLAYER_ID = 74L;

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
        CurrentUserHolder.set(USER, TENANT);
        service = new PlayerService(playerRepository, teamRepository, playerTeamRepository, statsService,
                accountScopeService, scopeQuerySupport, resourceGuard, personnelHistoryRecorder,
                playerTeamService, tenantQueryPolicyService, sysConfigService, playerClaimRepository,
                gamePlayerStatRepository);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("受限且无球队范围时，认领人仍可查看本人档案")
    void getAllowsSelfClaimedPlayerWithoutTeamScope() {
        Player player = player(PLAYER_ID, USER);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        EffectiveScope scope = mock(EffectiveScope.class);
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT)).thenReturn(List.of());

        Player result = service.get(PLAYER_ID);

        assertSame(player, result);
        verify(playerTeamService, never()).countCurrentEntriesInTeams(any(), anyList());
    }

    @Test
    @DisplayName("受限且无球队范围时，非本人认领档案仍 403")
    void getRejectsOtherPlayerWithoutTeamScope() {
        Player player = player(PLAYER_ID, 999L);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        EffectiveScope scope = mock(EffectiveScope.class);
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT)).thenReturn(List.of());
        when(playerTeamService.countCurrentEntriesInTeams(PLAYER_ID, List.of())).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.get(PLAYER_ID));

        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("受限且无球队范围时，球员列表不短路，仍查询并返回认领档案")
    void listKeepsRunningForSelfClaimedPlayer() {
        when(tenantQueryPolicyService.isGlobalQueryMode()).thenReturn(false);
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        EffectiveScope scope = mock(EffectiveScope.class);
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT)).thenReturn(List.of());
        Player claimed = player(PLAYER_ID, USER);
        when(playerRepository.findByUserIdAndTenantIdAndDeletedAtIsNull(USER, TENANT))
                .thenReturn(List.of(claimed));
        when(playerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(claimed)));

        var result = service.list(1, 20, null, null, null, null, null, null, null, null, null, null, null, null);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertSame(claimed, result.getList().get(0));
        verify(playerRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("受限且无球队范围、没有认领档案时，列表直接返回空页")
    void listShortCircuitsWithoutSelfClaimedPlayer() {
        when(tenantQueryPolicyService.isGlobalQueryMode()).thenReturn(false);
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        EffectiveScope scope = mock(EffectiveScope.class);
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT)).thenReturn(List.of());
        when(playerRepository.findByUserIdAndTenantIdAndDeletedAtIsNull(USER, TENANT)).thenReturn(List.of());

        var result = service.list(1, 20, null, null, null, null, null, null, null, null, null, null, null, null);

        assertEquals(0, result.getTotal());
        verify(playerRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("显式球队筛选保持严格：其他球队的认领球员不并入")
    void teamFilterDoesNotIncludeSelfClaimedPlayerFromOtherTeam() {
        when(tenantQueryPolicyService.isGlobalQueryMode()).thenReturn(false);
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT);
        EffectiveScope scope = mock(EffectiveScope.class);
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT)).thenReturn(List.of());
        Player claimed = player(PLAYER_ID, USER);
        when(playerRepository.findByUserIdAndTenantIdAndDeletedAtIsNull(USER, TENANT))
                .thenReturn(List.of(claimed));
        when(playerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(1, 20, null, null, 9L, null, null, null, null, null, null, null, null, null);

        ArgumentCaptor<Specification<Player>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(playerRepository).findAll(captor.capture(), any(Pageable.class));

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Player> root = mock(Root.class);
        Subquery<Long> subquery = mock(Subquery.class);
        Root<PlayerTeam> entry = mock(Root.class);
        Path<Object> playerIdPath = mock(Path.class);
        Path<Object> teamIdPath = mock(Path.class);
        when(query.subquery(Long.class)).thenReturn(subquery);
        when(subquery.from(PlayerTeam.class)).thenReturn(entry);
        lenient().when(root.get("id")).thenReturn(playerIdPath);
        lenient().when(entry.get("teamId")).thenReturn(teamIdPath);

        captor.getValue().toPredicate(root, query, cb);

        // 只允许「可见球队 OR 认领球员」这一处 or；球队筛选条件本身必须严格
        verify(cb, times(1)).or(nullable(Expression.class), nullable(Expression.class));
        // 可见范围子查询 + 显式球队子查询都要存在，防止球队条件被整体删掉
        verify(cb, times(2)).exists(any(Subquery.class));
    }

    private Player player(Long id, Long userId) {
        Player player = new Player();
        player.setId(id);
        player.setUserId(userId);
        player.setTenantId(TENANT);
        player.setName("蔡子杰");
        return player;
    }
}
