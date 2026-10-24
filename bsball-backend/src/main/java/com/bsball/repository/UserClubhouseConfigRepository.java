package com.bsball.repository;

import com.bsball.model.entity.UserClubhouseConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserClubhouseConfigRepository
extends JpaRepository<UserClubhouseConfig, Long> {
    Optional<UserClubhouseConfig> findFirstByUserIdAndTenantIdAndDeletedAtIsNull(Long userId, Long tenantId);

    /**
     * 首次保存/并发保存的原子 upsert，依赖部分唯一索引
     * ux_bs_user_clubhouse_config_user_tenant (user_id, tenant_id) WHERE deleted_at IS NULL。
     */
    @Modifying
    @Query(value = "INSERT INTO bs_user_clubhouse_config "
            + "(user_id, tenant_id, config_json, config_version, created_by, updated_by, created_at, updated_at) "
            + "VALUES (:userId, :tenantId, :configJson, 1, :userId, :userId, now(), now()) "
            + "ON CONFLICT (user_id, tenant_id) WHERE deleted_at IS NULL "
            + "DO UPDATE SET config_json = EXCLUDED.config_json, "
            + "config_version = bs_user_clubhouse_config.config_version + 1, "
            + "updated_by = EXCLUDED.updated_by, updated_at = now()", nativeQuery = true)
    int upsert(@Param(value = "userId") Long userId, @Param(value = "tenantId") Long tenantId,
            @Param(value = "configJson") String configJson);
}
