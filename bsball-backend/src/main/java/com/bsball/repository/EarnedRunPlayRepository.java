package com.bsball.repository;

import com.bsball.model.entity.EarnedRunPlayEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EarnedRunPlayRepository extends JpaRepository<EarnedRunPlayEntity, Long> {
    List<EarnedRunPlayEntity> findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
            Long tenantId, Long gameId, Integer inning, String half);

    void deleteByTenantIdAndGameIdAndInningAndHalf(
            Long tenantId, Long gameId, Integer inning, String half);
}
