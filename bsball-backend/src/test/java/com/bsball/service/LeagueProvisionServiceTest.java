/*
 * 账号权限重构（批次 3a，Task 3.3）：LeagueProvisionService 单元测试（Mockito 手工装配）。
 *
 * 覆盖：submitOrCreate 需审核（落申请、不建联盟）/ 免审核（直建 + 授权 + evict）；
 *      approve 成功（回填 / 状态 / evict）与 404（非 pending）；
 *      reject 成功与 403（非管理员）；审批权限（超管放行 / 租管限本租户）；待审列表分页。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.common.PageResult;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.League;
import com.bsball.model.entity.LeagueCreateRequest;
import com.bsball.model.entity.LeagueOwner;
import com.bsball.repository.LeagueCreateRequestRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeagueProvisionService：自助创建申请流与审核（#154）")
class LeagueProvisionServiceTest {

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private LeagueService leagueService;

    @Mock
    private LeagueCreateRequestRepository leagueCreateRequestRepository;

    @Mock
    private LeagueOwnerAssignService leagueOwnerAssignService;

    @Mock
    private AccountScopeService accountScopeService;

    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;

    @Mock
    private ApiPermissionService apiPermissionService;

    private LeagueProvisionService service;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        service = new LeagueProvisionService(sysConfigService, leagueService, leagueCreateRequestRepository,
                leagueOwnerAssignService, accountScopeService, tenantQueryPolicyService, apiPermissionService);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    // ------------------------------------------------------------- submitOrCreate

    @Test
    @DisplayName("submitOrCreate 需审核：仅落 pending 申请，不建联盟、不授权")
    void submitOrCreate_needApproval_createsRequestOnly() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(10L);
        when(sysConfigService.getBoolean(10L, LeagueProvisionService.CONFIG_REQUIRE_APPROVAL, true)).thenReturn(true);
        when(leagueCreateRequestRepository.save(any(LeagueCreateRequest.class))).thenAnswer(inv -> {
            LeagueCreateRequest r = inv.getArgument(0);
            r.setId(500L);
            return r;
        });

        League payload = new League();
        payload.setName("新联盟");
        payload.setNameEn("New League");
        payload.setDescription("简介");

        Map<String, Object> out = service.submitOrCreate(7L, payload);

        assertEquals(Boolean.TRUE, out.get("pending"));
        assertEquals(500L, ((Number) out.get("requestId")).longValue());

        ArgumentCaptor<LeagueCreateRequest> captor = ArgumentCaptor.forClass(LeagueCreateRequest.class);
        verify(leagueCreateRequestRepository).save(captor.capture());
        LeagueCreateRequest saved = captor.getValue();
        assertEquals(10L, saved.getTenantId());
        assertEquals(7L, saved.getApplicantUserId());
        assertEquals("新联盟", saved.getName());
        assertEquals("New League", saved.getNameEn());
        assertEquals("简介", saved.getDescription());
        assertEquals(LeagueCreateRequest.STATUS_PENDING, saved.getStatus());

        verify(leagueService, never()).createInternal(any());
        verify(leagueOwnerAssignService, never()).assignInternal(any(), any(), any(), any());
    }

    @Test
    @DisplayName("submitOrCreate 免审核：直建联盟 + 授主办方 SELF_CREATE + evict")
    void submitOrCreate_noApproval_createsLeagueAndAssigns() {
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(10L);
        when(sysConfigService.getBoolean(10L, LeagueProvisionService.CONFIG_REQUIRE_APPROVAL, true)).thenReturn(false);
        League payload = new League();
        payload.setName("直建联盟");
        League created = new League();
        created.setId(88L);
        when(leagueService.createInternal(payload)).thenReturn(created);

        Map<String, Object> out = service.submitOrCreate(7L, payload);

        assertEquals(Boolean.FALSE, out.get("pending"));
        assertEquals(88L, ((Number) out.get("id")).longValue());
        verify(leagueOwnerAssignService).assignInternal(7L, 10L, 88L, LeagueOwner.GRANT_SELF_CREATE);
        verify(accountScopeService).evictUserScopeCache(7L);
        verify(leagueCreateRequestRepository, never()).save(any());
    }

    // ------------------------------------------------------------- approve

    @Test
    @DisplayName("approve 成功：回填建联盟 + 授权申请人 + 申请置 approved + evict")
    void approve_success() {
        CurrentUserHolder.set(1L, 10L);
        when(apiPermissionService.isSuperAdmin(1L)).thenReturn(true);
        LeagueCreateRequest req = pendingRequest(500L, 10L, 7L);
        when(leagueCreateRequestRepository.findById(500L)).thenReturn(Optional.of(req));
        League created = new League();
        created.setId(88L);
        when(leagueService.createInternal(any(League.class))).thenReturn(created);
        when(leagueCreateRequestRepository.save(any(LeagueCreateRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        League result = service.approve(1L, 500L);

        assertEquals(88L, result.getId());
        ArgumentCaptor<League> payloadCaptor = ArgumentCaptor.forClass(League.class);
        verify(leagueService).createInternal(payloadCaptor.capture());
        assertEquals("新联盟", payloadCaptor.getValue().getName());
        assertEquals("New League", payloadCaptor.getValue().getNameEn());
        assertEquals("简介", payloadCaptor.getValue().getDescription());

        assertEquals(LeagueCreateRequest.STATUS_APPROVED, req.getStatus());
        assertEquals(88L, req.getLeagueId());
        assertEquals(1L, req.getReviewedBy());
        assertNotNull(req.getReviewedAt());
        verify(leagueOwnerAssignService).assignInternal(7L, 10L, 88L, LeagueOwner.GRANT_SELF_CREATE);
        verify(accountScopeService).evictUserScopeCache(7L);
    }

    @Test
    @DisplayName("approve 非 pending：404「申请不存在或已处理」")
    void approve_nonPending_notFound() {
        CurrentUserHolder.set(1L, 10L);
        when(apiPermissionService.isSuperAdmin(1L)).thenReturn(true);
        LeagueCreateRequest req = pendingRequest(500L, 10L, 7L);
        req.setStatus(LeagueCreateRequest.STATUS_APPROVED);
        when(leagueCreateRequestRepository.findById(500L)).thenReturn(Optional.of(req));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.approve(1L, 500L));
        assertEquals(404, ex.getCode());
        assertEquals("申请不存在或已处理", ex.getMessage());
        verify(leagueService, never()).createInternal(any());
    }

    @Test
    @DisplayName("approve 申请不存在：404")
    void approve_missing_notFound() {
        CurrentUserHolder.set(1L, 10L);
        when(apiPermissionService.isSuperAdmin(1L)).thenReturn(true);
        when(leagueCreateRequestRepository.findById(500L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.approve(1L, 500L));
        assertEquals(404, ex.getCode());
        assertEquals("申请不存在或已处理", ex.getMessage());
    }

    // ------------------------------------------------------------- reject

    @Test
    @DisplayName("reject 成功：申请置 rejected + 原因 + reviewed_*")
    void reject_success() {
        CurrentUserHolder.set(1L, 10L);
        when(apiPermissionService.isSuperAdmin(1L)).thenReturn(true);
        LeagueCreateRequest req = pendingRequest(500L, 10L, 7L);
        when(leagueCreateRequestRepository.findById(500L)).thenReturn(Optional.of(req));
        when(leagueCreateRequestRepository.save(any(LeagueCreateRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        service.reject(1L, 500L, "资料不足");

        assertEquals(LeagueCreateRequest.STATUS_REJECTED, req.getStatus());
        assertEquals("资料不足", req.getRejectReason());
        assertEquals(1L, req.getReviewedBy());
        assertNotNull(req.getReviewedAt());
        verify(leagueService, never()).createInternal(any());
    }

    @Test
    @DisplayName("reject 非管理员：403「仅管理员可审核联盟创建申请」")
    void reject_nonAdmin_forbidden() {
        CurrentUserHolder.set(2L, 10L);
        when(apiPermissionService.isSuperAdmin(2L)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(2L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.reject(2L, 500L, "x"));
        assertEquals(403, ex.getCode());
        assertEquals("仅管理员可审核联盟创建申请", ex.getMessage());
        verify(leagueCreateRequestRepository, never()).findById(any());
    }

    // ------------------------------------------------------------- approve 租管跨租户

    @Test
    @DisplayName("approve 租管跨租户：403「无权审核该申请」（超管放行）")
    void approve_tenantAdminCrossTenant_forbidden() {
        CurrentUserHolder.set(2L, 99L);
        when(apiPermissionService.isSuperAdmin(2L)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(2L)).thenReturn(true);
        LeagueCreateRequest req = pendingRequest(500L, 10L, 7L);
        when(leagueCreateRequestRepository.findById(500L)).thenReturn(Optional.of(req));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.approve(2L, 500L));
        assertEquals(403, ex.getCode());
        assertEquals("无权审核该申请", ex.getMessage());
        verify(leagueService, never()).createInternal(any());
    }

    // ------------------------------------------------------------- listPending

    @Test
    @DisplayName("listPending 超管：按租户返回 pending 分页")
    void listPending_superAdmin_returnsPage() {
        CurrentUserHolder.set(1L, 10L);
        when(apiPermissionService.isSuperAdmin(1L)).thenReturn(true);
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(10L);
        when(leagueCreateRequestRepository.findByTenantIdAndStatusAndDeletedAtIsNull(eq(10L),
                eq(LeagueCreateRequest.STATUS_PENDING))).thenReturn(List.of(
                pendingRequest(1L, 10L, 7L), pendingRequest(2L, 10L, 8L), pendingRequest(3L, 10L, 9L)));

        PageResult<LeagueCreateRequest> page = service.listPending(1L, 2, 2);

        assertEquals(3L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals(3L, page.getList().get(0).getId());
    }

    @Test
    @DisplayName("listPending 非管理员：403")
    void listPending_nonAdmin_forbidden() {
        CurrentUserHolder.set(2L, 10L);
        when(apiPermissionService.isSuperAdmin(2L)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(2L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.listPending(2L, 1, 20));
        assertEquals(403, ex.getCode());
        assertEquals("仅管理员可审核联盟创建申请", ex.getMessage());
    }

    private static LeagueCreateRequest pendingRequest(long id, long tenantId, long applicantUserId) {
        LeagueCreateRequest req = new LeagueCreateRequest();
        req.setId(id);
        req.setTenantId(tenantId);
        req.setApplicantUserId(applicantUserId);
        req.setName("新联盟");
        req.setNameEn("New League");
        req.setDescription("简介");
        req.setStatus(LeagueCreateRequest.STATUS_PENDING);
        return req;
    }
}
