package com.bsball.repository;

import com.bsball.model.entity.GameFieldingPosStat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameFieldingPosStatRepository extends JpaRepository<GameFieldingPosStat, Long> {
    List<GameFieldingPosStat> findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(Long gameId);

    /** 按比赛、球队、球员删除逐位置守备行（结果页按球员整体替换）。 */
    void deleteByGameIdAndTeamIdAndPlayerId(Long gameId, Long teamId, Long playerId);

    void deleteByGameId(Long gameId);
}
