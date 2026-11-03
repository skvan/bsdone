package com.bsball.repository;

import com.bsball.model.entity.GameFieldingAdjustment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameFieldingAdjustmentRepository extends JpaRepository<GameFieldingAdjustment, Long> {
    List<GameFieldingAdjustment> findByTenantIdAndGameIdOrderByInningAscTopBottomAscSourceSequenceAscIdAsc(
            Long tenantId, Long gameId);

    Optional<GameFieldingAdjustment> findByIdAndTenantIdAndGameId(
            Long id, Long tenantId, Long gameId);

    Optional<GameFieldingAdjustment> findFirstByTenantIdAndGameIdAndSourceEventIdAndPlayerIdOrderByIdAsc(
            Long tenantId, Long gameId, String sourceEventId, Long playerId);

    List<GameFieldingAdjustment> findByTenantIdAndGameIdAndStatus(
            Long tenantId, Long gameId, String status);

    List<GameFieldingAdjustment> findByGameIdAndStatus(Long gameId, String status);

    boolean existsByTenantIdAndGameIdAndPlayerIdAndStatus(
            Long tenantId, Long gameId, Long playerId, String status);

    boolean existsByTenantIdAndGameIdAndSourceEventIdAndPlayerIdAndStatus(
            Long tenantId, Long gameId, String sourceEventId, Long playerId, String status);
}
