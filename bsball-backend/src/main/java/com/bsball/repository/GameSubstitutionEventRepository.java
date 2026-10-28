package com.bsball.repository;

import com.bsball.model.entity.GameSubstitutionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameSubstitutionEventRepository extends JpaRepository<GameSubstitutionEvent, Long> {
    boolean existsByGameIdAndTransactionId(Long gameId, String transactionId);

    java.util.List<GameSubstitutionEvent> findByGameIdOrderBySequenceAscIdAsc(Long gameId);

    @Query("select coalesce(max(e.sequence), 0) from GameSubstitutionEvent e where e.gameId = :gameId")
    int findMaxSequence(@Param("gameId") Long gameId);
}
