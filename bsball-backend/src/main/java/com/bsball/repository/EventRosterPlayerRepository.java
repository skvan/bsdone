package com.bsball.repository;

import com.bsball.model.entity.EventRosterPlayer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRosterPlayerRepository
extends JpaRepository<EventRosterPlayer, Long> {
    public List<EventRosterPlayer> findByEventIdAndTeamIdAndDeletedAtIsNullOrderBySortAscIdAsc(Long eventId, Long teamId);

    public List<EventRosterPlayer> findByEventIdAndDeletedAtIsNull(Long eventId);

    public long countByEventIdAndTeamIdAndDeletedAtIsNull(Long eventId, Long teamId);
}
