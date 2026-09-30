/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.League
 *  com.bsball.repository.LeagueRepository
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.bsball.repository;

import com.bsball.model.entity.League;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueRepository
extends JpaRepository<League, Long> {
    public Page<League> findByDeletedAtIsNull(Pageable var1);

    public Page<League> findByIdInAndDeletedAtIsNull(Collection<Long> var1, Pageable var2);

    public Page<League> findByTenantIdAndDeletedAtIsNull(Long var1, Pageable var2);

    public Page<League> findByTenantIdAndIdInAndDeletedAtIsNull(Long var1, Collection<Long> var2, Pageable var3);

    @Deprecated
    public Page<League> findByTenantId(Long var1, Pageable var2);

    @Deprecated
    public Page<League> findByTenantIdAndIdIn(Long var1, Collection<Long> var2, Pageable var3);

    public List<League> findByTenantIdAndDeletedAtIsNullOrderBySortAscIdAsc(Long var1);

    public boolean existsByTenantIdAndNameIgnoreCaseAndDeletedAtIsNull(Long var1, String var2);

    public boolean existsByTenantIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(Long var1, String var2, Long var3);

    public long countByTenantIdAndDeletedAtIsNull(Long var1);

    /** 超管资产视图（spec §6.10）：平台已归还（platform_owned=true 且已软删）的联盟计数。 */
    public long countByPlatformOwnedTrueAndDeletedAtIsNotNull();
}

