package com.bsball.repository;

import com.bsball.model.entity.EventTeam;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventTeamRepository
extends JpaRepository<EventTeam, Long> {
    public Optional<EventTeam> findByEventIdAndTeamIdAndDeletedAtIsNull(Long eventId, Long teamId);

    public List<EventTeam> findByEventIdAndDeletedAtIsNullOrderByIdAsc(Long eventId);

    public boolean existsByEventIdAndDeletedAtIsNull(Long eventId);

    public List<EventTeam> findByTeamIdAndDeletedAtIsNullOrderByIdDesc(Long teamId);
}
