/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysRole
 *  com.bsball.model.entity.SysRoleApi
 *  com.bsball.model.entity.SysRoleMenu
 *  com.bsball.model.entity.SysTenant
 *  com.bsball.repository.SysRoleApiRepository
 *  com.bsball.repository.SysRoleMenuRepository
 *  com.bsball.repository.SysRoleRepository
 *  com.bsball.repository.SysTenantRepository
 *  com.bsball.repository.SysUserRoleRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.MenuExpansionHelper
 *  com.bsball.service.SysRoleService
 *  lombok.Generated
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.BaseEntity;
import com.bsball.model.entity.SysRole;
import com.bsball.model.entity.SysRoleApi;
import com.bsball.model.entity.SysRoleMenu;
import com.bsball.model.entity.SysTenant;
import com.bsball.repository.SysRoleApiRepository;
import com.bsball.repository.SysRoleMenuRepository;
import com.bsball.repository.SysRoleRepository;
import com.bsball.repository.SysTenantRepository;
import com.bsball.repository.SysUserRoleRepository;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.MenuExpansionHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class SysRoleService {
    private static final List<Long> ADMIN_REQUIRED_MENU_IDS = List.of((Object)1L, (Object)4L, (Object)5L, (Object)6L);
    private static final List<String> ASSIGN_OPTION_SYSTEM_CODES = List.of((Object)"tenant_admin", (Object)"guest");
    private final ApiPermissionService apiPermissionService;
    private final MenuExpansionHelper menuExpansionHelper;
    private final SysRoleRepository sysRoleRepository;
    private final SysRoleMenuRepository sysRoleMenuRepository;
    private final SysRoleApiRepository sysRoleApiRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final SysTenantRepository sysTenantRepository;

    public PageResult<SysRole> list(Long operatorUserId, Integer page, Integer pageSize, String keyword) {
        Page result;
        String kw;
        Pageable p = this.buildPageable(page, pageSize);
        String string = kw = keyword != null ? keyword.trim() : "";
        if (kw.isEmpty()) {
            kw = null;
        }
        if (this.apiPermissionService.isSuperAdmin(operatorUserId)) {
            result = kw == null ? this.sysRoleRepository.findAll(p) : this.sysRoleRepository.findAllWithKeyword(kw, p);
        } else if (this.apiPermissionService.isTenantAdmin(operatorUserId)) {
            Long tid = CurrentUserHolder.getTenantId();
            if (tid == null) {
                throw new BusinessException(400, "\u65e0\u6cd5\u786e\u5b9a\u5f53\u524d\u79df\u6237");
            }
            result = kw == null ? this.sysRoleRepository.findByTenantId(tid, p) : this.sysRoleRepository.findByTenantIdAndKeyword(tid, kw, p);
        } else {
            result = Page.empty((Pageable)p);
        }
        List roles = result.getContent();
        this.fillMenuIds(roles);
        this.fillApiIds(roles);
        return PageResult.of((List)roles, (long)result.getTotalElements());
    }

    public PageResult<SysRole> assignOptions(Long operatorUserId, Integer page, Integer pageSize, String keyword) {
        String kw;
        if (this.apiPermissionService.isSuperAdmin(operatorUserId)) {
            return this.list(operatorUserId, page, pageSize, keyword);
        }
        if (!this.apiPermissionService.isTenantAdmin(operatorUserId)) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        Long tid = CurrentUserHolder.getTenantId();
        if (tid == null) {
            throw new BusinessException(400, "\u65e0\u6cd5\u786e\u5b9a\u5f53\u524d\u79df\u6237");
        }
        Pageable p = this.buildPageable(page, pageSize);
        String string = kw = keyword != null ? keyword.trim() : "";
        if (kw.isEmpty()) {
            kw = null;
        }
        Page result = this.sysRoleRepository.findForAssignOptions(tid, ASSIGN_OPTION_SYSTEM_CODES, kw, p);
        List roles = result.getContent();
        this.fillMenuIds(roles);
        this.fillApiIds(roles);
        return PageResult.of((List)roles, (long)result.getTotalElements());
    }

    public SysRole get(Long operatorUserId, Long id) {
        SysRole role = this.sysRoleRepository.findById((Object)id).orElse(null);
        if (role == null) {
            return null;
        }
        this.assertCanAccessRole(operatorUserId, role);
        this.fillMenuIds(List.of((Object)role));
        this.fillApiIds(List.of((Object)role));
        return role;
    }

    @Transactional
    public SysRole create(Long operatorUserId, SysRole entity) {
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            throw new BusinessException(400, "\u89d2\u8272\u7f16\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String code = entity.getCode().trim();
        entity.setCode(code);
        if ("admin".equals(code) || "guest".equals(code) || "tenant_admin".equals(code)) {
            throw new BusinessException(400, "\u4e0d\u53ef\u4f7f\u7528\u7cfb\u7edf\u4fdd\u7559\u89d2\u8272\u7f16\u7801");
        }
        if (this.apiPermissionService.isSuperAdmin(operatorUserId)) {
            SysTenant t;
            Long bodyTid = entity.getTenantId();
            if (bodyTid != null && ((t = (SysTenant)this.sysTenantRepository.findById((Object)bodyTid).orElse(null)) == null || t.getDeletedAt() != null)) {
                throw new BusinessException(400, "\u79df\u6237\u4e0d\u5b58\u5728\u6216\u5df2\u5220\u9664");
            }
            entity.setTenantId(bodyTid);
        } else if (this.apiPermissionService.isTenantAdmin(operatorUserId)) {
            Long tid = CurrentUserHolder.getTenantId();
            if (tid == null) {
                throw new BusinessException(400, "\u65e0\u6cd5\u786e\u5b9a\u5f53\u524d\u79df\u6237");
            }
            entity.setTenantId(tid);
        } else {
            throw new BusinessException(403, "\u65e0\u6743\u521b\u5efa\u89d2\u8272");
        }
        this.validateCodeUniqueForCreate(entity);
        if (entity.getDescription() != null) {
            String d = entity.getDescription().trim();
            entity.setDescription(d.isEmpty() ? null : d);
        }
        SysRole saved = (SysRole)this.sysRoleRepository.save((Object)entity);
        if (entity.getMenuIds() != null && !entity.getMenuIds().isEmpty()) {
            ArrayList expanded = new ArrayList(this.menuExpansionHelper.expandWithAncestors((Collection)entity.getMenuIds()));
            List menuIds = "admin".equals(entity.getCode()) ? this.mergeRequiredMenuIds(null, "admin", expanded) : expanded.stream().sorted().collect(Collectors.toList());
            this.saveRoleMenus(saved.getId(), menuIds);
            saved.setMenuIds(menuIds);
        } else {
            saved.setMenuIds(entity.getMenuIds());
        }
        if (entity.getApiIds() != null && !entity.getApiIds().isEmpty()) {
            this.saveRoleApis(saved.getId(), entity.getApiIds());
            saved.setApiIds(entity.getApiIds());
        }
        return saved;
    }

    @Transactional
    public SysRole update(Long operatorUserId, Long id, SysRole entity) {
        String newCode;
        SysRole existing = this.sysRoleRepository.findById((Object)id).orElse(null);
        if (existing == null) {
            return null;
        }
        this.assertCanModifyRole(operatorUserId, existing);
        if (SysRoleService.isSystemSuperAdminRole((SysRole)existing)) {
            throw new BusinessException(403, "\u8d85\u7ea7\u7ba1\u7406\u5458\u89d2\u8272\u4e0d\u53ef\u4fee\u6539");
        }
        if (existing.getTenantId() == null && entity.getCode() != null && !entity.getCode().trim().equals(existing.getCode()) && ("admin".equals(existing.getCode()) || "guest".equals(existing.getCode()) || "tenant_admin".equals(existing.getCode()))) {
            throw new BusinessException(400, "\u7cfb\u7edf\u5185\u7f6e\u89d2\u8272\u4e0d\u53ef\u4fee\u6539\u7f16\u7801");
        }
        if (entity.getCode() != null && !entity.getCode().isBlank() && !(newCode = entity.getCode().trim()).equals(existing.getCode())) {
            if ("admin".equals(newCode) || "guest".equals(newCode) || "tenant_admin".equals(newCode)) {
                throw new BusinessException(400, "\u4e0d\u53ef\u4f7f\u7528\u7cfb\u7edf\u4fdd\u7559\u89d2\u8272\u7f16\u7801");
            }
            this.validateCodeUniqueForUpdate(existing, newCode);
            existing.setCode(newCode);
        }
        if (entity.getName() != null) {
            existing.setName(entity.getName());
        }
        if (entity.getStatus() != null) {
            existing.setStatus(entity.getStatus());
        }
        if (entity.getSort() != null) {
            existing.setSort(entity.getSort());
        }
        if (entity.getDescription() != null) {
            String d = entity.getDescription().trim();
            existing.setDescription(d.isEmpty() ? null : d);
        }
        if (entity.getApiIds() != null) {
            this.sysRoleApiRepository.deleteByRoleId(id);
            this.sysRoleApiRepository.flush();
            this.saveRoleApis(id, entity.getApiIds());
            existing.setApiIds(entity.getApiIds());
        } else {
            this.fillApiIds(List.of((Object)existing));
        }
        if (entity.getMenuIds() != null) {
            ArrayList expanded = new ArrayList(this.menuExpansionHelper.expandWithAncestors((Collection)entity.getMenuIds()));
            List merged = this.mergeRequiredMenuIds(id, existing.getCode(), expanded);
            this.sysRoleMenuRepository.deleteByRoleId(id);
            this.sysRoleMenuRepository.flush();
            this.saveRoleMenus(id, merged);
            existing.setMenuIds(merged);
        } else {
            this.fillMenuIds(List.of((Object)existing));
        }
        return (SysRole)this.sysRoleRepository.save((Object)existing);
    }

    private void validateCodeUniqueForCreate(SysRole entity) {
        Long tid = entity.getTenantId();
        String code = entity.getCode();
        if (tid == null ? this.sysRoleRepository.existsByTenantIdIsNullAndCode(code) : this.sysRoleRepository.existsByTenantIdAndCode(tid, code)) {
            throw new BusinessException(400, "\u89d2\u8272\u7f16\u7801\u5df2\u5b58\u5728");
        }
    }

    private void validateCodeUniqueForUpdate(SysRole existing, String newCode) {
        Optional o;
        Long tid = existing.getTenantId();
        if (tid == null ? (o = this.sysRoleRepository.findByTenantIdIsNullAndCode(newCode)).isPresent() && !((SysRole)o.get()).getId().equals(existing.getId()) : (o = this.sysRoleRepository.findByTenantIdAndCode(tid, newCode)).isPresent() && !((SysRole)o.get()).getId().equals(existing.getId())) {
            throw new BusinessException(400, "\u89d2\u8272\u7f16\u7801\u5df2\u5b58\u5728");
        }
    }

    private void assertCanAccessRole(Long operatorUserId, SysRole role) {
        if (this.apiPermissionService.isSuperAdmin(operatorUserId)) {
            return;
        }
        if (this.apiPermissionService.isTenantAdmin(operatorUserId)) {
            Long tid = CurrentUserHolder.getTenantId();
            if (role.getTenantId() == null) {
                throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u7cfb\u7edf\u7ea7\u89d2\u8272");
            }
            if (tid == null || !Objects.equals(role.getTenantId(), tid)) {
                throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u89d2\u8272");
            }
            return;
        }
        throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u89d2\u8272");
    }

    private void assertCanModifyRole(Long operatorUserId, SysRole existing) {
        if (this.apiPermissionService.isSuperAdmin(operatorUserId)) {
            return;
        }
        if (this.apiPermissionService.isTenantAdmin(operatorUserId)) {
            if (existing.getTenantId() == null) {
                throw new BusinessException(403, "\u65e0\u6743\u64cd\u4f5c\u7cfb\u7edf\u7ea7\u89d2\u8272");
            }
            Long tid = CurrentUserHolder.getTenantId();
            if (tid == null || !Objects.equals(existing.getTenantId(), tid)) {
                throw new BusinessException(403, "\u65e0\u6743\u64cd\u4f5c\u8be5\u89d2\u8272");
            }
            return;
        }
        throw new BusinessException(403, "\u65e0\u6743\u64cd\u4f5c\u89d2\u8272");
    }

    private void fillMenuIds(List<SysRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        List roleIds = roles.stream().map(BaseEntity::getId).toList();
        List rms = this.sysRoleMenuRepository.findByRoleIdIn(roleIds);
        for (SysRole r : roles) {
            List mids = rms.stream().filter(rm -> r.getId().equals(rm.getRoleId())).map(SysRoleMenu::getMenuId).sorted().collect(Collectors.toList());
            r.setMenuIds(mids);
        }
    }

    private void fillApiIds(List<SysRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        List roleIds = roles.stream().map(BaseEntity::getId).toList();
        List ras = this.sysRoleApiRepository.findByRoleIdIn(roleIds);
        for (SysRole r : roles) {
            List aids = ras.stream().filter(ra -> r.getId().equals(ra.getRoleId())).map(SysRoleApi::getApiId).sorted().collect(Collectors.toList());
            r.setApiIds(aids);
        }
    }

    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        List list = menuIds.stream().map(menuId -> {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            return rm;
        }).toList();
        this.sysRoleMenuRepository.saveAll((Iterable)list);
    }

    private void saveRoleApis(Long roleId, List<Long> apiIds) {
        if (apiIds == null || apiIds.isEmpty()) {
            return;
        }
        List list = apiIds.stream().distinct().map(apiId -> {
            SysRoleApi ra = new SysRoleApi();
            ra.setRoleId(roleId);
            ra.setApiId(apiId);
            return ra;
        }).toList();
        this.sysRoleApiRepository.saveAll((Iterable)list);
    }

    private List<Long> mergeRequiredMenuIds(Long roleId, String roleCode, List<Long> menuIds) {
        boolean isAdmin;
        boolean bl = isAdmin = roleId != null && roleId == 1L || "admin".equals(roleCode);
        if (!isAdmin) {
            return menuIds != null ? menuIds : List.of();
        }
        LinkedHashSet ids = new LinkedHashSet(menuIds != null ? menuIds : List.of());
        ids.addAll(ADMIN_REQUIRED_MENU_IDS);
        return ids.stream().sorted().collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long operatorUserId, Long id) {
        if (id == null) {
            return;
        }
        SysRole role = this.sysRoleRepository.findById((Object)id).orElse(null);
        if (role == null) {
            return;
        }
        this.assertCanModifyRole(operatorUserId, role);
        String code = role.getCode();
        if ("admin".equals(code) || "guest".equals(code) || "tenant_admin".equals(code)) {
            throw new BusinessException(400, "\u7cfb\u7edf\u5185\u7f6e\u89d2\u8272\u4e0d\u53ef\u5220\u9664");
        }
        this.sysUserRoleRepository.deleteByRoleId(id);
        this.sysUserRoleRepository.flush();
        this.sysRoleMenuRepository.deleteByRoleId(id);
        this.sysRoleMenuRepository.flush();
        this.sysRoleApiRepository.deleteByRoleId(id);
        this.sysRoleApiRepository.flush();
        this.sysRoleRepository.deleteById((Object)id);
    }

    private Pageable buildPageable(Integer page, Integer pageSize) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.ASC, (String[])new String[]{"sort", "id"}));
    }

    private static boolean isSystemSuperAdminRole(SysRole role) {
        return role != null && role.getTenantId() == null && "admin".equals(role.getCode());
    }

    @Generated
    public SysRoleService(ApiPermissionService apiPermissionService, MenuExpansionHelper menuExpansionHelper, SysRoleRepository sysRoleRepository, SysRoleMenuRepository sysRoleMenuRepository, SysRoleApiRepository sysRoleApiRepository, SysUserRoleRepository sysUserRoleRepository, SysTenantRepository sysTenantRepository) {
        this.apiPermissionService = apiPermissionService;
        this.menuExpansionHelper = menuExpansionHelper;
        this.sysRoleRepository = sysRoleRepository;
        this.sysRoleMenuRepository = sysRoleMenuRepository;
        this.sysRoleApiRepository = sysRoleApiRepository;
        this.sysUserRoleRepository = sysUserRoleRepository;
        this.sysTenantRepository = sysTenantRepository;
    }
}

