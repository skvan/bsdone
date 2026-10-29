package com.bsball.repository;

import com.bsball.model.entity.GameFieldingPosStat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameFieldingPosStatRepository extends JpaRepository<GameFieldingPosStat, Long> {
    List<GameFieldingPosStat> findByGameIdOrderByTeamIdAscPlayerIdAscPositionAsc(Long gameId);

    void deleteByGameId(Long gameId);
}
