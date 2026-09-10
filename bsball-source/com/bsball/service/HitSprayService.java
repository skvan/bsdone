/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.HitSprayDTO
 *  com.bsball.model.entity.HitSpray
 *  com.bsball.repository.HitSprayRepository
 *  com.bsball.service.HitSprayService
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.model.dto.HitSprayDTO;
import com.bsball.model.entity.HitSpray;
import com.bsball.repository.HitSprayRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HitSprayService {
    private final HitSprayRepository hitSprayRepository;

    @Transactional
    public HitSpray save(HitSprayDTO dto) {
        HitSpray entity = new HitSpray();
        entity.setGameId(dto.getGameId());
        entity.setPlayerId(dto.getPlayerId());
        entity.setTeamId(dto.getTeamId());
        entity.setInning(dto.getInning());
        entity.setHalf(dto.getHalf());
        entity.setBatterOrder(dto.getBatterOrder());
        entity.setOutsBefore(dto.getOutsBefore());
        entity.setRunnersBefore(dto.getRunnersBefore());
        entity.setResultCode(dto.getResultCode());
        entity.setBipCode(dto.getBipCode());
        entity.setRbi(dto.getRbi());
        entity.setSprayX(dto.getSprayX() != null ? BigDecimal.valueOf(dto.getSprayX()) : null);
        entity.setSprayY(dto.getSprayY() != null ? BigDecimal.valueOf(dto.getSprayY()) : null);
        entity.setSprayZone(dto.getSprayZone());
        entity.setSprayDepth(dto.getSprayDepth());
        entity.setRecordedAt(LocalDateTime.now());
        return (HitSpray)this.hitSprayRepository.save((Object)entity);
    }

    @Transactional
    public List<HitSpray> saveAll(List<HitSprayDTO> dtos) {
        return dtos.stream().map(arg_0 -> this.save(arg_0)).toList();
    }

    public List<HitSpray> getByGameId(Long gameId) {
        return this.hitSprayRepository.findByGameIdOrderByCreatedAtAsc(gameId);
    }

    public List<HitSpray> getByPlayerAndGame(Long playerId, Long gameId) {
        return this.hitSprayRepository.findByPlayerIdAndGameIdOrderByCreatedAtAsc(playerId, gameId);
    }

    public List<HitSpray> getByTeamAndGame(Long teamId, Long gameId) {
        return this.hitSprayRepository.findByTeamIdAndGameIdOrderByCreatedAtAsc(teamId, gameId);
    }

    @Generated
    public HitSprayService(HitSprayRepository hitSprayRepository) {
        this.hitSprayRepository = hitSprayRepository;
    }
}

