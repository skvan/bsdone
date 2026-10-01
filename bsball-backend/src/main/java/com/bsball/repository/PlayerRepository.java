/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.PlayerOptionDto
 *  com.bsball.model.entity.Player
 *  com.bsball.repository.PlayerRepository
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package com.bsball.repository;

import com.bsball.model.dto.PlayerOptionDto;
import com.bsball.model.entity.Player;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRepository
extends JpaRepository<Player, Long>,
JpaSpecificationExecutor<Player> {
    public Page<Player> findByDeletedAtIsNullAndTeamId(Long var1, Pageable var2);

    public Page<Player> findByDeletedAtIsNull(Pageable var1);

    public List<Player> findByDeletedAtIsNull();

    public List<Player> findByDeletedAtIsNullAndIdIn(List<Long> var1);

    public Optional<Player> findFirstByUserIdAndDeletedAtIsNull(Long userId);

    public List<Player> findByNameAndBirthDateAndTenantIdAndDeletedAtIsNull(String name, String birthDate, Long tenantId);

    @Query(value="select new com.bsball.model.dto.PlayerOptionDto(p.id, p.name, p.number, p.teamId) from Player p where p.deletedAt is null order by coalesce(p.sort, 0) asc, p.id asc")
    public List<PlayerOptionDto> findAllForSelect();

    @Query(value="select new com.bsball.model.dto.PlayerOptionDto(p.id, p.name, p.number, p.teamId) from Player p where p.deletedAt is null and p.tenantId = :tid order by coalesce(p.sort, 0) asc, p.id asc")
    public List<PlayerOptionDto> findAllForSelectByTenantId(@Param(value="tid") long var1);

    @Query(value="select new com.bsball.model.dto.PlayerOptionDto(p.id, p.name, p.number, p.teamId) from Player p where p.deletedAt is null and p.tenantId = :tid and p.teamId in :teamIds order by coalesce(p.sort, 0) asc, p.id asc")
    public List<PlayerOptionDto> findAllForSelectByTenantIdAndTeamIdIn(@Param(value="tid") long var1, @Param(value="teamIds") Collection<Long> var3);

    public List<Player> findByDeletedAtIsNullAndTenantId(long var1);

    @Query(value="select count(p) from Player p, Team t where p.deletedAt is null and t.deletedAt is null and p.teamId = t.id and p.userId = :userId and t.leagueId = :leagueId and p.id <> :excludePlayerId and t.id <> :excludeTeamId")
    public long countClaimedByUserInLeagueExcludingTeam(@Param(value="userId") long var1, @Param(value="leagueId") long var3, @Param(value="excludePlayerId") long var5, @Param(value="excludeTeamId") long var7);

    @Query(value="select p.id from Player p where p.deletedAt is null and lower(p.name) like lower(concat('%', :kw, '%'))")
    public List<Long> findIdsByNameLike(@Param(value="kw") String var1);

    @Query(value="select count(p) from Player p where p.deletedAt is null and p.tenantId = :tenantId and trim(p.name) = :name")
    public long countActiveByTenantIdAndFullName(@Param(value="tenantId") long var1, @Param(value="name") String var3);

    @Query(value="select count(p) from Player p where p.deletedAt is null and p.tenantId = :tenantId and trim(p.name) = :name and p.id <> :excludeId")
    public long countActiveByTenantIdAndFullNameExcludingId(@Param(value="tenantId") long var1, @Param(value="name") String var3, @Param(value="excludeId") long var4);

    /** 超管资产视图（spec §6.10）：平台已归还（platform_owned=true 且已软删）的球员计数。 */
    public long countByPlatformOwnedTrueAndDeletedAtIsNotNull();
}

