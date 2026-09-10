/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.GamePlayerStat
 *  com.bsball.repository.GamePlayerStatRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.bsball.repository;

import com.bsball.model.entity.GamePlayerStat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamePlayerStatRepository
extends JpaRepository<GamePlayerStat, Long> {
    public List<GamePlayerStat> findByGameId(Long var1);

    public List<GamePlayerStat> findByPlayerId(Long var1);

    public void deleteByGameId(Long var1);
}

