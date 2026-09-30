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

    /** 集合版：过滤软删（deletedAt is null）与租户；与单值版 findIdsByLeagueIdAndTenantId（不过滤软删）语义不同。 */
    @Query(value="select t.id from Team t where t.deletedAt is null and t.tenantId = :tenantId and t.leagueId in :leagueIds")
    public List<Long> findIdsByLeagueIdInAndTenantId(@Param(value="leagueIds") Collection<Long> var1, @Param(value="tenantId") Long var2);

    public List<Team> findByTenantIdAndDeletedAtIsNullOrderBySortAscIdAsc(Long var1);

    /**
     * 含已解散（不过滤 deletedAt）的名称查询：供积分榜与历史展示解析“曾出现过”的球队名，
     * 并携带 deletedAt 供调用方判定“已解散”标记。与“可选集合”选择器查询（一律带 deletedAt is null）语义不同，
     * 解散球队仍可解析名字（spec §6.7 历史口径：队名解析允许解析已解散球队名）。
     */
    @Query(value="select t from Team t where t.id in :ids")
    public List<Team> findByIdInIncludingDissolved(@Param(value="ids") Collection<Long> var1);
}

