/*
 * 账号权限重构（批次 2，Task 2.5）：PlayerClaimService 范围/守卫接线测试。
 *
 * 目的（对照 spec §12.2 越权矩阵与写保护接线）：
 *  - approve：ResourceGuard.assertCanReviewClaim 为唯一授权判定；通过后回写球员归属，
 *    并在事务提交后（单测无事务 → 立即）驱逐被认领账号的范围缓存（认领后权限即时变更）；
 *  - approve / reject：守卫 403 时一律不落库、不驱逐缓存（越权写不产生副作用）；
 *  - pendingForReviewer：空域（无归属球队）→ Specification 走 disjunction（库里即空页）；
 *    team_manager（有归属球队）→ 追加 team_manager 过滤 + 「球员在当前队」子查询；
 *    平台/租户管理员 → 仅呈 platform_admin 过滤，不落入 disjunction / 子查询分支。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；被测服务手工 new（@Generated 构造器）。
 * ResourceGuard / AccountScopeService 以桩打接口，验证「接线」而非其内部语义（后者另测）。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.common.PageResult;
import com.bsball.config.AccountProperties;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerClaim;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.repository.PlayerClaimInviteRepository;
import com.bsball.repository.PlayerClaimRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.SysUserRepository;
import com.bsball.repository.TeamManagerRepository;
import com.bsball.repository.TeamRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
@DisplayName("PlayerClaimService：范围收口与守卫/缓存失效接线（越权矩阵·认领）")
class PlayerClaimScopeWiringTest {

    private static final long CLAIM_ID = 9L;
    private static final long CLAIMANT_ID = 3L;
    private static final long REVIEWER_ID = 7L;
    private static final long PLAYER_ID = 1L;

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

    // ------------------------------------------------------------------ 写：approve

    @Test
    @DisplayName("approve 守卫放行：回写归属并在提交后驱逐被认领账号范围缓存（认领即时生效）")
    void approve_reviewGuardPasses_writesBackAndEvictsScopeCache() {
        PlayerClaim claim = pendingClaim();
        Player p = player(PLAYER_ID, null); // teamId 为空 → ensureSingleLeagueSingleTeam 短路，避免无关桩
        when(playerClaimRepository.findByIdAndDeletedAtIsNull(CLAIM_ID)).thenReturn(Optional.of(claim));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(p));
        when(playerClaimRepository.save(any(PlayerClaim.class))).thenAnswer(inv -> inv.getArgument(0));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        PlayerClaim result = service.approve(CLAIM_ID, REVIEWER_ID, "ok");

        assertEquals("approved", result.getStatus());
        assertEquals(CLAIMANT_ID, p.getUserId().longValue());
        verify(resourceGuard).assertCanReviewClaim(claim);
        // 认领后：被认领账号范围缓存即时失效（单测无事务 → 立即 evict）
        verify(accountScopeService).evictUserScopeCache(CLAIMANT_ID);
    }

    @Test
    @DisplayName("approve 守卫越权 403：不落库、不驱逐缓存（无副作用）")
    void approve_guardForbids_nothingPersisted() {
        PlayerClaim claim = pendingClaim();
        when(playerClaimRepository.findByIdAndDeletedAtIsNull(CLAIM_ID)).thenReturn(Optional.of(claim));
        doThrow(new BusinessException(403, "无权审核该认领"))
                .when(resourceGuard).assertCanReviewClaim(claim);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.approve(CLAIM_ID, REVIEWER_ID, "ok"));

        assertEquals(403, ex.getCode());
        assertEquals("无权审核该认领", ex.getMessage());
        verify(playerClaimRepository, never()).save(any(PlayerClaim.class));
        verifyNoInteractions(playerRepository);
        verify(accountScopeService, never()).evictUserScopeCache(any());
    }

    // ------------------------------------------------------------------ 写：reject

    @Test
    @DisplayName("reject 守卫越权 403：不落库")
    void reject_guardForbids_nothingPersisted() {
        PlayerClaim claim = pendingClaim();
        when(playerClaimRepository.findByIdAndDeletedAtIsNull(CLAIM_ID)).thenReturn(Optional.of(claim));
        doThrow(new BusinessException(403, "无权审核该认领"))
                .when(resourceGuard).assertCanReviewClaim(claim);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.reject(CLAIM_ID, REVIEWER_ID, "no"));

        assertEquals(403, ex.getCode());
        verify(playerClaimRepository, never()).save(any(PlayerClaim.class));
    }

    // ------------------------------------------------------------------ 读：pendingForReviewer（manage 头生效 + 空域空页）

    @Test
    @DisplayName("pendingForReviewer 空域：Specification 走 disjunction（库里即空页）")
    void pending_emptyScope_buildsDisjunction_emptyPage() {
        givenNonAdminReviewer();
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.empty());
        when(playerClaimRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResult<Map<String, Object>> result = service.pendingForReviewer(REVIEWER_ID, 1, 10, null, null);

        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
        // 空域：谓词为 disjunction → 数据库层面即空结果，绝不回退全量
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        captureSpec().toPredicate(mock(Root.class), mock(CriteriaQuery.class), cb);
        verify(cb).disjunction();
    }

    @Test
    @DisplayName("pendingForReviewer 球队管理员：按归属球队追加 team_manager 过滤 + 当前队子查询")
    void pending_teamManager_usesManagedTeamIdsSubquery() {
        givenNonAdminReviewer();
        when(accountScopeService.resolveCurrent())
                .thenReturn(EffectiveScope.restricted(false, Set.of(), Set.of(100L)));
        when(playerClaimRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pendingClaim())));

        PageResult<Map<String, Object>> result = service.pendingForReviewer(REVIEWER_ID, 1, 10, null, null);

        assertEquals(1, result.getList().size());
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<PlayerClaim> query = mock(CriteriaQuery.class);
        Root<PlayerClaim> root = mock(Root.class);
        Subquery<Long> sq = mock(Subquery.class);
        Root<PlayerTeam> entry = mock(Root.class);
        Path<Object> teamIdPath = mock(Path.class);
        Path<Object> playerIdPath = mock(Path.class);
        when(query.subquery(Long.class)).thenReturn(sq);
        when(sq.from(PlayerTeam.class)).thenReturn(entry);
        // 同一 CriteriaBuilder 树会对 get(...) 传入多种属性名，改用 lenient 避免严格桩参数不匹配误报
        lenient().when(entry.get("teamId")).thenReturn(teamIdPath);
        lenient().when(root.get("playerId")).thenReturn(playerIdPath);

        captureSpec().toPredicate(root, query, cb);

        // 有归属球队：落入「当前队子查询」分支，而非 disjunction；不得回退全量
        verify(query).subquery(Long.class);
        verify(cb, never()).disjunction();
    }

    @Test
    @DisplayName("pendingForReviewer 平台管理员：仅呈 platform_admin 过滤（既不 disjunction 也不子查询）")
    void pending_platformAdmin_narrowNoDisjunctionNoSubquery() {
        when(apiPermissionService.isSuperAdmin(REVIEWER_ID)).thenReturn(true);
        when(apiPermissionService.isTenantAdmin(REVIEWER_ID)).thenReturn(false);
        when(accountScopeService.resolveCurrent()).thenReturn(EffectiveScope.empty());
        when(playerClaimRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResult<Map<String, Object>> result = service.pendingForReviewer(REVIEWER_ID, 1, 10, null, null);

        assertTrue(result.getList().isEmpty());
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<PlayerClaim> query = mock(CriteriaQuery.class);

        captureSpec().toPredicate(mock(Root.class), query, cb);

        // 管理员分支：不误判空域、不走球队子查询（仅平台认领）
        verify(cb, never()).disjunction();
        verify(query, never()).subquery(any(Class.class));
    }

    // ------------------------------------------------------------------ 辅助

    private void givenNonAdminReviewer() {
        when(apiPermissionService.isSuperAdmin(REVIEWER_ID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(REVIEWER_ID)).thenReturn(false);
    }

    /** 捕获服务传入 findAll 的 Specification，用于分支断言（须在调用被测方法之后）。 */
    private Specification<PlayerClaim> captureSpec() {
        ArgumentCaptor<Specification<PlayerClaim>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(playerClaimRepository).findAll(captor.capture(), any(Pageable.class));
        return captor.getValue();
    }

    private static PlayerClaim pendingClaim() {
        PlayerClaim c = new PlayerClaim();
        c.setId(CLAIM_ID);
        c.setPlayerId(PLAYER_ID);
        c.setUserId(CLAIMANT_ID);
        c.setStatus("pending");
        c.setReviewerType("team_manager");
        return c;
    }

    private static Player player(Long id, Long userId) {
        Player p = new Player();
        p.setId(id);
        p.setUserId(userId);
        return p;
    }
}
