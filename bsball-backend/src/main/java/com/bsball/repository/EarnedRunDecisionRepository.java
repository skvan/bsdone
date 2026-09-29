package com.bsball.repository;

import com.bsball.model.entity.EarnedRunDecisionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EarnedRunDecisionRepository extends JpaRepository<EarnedRunDecisionEntity, Long> {
    java.util.Optional<EarnedRunDecisionEntity> findByIdAndTenantIdAndGameId(
            Long id, Long tenantId, Long gameId);
    List<EarnedRunDecisionEntity> findByTenantIdAndGameIdAndPlayIdIn(
            Long tenantId, Long gameId, List<Long> playIds);

    void deleteByTenantIdAndGameIdAndPlayIdIn(Long tenantId, Long gameId, List<Long> playIds);
}
