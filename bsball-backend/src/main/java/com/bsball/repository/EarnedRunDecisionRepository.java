package com.bsball.repository;

import com.bsball.model.entity.EarnedRunDecisionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EarnedRunDecisionRepository extends JpaRepository<EarnedRunDecisionEntity, Long> {
    List<EarnedRunDecisionEntity> findByTenantIdAndGameIdAndPlayIdIn(
            Long tenantId, Long gameId, List<Long> playIds);

    void deleteByTenantIdAndGameIdAndPlayIdIn(Long tenantId, Long gameId, List<Long> playIds);
}
