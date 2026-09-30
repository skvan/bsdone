/*
 * 账号权限重构（批次 3a，Task 3.3）：联盟自助创建申请流与审核服务（LeagueProvisionService）。
 *
 * 职责：门户账号（member / team_manager / league_organizer）自助创建联盟时，按租户级开关
 * （portalLeagueCreateRequireApproval，默认 true）决定「落申请待审核」或「直建联盟并自授主办方」；
 * 管理员侧提供回填建联盟（approve）/ 驳回（reject）与待审列表查询。
 *
 * 语义铁律：
 *  - 审批开关按「当前租户」读取（tenantQueryPolicyService.requiredTenantId()）；
 *  - 审批通过：按申请回填 name / nameEn / description 落库，再把申请人授为 GRANT_SELF_CREATE 主办方；
 *  - 写路径一律落 reviewedBy / reviewedAt；approve / reject 对「不存在 / 已处理（非 pending）」统一 404；
 *  - 审核者权限：仅超管 / 租户管理员（ApiPermissionService），否则 403；租户管理员限本租户（超管放行）；
 *  - 关系变更（assignInternal 内部已 evict）+ 本服务对申请人再 evict，双保险失效范围缓存。
 *
 * 循环依赖说明：本服务需调用 LeagueService.createInternal；而 LeagueService.createForCurrentUser
 * 又需调用本服务的 submitOrCreate，形成 LeagueService ⇄ LeagueProvisionService 环。
 * 采用「本服务以 @Lazy LeagueService 注入」的最小方案打破环（见构造器），
 * 保持 LeagueService 侧为普通构造注入，语义直观、改动最小。
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.League;
import com.bsball.model.entity.LeagueCreateRequest;
import com.bsball.model.entity.LeagueOwner;
import com.bsball.repository.LeagueCreateRequestRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeagueProvisionService {

    /** 租户级开关：门户自助建联盟是否需要平台审核（缺省 true）。 */
    public static final String CONFIG_REQUIRE_APPROVAL = "portalLeagueCreateRequireApproval";

    private final SysConfigService sysConfigService;
    private final LeagueService leagueService;
    private final LeagueCreateRequestRepository leagueCreateRequestRepository;
    private final LeagueOwnerAssignService leagueOwnerAssignService;
    private final AccountScopeService accountScopeService;
    private final TenantQueryPolicyService tenantQueryPolicyService;
    private final ApiPermissionService apiPermissionService;

    /**
     * 门户自助创建分派：需审核则落 pending 申请；否则直建联盟并把申请人授为主办方（SELF_CREATE）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitOrCreate(Long userId, League payload) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        boolean needApproval = this.sysConfigService.getBoolean(tid, CONFIG_REQUIRE_APPROVAL, true);
        if (needApproval) {
            LeagueCreateRequest req = new LeagueCreateRequest();
            req.setTenantId(Long.valueOf(tid));
            req.setApplicantUserId(userId);
            req.setName(payload == null ? null : payload.getName());
            req.setNameEn(payload == null ? null : payload.getNameEn());
            req.setDescription(payload == null ? null : payload.getDescription());
            req.setStatus(LeagueCreateRequest.STATUS_PENDING);
            LeagueCreateRequest saved = this.leagueCreateRequestRepository.save(req);
            return Map.of("pending", true, "requestId", saved.getId());
        }
        League created = this.leagueService.createInternal(payload);
        this.leagueOwnerAssignService.assignInternal(userId, Long.valueOf(tid), created.getId(),
                LeagueOwner.GRANT_SELF_CREATE);
        this.accountScopeService.evictUserScopeCache(userId);
        return Map.of("pending", false, "id", created.getId());
    }

    /**
     * 审核通过：按申请回填建联盟，授权申请人，申请置 approved + leagueId + reviewed_*，失效申请人缓存。
     */
    @Transactional(rollbackFor = Exception.class)
    public League approve(Long reviewerId, Long requestId) {
        LeagueCreateRequest req = this.requirePending(reviewerId, requestId);
        League payload = new League();
        payload.setName(req.getName());
        payload.setNameEn(req.getNameEn());
        payload.setDescription(req.getDescription());
        League created = this.leagueService.createInternal(payload);
        this.leagueOwnerAssignService.assignInternal(req.getApplicantUserId(), req.getTenantId(), created.getId(),
                LeagueOwner.GRANT_SELF_CREATE);
        req.setStatus(LeagueCreateRequest.STATUS_APPROVED);
        req.setLeagueId(created.getId());
        req.setReviewedBy(reviewerId);
        req.setReviewedAt(LocalDateTime.now());
        this.leagueCreateRequestRepository.save(req);
        this.accountScopeService.evictUserScopeCache(req.getApplicantUserId());
        return created;
    }

    /**
     * 审核驳回：申请置 rejected + rejectReason + reviewed_*。
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long reviewerId, Long requestId, String reason) {
        LeagueCreateRequest req = this.requirePending(reviewerId, requestId);
        req.setStatus(LeagueCreateRequest.STATUS_REJECTED);
        req.setRejectReason(reason);
        req.setReviewedBy(reviewerId);
        req.setReviewedAt(LocalDateTime.now());
        this.leagueCreateRequestRepository.save(req);
    }

    /**
     * 待审申请分页（当前租户）：默认 page=1、pageSize=20；管理员可见。
     */
    public PageResult<LeagueCreateRequest> listPending(Long reviewerId, Integer page, Integer pageSize) {
        this.assertReviewer(reviewerId);
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        List<LeagueCreateRequest> all = this.leagueCreateRequestRepository
                .findByTenantIdAndStatusAndDeletedAtIsNull(Long.valueOf(tid), LeagueCreateRequest.STATUS_PENDING);
        int p = page != null && page > 0 ? page : 1;
        int ps = pageSize != null && pageSize > 0 ? pageSize : 20;
        int from = Math.min((p - 1) * ps, all.size());
        int to = Math.min(from + ps, all.size());
        return PageResult.of(all.subList(from, to), all.size());
    }

    /** 审核者权限：须为超管 / 租户管理员，否则 403（未登录 401）。风格对齐 LeagueOwnerAssignService。 */
    private void assertReviewer(Long reviewerId) {
        if (reviewerId == null) throw new BusinessException(401, "请先登录");
        if (!this.apiPermissionService.isSuperAdmin(reviewerId) && !this.apiPermissionService.isTenantAdmin(reviewerId)) {
            throw new BusinessException(403, "仅管理员可审核联盟创建申请");
        }
    }

    /**
     * 取待处理申请：先校验审核者权限；申请须存在、未软删且 status=pending，否则 404；
     * 租户管理员限本租户（超管放行），越权 403（对齐取件语义）。
     */
    private LeagueCreateRequest requirePending(Long reviewerId, Long requestId) {
        this.assertReviewer(reviewerId);
        LeagueCreateRequest req = requestId == null ? null
                : this.leagueCreateRequestRepository.findById(requestId).orElse(null);
        if (req == null || req.getDeletedAt() != null
                || !LeagueCreateRequest.STATUS_PENDING.equals(req.getStatus())) {
            throw new BusinessException(404, "申请不存在或已处理");
        }
        if (!this.apiPermissionService.isSuperAdmin(reviewerId)) {
            Long tenantId = CurrentUserHolder.getTenantId();
            if (tenantId != null && !tenantId.equals(req.getTenantId())) {
                throw new BusinessException(403, "无权审核该申请");
            }
        }
        return req;
    }

    @Generated
    public LeagueProvisionService(SysConfigService sysConfigService, @Lazy LeagueService leagueService,
            LeagueCreateRequestRepository leagueCreateRequestRepository,
            LeagueOwnerAssignService leagueOwnerAssignService, AccountScopeService accountScopeService,
            TenantQueryPolicyService tenantQueryPolicyService, ApiPermissionService apiPermissionService) {
        this.sysConfigService = sysConfigService;
        this.leagueService = leagueService;
        this.leagueCreateRequestRepository = leagueCreateRequestRepository;
        this.leagueOwnerAssignService = leagueOwnerAssignService;
        this.accountScopeService = accountScopeService;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
        this.apiPermissionService = apiPermissionService;
    }
}
