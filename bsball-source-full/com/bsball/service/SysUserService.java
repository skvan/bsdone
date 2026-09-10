/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.dto.TenantBrief
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysRole
 *  com.bsball.model.entity.SysTenant
 *  com.bsball.model.entity.SysUser
 *  com.bsball.model.entity.SysUserRole
 *  com.bsball.model.entity.SysUserTenant
 *  com.bsball.repository.SysRoleRepository
 *  com.bsball.repository.SysTenantRepository
 *  com.bsball.repository.SysUserRepository
 *  com.bsball.repository.SysUserRoleRepository
 *  com.bsball.repository.SysUserTenantRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.SysUserService
 *  com.bsball.service.SysUserTenantManageService
 *  com.bsball.utils.PasswordEncoder
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
import com.bsball.common.PaginationSupport;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.TenantBrief;
import com.bsball.model.entity.BaseEntity;
import com.bsball.model.entity.SysRole;
import com.bsball.model.entity.SysTenant;
import com.bsball.model.entity.SysUser;
import com.bsball.model.entity.SysUserRole;
import com.bsball.model.entity.SysUserTenant;
import com.bsball.repository.SysRoleRepository;
import com.bsball.repository.SysTenantRepository;
import com.bsball.repository.SysUserRepository;
import com.bsball.repository.SysUserRoleRepository;
import com.bsball.repository.SysUserTenantRepository;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.SysUserTenantManageService;
import com.bsball.utils.PasswordEncoder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
public class SysUserService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9]{4,16}$");
    private final SysUserRepository sysUserRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final SysUserTenantRepository sysUserTenantRepository;
    private final SysTenantRepository sysTenantRepository;
    private final SysUserTenantManageService sysUserTenantManageService;
    private final ApiPermissionService apiPermissionService;
    private final SysRoleRepository sysRoleRepository;

    private static void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException(400, "\u7528\u6237\u540d\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (!USERNAME_PATTERN.matcher(username.trim()).matches()) {
            throw new BusinessException(400, "\u7528\u6237\u540d\u53ea\u80fd\u5305\u542b\u5c0f\u5199\u5b57\u6bcd\u548c\u6570\u5b57\uff0c\u957f\u5ea6 4\uff5e16 \u4f4d");
        }
    }

    public PageResult<SysUser> list(Integer page, Integer pageSize, String keyword, boolean allTenants) {
        Pageable p = this.buildPageable(page, pageSize);
        Long op = CurrentUserHolder.get();
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            Long tid;
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            preds.add(cb.isNull((Expression)root.get("deletedAt")));
            if (keyword != null && !keyword.isBlank()) {
                String k = "%" + keyword.toLowerCase() + "%";
                preds.add(cb.or(new Predicate[]{cb.like(cb.lower((Expression)root.get("username")), k), cb.like(cb.lower((Expression)root.get("nickname")), k), cb.like(cb.lower(cb.coalesce((Expression)root.get("email"), cb.literal((Object)""))), k), cb.like(cb.lower(cb.coalesce((Expression)root.get("phone"), cb.literal((Object)""))), k), cb.like(cb.lower(cb.coalesce((Expression)root.get("description"), cb.literal((Object)""))), k)}));
            }
            if (this.shouldRestrictUsersByCurrentTenant(op, allTenants) && (tid = CurrentUserHolder.getTenantId()) != null) {
                Subquery sq = q.subquery(Long.class);
                Root ut = sq.from(SysUserTenant.class);
                sq.select((Expression)ut.get("userId"));
                sq.where(new Predicate[]{cb.equal((Expression)ut.get("userId"), (Expression)root.get("id")), cb.equal((Expression)ut.get("tenantId"), (Object)tid), cb.isNull((Expression)ut.get("deletedAt"))});
                preds.add(cb.exists(sq));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Page result = this.sysUserRepository.findAll((Specification)spec, p);
        List users = result.getContent();
        this.fillRoleIds(users);
        this.fillTenants(users);
        return PageResult.of((List)users, (long)result.getTotalElements());
    }

    public SysUser get(Long id) {
        Long tid;
        SysUser user = this.sysUserRepository.findById((Object)id).orElse(null);
        if (user == null || user.getDeletedAt() != null) {
            throw new BusinessException(404, "\u7528\u6237\u4e0d\u5b58\u5728");
        }
        Long op = CurrentUserHolder.get();
        if (this.shouldRestrictUsersByCurrentTenant(op, false) && ((tid = CurrentUserHolder.getTenantId()) == null || !this.sysUserTenantRepository.existsByUserIdAndTenantIdAndDeletedAtIsNull(id, tid))) {
            throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u7528\u6237");
        }
        this.fillRoleIds(List.of((Object)user));
        this.fillTenants(List.of((Object)user));
        return user;
    }

    @Transactional
    public SysUser create(SysUser entity) {
        boolean targetIsSystemAdmin;
        SysUserService.validateUsername((String)entity.getUsername());
        if (this.sysUserRepository.findByUsernameAndDeletedAtIsNull(entity.getUsername().trim()).isPresent()) {
            throw new BusinessException(400, "\u7528\u6237\u540d\u5df2\u5b58\u5728");
        }
        Long op = CurrentUserHolder.get();
        List reqTenants = entity.getTenantIds();
        if (reqTenants != null && !reqTenants.isEmpty() && !this.canAssignTenantMembershipOnWrite(op, reqTenants)) {
            throw new BusinessException(403, "\u4ec5\u8d85\u7ea7\u7ba1\u7406\u5458\u53ef\u6307\u5b9a\u591a\u4e2a\u79df\u6237\uff1b\u79df\u6237\u7ba1\u7406\u5458\u4ec5\u53ef\u5c06\u7528\u6237\u52a0\u5165\u5f53\u524d\u79df\u6237");
        }
        if (entity.getPassword() != null && !entity.getPassword().isEmpty() && !PasswordEncoder.isEncoded((String)entity.getPassword())) {
            entity.setPassword(PasswordEncoder.encode((CharSequence)entity.getPassword()));
        }
        entity.setUsername(entity.getUsername().trim());
        SysUser saved = (SysUser)this.sysUserRepository.save((Object)entity);
        List rolesToSave = this.sanitizeRoleIdsForTenantOperator(op, entity.getRoleIds());
        this.assertSuperAdminRoleUnique(saved.getId(), rolesToSave);
        if (rolesToSave != null && !rolesToSave.isEmpty()) {
            this.saveUserRoles(saved.getId(), rolesToSave);
        }
        if (targetIsSystemAdmin = this.containsSystemAdminRole(rolesToSave)) {
            if (reqTenants != null && !reqTenants.isEmpty()) {
                throw new BusinessException(400, "\u8d85\u7ea7\u7ba1\u7406\u5458\u8d26\u53f7\u4e0d\u5e94\u7ed1\u5b9a\u79df\u6237");
            }
            this.sysUserTenantRepository.deleteByUserIdHard(saved.getId());
        } else if (reqTenants != null && !reqTenants.isEmpty()) {
            this.sysUserTenantManageService.replace(CurrentUserHolder.get().longValue(), saved.getId().longValue(), reqTenants);
        } else {
            this.assignDefaultTenantMembership(saved.getId());
        }
        this.fillRoleIds(List.of((Object)saved));
        this.fillTenants(List.of((Object)saved));
        return saved;
    }

    @Transactional
    public SysUser update(Long id, SysUser entity) {
        List effectiveRoleIds;
        boolean targetIsSystemAdmin;
        Long op = CurrentUserHolder.get();
        SysUser existing = this.sysUserRepository.findById((Object)id).orElse(null);
        if (existing == null) {
            return null;
        }
        if (this.shouldRestrictUsersByCurrentTenant(op, false)) {
            Long tid = CurrentUserHolder.getTenantId();
            if (tid == null || !this.sysUserTenantRepository.existsByUserIdAndTenantIdAndDeletedAtIsNull(id, tid)) {
                throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u7528\u6237");
            }
            if (this.apiPermissionService.isTenantAdmin(op) && !this.apiPermissionService.isSuperAdmin(op) && this.apiPermissionService.isSuperAdmin(id)) {
                throw new BusinessException(403, "\u4e0d\u80fd\u4fee\u6539\u8d85\u7ea7\u7ba1\u7406\u5458");
            }
        }
        if (entity.getTenantIds() != null && !this.canAssignTenantMembershipOnWrite(op, entity.getTenantIds())) {
            throw new BusinessException(403, "\u4ec5\u8d85\u7ea7\u7ba1\u7406\u5458\u53ef\u4fee\u6539\u591a\u79df\u6237\u6210\u5458\uff1b\u79df\u6237\u7ba1\u7406\u5458\u4ec5\u53ef\u5c06\u7528\u6237\u7ed1\u5b9a\u5230\u5f53\u524d\u79df\u6237");
        }
        if (entity.getUsername() != null && !entity.getUsername().trim().equals(existing.getUsername())) {
            SysUserService.validateUsername((String)entity.getUsername());
            if (this.sysUserRepository.findByUsernameAndDeletedAtIsNull(entity.getUsername().trim()).isPresent()) {
                throw new BusinessException(400, "\u7528\u6237\u540d\u5df2\u5b58\u5728");
            }
            entity.setUsername(entity.getUsername().trim());
        } else if (entity.getUsername() != null) {
            entity.setUsername(existing.getUsername());
        }
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        entity.setCreatedBy(existing.getCreatedBy());
        if (entity.getPassword() != null && !entity.getPassword().isEmpty() && !PasswordEncoder.isEncoded((String)entity.getPassword())) {
            entity.setPassword(PasswordEncoder.encode((CharSequence)entity.getPassword()));
        } else if (entity.getPassword() == null || entity.getPassword().isEmpty()) {
            entity.setPassword(existing.getPassword());
        }
        if (entity.getRoleIds() != null) {
            List newRoleIds;
            List sanitized = this.sanitizeRoleIdsForTenantOperator(op, entity.getRoleIds());
            List existingRoleIds = this.sysUserRoleRepository.findByUserId(id).stream().map(SysUserRole::getRoleId).sorted().toList();
            if (!existingRoleIds.equals(newRoleIds = sanitized.stream().sorted().toList())) {
                this.assertSuperAdminRoleUnique(id, sanitized);
                this.sysUserRoleRepository.deleteByUserId(id);
                this.sysUserRoleRepository.flush();
                this.saveUserRoles(id, sanitized);
            }
        }
        if (targetIsSystemAdmin = this.containsSystemAdminRole(effectiveRoleIds = entity.getRoleIds() != null ? this.sanitizeRoleIdsForTenantOperator(op, entity.getRoleIds()) : this.sysUserRoleRepository.findByUserId(id).stream().map(SysUserRole::getRoleId).toList())) {
            if (entity.getTenantIds() != null && !entity.getTenantIds().isEmpty()) {
                throw new BusinessException(400, "\u8d85\u7ea7\u7ba1\u7406\u5458\u8d26\u53f7\u4e0d\u5e94\u7ed1\u5b9a\u79df\u6237");
            }
            this.sysUserTenantRepository.deleteByUserIdHard(id);
        }
        SysUser saved = (SysUser)this.sysUserRepository.save((Object)entity);
        if (!targetIsSystemAdmin && entity.getTenantIds() != null) {
            this.sysUserTenantManageService.replace(CurrentUserHolder.get().longValue(), id.longValue(), entity.getTenantIds());
        }
        this.fillRoleIds(List.of((Object)saved));
        this.fillTenants(List.of((Object)saved));
        return saved;
    }

    private boolean containsSystemAdminRole(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        SysRole admin = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        return admin != null && roleIds.contains(admin.getId());
    }

    private void fillRoleIds(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        for (SysUser u : users) {
            List rids = this.sysUserRoleRepository.findByUserId(u.getId()).stream().map(SysUserRole::getRoleId).sorted().collect(Collectors.toList());
            u.setRoleIds(rids);
        }
    }

    private void fillTenants(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List userIds = users.stream().map(BaseEntity::getId).toList();
        List all = this.sysUserTenantRepository.findByUserIdInAndDeletedAtIsNull((Collection)userIds);
        Map userToTenantIds = all.stream().collect(Collectors.groupingBy(SysUserTenant::getUserId, Collectors.mapping(SysUserTenant::getTenantId, Collectors.toList())));
        HashSet allTenantIds = new HashSet();
        for (List ids : userToTenantIds.values()) {
            allTenantIds.addAll(ids);
        }
        HashMap tenantMap = new HashMap();
        if (!allTenantIds.isEmpty()) {
            this.sysTenantRepository.findAllById(allTenantIds).forEach(t -> tenantMap.put(t.getId(), t));
        }
        for (SysUser u : users) {
            List tids = userToTenantIds.getOrDefault(u.getId(), List.of()).stream().sorted().toList();
            List briefs = tids.stream().map(tid -> {
                SysTenant t = (SysTenant)tenantMap.get(tid);
                return t == null ? new TenantBrief(tid, "?", "?") : new TenantBrief(t.getId(), t.getName(), t.getCode());
            }).toList();
            u.setTenants(briefs);
            u.setTenantIds(tids);
        }
    }

    private void assignDefaultTenantMembership(Long userId) {
        Long tid = CurrentUserHolder.getTenantId();
        if (tid == null || !this.sysTenantRepository.findById((Object)tid).filter(t -> t.getDeletedAt() == null).isPresent()) {
            tid = this.sysTenantRepository.findFirstByDeletedAtIsNullOrderByIdAsc().map(BaseEntity::getId).orElse(null);
        }
        if (tid == null) {
            return;
        }
        SysUserTenant ut = new SysUserTenant();
        ut.setUserId(userId);
        ut.setTenantId(tid);
        this.sysUserTenantRepository.save((Object)ut);
    }

    private void assertSuperAdminRoleUnique(Long targetUserId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty() || targetUserId == null) {
            return;
        }
        SysRole admin = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        if (admin == null || !roleIds.contains(admin.getId())) {
            return;
        }
        long others = this.sysUserRoleRepository.countDistinctActiveUsersWithRoleExcept(admin.getId(), targetUserId);
        if (others > 0L) {
            throw new BusinessException(400, "\u8d85\u7ea7\u7ba1\u7406\u5458\u89d2\u8272\u53ea\u80fd\u7ed1\u5b9a\u4e00\u540d\u7528\u6237\uff0c\u8bf7\u5148\u89e3\u9664\u5176\u4ed6\u7528\u6237\u7684\u8be5\u89d2\u8272\u540e\u518d\u5206\u914d");
        }
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List list = roleIds.stream().map(roleId -> {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            return ur;
        }).toList();
        this.sysUserRoleRepository.saveAll((Iterable)list);
    }

    public void delete(Long id) {
        this.delete(id, null);
    }

    @Transactional
    public void delete(Long id, Long deletedBy) {
        SysUser u;
        Long op = CurrentUserHolder.get();
        if (this.shouldRestrictUsersByCurrentTenant(op, false)) {
            Long tid = CurrentUserHolder.getTenantId();
            if (tid == null || !this.sysUserTenantRepository.existsByUserIdAndTenantIdAndDeletedAtIsNull(id, tid)) {
                throw new BusinessException(403, "\u65e0\u6743\u5220\u9664\u8be5\u7528\u6237");
            }
            if (this.apiPermissionService.isTenantAdmin(op) && !this.apiPermissionService.isSuperAdmin(op) && this.apiPermissionService.isSuperAdmin(id)) {
                throw new BusinessException(403, "\u4e0d\u80fd\u5220\u9664\u8d85\u7ea7\u7ba1\u7406\u5458");
            }
        }
        if ((u = (SysUser)this.sysUserRepository.findById((Object)id).orElse(null)) != null) {
            u.setDeletedAt(LocalDateTime.now());
            u.setDeletedBy(deletedBy);
            this.sysUserRepository.save((Object)u);
            this.sysUserTenantRepository.deleteByUserIdHard(id);
        }
    }

    private boolean canAssignTenantMembershipOnWrite(Long operatorId, List<Long> tenantIds) {
        if (tenantIds == null) {
            return true;
        }
        if (tenantIds.isEmpty()) {
            return operatorId != null && this.apiPermissionService.isSuperAdmin(operatorId);
        }
        if (operatorId == null) {
            return false;
        }
        if (this.apiPermissionService.isSuperAdmin(operatorId)) {
            return true;
        }
        if (this.apiPermissionService.isTenantAdmin(operatorId)) {
            Long cur = CurrentUserHolder.getTenantId();
            return tenantIds.size() == 1 && cur != null && Objects.equals(tenantIds.get(0), cur);
        }
        return false;
    }

    private List<Long> sanitizeRoleIdsForTenantOperator(Long operatorId, List<Long> roleIds) {
        if (roleIds == null) {
            return null;
        }
        if (operatorId == null || this.apiPermissionService.isSuperAdmin(operatorId)) {
            return roleIds;
        }
        if (!this.apiPermissionService.isTenantAdmin(operatorId)) {
            return roleIds;
        }
        Long tid = CurrentUserHolder.getTenantId();
        SysRole admin = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        Long adminId = admin != null ? admin.getId() : null;
        List bound = this.sysRoleRepository.findAllById(roleIds);
        HashMap<Long, SysRole> byId = new HashMap<Long, SysRole>();
        for (SysRole r : bound) {
            byId.put(r.getId(), r);
        }
        return roleIds.stream().filter(rid -> adminId == null || !Objects.equals(rid, adminId)).filter(rid -> {
            SysRole r = (SysRole)byId.get(rid);
            if (r == null) {
                return false;
            }
            if (r.getTenantId() == null) {
                String c = r.getCode();
                return "tenant_admin".equals(c) || "guest".equals(c);
            }
            return tid != null && Objects.equals(r.getTenantId(), tid);
        }).collect(Collectors.toList());
    }

    private Pageable buildPageable(Integer page, Integer pageSize) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"id"}));
    }

    private boolean shouldRestrictUsersByCurrentTenant(Long operatorId, boolean allTenants) {
        if (operatorId == null) {
            return false;
        }
        boolean superAdminLike = this.isSuperAdminLike(operatorId);
        if (allTenants && superAdminLike) {
            return false;
        }
        if (CurrentUserHolder.getTenantId() == null) {
            return false;
        }
        return this.apiPermissionService.isTenantAdmin(operatorId) || superAdminLike;
    }

    private boolean isSuperAdminLike(Long userId) {
        if (userId == null) {
            return false;
        }
        if (this.apiPermissionService.isSuperAdmin(userId)) {
            return true;
        }
        SysUser u = this.sysUserRepository.findById((Object)userId).orElse(null);
        return u != null && "admin".equalsIgnoreCase(String.valueOf(u.getUsername()).trim());
    }

    @Generated
    public SysUserService(SysUserRepository sysUserRepository, SysUserRoleRepository sysUserRoleRepository, SysUserTenantRepository sysUserTenantRepository, SysTenantRepository sysTenantRepository, SysUserTenantManageService sysUserTenantManageService, ApiPermissionService apiPermissionService, SysRoleRepository sysRoleRepository) {
        this.sysUserRepository = sysUserRepository;
        this.sysUserRoleRepository = sysUserRoleRepository;
        this.sysUserTenantRepository = sysUserTenantRepository;
        this.sysTenantRepository = sysTenantRepository;
        this.sysUserTenantManageService = sysUserTenantManageService;
        this.apiPermissionService = apiPermissionService;
        this.sysRoleRepository = sysRoleRepository;
    }
}

