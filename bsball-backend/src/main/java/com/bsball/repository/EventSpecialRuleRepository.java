package com.bsball.repository;

import com.bsball.model.entity.EventSpecialRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSpecialRuleRepository extends JpaRepository<EventSpecialRule, Long> {
    List<EventSpecialRule> findByEventId(Long eventId);

    Optional<EventSpecialRule> findByEventIdAndRuleKey(Long eventId, String ruleKey);

    boolean existsByEventId(Long eventId);
}
