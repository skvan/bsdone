package com.bsball.repository;

import com.bsball.model.entity.LeagueSpecialRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 联赛级特殊规则开关仓储（Issue #275）。
 */
public interface LeagueSpecialRuleRepository
extends JpaRepository<LeagueSpecialRule, Long> {
    public List<LeagueSpecialRule> findByLeagueId(Long leagueId);

    public Optional<LeagueSpecialRule> findByLeagueIdAndRuleKey(Long leagueId, String ruleKey);
}
