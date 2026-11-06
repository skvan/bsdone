package com.bsball.repository;

import com.bsball.model.entity.GameStatAdjustment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameStatAdjustmentRepository extends JpaRepository<GameStatAdjustment, Long> {
    List<GameStatAdjustment> findByTenantIdAndGameIdOrderByOperatedAtDescIdDesc(
            Long tenantId, Long gameId);
}
