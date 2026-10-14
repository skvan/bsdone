package com.bsball.repository;

import com.bsball.model.entity.EventTeam;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventTeamRepository
extends JpaRepository<EventTeam, Long> {
    public Optional<EventTeam> findByEventIdAndTeamIdAndDeletedAtIsNull(Long eventId, Long teamId);

    /** 电子秩序册：名单提交串行化行锁（同一赛事同一球队并发提交互斥，防唯一索引冲突 500）。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value="select r from EventTeam r where r.eventId = :eventId and r.teamId = :teamId and r.deletedAt is null")
    public Optional<EventTeam> findForUpdate(@Param(value="eventId") Long eventId, @Param(value="teamId") Long teamId);

    public List<EventTeam> findByEventIdAndDeletedAtIsNullOrderByIdAsc(Long eventId);

    public boolean existsByEventIdAndDeletedAtIsNull(Long eventId);

    public List<EventTeam> findByTeamIdAndDeletedAtIsNullOrderByIdDesc(Long teamId);

    /** 排赛候选/报名制门槛（Issue #318 决策 8/11）：已确认且名单已锁定的参赛登记。 */
    public List<EventTeam> findByEventIdAndStatusAndRosterLockedTrueAndDeletedAtIsNull(Long eventId, String status);
}
