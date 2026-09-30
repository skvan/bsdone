/*
 * 账号权限重构（批次 2，Task 2.5）：查询层收口组件 ScopeQuerySupport 的三态直测。
 *
 * 语义铁律（对照 spec §12.2 管理视图窄读）：
 *  - 非管理上下文（未带 X-Scope-Context: manage）→ null（宽读公开数据，行为不变）；
 *  - 管理上下文 + 租户内不受限 → null（宽读）；
 *  - 管理上下文 + 受限：空集原样返回空集（调用方据此渲染空页，不得再查库）；非空为 IN 集合。
 *
 * ScopeContextHolder 为 ThreadLocal，用例逐条 setManage 且 @AfterEach 清理，避免跨用例污染。
 */
package com.bsball.service.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.core.ScopeContextHolder;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.repository.EventRepository;
import com.bsball.repository.TeamRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScopeQuerySupport：管理视图窄读三态（null / 空集 / 非空）")
class ScopeQuerySupportTest {

    private static final long TENANT_ID = 10L;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private EventRepository eventRepository;

    private ScopeQuerySupport support;

    @BeforeEach
    void setUp() {
        ScopeContextHolder.clear();
        support = new ScopeQuerySupport(teamRepository, eventRepository);
    }

    @AfterEach
    void tearDown() {
        // ScopeContextHolder 为 ThreadLocal，用例结束显式清理，避免跨用例污染
        ScopeContextHolder.clear();
    }

    // ------------------------------------------------------------------ visibleLeagueIds

    @Test
    @DisplayName("可见联盟：漏带 manage 头（非管理上下文）→ null 宽读，不查库")
    void visibleLeagueIds_nonManage_isNull_noQuery() {
        ScopeContextHolder.setManage(false);

        assertNull(support.visibleLeagueIds(EffectiveScope.restricted(false, Set.of(10L), Set.of())));
        verifyNoInteractions(teamRepository, eventRepository);
    }

    @Test
    @DisplayName("可见联盟：manage + 租户内不受限 → null 宽读，不查库")
    void visibleLeagueIds_manageUnrestricted_isNull_noQuery() {
        ScopeContextHolder.setManage(true);

        assertNull(support.visibleLeagueIds(EffectiveScope.unrestricted()));
        verifyNoInteractions(teamRepository, eventRepository);
    }

    @Test
    @DisplayName("可见联盟：manage + 受限非空 → 原样返回联盟集合")
    void visibleLeagueIds_manageRestricted_returnsLeagueIds() {
        ScopeContextHolder.setManage(true);

        List<Long> ids = support.visibleLeagueIds(EffectiveScope.restricted(false, Set.of(10L, 11L), Set.of()));

        assertEquals(Set.of(10L, 11L), Set.copyOf(ids));
        verifyNoInteractions(teamRepository, eventRepository);
    }

    @Test
    @DisplayName("可见联盟：manage + 受限空集 → 空集（不得优化成 null）")
    void visibleLeagueIds_manageRestrictedEmpty_returnsEmpty() {
        ScopeContextHolder.setManage(true);

        assertTrue(support.visibleLeagueIds(EffectiveScope.empty()).isEmpty());
        verifyNoInteractions(teamRepository, eventRepository);
    }

    // ------------------------------------------------------------------ visibleTeamIds

    @Test
    @DisplayName("可见球队：非管理上下文 → null 宽读，不查库")
    void visibleTeamIds_nonManage_isNull() {
        ScopeContextHolder.setManage(false);

        assertNull(support.visibleTeamIds(EffectiveScope.restricted(false, Set.of(10L), Set.of(100L)), TENANT_ID));
        verifyNoInteractions(teamRepository);
    }

    @Test
    @DisplayName("可见球队：manage + 受限空集 → 空集且不查库（空集不查库）")
    void visibleTeamIds_manageRestrictedEmpty_noQuery() {
        ScopeContextHolder.setManage(true);

        assertTrue(support.visibleTeamIds(EffectiveScope.empty(), TENANT_ID).isEmpty());
        verifyNoInteractions(teamRepository);
    }

    @Test
    @DisplayName("可见球队：manage + 受限 → 显式 teamIds ∪ 由联盟派生的同租户球队")
    void visibleTeamIds_manageRestricted_unionOfExplicitAndDerived() {
        ScopeContextHolder.setManage(true);
        when(teamRepository.findIdsByLeagueIdInAndTenantId(Set.of(10L), TENANT_ID))
                .thenReturn(List.of(200L, 201L));

        List<Long> ids = support.visibleTeamIds(
                EffectiveScope.restricted(false, Set.of(10L), Set.of(100L)), TENANT_ID);

        assertEquals(Set.of(100L, 200L, 201L), Set.copyOf(ids));
        // 派生查询必须带租户限定（同租户、未删除）
        verify(teamRepository).findIdsByLeagueIdInAndTenantId(Set.of(10L), TENANT_ID);
    }

    @Test
    @DisplayName("可见球队：manage + 仅显式 teamIds（无联盟）→ 不查派生库")
    void visibleTeamIds_manageRestricted_teamOnly_noQuery() {
        ScopeContextHolder.setManage(true);

        List<Long> ids = support.visibleTeamIds(
                EffectiveScope.restricted(false, Set.of(), Set.of(100L)), TENANT_ID);

        assertEquals(List.of(100L), ids);
        verifyNoInteractions(teamRepository);
    }

    // ------------------------------------------------------------------ visibleEventIds

    @Test
    @DisplayName("可见赛事：非管理上下文 → null 宽读，不查库")
    void visibleEventIds_nonManage_isNull() {
        ScopeContextHolder.setManage(false);

        assertNull(support.visibleEventIds(EffectiveScope.restricted(false, Set.of(10L), Set.of()), TENANT_ID));
        verifyNoInteractions(eventRepository);
    }

    @Test
    @DisplayName("可见赛事：manage + 受限无联盟 → 空集且不查库")
    void visibleEventIds_manageRestricted_noLeague_empty_noQuery() {
        ScopeContextHolder.setManage(true);

        assertTrue(support.visibleEventIds(
                EffectiveScope.restricted(false, Set.of(), Set.of(100L)), TENANT_ID).isEmpty());
        verifyNoInteractions(eventRepository);
    }

    @Test
    @DisplayName("可见赛事：manage + 受限含联盟 → 由联盟派生的同租户赛事")
    void visibleEventIds_manageRestricted_derivedFromLeagues() {
        ScopeContextHolder.setManage(true);
        when(eventRepository.findIdsByLeagueIdInAndTenantId(Set.of(10L), TENANT_ID)).thenReturn(List.of(5L, 6L));

        assertEquals(List.of(5L, 6L),
                support.visibleEventIds(EffectiveScope.restricted(false, Set.of(10L), Set.of()), TENANT_ID));
        verify(eventRepository).findIdsByLeagueIdInAndTenantId(Set.of(10L), TENANT_ID);
    }
}
