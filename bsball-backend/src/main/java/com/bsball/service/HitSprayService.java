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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HitSprayService {
    private static final int RID_MAX_LENGTH = 64;
    private final HitSprayRepository hitSprayRepository;

    @Transactional
    public HitSpray save(HitSprayDTO dto) {
        List<HitSpray> saved = this.saveAll(List.of(dto));
        return saved.isEmpty() ? null : saved.get(0);
    }

    /**
     * 批量落库；带 rid 的记录按 (gameId, rid) 幂等，重复提交直接跳过。
     * 历史前端没有 rid，仍按原语义插入。
     */
    @Transactional
    public List<HitSpray> saveAll(List<HitSprayDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        List<HitSprayDTO> unique = new ArrayList<>();
        Set<String> batchKeys = new HashSet<>();
        for (HitSprayDTO dto : dtos) {
            if (dto == null) {
                continue;
            }
            String rid = this.normalizeRid(dto.getRid());
            if (rid == null) {
                unique.add(dto);
                continue;
            }
            String key = this.idempotencyKey(dto.getGameId(), rid);
            if (batchKeys.add(key)) {
                unique.add(dto);
            }
        }
        if (unique.isEmpty()) {
            return List.of();
        }

        Map<Long, List<String>> ridsByGame = new HashMap<>();
        for (HitSprayDTO dto : unique) {
            String rid = this.normalizeRid(dto.getRid());
            if (rid != null && dto.getGameId() != null) {
                ridsByGame.computeIfAbsent(dto.getGameId(), k -> new ArrayList<>()).add(rid);
            }
        }
        Set<String> existing = new HashSet<>();
        for (Map.Entry<Long, List<String>> entry : ridsByGame.entrySet()) {
            this.hitSprayRepository.findByGameIdAndRidIn(entry.getKey(), entry.getValue()).stream()
                    .filter(h -> h.getRid() != null)
                    .forEach(h -> existing.add(this.idempotencyKey(h.getGameId(), h.getRid())));
        }

        List<HitSpray> toSave = new ArrayList<>();
        for (HitSprayDTO dto : unique) {
            String rid = this.normalizeRid(dto.getRid());
            if (rid != null && dto.getGameId() != null
                    && existing.contains(this.idempotencyKey(dto.getGameId(), rid))) {
                continue;
            }
            toSave.add(this.toEntity(dto, rid));
        }
        return toSave.isEmpty() ? List.of() : this.hitSprayRepository.saveAll(toSave);
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

    private String normalizeRid(String rid) {
        if (rid == null) {
            return null;
        }
        String value = rid.trim();
        if (value.isEmpty()) {
            return null;
        }
        return value.length() > RID_MAX_LENGTH ? value.substring(0, RID_MAX_LENGTH) : value;
    }

    private String idempotencyKey(Long gameId, String rid) {
        return String.valueOf(gameId) + "|" + rid;
    }

    private HitSpray toEntity(HitSprayDTO dto, String rid) {
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
        entity.setRid(rid);
        return entity;
    }

    @Generated
    public HitSprayService(HitSprayRepository hitSprayRepository) {
        this.hitSprayRepository = hitSprayRepository;
    }
}
