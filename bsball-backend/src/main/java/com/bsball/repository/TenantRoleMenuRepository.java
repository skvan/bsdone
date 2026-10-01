/*
 * 账号权限重构（批次 3b，Task 3.15b / spec §8.6）：门户角色租户级目录覆盖明细仓储。
 */
package com.bsball.repository;

import com.bsball.model.entity.TenantRoleMenu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRoleMenuRepository
extends JpaRepository<TenantRoleMenu, Long> {
    public List<TenantRoleMenu> findByConfigId(Long configId);

    public List<TenantRoleMenu> findByConfigIdIn(List<Long> configIds);

    @Modifying
    @Query(value="DELETE FROM TenantRoleMenu e WHERE e.configId = :configId")
    public void deleteByConfigId(@Param(value="configId") Long configId);
}
