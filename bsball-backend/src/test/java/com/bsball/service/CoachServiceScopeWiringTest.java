/*
 * 账号权限重构（批次 2，终审修复）：CoachService 读路径收口与写保护接线测试。
 *
 * 目的（对照 spec §12.2 越权矩阵，与 TeamServiceScopeWiringTest 同构）：
 *  - list/listForSelect：改用 ScopeQuerySupport.visibleTeamIds 三分支——
 *    null ⇒ 仅租户过滤（宽读，跳过球队谓词）；空集 ⇒ 空页且不查库；非空 ⇒ teamId IN 可见集合；
 *  - 窄域语义：仅返回 teamId ∈ visible 的教练，绝不放行「自由教练 / teamId 为空」；
 *  - get：越权判定同用 visibleTeamIds.contains（含联盟派生）；域外 / null 队受限 ⇒ 403「无权查看该教练」；
 *    跨租户 ⇒ 租户校验分支返回 null（不进入范围判定，语义与 TeamService.get 一致）；
 *  - 写：update 改队仍保留「旧队 + 新队」双次 ResourceGuard 校验（回归）。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；CoachService 手工 new（@Generated 构造器）。
 * ScopeQuerySupport / ResourceGuard 以桩打接口，验证「接线」而非其内部语义（后者另测）。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.common.PageResult;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.CoachOptionDto;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.entity.Coach;
import com.bsball.model.entity.Team;
import com.bsball.repository.CoachRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.query.ScopeQuerySupport;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
@DisplayName("CoachService：读路径范围收口与写保护接线（越权矩阵·教练）")
class CoachServiceScopeWiringTest {

    private static final long TENANT_ID = 10L;
    private static final long OTHER_TENANT = 99L;

    @Mock
    private CoachRepository coachRepository;

    @Mock
    private TeamRepository teamRepository;

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

    private CoachService service;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        service = new CoachService(coachRepository, teamRepository, accountScopeService, scopeQuerySupport,
                resourceGuard, personnelHistoryRecorder, tenantQueryPolicyService);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    // ------------------------------------------------------------------ 读：listForSelect

    @Test
    @DisplayName("listForSelect 空域：返回空列表且不查库（空集不查库）")
    void listForSelect_emptyScope_emptyList_noQuery() {
        EffectiveScope scope = EffectiveScope.empty();
        givenListContext(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT_ID)).thenReturn(List.of());

        List<CoachOptionDto> result = service.listForSelect();

        assertTrue(result.isEmpty());
        verifyNoInteractions(coachRepository);
    }

    @Test
    @DisplayName("listForSelect 宽读（null）：按租户全量，跳过球队过滤")
    void listForSelect_wideRead_queriesTenant() {
        EffectiveScope scope = EffectiveScope.unrestricted();
        givenListContext(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT_ID)).thenReturn(null);
        when(coachRepository.findAllForSelectByTenantId(TENANT_ID))
                .thenReturn(List.of(dto(1L, null), dto(2L, 100L)));

        List<CoachOptionDto> result = service.listForSelect();

        assertEquals(2, result.size());
        verify(coachRepository).findAllForSelectByTenantId(TENANT_ID);
        verify(coachRepository, never()).findAllForSelectByTenantIdAndTeamIdIn(anyLong(), any());
        verify(coachRepository, never()).findAllForSelectByTenantIdAndFreeOrTeamIdIn(anyLong(), any());
        verify(coachRepository, never()).findAllForSelectByTenantIdAndTeamIdIsNull(anyLong());
    }

    @Test
    @DisplayName("listForSelect 窄域（非空）：只按 teamId IN 可见集合（不含自由教练 / teamId 为空）")
    void listForSelect_manageRestricted_teamIdInOnly() {
        EffectiveScope scope = EffectiveScope.restricted(false, Set.of(), Set.of(100L, 200L));
        givenListContext(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT_ID)).thenReturn(List.of(100L, 200L));
        when(coachRepository.findAllForSelectByTenantIdAndTeamIdIn(TENANT_ID, List.of(100L, 200L)))
                .thenReturn(List.of(dto(2L, 100L)));

        List<CoachOptionDto> result = service.listForSelect();

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).teamId().longValue());
        verify(coachRepository).findAllForSelectByTenantIdAndTeamIdIn(eq(TENANT_ID), eq(List.of(100L, 200L)));
        // 窄域绝不回退「自由教练 / teamId 为空」放行，也不退化为全租户宽读
        verify(coachRepository, never()).findAllForSelectByTenantIdAndFreeOrTeamIdIn(anyLong(), any());
        verify(coachRepository, never()).findAllForSelectByTenantIdAndTeamIdIsNull(anyLong());
        verify(coachRepository, never()).findAllForSelectByTenantId(anyLong());
    }

    // ------------------------------------------------------------------ 读：list

    @Test
    @DisplayName("list 空域：返回空页且不查库（空集不查库）")
    void list_emptyScope_emptyPage_noQuery() {
        EffectiveScope scope = EffectiveScope.empty();
        givenListContext(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT_ID)).thenReturn(List.of());

        PageResult<Coach> result = service.list(1, 10, null, null);

        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
        verifyNoInteractions(coachRepository);
    }

    @Test
    @DisplayName("list 宽读（null）：仅租户过滤，规格中不含 teamId 谓词")
    void list_wideRead_skipsTeamFilter() {
        EffectiveScope scope = EffectiveScope.unrestricted();
        givenListContext(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT_ID)).thenReturn(null);
        when(coachRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(coach(1L, TENANT_ID, 100L))));

        PageResult<Coach> result = service.list(1, 10, null, null);

        assertEquals(1, result.getList().size());
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<Coach> root = mock(Root.class);
        captureSpec().toPredicate(root, mock(CriteriaQuery.class), cb);
        // 宽读：规格绝不访问 teamId（既不 IN 也不 isNull / or）
        verify(root, never()).get("teamId");
    }

    @Test
    @DisplayName("list 窄域（非空）：追加 teamId IN 可见集合（不含 teamId 为空 / 自由教练）")
    void list_manageRestricted_teamIdInPredicate() {
        EffectiveScope scope = EffectiveScope.restricted(false, Set.of(), Set.of(100L));
        givenListContext(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT_ID)).thenReturn(List.of(100L));
        when(coachRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(coach(1L, TENANT_ID, 100L))));

        PageResult<Coach> result = service.list(1, 10, null, null);

        assertEquals(1, result.getList().size());
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<Coach> root = mock(Root.class);
        Path<Object> teamPath = mock(Path.class);
        // 同一 root.get(...) 被代码以多个属性名（deletedAt / tenantId / teamId）求值，严格桩会误报参数不匹配，
        // 故对该 get("teamId") 桩使用 lenient（与 PlayerClaimScopeWiringTest 同因同解）。
        lenient().when(root.get("teamId")).thenReturn(teamPath);

        captureSpec().toPredicate(root, mock(CriteriaQuery.class), cb);

        // teamId IN visible
        verify(teamPath).in(any(Collection.class));
        // 无「teamId is null」（自由教练）分支：isNull 仅用于 deletedAt（此处 root.get("deletedAt") 返回 null）
        verify(cb, never()).isNull(teamPath);
    }

    // ------------------------------------------------------------------ 读：get

    @Test
    @DisplayName("get 本域命中：返回教练")
    void get_ownTeam_returns() {
        Coach c = coach(100L, TENANT_ID, 500L);
        when(coachRepository.findById(100L)).thenReturn(Optional.of(c));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        EffectiveScope scope = EffectiveScope.restricted(false, Set.of(), Set.of(500L));
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT_ID)).thenReturn(List.of(500L));

        assertEquals(c, service.get(100L));
    }

    @Test
    @DisplayName("get 他域：403「无权查看该教练」（文案逐字）")
    void get_otherDomain_forbidden() {
        when(coachRepository.findById(101L)).thenReturn(Optional.of(coach(101L, TENANT_ID, 500L)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        EffectiveScope scope = EffectiveScope.restricted(false, Set.of(), Set.of(100L));
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT_ID)).thenReturn(List.of(100L));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.get(101L));
        assertEquals(403, ex.getCode());
        assertEquals("无权查看该教练", ex.getMessage());
    }

    @Test
    @DisplayName("get 受限且自由教练（teamId 为空）：403「无权查看该教练」")
    void get_restrictedFreeCoach_forbidden() {
        when(coachRepository.findById(102L)).thenReturn(Optional.of(coach(102L, TENANT_ID, null)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        EffectiveScope scope = EffectiveScope.restricted(false, Set.of(), Set.of(100L));
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT_ID)).thenReturn(List.of(100L));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.get(102L));
        assertEquals(403, ex.getCode());
        assertEquals("无权查看该教练", ex.getMessage());
    }

    @Test
    @DisplayName("get 跨租户：租户校验分支返回 null（不进入范围判定，语义与 Team 一致）")
    void get_crossTenant_null() {
        when(coachRepository.findById(100L)).thenReturn(Optional.of(coach(100L, OTHER_TENANT, 500L)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);

        assertNull(service.get(100L));
        verifyNoInteractions(scopeQuerySupport);
    }

    // ------------------------------------------------------------------ 写：update（改队双校回归）

    @Test
    @DisplayName("update 改队：旧队与新队均过 ResourceGuard（双校仍在）并落库")
    void update_changeTeam_doubleGuard_andSaved() {
        Coach existing = coach(100L, TENANT_ID, 100L);
        when(coachRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);

        Coach updated = new Coach();
        updated.setTeamId(200L);
        updated.setName("新教练");
        when(teamRepository.findById(200L)).thenReturn(Optional.of(team(200L, TENANT_ID)));
        when(teamRepository.existsById(200L)).thenReturn(true);
        when(coachRepository.save(updated)).thenReturn(updated);

        Coach saved = service.update(100L, updated);

        assertEquals(updated, saved);
        verify(resourceGuard).assertCanManageTeam(100L);
        verify(resourceGuard).assertCanManageTeam(200L);
        verify(coachRepository).save(updated);
    }

    // ------------------------------------------------------------------ 辅助

    /** list / listForSelect 路径公共上下文（含 global 模式判定）。get 路径不调用 isGlobalQueryMode，另行就地打桩。 */
    private void givenListContext(EffectiveScope scope) {
        when(tenantQueryPolicyService.isGlobalQueryMode()).thenReturn(false);
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
    }

    /** 捕获服务传入 findAll 的 Specification，用于分支断言（须在调用被测方法之后）。 */
    private Specification<Coach> captureSpec() {
        ArgumentCaptor<Specification<Coach>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(coachRepository).findAll(captor.capture(), any(Pageable.class));
        return captor.getValue();
    }

    private static CoachOptionDto dto(long id, Long teamId) {
        return new CoachOptionDto(id, "教练" + id, teamId);
    }

    private static Coach coach(long id, long tenantId, Long teamId) {
        Coach c = new Coach();
        c.setId(id);
        c.setTenantId(tenantId);
        c.setTeamId(teamId);
        c.setName("教练" + id);
        return c;
    }

    private static Team team(long id, long tenantId) {
        Team t = new Team();
        t.setId(id);
        t.setTenantId(tenantId);
        t.setName("球队" + id);
        return t;
    }
}
