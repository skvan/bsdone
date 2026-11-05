package com.bsball.repository;

import com.bsball.model.entity.ErDecisionAdjustment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErDecisionAdjustmentRepository
        extends JpaRepository<ErDecisionAdjustment, Long> {
    List<ErDecisionAdjustment> findByTenantIdAndGameIdAndStatus(
            Long tenantId, Long gameId, String status);

    List<ErDecisionAdjustment> findByGameIdAndStatus(Long gameId, String status);

    Optional<ErDecisionAdjustment> findByIdAndTenantIdAndGameId(
            Long id, Long tenantId, Long gameId);

    Optional<ErDecisionAdjustment> findFirstByTenantIdAndGameIdAndSourceEventIdAndRunnerIdAndStatusOrderByIdDesc(
            Long tenantId, Long gameId, String sourceEventId, Long runnerId, String status);

    List<ErDecisionAdjustment> findByTenantIdAndGameIdAndSourceEventIdAndRunnerIdAndStatus(
            Long tenantId, Long gameId, String sourceEventId, Long runnerId, String status);
}
