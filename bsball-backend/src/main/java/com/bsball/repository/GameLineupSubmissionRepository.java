package com.bsball.repository;

import com.bsball.model.entity.GameLineupSubmission;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 赛前阵容提交仓储：一场比赛每队一条（软删行除外）。 */
public interface GameLineupSubmissionRepository extends JpaRepository<GameLineupSubmission, Long> {

    Optional<GameLineupSubmission> findByGameIdAndTeamIdAndDeletedAtIsNull(Long gameId, Long teamId);

    List<GameLineupSubmission> findByGameIdAndDeletedAtIsNullOrderByIdAsc(Long gameId);

    List<GameLineupSubmission> findByGameIdInAndTeamIdAndDeletedAtIsNull(Collection<Long> gameIds, Long teamId);

    /** 球队赛前阵容保存：对已有行加悲观锁，串行化同一场同一队的并发写。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from GameLineupSubmission s where s.gameId = :gameId and s.teamId = :teamId and s.deletedAt is null")
    Optional<GameLineupSubmission> findForUpdate(@Param("gameId") Long gameId, @Param("teamId") Long teamId);

    /** 撤销报名/移除参赛时清理该赛事该队的赛前阵容（软删留痕）。 */
    @Modifying
    @Query("update GameLineupSubmission s set s.deletedAt = :now, s.deletedBy = :userId "
            + "where s.eventId = :eventId and s.teamId = :teamId and s.deletedAt is null")
    int softDeleteByEventIdAndTeamId(@Param("eventId") Long eventId, @Param("teamId") Long teamId,
            @Param("now") LocalDateTime now, @Param("userId") Long userId);
}
