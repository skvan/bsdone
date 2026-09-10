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
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HitSprayRepository
extends JpaRepository<HitSpray, Long> {
    public List<HitSpray> findByGameIdOrderByCreatedAtAsc(Long var1);

    public List<HitSpray> findByPlayerIdAndGameIdOrderByCreatedAtAsc(Long var1, Long var2);

    public List<HitSpray> findByTeamIdAndGameIdOrderByCreatedAtAsc(Long var1, Long var2);
}

