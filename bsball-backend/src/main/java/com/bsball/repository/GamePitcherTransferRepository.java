package com.bsball.repository;

import com.bsball.model.entity.GamePitcherTransfer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamePitcherTransferRepository extends JpaRepository<GamePitcherTransfer, Long> {
    Optional<GamePitcherTransfer> findByIdAndTenantIdAndGameId(Long id, Long tenantId, Long gameId);
}
