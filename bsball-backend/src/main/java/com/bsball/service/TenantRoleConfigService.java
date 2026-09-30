/*
 * 账号权限重构（批次 3b，Task 3.15b / spec §8.6）：门户角色租户级目录配置覆盖层。
 *
 * 职责：
 *  - 生效合并（resolveEffectiveMenuIds）：平台级门户角色（team_manager / league_organizer）在本租户存在覆盖行时，
 *    以覆盖集合（可为空集）替换其全局目录；否则回落全局 sys_role_menu 绑定。其它角色一律全局。
 *  - 覆盖读写（saveOverride / clearOverride）：全量替换语义；仅操作覆盖两表，绝不触碰全局角色/绑定行。
 *
 * 说明：本服务不依赖 ApiPermissionService（避免循环依赖）；缓存失效由调用方（SysRoleService）负责。
 */
package com.bsball.service;

import com.bsball.model.entity.SysRole;
import com.bsball.model.entity.SysRoleMenu;
import com.bsball.model.entity.TenantRoleMenu;
import com.bsball.model.entity.TenantRoleMenuConfig;
import com.bsball.repository.SysRoleMenuRepository;
import com.bsball.repository.SysRoleRepository;
import com.bsball.repository.TenantRoleMenuConfigRepository;
import com.bsball.repository.TenantRoleMenuRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantRoleConfigService {
    /*
     * 参与租户级覆盖的平台门户角色编码白名单（spec §8.6）。
     * 注：member 暂不纳入（报告注明可扩）；其它角色照旧全局。
     */
    public static final Set<String> PORTAL_OVERRIDE_ROLE_CODES = Set.of("team_manager", "league_organizer");

    private final TenantRoleMenuConfigRepository configRepository;
    private final TenantRoleMenuRepository menuRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysRoleMenuRepository sysRoleMenuRepository;

    /**
     * 是否为可参与租户级覆盖的平台门户角色编码。
     */
    public boolean isOverridableRoleCode(String roleCode) {
        return roleCode != null && PORTAL_OVERRIDE_ROLE_CODES.contains(roleCode);
    }

    /**
     * 计算用户角色集合在指定租户下的「生效菜单集合」并集：
     * 平台门户角色若存在本租户覆盖行 → 用覆盖集合（可空）；否则用全局绑定。其它角色一律全局。
     *
     * @param tenantId 生效租户（null 表示无租户上下文，全部走全局）
     * @param roleIds  用户角色ID集合
     * @return 生效菜单ID集合（去重）
     */
    public Set<Long> resolveEffectiveMenuIds(Long tenantId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> distinctRoleIds = roleIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        if (distinctRoleIds.isEmpty()) {
            return Set.of();
        }
        List<Long> idList = List.copyOf(distinctRoleIds);
        Map<Long, String> codeByRole = loadRoleCodes(idList);
        Map<Long, Set<Long>> globalByRole = loadGlobalMenuIds(idList);
        Map<Long, Set<Long>> overrideByRole = loadOverrideMenuIds(tenantId, idList);
        HashSet<Long> out = new HashSet<Long>();
        for (Long roleId : idList) {
            if (tenantId != null && this.isOverridableRoleCode(codeByRole.get(roleId)) && overrideByRole.containsKey(roleId)) {
                out.addAll(overrideByRole.get(roleId));
            } else {
                out.addAll(globalByRole.getOrDefault(roleId, Set.of()));
            }
        }
        return out;
    }

    /**
     * 单角色「生效菜单集合」：存在本租户覆盖行 → 覆盖集合（可空）；否则回落给定全局集合。
     * 调用方须确保 roleId 属于平台门户角色（非门户角色不应调用）。
     */
    public Set<Long> resolveEffectiveMenuIds(Long tenantId, Long roleId, Set<Long> globalMenuIds) {
        Set<Long> fallback = globalMenuIds == null ? Set.of() : globalMenuIds;
        if (tenantId == null || roleId == null) {
            return fallback;
        }
        Optional<TenantRoleMenuConfig> config = this.configRepository.findByTenantIdAndRoleId(tenantId, roleId);
        if (config.isEmpty()) {
            return fallback;
        }
        return this.findOverrideMenuIds(config.get().getId());
    }

    /**
     * 查询指定租户/角色的覆盖配置头（存在即启用覆盖）。
     */
    public Optional<TenantRoleMenuConfig> findConfig(Long tenantId, Long roleId) {
        if (tenantId == null || roleId == null) {
            return Optional.empty();
        }
        return this.configRepository.findByTenantIdAndRoleId(tenantId, roleId);
    }

    /**
     * 查询覆盖配置下辖的菜单/按钮集合。
     */
    public Set<Long> findOverrideMenuIds(Long configId) {
        if (configId == null) {
            return Set.of();
        }
        return this.menuRepository.findByConfigId(configId).stream().map(TenantRoleMenu::getMenuId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * 保存（upsert）租户级覆盖：全量替换明细，空集合=启用空覆盖。仅写覆盖两表。
     *
     * @return 覆盖配置头
     */
    @Transactional
    public TenantRoleMenuConfig saveOverride(Long tenantId, Long roleId, Collection<Long> menuIds, Long operatorUserId) {
        TenantRoleMenuConfig config = this.configRepository.findByTenantIdAndRoleId(tenantId, roleId).orElseGet(() -> {
            TenantRoleMenuConfig created = new TenantRoleMenuConfig();
            created.setTenantId(tenantId);
            created.setRoleId(roleId);
            return created;
        });
        config.setUpdatedBy(operatorUserId);
        config = (TenantRoleMenuConfig)this.configRepository.save(config);
        this.menuRepository.deleteByConfigId(config.getId());
        this.menuRepository.flush();
        if (menuIds != null && !menuIds.isEmpty()) {
            Long configId = config.getId();
            List<TenantRoleMenu> rows = menuIds.stream().filter(Objects::nonNull).distinct().map(menuId -> {
                TenantRoleMenu row = new TenantRoleMenu();
                row.setConfigId(configId);
                row.setMenuId(menuId);
                return row;
            }).toList();
            this.menuRepository.saveAll(rows);
        }
        return config;
    }

    /**
     * 删除租户级覆盖（回落全局）。仅操作覆盖两表。
     */
    @Transactional
    public void clearOverride(Long tenantId, Long roleId) {
        Optional<TenantRoleMenuConfig> config = this.configRepository.findByTenantIdAndRoleId(tenantId, roleId);
        if (config.isEmpty()) {
            return;
        }
        Long configId = config.get().getId();
        this.menuRepository.deleteByConfigId(configId);
        this.menuRepository.flush();
        this.configRepository.deleteById(configId);
    }

    private Map<Long, String> loadRoleCodes(List<Long> roleIds) {
        HashMap<Long, String> out = new HashMap<Long, String>();
        for (SysRole role : this.sysRoleRepository.findAllById(roleIds)) {
            if (role == null || role.getId() == null) continue;
            out.put(role.getId(), role.getCode());
        }
        return out;
    }

    private Map<Long, Set<Long>> loadGlobalMenuIds(List<Long> roleIds) {
        HashMap<Long, Set<Long>> out = new HashMap<Long, Set<Long>>();
        for (SysRoleMenu rm : this.sysRoleMenuRepository.findByRoleIdIn(roleIds)) {
            if (rm == null || rm.getRoleId() == null || rm.getMenuId() == null) continue;
            out.computeIfAbsent(rm.getRoleId(), k -> new HashSet<Long>()).add(rm.getMenuId());
        }
        return out;
    }

    private Map<Long, Set<Long>> loadOverrideMenuIds(Long tenantId, List<Long> roleIds) {
        if (tenantId == null || roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }
        List<TenantRoleMenuConfig> configs = this.configRepository.findByTenantIdAndRoleIdIn(tenantId, roleIds);
        if (configs == null || configs.isEmpty()) {
            return Map.of();
        }
        List<Long> configIds = configs.stream().map(TenantRoleMenuConfig::getId).filter(Objects::nonNull).toList();
        HashMap<Long, Set<Long>> menuByConfig = new HashMap<Long, Set<Long>>();
        if (!configIds.isEmpty()) {
            for (TenantRoleMenu row : this.menuRepository.findByConfigIdIn(configIds)) {
                if (row == null || row.getConfigId() == null || row.getMenuId() == null) continue;
                menuByConfig.computeIfAbsent(row.getConfigId(), k -> new HashSet<Long>()).add(row.getMenuId());
            }
        }
        HashMap<Long, Set<Long>> out = new HashMap<Long, Set<Long>>();
        for (TenantRoleMenuConfig config : configs) {
            if (config == null || config.getRoleId() == null) continue;
            out.put(config.getRoleId(), menuByConfig.getOrDefault(config.getId(), Set.of()));
        }
        return out;
    }

    @Generated
    public TenantRoleConfigService(TenantRoleMenuConfigRepository configRepository, TenantRoleMenuRepository menuRepository, SysRoleRepository sysRoleRepository, SysRoleMenuRepository sysRoleMenuRepository) {
        this.configRepository = configRepository;
        this.menuRepository = menuRepository;
        this.sysRoleRepository = sysRoleRepository;
        this.sysRoleMenuRepository = sysRoleMenuRepository;
    }
}
