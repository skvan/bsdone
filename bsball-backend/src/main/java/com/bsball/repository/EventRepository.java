/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.Event
 *  com.bsball.repository.EventRepository
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.bsball.repository;

import com.bsball.model.entity.Event;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository
extends JpaRepository<Event, Long> {
    public Page<Event> findByDeletedAtIsNull(Pageable var1);

    public Page<Event> findByLeagueIdInAndDeletedAtIsNull(Collection<Long> var1, Pageable var2);

    public Page<Event> findByTenantIdAndDeletedAtIsNull(Long var1, Pageable var2);

    public Page<Event> findByTenantIdAndLeagueIdInAndDeletedAtIsNull(Long var1, Collection<Long> var2, Pageable var3);

    public List<Event> findByTenantIdAndLeagueIdInAndDeletedAtIsNull(Long var1, Collection<Long> var2);

    public Page<Event> findByTenantId(Long var1, Pageable var2);

    public Page<Event> findByTenantIdAndLeagueIdIn(Long var1, Collection<Long> var2, Pageable var3);

    public List<Event> findByTenantIdAndLeagueIdIn(Long var1, Collection<Long> var2);

    @Query(value="select e.id from Event e where e.deletedAt is null and e.tenantId = :tenantId and e.leagueId in :leagueIds")
    public List<Long> findIdsByLeagueIdInAndTenantId(@Param(value="leagueIds") Collection<Long> var1, @Param(value="tenantId") Long var2);
}

