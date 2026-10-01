/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.config.AccountProperties
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.Player
 *  com.bsball.model.entity.PlayerClaim
 *  com.bsball.model.entity.PlayerClaimInvite
 *  com.bsball.model.entity.SysUser
 *  com.bsball.model.entity.Team
 *  com.bsball.model.entity.TeamManager
 *  com.bsball.repository.PlayerClaimInviteRepository
 *  com.bsball.repository.PlayerClaimRepository
 *  com.bsball.repository.PlayerRepository
 *  com.bsball.repository.SysUserRepository
 *  com.bsball.repository.TeamManagerRepository
 *  com.bsball.repository.TeamRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.PlayerClaimService
 *  jakarta.persistence.criteria.CriteriaBuilder
 *  jakarta.persistence.criteria.CriteriaQuery
 *  jakarta.persistence.criteria.Expression
 *  jakarta.persistence.criteria.Predicate
 *  jakarta.persistence.criteria.Root
 *  jakarta.persistence.criteria.Subquery
 *  lombok.Generated
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.config.AccountProperties;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.PlayerTeamEntryDto;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerClaim;
import com.bsball.model.entity.PlayerClaimInvite;
import com.bsball.model.entity.PlayerTeam;
import com.bsball.model.entity.SysUser;
import com.bsball.model.entity.Team;
import com.bsball.model.entity.TeamManager;
import com.bsball.repository.PlayerClaimInviteRepository;
import com.bsball.repository.PlayerClaimRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.PlayerTeamRepository;
import com.bsball.repository.SysUserRepository;
import com.bsball.repository.TeamManagerRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.ApiPermissionService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class PlayerClaimService {
    private final AccountProperties accountProperties;
    private final ApiPermissionService apiPermissionService;
    private final PlayerClaimRepository playerClaimRepository;
    private final PlayerClaimInviteRepository playerClaimInviteRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final TeamManagerRepository teamManagerRepository;
    private final SysUserRepository sysUserRepository;
    private final PlayerTeamService playerTeamService;
    private final AccountScopeService accountScopeService;
    private final ResourceGuard resourceGuard;
    private final PlayerService playerService;
    private final PersonnelHistoryRecorder personnelHistoryRecorder;
    private final PlayerTeamRepository playerTeamRepository;

    @Transactional
    public PlayerClaim submitClaim(Long userId, Long playerId, String remark, Long inviteId) {
        Player player = this.playerRepository.findById(playerId).orElse(null);
        if (player == null || player.getDeletedAt() != null) {
            throw new BusinessException(404, "\u7403\u5458\u4e0d\u5b58\u5728");
        }
        if (player.getUserId() != null) {
            throw new BusinessException(400, "\u8be5\u7403\u5458\u5df2\u88ab\u5176\u4ed6\u8d26\u53f7\u8ba4\u9886");
        }
        if (this.playerClaimRepository.existsByUserIdAndStatusAndDeletedAtIsNull(userId, "approved")) {
            throw new BusinessException(400, "\u60a8\u5df2\u6709\u901a\u8fc7\u8ba4\u8bc1\u7684\u7403\u5458\u8d44\u6599\uff0c\u4e0d\u80fd\u518d\u7533\u8bf7\u8ba4\u9886");
        }
        if (this.playerClaimRepository.existsByUserIdAndStatusAndDeletedAtIsNull(userId, "pending")) {
            throw new BusinessException(400, "\u60a8\u5df2\u6709\u5f85\u5ba1\u6838\u7684\u8ba4\u9886\u7533\u8bf7\uff0c\u8bf7\u5148\u53d6\u6d88\u540e\u518d\u7533\u8bf7");
        }
        this.playerClaimRepository.findTopByUserIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(userId, "cancelled").ifPresent(last -> {
            if (last.getUpdatedAt() != null) {
                LocalDateTime readyAt = PlayerClaimService.addBusinessDays((LocalDateTime)last.getUpdatedAt(), (int)3);
                if (LocalDateTime.now().isBefore(readyAt)) {
                    throw new BusinessException(400, "\u53d6\u6d88\u8ba4\u9886\u540e\u9700\u7b49\u5f85 3 \u4e2a\u5de5\u4f5c\u65e5\uff08\u81f3 " + readyAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\uff09\u624d\u80fd\u91cd\u65b0\u7533\u8bf7");
                }
            }
        });
        if (this.playerClaimRepository.existsByPlayerIdAndStatusAndDeletedAtIsNull(playerId, "pending")) {
            throw new BusinessException(400, "\u8be5\u7403\u5458\u5df2\u6709\u5f85\u5ba1\u6838\u7684\u8ba4\u9886\u7533\u8bf7");
        }
        String reviewerType = this.resolveReviewerType(player);
        LocalDateTime now = LocalDateTime.now();
        PlayerClaim claim = new PlayerClaim();
        claim.setTenantId(player.getTenantId());
        claim.setUserId(userId);
        claim.setPlayerId(playerId);
        claim.setStatus("pending");
        claim.setReviewerType(reviewerType);
        claim.setRemark(remark);
        claim.setInviteId(inviteId);
        claim.setCreatedAt(now);
        claim.setUpdatedAt(now);
        return (PlayerClaim)this.playerClaimRepository.save(claim);
    }

    public List<PlayerClaim> myClaims(Long userId) {
        return this.playerClaimRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
    }

    public PageResult<Map<String, Object>> pendingForReviewer(Long reviewerId, Integer page, Integer pageSize, String keyword, String reviewerType) {
        List kwUserIds;
        boolean superAdmin = this.apiPermissionService.isSuperAdmin(reviewerId);
        boolean tenantAdmin = this.apiPermissionService.isTenantAdmin(reviewerId);
        Collection<Long> managedTeamIds = this.accountScopeService.resolveCurrent().getTeamIds();
        String reviewerTypeFilter = reviewerType != null && !reviewerType.isBlank() ? reviewerType.trim() : null;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        List kwPlayerIds = hasKeyword ? this.playerRepository.findIdsByNameLike(keyword.trim()) : List.of();
        List list = kwUserIds = hasKeyword ? this.sysUserRepository.findIdsByKeyword(keyword.trim()) : List.of();
        if (hasKeyword && kwPlayerIds.isEmpty() && kwUserIds.isEmpty()) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        Specification spec = (root, q, cb) -> {
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            preds.add(cb.equal((Expression)root.get("status"), "pending"));
            preds.add(cb.isNull((Expression)root.get("deletedAt")));
            if (superAdmin || tenantAdmin) {
                // 管理员列表仅呈平台认领；team_manager 认领由球队管理员的范围内列表呈现；
                // 管理员仍可按 id 审任意认领（守卫对 unrestricted 直通）。
                preds.add(cb.equal((Expression)root.get("reviewerType"), "platform_admin"));
            } else if (!managedTeamIds.isEmpty()) {
                preds.add(cb.equal((Expression)root.get("reviewerType"), "team_manager"));
                preds.add(root.get("playerId").in(new Expression[]{this.subqueryPlayerIdsForTeams(cb, q, managedTeamIds)}));
            } else {
                preds.add(cb.disjunction());
            }
            if (reviewerTypeFilter != null) {
                preds.add(cb.equal((Expression)root.get("reviewerType"), (Object)reviewerTypeFilter));
            }
            if (hasKeyword) {
                ArrayList<Predicate> kw = new ArrayList<Predicate>();
                if (!kwPlayerIds.isEmpty()) {
                    kw.add(root.get("playerId").in((Collection)kwPlayerIds));
                }
                if (!kwUserIds.isEmpty()) {
                    kw.add(root.get("userId").in((Collection)kwUserIds));
                }
                preds.add(cb.or(kw.toArray(new Predicate[0])));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        PageRequest p = PageRequest.of((int)Math.max(0, page - 1), (int)Math.max(1, pageSize), (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdAt"}));
        Page result = this.playerClaimRepository.findAll((Specification)spec, (Pageable)p);
        return PageResult.of((List)this.enrichClaims(result.getContent()), (long)result.getTotalElements());
    }

    private List<Map<String, Object>> enrichClaims(List<PlayerClaim> list) {
        ArrayList<Map<String, Object>> rows = new ArrayList<Map<String, Object>>(list.size());
        if (list.isEmpty()) {
            return rows;
        }
        HashMap playerNames = new HashMap();
        for (PlayerClaim c : list) {
            if (c.getPlayerId() == null || playerNames.containsKey(c.getPlayerId())) continue;
            this.playerRepository.findById(c.getPlayerId()).ifPresent(pl -> playerNames.put(pl.getId(), pl.getName()));
        }
        HashMap users = new HashMap();
        for (PlayerClaim c : list) {
            if (c.getUserId() == null || users.containsKey(c.getUserId())) continue;
            this.sysUserRepository.findById(c.getUserId()).ifPresent(u -> users.put(u.getId(), u));
        }
        for (PlayerClaim c : list) {
            SysUser u2 = c.getUserId() != null ? (SysUser)users.get(c.getUserId()) : null;
            HashMap<String, Object> row = new HashMap<String, Object>();
            row.put("id", c.getId());
            row.put("playerId", c.getPlayerId());
            row.put("playerName", c.getPlayerId() != null ? (Object)playerNames.get(c.getPlayerId()) : null);
            row.put("userId", c.getUserId());
            row.put("username", u2 != null ? u2.getUsername() : null);
            row.put("realName", u2 != null ? u2.getRealName() : null);
            row.put("phone", u2 != null ? u2.getPhone() : null);
            row.put("email", u2 != null ? u2.getEmail() : null);
            row.put("reviewerType", c.getReviewerType());
            row.put("remark", c.getRemark());
            row.put("status", c.getStatus());
            row.put("createdAt", c.getCreatedAt());
            rows.add(row);
        }
        return rows;
    }

    @Transactional
    public PlayerClaim cancelClaim(Long userId, Long claimId) {
        PlayerClaim claim = (PlayerClaim)this.playerClaimRepository.findByIdAndDeletedAtIsNull(claimId).orElseThrow(() -> new BusinessException(404, "\u7533\u8bf7\u4e0d\u5b58\u5728"));
        if (!claim.getUserId().equals(userId)) {
            throw new BusinessException(403, "\u65e0\u6743\u64cd\u4f5c\u8be5\u7533\u8bf7");
        }
        if (!"pending".equals(claim.getStatus())) {
            throw new BusinessException(400, "\u4ec5\u5f85\u5ba1\u6838\u7684\u7533\u8bf7\u53ef\u53d6\u6d88");
        }
        claim.setStatus("cancelled");
        claim.setUpdatedAt(LocalDateTime.now());
        return (PlayerClaim)this.playerClaimRepository.save(claim);
    }

    private static LocalDateTime addBusinessDays(LocalDateTime t, int days) {
        LocalDateTime result = t;
        int added = 0;
        while (added < days) {
            DayOfWeek d = (result = result.plusDays(1L)).getDayOfWeek();
            if (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) continue;
            ++added;
        }
        return result;
    }

    @Transactional
    public PlayerClaim approve(Long claimId, Long reviewerId, String remark) {
        PlayerClaim claim = this.requireClaimForReview(claimId);
        Player player = (Player)this.playerRepository.findById(claim.getPlayerId()).orElseThrow(() -> new BusinessException(404, "\u7403\u5458\u4e0d\u5b58\u5728"));
        if (player.getUserId() != null) {
            throw new BusinessException(400, "\u8be5\u7403\u5458\u5df2\u88ab\u8ba4\u9886");
        }
        this.ensureSingleLeagueSingleTeam(claim.getUserId(), player);
        LocalDateTime now = LocalDateTime.now();
        claim.setStatus("approved");
        claim.setReviewerId(reviewerId);
        claim.setReviewedAt(now);
        claim.setUpdatedAt(now);
        this.playerClaimRepository.save(claim);
        player.setUserId(claim.getUserId());
        player.setUpdatedAt(now);
        this.playerRepository.save(player);
        this.accountScopeService.evictUserScopeCacheAfterCommit(claim.getUserId());
        return claim;
    }

    @Transactional
    public PlayerClaim reject(Long claimId, Long reviewerId, String reason) {
        PlayerClaim claim = this.requireClaimForReview(claimId);
        LocalDateTime now = LocalDateTime.now();
        claim.setStatus("rejected");
        claim.setReviewerId(reviewerId);
        claim.setReviewedAt(now);
        claim.setRejectReason(reason);
        claim.setUpdatedAt(now);
        return (PlayerClaim)this.playerClaimRepository.save(claim);
    }

    @Transactional
    public Map<String, Object> createInvite(Long creatorId, Long teamId, Long playerId, Integer expireHours, Integer maxUses, String remark) {
        // 批 2 沉淀⑩收敛：改用统一写保护守卫（ResourceGuard.assertCanManageTeam），与其它写路径同源。
        // 语义：租户内不受限（超管/租管）放行；受限身份需命中自有球队集合（super/tenant admin 之外仅团队负责人/联盟派生域），否则 403。
        this.resourceGuard.assertCanManageTeam(teamId);
        Team team = (Team)this.teamRepository.findById(teamId).orElseThrow(() -> new BusinessException(404, "球队不存在"));
        if (playerId != null) {
            Player p = (Player)this.playerRepository.findById(playerId).orElseThrow(() -> new BusinessException(404, "球员不存在"));
            // 保留租户一致性：删除 isCurrentlyInTeam 硬校验后，仍需保证受邀球员与球队同租户。
            if (!Objects.equals(p.getTenantId(), team.getTenantId())) {
                throw new BusinessException(400, "球员与球队不属于同一租户");
            }
            if (p.getUserId() != null) {
                throw new BusinessException(400, "该球员已被认领");
            }
        }
        int hours = expireHours != null && expireHours > 0 ? expireHours.intValue() : this.accountProperties.getInviteDefaultExpireHours();
        int uses = maxUses != null && maxUses > 0 ? maxUses.intValue() : this.accountProperties.getInviteDefaultMaxUses();
        LocalDateTime now = LocalDateTime.now();
        PlayerClaimInvite invite = new PlayerClaimInvite();
        invite.setTenantId(team.getTenantId());
        invite.setTeamId(teamId);
        invite.setPlayerId(playerId);
        invite.setToken(UUID.randomUUID().toString().replace("-", ""));
        invite.setCreatedBy(creatorId);
        invite.setExpiresAt(now.plusHours(hours));
        invite.setMaxUses(Integer.valueOf(uses));
        invite.setUsedCount(Integer.valueOf(0));
        invite.setStatus("active");
        invite.setRemark(remark);
        this.playerClaimInviteRepository.save(invite);
        HashMap<String, Object> out = new HashMap<String, Object>();
        out.put("id", invite.getId());
        out.put("token", invite.getToken());
        out.put("expiresAt", invite.getExpiresAt());
        out.put("teamId", teamId);
        out.put("playerId", playerId);
        return out;
    }

    public Map<String, Object> getInvitePublic(String token) {
        PlayerClaimInvite invite = this.requireActiveInvite(token);
        Team team = this.teamRepository.findById(invite.getTeamId()).orElse(null);
        HashMap<String, Object> out = new HashMap<String, Object>();
        out.put("token", invite.getToken());
        out.put("teamId", invite.getTeamId());
        out.put("teamName", team != null ? team.getName() : null);
        out.put("playerId", invite.getPlayerId());
        out.put("expiresAt", invite.getExpiresAt());
        out.put("remark", invite.getRemark());
        if (invite.getPlayerId() != null) {
            this.playerRepository.findById(invite.getPlayerId()).ifPresent(p -> {
                out.put("playerName", p.getName());
                out.put("playerNumber", p.getNumber());
            });
        }
        return out;
    }

    /**
     * 凭邀请接受：
     * <ul>
     *   <li>{@code targetPlayerId}（邀请绑定球员 或 入参 playerId）非空 → 原认领关联语义（经 {@link #submitClaim} 落一条待审 PlayerClaim）；</li>
     *   <li>{@code targetPlayerId} 为空 且 该用户已有档案 → 已在受邀队则幂等早返回；否则直接以存量档案入队
     *       （不调 {@link PlayerService#createSelfProfile}、不需 {@code draft}）；</li>
     *   <li>{@code targetPlayerId} 为空 且 该用户无档案 → 需 {@code draft} 非空，自助建档（复用 {@link PlayerService#createSelfProfile} 校验链）
     *       并写 player_team 入队（{@code current=true}）+ 镜像回写 + join 沿革 + currentJoinRecordId 回写；</li>
     *   <li>无目标球员且无档案且无草稿 → 400。</li>
     * </ul>
     * token 维持单次/限次 + 有效期；仅在真正发生写入的路径消费（建档+入队 / 存量入队 / 认领落库），幂等早返回不消费。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> claimOrRegisterViaInvite(Long userId, String token, Long playerId, Player draft) {
        PlayerClaimInvite invite = this.requireActiveInvite(token);
        Long targetPlayerId = invite.getPlayerId() != null ? invite.getPlayerId() : playerId;
        if (targetPlayerId != null) {
            return this.claimViaInviteForPlayer(userId, invite, targetPlayerId);
        }
        return this.registerAndJoinViaInvite(userId, invite, draft);
    }

    /** 原认领关联语义：保留租户一致性（跨租户拒绝）与「已认领」校验（由 submitClaim 兜底）。 */
    private Map<String, Object> claimViaInviteForPlayer(Long userId, PlayerClaimInvite invite, Long targetPlayerId) {
        Player player = (Player)this.playerRepository.findById(targetPlayerId).orElseThrow(() -> new BusinessException(404, "球员不存在"));
        if (!Objects.equals(player.getTenantId(), invite.getTenantId())) {
            throw new BusinessException(400, "球员与邀请球队不属于同一租户");
        }
        PlayerClaim claim = this.submitClaim(userId, targetPlayerId, null, invite.getId());
        // 认领落库属写入路径 → 消费邀请（幂等早返回等无写入路径不消费）。
        this.consumeInvite(invite);
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("mode", "claim");
        out.put("claimId", claim.getId());
        out.put("playerId", claim.getPlayerId());
        out.put("status", claim.getStatus());
        return out;
    }

    /**
     * 邀请入队（两条路径，均复用 {@link #applyInviteJoin} 落库）：
     * <ul>
     *   <li>存量档案（{@code existing != null}）：已在受邀队 → 幂等早返回（无写入、不消费令牌）；
     *       否则直接以存量档案入队（不建档、不需 draft）；</li>
     *   <li>无档案（{@code existing == null}）：需 draft 非空，自助建档后入队。</li>
     * </ul>
     */
    private Map<String, Object> registerAndJoinViaInvite(Long userId, PlayerClaimInvite invite, Player draft) {
        Long teamId = invite.getTeamId();
        Player existing = this.playerRepository.findFirstByUserIdAndDeletedAtIsNull(userId).orElse(null);
        if (existing != null) {
            if (this.playerTeamService.currentTeamIds(existing.getId()).contains(teamId)) {
                // 幂等早返回：已在受邀队，无写入 → 不消费令牌。
                return this.joinResult(existing.getId(), teamId, "already_joined");
            }
            // 存量档案直接入队（不调 createSelfProfile、不需 draft）。
            this.applyInviteJoin(existing, teamId);
            this.accountScopeService.evictUserScopeCacheAfterCommit(userId);
            this.consumeInvite(invite);
            return this.joinResult(existing.getId(), teamId, "joined");
        }
        if (draft == null) {
            throw new BusinessException(400, "请指定要认领的球员或提供建档信息");
        }
        // TODO(批3b硬化)：并发双击可能双建档（bs_player.user_id 无唯一约束）；见
        // PlayerService.createSelfProfile 的 ux_bs_player_user_active 唯一索引硬化，本轮不改约束。
        Player created = this.playerService.createSelfProfile(userId, draft);
        this.applyInviteJoin(created, teamId);
        this.accountScopeService.evictUserScopeCacheAfterCommit(userId);
        this.consumeInvite(invite);
        return this.joinResult(created.getId(), teamId, "registered");
    }

    /**
     * 入队落库：<b>追加语义</b>——desired = 既有全部经历（当前字段原样保留） + 受邀队经历（current=true）。
     * 受邀队已在经历中则仅置该队 current=true；否则追加新经历。以完整 desired 调 plan，{@code toDelete} 恒为空，
     * 杜绝“存量档案凭邀请入队时其余经历被静默软删”。落库前对 {@code toDelete} 做防御性删除守卫（双保险）。
     * 随后写镜像 + join 沿革，并回写 currentJoinRecordId（对齐 PlayerService.create）。
     */
    private void applyInviteJoin(Player player, Long teamId) {
        List<PlayerTeam> existing = player.getId() == null ? List.of()
                : this.playerTeamRepository.findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(player.getId());
        List<PlayerTeamEntryDto> desired = new ArrayList<>(existing.size() + 1);
        boolean invitedPresent = false;
        for (PlayerTeam e : existing) {
            if (Objects.equals(e.getTeamId(), teamId)) {
                // 受邀队已存在：仅置 current=true，余字段原样保留。
                desired.add(new PlayerTeamEntryDto(e.getId(), e.getTeamId(), null, e.getNumber(),
                        e.getPositionsList(), Boolean.TRUE, e.getSort()));
                invitedPresent = true;
            } else {
                desired.add(new PlayerTeamEntryDto(e.getId(), e.getTeamId(), null, e.getNumber(),
                        e.getPositionsList(), Boolean.TRUE.equals(e.getCurrent()), e.getSort()));
            }
        }
        if (!invitedPresent) {
            desired.add(new PlayerTeamEntryDto(null, teamId, null, null, null, Boolean.TRUE, null));
        }
        PlayerTeamService.PlayerTeamSyncPlan plan = this.playerTeamService.plan(player, desired, true);
        // 防御双保险：追加语义下 toDelete 应为空；若非空（异常情形）须过删除守卫，杜绝静默软删（复用 PlayerService 守卫，勿复制）。
        if (!plan.toDelete().isEmpty()) {
            this.playerService.assertEntriesDeletable(player.getId(), plan.toDelete());
        }
        this.playerTeamService.applyMirror(player, plan);
        Player saved = (Player)this.playerRepository.save(player);
        this.playerTeamService.persistPlan(saved.getId(), plan);
        Long joinRecordId = this.personnelHistoryRecorder.recordPlayerTeamTransitions(
                saved, plan.beforeCurrentTeamIds(), plan.afterCurrentTeamIds());
        if (joinRecordId != null) {
            saved.setCurrentJoinRecordId(joinRecordId);
            this.playerRepository.save(saved);
        }
    }

    /**
     * 邀请入队结果：mode 透传子语义——新建档入队={@code registered}、存量档案直接入队={@code joined}、
     * 已在队幂等早返回={@code already_joined}（前端据此区分提示与埋点，三者均为终态成功）。
     */
    private Map<String, Object> joinResult(Long playerId, Long teamId, String mode) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("mode", mode);
        out.put("playerId", playerId);
        out.put("teamId", teamId);
        return out;
    }

    /** 消费邀请：计数 +1，达上限置 expired（现状机制）。 */
    private void consumeInvite(PlayerClaimInvite invite) {
        invite.setUsedCount(Integer.valueOf(invite.getUsedCount() + 1));
        if (invite.getUsedCount() >= invite.getMaxUses()) {
            invite.setStatus("expired");
        }
        invite.setUpdatedAt(LocalDateTime.now());
        this.playerClaimInviteRepository.save(invite);
    }

    private void ensureSingleLeagueSingleTeam(Long userId, Player target) {
        if (userId == null || target.getTeamId() == null) {
            return;
        }
        Team team = this.teamRepository.findById(target.getTeamId()).orElse(null);
        if (team == null || team.getLeagueId() == null || team.getLeagueId() == 0L) {
            return;
        }
        long cnt = this.playerRepository.countClaimedByUserInLeagueExcludingTeam(userId.longValue(), team.getLeagueId().longValue(), target.getId().longValue(), team.getId().longValue());
        if (cnt > 0L) {
            throw new BusinessException(400, "\u8ba4\u9886\u5931\u8d25\uff1a\u540c\u4e00\u8054\u76df\u5185\u60a8\u5df2\u8ba4\u9886\u5176\u4ed6\u7403\u961f\u7684\u7403\u5458\uff0c\u6bcf\u540d\u7403\u5458\u5728\u5355\u4e00\u8054\u76df\u53ea\u80fd\u53c2\u52a0\u4e00\u961f");
        }
    }

    /**
     * 校验认领可审：授权主体恒为 CurrentUserHolder（经范围中枢/守卫判定）；本方法不含 reviewerId 入参，
     * reviewerId 由调用方在落库时留痕，不参与判定。
     */
    private PlayerClaim requireClaimForReview(Long claimId) {
        PlayerClaim claim = (PlayerClaim)this.playerClaimRepository.findByIdAndDeletedAtIsNull(claimId).orElseThrow(() -> new BusinessException(404, "\u7533\u8bf7\u4e0d\u5b58\u5728"));
        if (!"pending".equals(claim.getStatus())) {
            throw new BusinessException(400, "\u8be5\u7533\u8bf7\u5df2\u5904\u7406");
        }
        this.resourceGuard.assertCanReviewClaim(claim);
        return claim;
    }

    private String resolveReviewerType(Player player) {
        for (Long teamId : this.playerTeamService.currentTeamIds(player.getId())) {
            List<TeamManager> managers = this.teamManagerRepository.findByTeamIdAndStatusAndDeletedAtIsNull(teamId, "active");
            if (!managers.isEmpty()) {
                return "team_manager";
            }
        }
        return "platform_admin";
    }

    private PlayerClaimInvite requireActiveInvite(String token) {
        PlayerClaimInvite invite = (PlayerClaimInvite)this.playerClaimInviteRepository.findByTokenAndDeletedAtIsNull(token).orElseThrow(() -> new BusinessException(404, "\u9080\u8bf7\u94fe\u63a5\u65e0\u6548"));
        if (!"active".equals(invite.getStatus())) {
            throw new BusinessException(400, "\u9080\u8bf7\u94fe\u63a5\u5df2\u5931\u6548");
        }
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus("expired");
            this.playerClaimInviteRepository.save(invite);
            throw new BusinessException(400, "\u9080\u8bf7\u94fe\u63a5\u5df2\u8fc7\u671f");
        }
        if (invite.getUsedCount() >= invite.getMaxUses()) {
            throw new BusinessException(400, "\u9080\u8bf7\u94fe\u63a5\u5df2\u8fbe\u4f7f\u7528\u4e0a\u9650");
        }
        return invite;
    }

    private Subquery<Long> subqueryPlayerIdsForTeams(CriteriaBuilder cb, CriteriaQuery<?> q, Collection<Long> teamIds) {
        Subquery<Long> sq = q.subquery(Long.class);
        Root<PlayerTeam> entry = sq.from(PlayerTeam.class);
        sq.select((Expression)entry.get("playerId"));
        if (teamIds == null || teamIds.isEmpty()) {
            sq.where((Expression)cb.disjunction());
            return sq;
        }
        sq.where((Expression)cb.and(
                entry.get("teamId").in(teamIds),
                cb.isTrue(entry.get("current")),
                cb.isNull((Expression)entry.get("deletedAt"))));
        return sq;
    }

    @Generated
    public PlayerClaimService(AccountProperties accountProperties, ApiPermissionService apiPermissionService, PlayerClaimRepository playerClaimRepository, PlayerClaimInviteRepository playerClaimInviteRepository, PlayerRepository playerRepository, TeamRepository teamRepository, TeamManagerRepository teamManagerRepository, SysUserRepository sysUserRepository, PlayerTeamService playerTeamService, AccountScopeService accountScopeService, ResourceGuard resourceGuard, PlayerService playerService, PersonnelHistoryRecorder personnelHistoryRecorder, PlayerTeamRepository playerTeamRepository) {
        this.accountProperties = accountProperties;
        this.apiPermissionService = apiPermissionService;
        this.playerClaimRepository = playerClaimRepository;
        this.playerClaimInviteRepository = playerClaimInviteRepository;
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.teamManagerRepository = teamManagerRepository;
        this.sysUserRepository = sysUserRepository;
        this.playerTeamService = playerTeamService;
        this.accountScopeService = accountScopeService;
        this.resourceGuard = resourceGuard;
        this.playerService = playerService;
        this.personnelHistoryRecorder = personnelHistoryRecorder;
        this.playerTeamRepository = playerTeamRepository;
    }
}

