/*
 * 账号权限重构（批次 2，终审修复）：HistoryRecordService.create 目标队租户校验接线测试。
 *
 * 背景：targetType=="team" 分支原先仅做 ResourceGuard.assertCanManageTeam(targetId)，
 * 不受限身份（超管 / 租管）守卫直通，可把 targetId 指向他租户球队 → 跨租户悬空引用。
 * 本测试锁定与相邻 relatedObject 分支对称的租户归属校验：
 *  - 目标队他租户 / 不存在 → 400，且不落库、守卫不被触达；
 *  - 目标队属本租户 → 守卫放行并落库（tenantId 取当前租户）。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；HistoryRecordService 手工 new（@Generated 构造器）。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.exception.BusinessException;
import com.bsball.model.entity.HistoryRecord;
import com.bsball.model.entity.Team;
import com.bsball.repository.HistoryRecordRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistoryRecordService：create 目标队租户校验接线（跨租户悬空引用防护）")
class HistoryRecordServiceScopeWiringTest {

    private static final long TENANT_ID = 10L;
    private static final long OTHER_TENANT = 99L;

    @Mock
    private HistoryRecordRepository historyRecordRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private AccountScopeService accountScopeService;

    @Mock
    private ScopeQuerySupport scopeQuerySupport;

    @Mock
    private ResourceGuard resourceGuard;

    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;

    private HistoryRecordService service;

    @BeforeEach
    void setUp() {
        service = new HistoryRecordService(historyRecordRepository, teamRepository, accountScopeService,
                scopeQuerySupport, resourceGuard, tenantQueryPolicyService);
    }

    @Test
    @DisplayName("create 目标队指向他租户（不受限身份）：400「球队与当前租户不一致」，不落库、守卫不被触达")
    void create_targetTeamOtherTenant_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        HistoryRecord entity = record("team", 500L, null, null);
        when(teamRepository.findById(500L)).thenReturn(Optional.of(team(500L, OTHER_TENANT)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(entity));

        assertEquals(400, ex.getCode());
        assertEquals("球队与当前租户不一致", ex.getMessage());
        verify(historyRecordRepository, never()).save(any());
        verify(resourceGuard, never()).assertCanManageTeam(any());
    }

    @Test
    @DisplayName("create 目标队属本租户：守卫放行并落库（tenantId 取当前租户）")
    void create_targetTeamSameTenant_saved() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        HistoryRecord entity = record("team", 500L, null, null);
        when(teamRepository.findById(500L)).thenReturn(Optional.of(team(500L, TENANT_ID)));
        when(historyRecordRepository.save(entity)).thenReturn(entity);

        HistoryRecord saved = service.create(entity);

        assertEquals(entity, saved);
        assertEquals(TENANT_ID, entity.getTenantId().longValue());
        verify(resourceGuard).assertCanManageTeam(500L);
        verify(historyRecordRepository).save(entity);
    }

    @Test
    @DisplayName("create 关联对象他租户（回归）：400「球队与当前租户不一致」，不落库")
    void create_relatedObjectOtherTenant_rejected() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        HistoryRecord entity = record(null, null, "team", 500L);
        when(teamRepository.findById(500L)).thenReturn(Optional.of(team(500L, OTHER_TENANT)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(entity));

        assertEquals(400, ex.getCode());
        assertEquals("球队与当前租户不一致", ex.getMessage());
        verify(historyRecordRepository, never()).save(any());
    }

    private static HistoryRecord record(String targetType, Long targetId, String relatedType, Long relatedId) {
        HistoryRecord r = new HistoryRecord();
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        r.setRelatedObjectType(relatedType);
        r.setRelatedObjectId(relatedId);
        return r;
    }

    private static Team team(long id, long tenantId) {
        Team t = new Team();
        t.setId(id);
        t.setTenantId(tenantId);
        t.setName("球队" + id);
        return t;
    }
}
