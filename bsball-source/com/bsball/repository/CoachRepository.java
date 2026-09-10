/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.CoachOptionDto
 *  com.bsball.model.entity.Coach
 *  com.bsball.repository.CoachRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package com.bsball.repository;

import com.bsball.model.dto.CoachOptionDto;
import com.bsball.model.entity.Coach;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoachRepository
extends JpaRepository<Coach, Long>,
JpaSpecificationExecutor<Coach> {
    @Query(value="select new com.bsball.model.dto.CoachOptionDto(c.id, c.name, c.teamId) from Coach c where c.deletedAt is null order by coalesce(c.sort, 0) asc, c.id asc")
    public List<CoachOptionDto> findAllForSelect();

    @Query(value="select new com.bsball.model.dto.CoachOptionDto(c.id, c.name, c.teamId) from Coach c where c.deletedAt is null and c.tenantId = :tid order by coalesce(c.sort, 0) asc, c.id asc")
    public List<CoachOptionDto> findAllForSelectByTenantId(@Param(value="tid") long var1);

    @Query(value="select new com.bsball.model.dto.CoachOptionDto(c.id, c.name, c.teamId) from Coach c where c.deletedAt is null and c.tenantId = :tid and c.teamId in :teamIds order by coalesce(c.sort, 0) asc, c.id asc")
    public List<CoachOptionDto> findAllForSelectByTenantIdAndTeamIdIn(@Param(value="tid") long var1, @Param(value="teamIds") Collection<Long> var3);

    @Query(value="select new com.bsball.model.dto.CoachOptionDto(c.id, c.name, c.teamId) from Coach c where c.deletedAt is null and c.tenantId = :tid and c.teamId is null order by coalesce(c.sort, 0) asc, c.id asc")
    public List<CoachOptionDto> findAllForSelectByTenantIdAndTeamIdIsNull(@Param(value="tid") long var1);

    @Query(value="select new com.bsball.model.dto.CoachOptionDto(c.id, c.name, c.teamId) from Coach c where c.deletedAt is null and c.tenantId = :tid and (c.teamId is null or c.teamId in :teamIds) order by coalesce(c.sort, 0) asc, c.id asc")
    public List<CoachOptionDto> findAllForSelectByTenantIdAndFreeOrTeamIdIn(@Param(value="tid") long var1, @Param(value="teamIds") Collection<Long> var3);
}

