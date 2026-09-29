package com.bsball.repository;

import com.bsball.model.entity.LeagueOwner;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueOwnerRepository
extends JpaRepository<LeagueOwner, Long> {
    public List<LeagueOwner> findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(Long userId, Long tenantId, String status);

    public List<LeagueOwner> findByLeagueIdAndStatusAndDeletedAtIsNull(Long leagueId, String status);

    public List<LeagueOwner> findByLeagueIdAndDeletedAtIsNull(Long leagueId);

    public boolean existsByLeagueIdAndUserIdAndDeletedAtIsNull(Long leagueId, Long userId);
}
