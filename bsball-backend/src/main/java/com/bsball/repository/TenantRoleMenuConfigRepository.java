/*
 * 账号权限重构（批次 3b，Task 3.15b / spec §8.6）：门户角色租户级目录覆盖配置头仓储。
 */
package com.bsball.repository;

import com.bsball.model.entity.TenantRoleMenuConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRoleMenuConfigRepository
extends JpaRepository<TenantRoleMenuConfig, Long> {
    public Optional<TenantRoleMenuConfig> findByTenantIdAndRoleId(Long tenantId, Long roleId);

    public List<TenantRoleMenuConfig> findByTenantIdAndRoleIdIn(Long tenantId, List<Long> roleIds);
}
