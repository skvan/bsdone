package com.bsball.repository;

import com.bsball.model.entity.EarnedRunDecisionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EarnedRunDecisionRepository extends JpaRepository<EarnedRunDecisionEntity, Long> {
    java.util.Optional<EarnedRunDecisionEntity> findByIdAndTenantIdAndGameId(
            Long id, Long tenantId, Long gameId);
    List<EarnedRunDecisionEntity> findByTenantIdAndGameIdAndPlayIdIn(
            Long tenantId, Long gameId, List<Long> playIds);

    /** 该场是否仍有以该投手为责任投手的自责分判定（删除投手统计行前的守卫）。 */
    boolean existsByGameIdAndResponsiblePitcherId(Long gameId, Long responsiblePitcherId);

    void deleteByTenantIdAndGameIdAndPlayIdIn(Long tenantId, Long gameId, List<Long> playIds);
}
