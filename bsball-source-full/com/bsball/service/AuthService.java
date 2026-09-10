/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.TenantProperties
 *  com.bsball.exception.BusinessException
 *  com.bsball.exception.UnauthorizedException
 *  com.bsball.model.entity.SysMenu
 *  com.bsball.model.entity.SysRoleMenu
 *  com.bsball.model.entity.SysTenant
 *  com.bsball.model.entity.SysUser
 *  com.bsball.model.entity.SysUserTenant
 *  com.bsball.repository.SysMenuRepository
 *  com.bsball.repository.SysRoleMenuRepository
 *  com.bsball.repository.SysTenantRepository
 *  com.bsball.repository.SysUserRepository
 *  com.bsball.repository.SysUserRoleRepository
 *  com.bsball.repository.SysUserTenantRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.AuthService
 *  com.bsball.service.JwtService
 *  com.bsball.service.JwtService$TokenAuth
 *  com.bsball.utils.PasswordEncoder
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.config.TenantProperties;
import com.bsball.exception.BusinessException;
import com.bsball.exception.UnauthorizedException;
import com.bsball.model.entity.SysMenu;
import com.bsball.model.entity.SysRoleMenu;
import com.bsball.model.entity.SysTenant;
import com.bsball.model.entity.SysUser;
import com.bsball.model.entity.SysUserTenant;
import com.bsball.repository.SysMenuRepository;
import com.bsball.repository.SysRoleMenuRepository;
import com.bsball.repository.SysTenantRepository;
import com.bsball.repository.SysUserRepository;
import com.bsball.repository.SysUserRoleRepository;
import com.bsball.repository.SysUserTenantRepository;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.JwtService;
import com.bsball.utils.PasswordEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Generated;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final ApiPermissionService apiPermissionService;
    private final JwtService jwtService;
    private final TenantProperties tenantProperties;
    private final SysUserRepository sysUserRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final SysRoleMenuRepository sysRoleMenuRepository;
    private final SysMenuRepository sysMenuRepository;
    private final SysUserTenantRepository sysUserTenantRepository;
    private final SysTenantRepository sysTenantRepository;
    private static final Pattern PHONE_LOGIN_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    public Map<String, Object> login(String username, String password, Long requestedTenantId) {
        Long tenantId;
        String identifier = username == null ? "" : username.trim();
        SysUser user = (PHONE_LOGIN_PATTERN.matcher(identifier).matches() ? this.sysUserRepository.findByPhoneAndDeletedAtIsNull(identifier) : this.sysUserRepository.findByUsernameAndDeletedAtIsNull(identifier)).orElse(null);
        if (user == null) {
            throw new UnauthorizedException("\u7528\u6237\u540d\u6216\u5bc6\u7801\u9519\u8bef");
        }
        String stored = user.getPassword();
        boolean passwordOk = PasswordEncoder.matches((CharSequence)password, (String)stored);
        if (!passwordOk && !PasswordEncoder.isEncoded((String)stored) && Objects.equals(stored, password)) {
            passwordOk = true;
            user.setPassword(PasswordEncoder.encode((CharSequence)password));
            this.sysUserRepository.save((Object)user);
        }
        if (!passwordOk) {
            throw new UnauthorizedException("\u7528\u6237\u540d\u6216\u5bc6\u7801\u9519\u8bef");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "\u8d26\u53f7\u5df2\u7981\u7528");
        }
        boolean superAdmin = this.apiPermissionService.isSuperAdmin(user.getId());
        List utList = this.sysUserTenantRepository.findByUserIdAndDeletedAtIsNull(user.getId());
        if (!superAdmin && utList.isEmpty()) {
            throw new BusinessException(403, "\u672a\u5206\u914d\u4efb\u4f55\u79df\u6237\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458");
        }
        if (superAdmin) {
            tenantId = this.resolveSuperAdminLoginTenant(requestedTenantId, utList);
        } else if (requestedTenantId != null) {
            boolean inRequested = utList.stream().anyMatch(ut -> ut.getTenantId().equals(requestedTenantId));
            if (!inRequested) {
                throw new UnauthorizedException("\u7528\u6237\u540d\u6216\u5bc6\u7801\u9519\u8bef");
            }
            tenantId = requestedTenantId;
        } else {
            throw new UnauthorizedException("\u7528\u6237\u540d\u6216\u5bc6\u7801\u9519\u8bef");
        }
        String token = this.jwtService.createToken(user.getId(), tenantId);
        Map userInfo = this.toAuthUser(user, tenantId);
        HashMap<String, Object> res = new HashMap<String, Object>();
        res.put("token", token);
        res.put("user", userInfo);
        return res;
    }

    public Map<String, Object> loginWithoutPassword(SysUser user, Long requestedTenantId) {
        Long tenantId;
        if (user == null) {
            throw new UnauthorizedException("\u8bf7\u5148\u767b\u5f55");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "\u8d26\u53f7\u5df2\u7981\u7528");
        }
        boolean superAdmin = this.apiPermissionService.isSuperAdmin(user.getId());
        List utList = this.sysUserTenantRepository.findByUserIdAndDeletedAtIsNull(user.getId());
        if (!superAdmin && utList.isEmpty()) {
            throw new BusinessException(403, "\u672a\u5206\u914d\u4efb\u4f55\u79df\u6237\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458");
        }
        if (superAdmin) {
            tenantId = this.resolveSuperAdminLoginTenant(requestedTenantId, utList);
        } else if (requestedTenantId != null) {
            boolean inRequested = utList.stream().anyMatch(ut -> ut.getTenantId().equals(requestedTenantId));
            if (!inRequested) {
                throw new UnauthorizedException("\u65e0\u6743\u767b\u5f55\u8be5\u79df\u6237");
            }
            tenantId = requestedTenantId;
        } else {
            throw new UnauthorizedException("\u8bf7\u6307\u5b9a\u79df\u6237");
        }
        String token = this.jwtService.createToken(user.getId(), tenantId);
        Map userInfo = this.toAuthUser(user, tenantId);
        HashMap<String, Object> res = new HashMap<String, Object>();
        res.put("token", token);
        res.put("user", userInfo);
        return res;
    }

    public Map<String, Object> switchTenant(Long userId, Long newTenantId) {
        SysUser user = this.sysUserRepository.findById((Object)userId).orElse(null);
        if (user == null || user.getStatus() != null && user.getStatus() == 0) {
            throw new UnauthorizedException("\u8bf7\u5148\u767b\u5f55");
        }
        if (newTenantId == null) {
            throw new BusinessException(400, "tenantId \u4e0d\u80fd\u4e3a\u7a7a");
        }
        boolean superAdmin = this.apiPermissionService.isSuperAdmin(userId);
        if (superAdmin) {
            if (Objects.equals(newTenantId, 0L)) {
                String token = this.jwtService.createToken(userId, Long.valueOf(0L));
                return Map.of((Object)"token", (Object)token, (Object)"user", (Object)this.toAuthUser(user, Long.valueOf(0L)));
            }
            Optional target = this.sysTenantRepository.findById((Object)newTenantId);
            if (target.isEmpty() || ((SysTenant)target.get()).getDeletedAt() != null || !((SysTenant)target.get()).isActive()) {
                throw new BusinessException(403, "\u65e0\u6743\u5207\u6362\u5230\u8be5\u79df\u6237");
            }
        } else if (!this.sysUserTenantRepository.existsByUserIdAndTenantIdAndDeletedAtIsNull(userId, newTenantId)) {
            throw new BusinessException(403, "\u65e0\u6743\u5207\u6362\u5230\u8be5\u79df\u6237");
        }
        String token = this.jwtService.createToken(userId, newTenantId);
        return Map.of((Object)"token", (Object)token, (Object)"user", (Object)this.toAuthUser(user, newTenantId));
    }

    public Map<String, Object> me(String token) {
        JwtService.TokenAuth auth = this.jwtService.authenticateBearerToken(token);
        Long userId = auth.userId();
        if (userId == null) {
            throw new UnauthorizedException("\u8bf7\u5148\u767b\u5f55");
        }
        SysUser user = this.sysUserRepository.findById((Object)userId).orElse(null);
        if (user == null || user.getStatus() != null && user.getStatus() == 0) {
            throw new UnauthorizedException("\u8bf7\u5148\u767b\u5f55");
        }
        Long tid = auth.tenantId();
        if (tid == null) {
            tid = this.apiPermissionService.isSuperAdmin(userId) ? 0L : this.tenantProperties.getDefaultId();
        }
        return Map.of((Object)"user", (Object)this.toAuthUser(user, tid));
    }

    private Long resolveSuperAdminLoginTenant(Long requestedTenantId, List<SysUserTenant> utList) {
        Optional opt;
        if (requestedTenantId != null && (opt = this.sysTenantRepository.findById((Object)requestedTenantId)).isPresent() && ((SysTenant)opt.get()).getDeletedAt() == null && ((SysTenant)opt.get()).isActive()) {
            return requestedTenantId;
        }
        if (utList != null && !utList.isEmpty()) {
            return utList.get(0).getTenantId();
        }
        return 0L;
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = this.sysUserRepository.findById((Object)userId).orElse(null);
        if (user == null) {
            throw new BusinessException(401, "\u8bf7\u5148\u767b\u5f55");
        }
        if (!PasswordEncoder.matches((CharSequence)oldPassword, (String)user.getPassword())) {
            throw new BusinessException(400, "\u539f\u5bc6\u7801\u9519\u8bef");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException(400, "\u65b0\u5bc6\u7801\u81f3\u5c11 6 \u4f4d");
        }
        user.setPassword(PasswordEncoder.encode((CharSequence)newPassword));
        this.sysUserRepository.save((Object)user);
    }

    private Map<String, Object> toAuthUser(SysUser user, Long currentTenantId) {
        List roleIds = this.sysUserRoleRepository.findByUserId(user.getId()).stream().map(ur -> ur.getRoleId()).sorted().collect(Collectors.toList());
        List roleIdsForMenu = roleIds.isEmpty() ? List.of() : roleIds;
        List rms = this.sysRoleMenuRepository.findByRoleIdIn(roleIdsForMenu);
        Set menuIds = rms.stream().map(SysRoleMenu::getMenuId).filter(Objects::nonNull).collect(Collectors.toSet());
        List menuPaths = List.of();
        List perms = List.of();
        if (!menuIds.isEmpty()) {
            List menus = this.sysMenuRepository.findAllById(menuIds);
            menuPaths = menus.stream().map(SysMenu::getPath).filter(p -> p != null && !p.isBlank()).distinct().sorted().toList();
            perms = menus.stream().map(SysMenu::getPermission).filter(p -> p != null && !p.isBlank()).map(String::trim).distinct().sorted().toList();
        }
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("nickname", user.getNickname());
        m.put("roleIds", roleIds);
        m.put("menuIds", new ArrayList(menuIds));
        m.put("menuPaths", menuPaths);
        m.put("perms", new ArrayList(perms));
        m.put("tenantId", currentTenantId);
        m.put("tenantCode", this.resolveTenantCodeString(currentTenantId));
        m.put("tenants", this.buildTenantOptions(user.getId()));
        m.put("strictDataScope", this.tenantProperties.isStrictDataScope());
        m.put("superAdmin", this.apiPermissionService.isSuperAdmin(user.getId()));
        m.put("tenantAdmin", this.apiPermissionService.isTenantAdmin(user.getId()));
        return m;
    }

    private String resolveTenantCodeString(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        return this.sysTenantRepository.findById((Object)tenantId).filter(t -> t.getDeletedAt() == null).map(SysTenant::getCode).orElse(null);
    }

    private List<Map<String, Object>> buildTenantOptions(Long userId) {
        List uts = this.sysUserTenantRepository.findByUserIdAndDeletedAtIsNull(userId);
        ArrayList<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (SysUserTenant ut : uts) {
            Optional t = this.sysTenantRepository.findById((Object)ut.getTenantId());
            if (t.isEmpty() || ((SysTenant)t.get()).getDeletedAt() != null) continue;
            SysTenant tenant = (SysTenant)t.get();
            HashMap<String, Object> row = new HashMap<String, Object>();
            row.put("id", tenant.getId());
            row.put("name", tenant.getName());
            row.put("code", tenant.getCode());
            out.add(row);
        }
        return out;
    }

    @Generated
    public AuthService(ApiPermissionService apiPermissionService, JwtService jwtService, TenantProperties tenantProperties, SysUserRepository sysUserRepository, SysUserRoleRepository sysUserRoleRepository, SysRoleMenuRepository sysRoleMenuRepository, SysMenuRepository sysMenuRepository, SysUserTenantRepository sysUserTenantRepository, SysTenantRepository sysTenantRepository) {
        this.apiPermissionService = apiPermissionService;
        this.jwtService = jwtService;
        this.tenantProperties = tenantProperties;
        this.sysUserRepository = sysUserRepository;
        this.sysUserRoleRepository = sysUserRoleRepository;
        this.sysRoleMenuRepository = sysRoleMenuRepository;
        this.sysMenuRepository = sysMenuRepository;
        this.sysUserTenantRepository = sysUserTenantRepository;
        this.sysTenantRepository = sysTenantRepository;
    }
}

