/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.TeamOptionDto
 *  com.bsball.model.entity.Team
 *  com.bsball.repository.TeamRepository
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package com.bsball.repository;

import com.bsball.model.dto.TeamOptionDto;
import com.bsball.model.entity.Team;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository
extends JpaRepository<Team, Long> {
    @Query(value="select new com.bsball.model.dto.TeamOptionDto(t.id, t.name, t.shortName, t.logo) from Team t where t.deletedAt is null order by coalesce(t.sort, 0) asc, t.id asc")
    public List<TeamOptionDto> findAllForSelect();

    @Query(value="select new com.bsball.model.dto.TeamOptionDto(t.id, t.name, t.shortName, t.logo) from Team t where t.deletedAt is null and t.tenantId = :tenantId order by coalesce(t.sort, 0) asc, t.id asc")
    public List<TeamOptionDto> findForSelectByTenantId(@Param(value="tenantId") Long var1);

    @Query(value="select new com.bsball.model.dto.TeamOptionDto(t.id, t.name, t.shortName, t.logo) from Team t where t.deletedAt is null and t.tenantId = :tenantId and t.id in :ids order by coalesce(t.sort, 0) asc, t.id asc")
    public List<TeamOptionDto> findForSelectByTenantIdAndIdIn(@Param(value="tenantId") Long var1, @Param(value="ids") Collection<Long> var2);

    @Query(value="select new com.bsball.model.dto.TeamOptionDto(t.id, t.name, t.shortName, t.logo) from Team t where t.deletedAt is null and t.id in :ids order by coalesce(t.sort, 0) asc, t.id asc")
    public List<TeamOptionDto> findForSelectByIdIn(@Param(value="ids") Collection<Long> var1);

    public Page<Team> findByDeletedAtIsNull(Pageable var1);

    public Page<Team> findByIdInAndDeletedAtIsNull(Collection<Long> var1, Pageable var2);

    public Page<Team> findByTenantIdAndDeletedAtIsNull(Long var1, Pageable var2);

    public Page<Team> findByTenantIdAndIdInAndDeletedAtIsNull(Long var1, Collection<Long> var2, Pageable var3);

    public Page<Team> findByTenantId(Long var1, Pageable var2);

    public Page<Team> findByTenantIdAndIdIn(Long var1, Collection<Long> var2, Pageable var3);

    @Query(value="select t.id from Team t where t.leagueId = :leagueId and t.tenantId = :tenantId")
    public List<Long> findIdsByLeagueIdAndTenantId(@Param(value="leagueId") Long var1, @Param(value="tenantId") Long var2);

    public List<Team> findByTenantIdAndDeletedAtIsNullOrderBySortAscIdAsc(Long var1);
}

