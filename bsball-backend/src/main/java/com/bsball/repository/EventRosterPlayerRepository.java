package com.bsball.repository;

import com.bsball.model.entity.EventRosterPlayer;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRosterPlayerRepository
extends JpaRepository<EventRosterPlayer, Long> {
    public List<EventRosterPlayer> findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(Long eventId, Long teamId);

    public List<EventRosterPlayer> findByEventIdAndDeletedAtIsNull(Long eventId);

    public List<EventRosterPlayer> findByEventIdAndTeamIdInAndDeletedAtIsNull(Long eventId, Collection<Long> teamIds);

    public long countByEventIdAndTeamIdAndDeletedAtIsNull(Long eventId, Long teamId);
}
