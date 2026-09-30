package com.bsball.repository;

import com.bsball.model.entity.LeagueCreateRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueCreateRequestRepository
extends JpaRepository<LeagueCreateRequest, Long> {
    public List<LeagueCreateRequest> findByTenantIdAndStatusAndDeletedAtIsNull(Long tenantId, String status);

    public List<LeagueCreateRequest> findByTenantIdAndDeletedAtIsNull(Long tenantId);
}
