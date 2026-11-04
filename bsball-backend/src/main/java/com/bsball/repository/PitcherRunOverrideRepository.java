package com.bsball.repository;

import com.bsball.model.entity.PitcherRunOverride;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PitcherRunOverrideRepository extends JpaRepository<PitcherRunOverride, Long> {
    List<PitcherRunOverride> findByTenantIdAndGameIdAndStatus(Long tenantId, Long gameId, String status);

    Optional<PitcherRunOverride> findByIdAndTenantIdAndGameId(
            Long id, Long tenantId, Long gameId);

    Optional<PitcherRunOverride> findFirstByTenantIdAndGameIdAndPlayerIdAndStatusOrderByIdDesc(
            Long tenantId, Long gameId, Long playerId, String status);

    boolean existsByTenantIdAndGameIdAndPlayerIdAndStatus(
            Long tenantId, Long gameId, Long playerId, String status);
}
