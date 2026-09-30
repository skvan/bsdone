/*
 * 账号权限重构（批次 3a）：联盟主办方指派服务（LeagueOwnerAssignService）。
 *
 * 职责：维护 bs_league_owner 关系（多主办方并存），供管理端指派 / 交接 / 撤回，以及
 * 自助创建链路（LeagueProvisionService）的程序化写入。
 *  - assignInternal：内部 / 程序化指派（不校验 operator），幂等插入 + 失效该用户范围缓存；
 *  - assign / revoke / handover：管理端操作，先校验 operator 为超管 / 租户管理员；
 *  - 失效语义：任何关系变更后 evict 相关用户的 AccountScopeService 范围缓存。
 *
 * 语义铁律（对齐 spec §6.1）：
 *  - 存量联盟无归属行 = 平台代管；管理员可后补指派（不做数据回填）；
 *  - 撤回沿用仓库既有惯例（对齐 TeamManagerApi.remove）：status=inactive + 软删（deletedAt），
 *    以释放部分唯一键槽位，供再指派时重新落 active 行。
 */
package com.bsball.service;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.League;
import com.bsball.model.entity.LeagueOwner;
import com.bsball.repository.LeagueOwnerRepository;
import com.bsball.repository.LeagueRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeagueOwnerAssignService {

    private final LeagueOwnerRepository leagueOwnerRepository;
    private final LeagueRepository leagueRepository;
    private final ApiPermissionService apiPermissionService;
    private final AccountScopeService accountScopeService;

    /**
     * 内部 / 程序化指派（不校验 operator）：供自助创建链路（LeagueProvisionService）复用。
     * <p>幂等：同一 (league, user) 已存在未软删行时不重复插入（no-op，返回 null）。
     * 插入后失效该用户范围缓存。
     */
    @Transactional(rollbackFor = Exception.class)
    public LeagueOwner assignInternal(Long userId, Long tenantId, Long leagueId, String source) {
        if (userId == null || tenantId == null || leagueId == null) return null;
        if (this.leagueOwnerRepository.existsByLeagueIdAndUserIdAndDeletedAtIsNull(leagueId, userId)) {
            return null;
        }
        LeagueOwner owner = new LeagueOwner();
        owner.setTenantId(tenantId);
        owner.setLeagueId(leagueId);
        owner.setUserId(userId);
        owner.setStatus(LeagueOwner.STATUS_ACTIVE);
        owner.setGrantSource(source == null ? LeagueOwner.GRANT_ADMIN_ASSIGN : source);
        LeagueOwner saved = this.leagueOwnerRepository.save(owner);
        this.accountScopeService.evictUserScopeCache(userId);
        return saved;
    }

    /** 管理端指派：校验 operator 为超管 / 租户管理员；league 存在且同租户；source=ADMIN_ASSIGN。 */
    @Transactional(rollbackFor = Exception.class)
    public LeagueOwner assign(Long operatorId, Long leagueId, Long userId) {
        this.assertAdmin(operatorId);
        if (userId == null) throw new BusinessException(400, "userId 不能为空");
        League league = this.requireLeagueForOperator(operatorId, leagueId);
        return this.assignInternal(userId, league.getTenantId(), leagueId, LeagueOwner.GRANT_ADMIN_ASSIGN);
    }

    /** 管理端撤回：把该 (league, user) 的 active 行置 inactive 并软删，失效该用户缓存。 */
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long operatorId, Long leagueId, Long userId) {
        this.assertAdmin(operatorId);
        if (userId == null) throw new BusinessException(400, "userId 不能为空");
        this.requireLeagueForOperator(operatorId, leagueId);
        this.revokeInternal(leagueId, userId, operatorId);
        this.accountScopeService.evictUserScopeCache(userId);
    }

    /** 管理端交接：revoke(from) + assignInternal(to, ADMIN_ASSIGN)，失效双方缓存。 */
    @Transactional(rollbackFor = Exception.class)
    public void handover(Long operatorId, Long leagueId, Long fromUserId, Long toUserId) {
        this.assertAdmin(operatorId);
        if (toUserId == null) throw new BusinessException(400, "userId 不能为空");
        League league = this.requireLeagueForOperator(operatorId, leagueId);
        if (fromUserId != null) {
            this.revokeInternal(leagueId, fromUserId, operatorId);
            this.accountScopeService.evictUserScopeCache(fromUserId);
        }
        this.assignInternal(toUserId, league.getTenantId(), leagueId, LeagueOwner.GRANT_ADMIN_ASSIGN);
        this.accountScopeService.evictUserScopeCache(toUserId);
    }

    /** 撤回内核：不校验 operator、不失效缓存，供 revoke / handover 复用。 */
    private void revokeInternal(Long leagueId, Long userId, Long operatorId) {
        LocalDateTime now = LocalDateTime.now();
        List<LeagueOwner> actives = this.leagueOwnerRepository
                .findByLeagueIdAndStatusAndDeletedAtIsNull(leagueId, LeagueOwner.STATUS_ACTIVE);
        for (LeagueOwner owner : actives) {
            if (userId.equals(owner.getUserId())) {
                owner.setStatus(LeagueOwner.STATUS_INACTIVE);
                owner.setDeletedAt(now);
                owner.setDeletedBy(operatorId);
                this.leagueOwnerRepository.save(owner);
            }
        }
    }

    /** operator 校验：须为超管 / 租户管理员，否则 403（未登录 401）。 */
    private void assertAdmin(Long operatorId) {
        if (operatorId == null) throw new BusinessException(401, "请先登录");
        if (!this.apiPermissionService.isSuperAdmin(operatorId) && !this.apiPermissionService.isTenantAdmin(operatorId)) {
            throw new BusinessException(403, "仅管理员可管理联盟主办方");
        }
    }

    /** league 存在 + 同租户校验（超管跨租户放行）；风格对齐现有服务（400/403 文案）。 */
    private League requireLeagueForOperator(Long operatorId, Long leagueId) {
        League league = leagueId == null ? null : this.leagueRepository.findById(leagueId).orElse(null);
        if (league == null || league.getDeletedAt() != null) {
            throw new BusinessException(400, "联盟不存在");
        }
        if (!this.apiPermissionService.isSuperAdmin(operatorId)) {
            Long tenantId = CurrentUserHolder.getTenantId();
            if (tenantId != null && !tenantId.equals(league.getTenantId())) {
                throw new BusinessException(403, "无权管理该联盟");
            }
        }
        return league;
    }

    @Generated
    public LeagueOwnerAssignService(LeagueOwnerRepository leagueOwnerRepository, LeagueRepository leagueRepository,
            ApiPermissionService apiPermissionService, AccountScopeService accountScopeService) {
        this.leagueOwnerRepository = leagueOwnerRepository;
        this.leagueRepository = leagueRepository;
        this.apiPermissionService = apiPermissionService;
        this.accountScopeService = accountScopeService;
    }
}
