package com.bsball.repository;

import com.bsball.model.entity.GameStatBase;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameStatBaseRepository extends JpaRepository<GameStatBase, Long> {
    List<GameStatBase> findByTenantIdAndGameIdOrderByPlayerIdAscIsPitcherAsc(
            Long tenantId, Long gameId);
}
