package com.bsball.repository;

import com.bsball.model.entity.PlayerTeam;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerTeamRepository
extends JpaRepository<PlayerTeam, Long> {
    public List<PlayerTeam> findByPlayerIdAndDeletedAtIsNullOrderBySortAscIdAsc(Long playerId);

    public List<PlayerTeam> findByPlayerIdInAndDeletedAtIsNullOrderBySortAscIdAsc(Collection<Long> playerIds);

    public List<PlayerTeam> findByPlayerIdInAndDeletedAtIsNullAndCurrentTrue(Collection<Long> playerIds);

    public Optional<PlayerTeam> findFirstByPlayerIdAndTeamIdAndDeletedAtIsNotNullOrderByIdDesc(Long playerId, Long teamId);

    @Query(value="select e.teamId from PlayerTeam e where e.deletedAt is null and e.playerId = :playerId and e.current = true order by e.sort asc, e.id asc")
    public List<Long> findCurrentTeamIdsByPlayerId(@Param(value="playerId") Long playerId);

    @Query(value="select count(e) from PlayerTeam e where e.deletedAt is null and e.playerId = :playerId and e.current = true and e.teamId = :teamId")
    public long countCurrentEntry(@Param(value="playerId") Long playerId, @Param(value="teamId") Long teamId);

    @Query(value="select count(e) from PlayerTeam e where e.deletedAt is null and e.playerId = :playerId and e.current = true and e.teamId in :teamIds")
    public long countCurrentEntryInTeams(@Param(value="playerId") Long playerId, @Param(value="teamIds") Collection<Long> teamIds);

    @Query(value="select p.id, p.name, e.number, e.positions, p.batHand, p.throwHand, p.status from PlayerTeam e, Player p where e.deletedAt is null and p.deletedAt is null and p.id = e.playerId and e.current = true and e.tenantId = :tid and e.teamId = :teamId order by coalesce(p.sort, 0) asc, p.id asc")
    public List<Object[]> findTeamPlayerOptionFields(@Param(value="tid") long tid, @Param(value="teamId") long teamId);
}
