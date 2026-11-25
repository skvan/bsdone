/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.HitSpray
 *  com.bsball.repository.HitSprayRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.bsball.repository;

import com.bsball.model.entity.HitSpray;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HitSprayRepository
extends JpaRepository<HitSpray, Long> {
    public List<HitSpray> findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long gameId);

    public List<HitSpray> findByPlayerIdAndGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long playerId, Long gameId);

    public List<HitSpray> findByTeamIdAndGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long teamId, Long gameId);

    /**
     * 按比赛 + 客户端幂等键查询；不过滤 deleted_at，保证已软删 rid 的重试仍被识别并跳过。
     */
    public List<HitSpray> findByGameIdAndRidIn(Long gameId, Collection<String> rids);
}
