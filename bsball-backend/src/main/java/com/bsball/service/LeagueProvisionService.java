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
 *  - 关系变更（assignInternal 内部已 evict）+ 本服务对申请人再 evict，双保险失效范围缓存；
 *    本服务 evict 一律后置到事务提交后（evictUserScopeCacheAfterCommit，对齐批 2 PlayerClaimService 惯例）。
 *
 * 循环依赖说明：本服务需调用 LeagueService.createInternal；而 LeagueService.createForCurrentUser
 * 又需调用本服务的 submitOrCreate，形成 LeagueService ⇄ LeagueProvisionService 环。
 * 采用「本服务以 @Lazy LeagueService 注入」的最小方案打破环（见构造器），
 * 保持 LeagueService 侧为普通构造注入，语义直观、改动最小。
 * TODO(批 3a 已裁定硬化项/M6)：后续可评估 ObjectProvider / ApplicationEvent 等替代 @Lazy 的更清晰解环方式。
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
            // 终审修复（#154）：待审分支补名称校验，与直建路径（LeagueService.createInternalForTenant）同口径，
            // 避免缺名/空白时 bs_league_create_request.name NOT NULL 违约落 500；
            // 空/空白 → 400「联盟名称不能为空」；超 200（列定义 length=200）→ 400「联盟名称过长」。
            String name = payload == null || payload.getName() == null ? "" : payload.getName().trim();
            if (name.isEmpty()) {
                throw new BusinessException(400, "联盟名称不能为空");
            }
            if (name.length() > 200) {
                throw new BusinessException(400, "联盟名称过长");
            }
            LeagueCreateRequest req = new LeagueCreateRequest();
            req.setTenantId(Long.valueOf(tid));
            req.setApplicantUserId(userId);
            req.setName(name);
            req.setNameEn(payload == null ? null : payload.getNameEn());
            req.setDescription(payload == null ? null : payload.getDescription());
            req.setStatus(LeagueCreateRequest.STATUS_PENDING);
            LeagueCreateRequest saved = this.leagueCreateRequestRepository.save(req);
            return Map.of("pending", true, "requestId", saved.getId());
        }
        League created = this.leagueService.createInternal(payload);
        this.leagueOwnerAssignService.assignInternal(userId, Long.valueOf(tid), created.getId(),
                LeagueOwner.GRANT_SELF_CREATE);
        this.accountScopeService.evictUserScopeCacheAfterCommit(userId);
        return Map.of("pending", false, "id", created.getId());
    }

    /**
     * 审核通过：按申请回填建联盟，授权申请人，申请置 approved + leagueId + reviewed_*，失效申请人缓存。
     */
    @Transactional(rollbackFor = Exception.class)
    public League approve(Long reviewerId, Long requestId) {
        // TODO(批 3a 已裁定硬化项/I5)：approve 并发双击——沿用既有惯例（requirePending 校验 pending），
        //  同请求并发提交仍可能双建联盟，后续批次以乐观锁/状态条件更新硬化。
        LeagueCreateRequest req = this.requirePending(reviewerId, requestId);
        League payload = new League();
        payload.setName(req.getName());
        payload.setNameEn(req.getNameEn());
        payload.setDescription(req.getDescription());
        // I1：联盟必须落在「申请租户」，而非审核者上下文租户（超管全局 token=0）。
        League created = this.leagueService.createInternalForTenant(payload, req.getTenantId());
        this.leagueOwnerAssignService.assignInternal(req.getApplicantUserId(), req.getTenantId(), created.getId(),
                LeagueOwner.GRANT_SELF_CREATE);
        req.setStatus(LeagueCreateRequest.STATUS_APPROVED);
        req.setLeagueId(created.getId());
        req.setReviewedBy(reviewerId);
        req.setReviewedAt(LocalDateTime.now());
        this.leagueCreateRequestRepository.save(req);
        this.accountScopeService.evictUserScopeCacheAfterCommit(req.getApplicantUserId());
        return created;
    }

    /**
     * 审核驳回：申请置 rejected + rejectReason + reviewed_*。
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long reviewerId, Long requestId, String reason) {
        LeagueCreateRequest req = this.requirePending(reviewerId, requestId);
        // M5：驳回原因长度校验（列定义 length=500，超长则 400，避免落库截断/报错）。
        if (reason != null && reason.length() > 500) {
            throw new BusinessException(400, "驳回原因过长");
        }
        req.setStatus(LeagueCreateRequest.STATUS_REJECTED);
        req.setRejectReason(reason);
        req.setReviewedBy(reviewerId);
        req.setReviewedAt(LocalDateTime.now());
        this.leagueCreateRequestRepository.save(req);
    }

    /**
     * 待审申请分页：默认 page=1、pageSize=20；管理员可见。
     * 超管全局令牌（tenant=0/null，isGlobalQueryMode）下跨租户列举，与 approve/reject 的超管跨租户语义一致；
     * 其余（含切租户后的超管/租户管理员）限本租户（批 3a 沉淀⑨收敛）。
     */
    public PageResult<LeagueCreateRequest> listPending(Long reviewerId, Integer page, Integer pageSize) {
        // TODO(批 3a 已裁定硬化项/M4)：当前为内存分页（全量拉取后 subList）；数据量增大后改为仓储层分页。
        this.assertReviewer(reviewerId);
        List<LeagueCreateRequest> all;
        if (this.apiPermissionService.isSuperAdmin(reviewerId) && this.tenantQueryPolicyService.isGlobalQueryMode()) {
            all = this.leagueCreateRequestRepository.findByStatusAndDeletedAtIsNull(LeagueCreateRequest.STATUS_PENDING);
        } else {
            long tid = this.tenantQueryPolicyService.requiredTenantId();
            all = this.leagueCreateRequestRepository
                    .findByTenantIdAndStatusAndDeletedAtIsNull(Long.valueOf(tid), LeagueCreateRequest.STATUS_PENDING);
        }
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
