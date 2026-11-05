package com.bsball.repository;

import com.bsball.model.entity.GameIdentityTransfer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameIdentityTransferRepository
        extends JpaRepository<GameIdentityTransfer, Long> {
    Optional<GameIdentityTransfer> findByIdAndTenantIdAndGameId(
            Long id, Long tenantId, Long gameId);

    List<GameIdentityTransfer> findByTenantIdAndGameIdOrderByIdDesc(
            Long tenantId, Long gameId);
}
