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

import com.bsball.core.CurrentUserHolder;
import com.bsball.model.dto.HitSprayDTO;
import com.bsball.model.entity.HitSpray;
import com.bsball.repository.HitSprayRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HitSprayService {
    private static final int RID_MAX_LENGTH = 64;
    private static final String KEY_DELIMITER = "|";
    private final HitSprayRepository hitSprayRepository;

    @Transactional
    public HitSpray save(HitSprayDTO dto) {
        List<HitSpray> saved = this.saveAll(List.of(dto));
        return saved.isEmpty() ? null : saved.get(0);
    }

    /**
     * 批量落库。
     * <p>幂等与去重规则：
     * <ol>
     *   <li>按 {@code (gameId, rid)} 幂等：已存在同 rid（含已软删行）直接跳过；</li>
     *   <li>同一批次内同 rid 只保留第一条、同一打席键只保留最后一条；</li>
     *   <li>打席键 = {@code gameId + inning + half + batterOrder + playerId + outsBefore + runnersBefore}
     *       （{@code runnersBefore} 的 null 与空串视为相同，{@code teamId} 不参与）；
     *       键字段不全的记录不做打席键去重，按 rid/插入语义处理；</li>
     *   <li>若同场同键已有活跃行（{@code deleted_at IS NULL}）且 rid 不同：软删旧行后插入新行，
     *       保留旧 rid 供延迟重试识别；不原地更新旧行，避免覆盖旧 rid 破坏幂等。</li>
     * </ol>
     *
     * @return 实际新插入的行（不含被软删的旧行）
     */
    @Transactional
    public List<HitSpray> saveAll(List<HitSprayDTO> dtos) {
        List<HitSprayDTO> unique = this.distinctPayloads(dtos);
        if (unique.isEmpty()) {
            return List.of();
        }
        List<HitSprayDTO> pending = this.excludeExistingRids(unique);
        if (pending.isEmpty()) {
            return List.of();
        }
        Map<Long, Map<String, List<HitSpray>>> activeRows = this.loadActiveAtBatRows(pending);
        List<HitSpray> obsolete = new ArrayList<>();
        List<HitSpray> toSave = new ArrayList<>();
        for (HitSprayDTO dto : pending) {
            this.collectObsolete(dto, activeRows, obsolete);
            toSave.add(this.toEntity(dto, this.normalizeRid(dto.getRid())));
        }
        if (!obsolete.isEmpty()) {
            this.hitSprayRepository.saveAll(obsolete);
        }
        return this.hitSprayRepository.saveAll(toSave);
    }

    public List<HitSpray> getByGameId(Long gameId) {
        return this.hitSprayRepository.findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(gameId);
    }

    public List<HitSpray> getByPlayerAndGame(Long playerId, Long gameId) {
        return this.hitSprayRepository.findByPlayerIdAndGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(playerId, gameId);
    }

    public List<HitSpray> getByTeamAndGame(Long teamId, Long gameId) {
        return this.hitSprayRepository.findByTeamIdAndGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(teamId, gameId);
    }

    /**
     * 批次内去重：先按 rid 保留首条（兼容旧语义），再按打席键保留最后一条（后写入视为修正）。
     */
    private List<HitSprayDTO> distinctPayloads(List<HitSprayDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        List<HitSprayDTO> ridUnique = new ArrayList<>();
        Set<String> ridKeys = new HashSet<>();
        for (HitSprayDTO dto : dtos) {
            if (dto == null) {
                continue;
            }
            String rid = this.normalizeRid(dto.getRid());
            if (rid == null) {
                ridUnique.add(dto);
                continue;
            }
            if (ridKeys.add(this.idempotencyKey(dto.getGameId(), rid))) {
                ridUnique.add(dto);
            }
        }
        List<HitSprayDTO> result = new ArrayList<>();
        Set<String> atBatKeys = new HashSet<>();
        for (int i = ridUnique.size() - 1; i >= 0; i--) {
            HitSprayDTO dto = ridUnique.get(i);
            String key = this.atBatKey(dto);
            if (key != null && !atBatKeys.add(key)) {
                continue;
            }
            result.add(dto);
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * 剔除已存在的 rid（查询不过滤 deleted_at，已软删行同样命中，避免旧 rid 重试复活成新行）。
     */
    private List<HitSprayDTO> excludeExistingRids(List<HitSprayDTO> dtos) {
        Map<Long, List<String>> ridsByGame = new HashMap<>();
        for (HitSprayDTO dto : dtos) {
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
        List<HitSprayDTO> pending = new ArrayList<>();
        for (HitSprayDTO dto : dtos) {
            String rid = this.normalizeRid(dto.getRid());
            if (rid != null && dto.getGameId() != null
                    && existing.contains(this.idempotencyKey(dto.getGameId(), rid))) {
                continue;
            }
            pending.add(dto);
        }
        return pending;
    }

    /**
     * 只为“含完整打席键”的记录按场加载活跃行，并按打席键建索引。
     */
    private Map<Long, Map<String, List<HitSpray>>> loadActiveAtBatRows(List<HitSprayDTO> dtos) {
        Set<Long> gameIds = new LinkedHashSet<>();
        for (HitSprayDTO dto : dtos) {
            if (this.atBatKey(dto) != null) {
                gameIds.add(dto.getGameId());
            }
        }
        Map<Long, Map<String, List<HitSpray>>> result = new HashMap<>();
        for (Long gameId : gameIds) {
            Map<String, List<HitSpray>> byKey = new HashMap<>();
            for (HitSpray row : this.hitSprayRepository.findByGameIdAndDeletedAtIsNullOrderByCreatedAtAsc(gameId)) {
                String key = this.atBatKey(row);
                if (key != null) {
                    byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
                }
            }
            result.put(gameId, byKey);
        }
        return result;
    }

    /**
     * 收集同场同键的活跃旧行并打上软删标记（保留旧 rid；返回前统一持久化）。
     */
    private void collectObsolete(HitSprayDTO dto,
                                 Map<Long, Map<String, List<HitSpray>>> activeRows,
                                 List<HitSpray> obsolete) {
        String key = this.atBatKey(dto);
        if (key == null || dto.getGameId() == null) {
            return;
        }
        Map<String, List<HitSpray>> byKey = activeRows.get(dto.getGameId());
        if (byKey == null) {
            return;
        }
        List<HitSpray> rows = byKey.get(key);
        if (rows == null) {
            return;
        }
        for (HitSpray row : rows) {
            row.setDeletedAt(LocalDateTime.now());
            row.setDeletedBy(CurrentUserHolder.get());
            obsolete.add(row);
        }
    }

    private String atBatKey(HitSprayDTO dto) {
        if (dto == null) {
            return null;
        }
        return this.atBatKey(dto.getGameId(), dto.getInning(), dto.getHalf(), dto.getBatterOrder(),
                dto.getPlayerId(), dto.getOutsBefore(), dto.getRunnersBefore());
    }

    private String atBatKey(HitSpray row) {
        if (row == null) {
            return null;
        }
        return this.atBatKey(row.getGameId(), row.getInning(), row.getHalf(), row.getBatterOrder(),
                row.getPlayerId(), row.getOutsBefore(), row.getRunnersBefore());
    }

    private String atBatKey(Long gameId, Short inning, String half, Short batterOrder,
                            Long playerId, Short outsBefore, String runnersBefore) {
        if (gameId == null || inning == null || half == null || half.isBlank()
                || batterOrder == null || playerId == null || outsBefore == null) {
            return null;
        }
        return gameId + KEY_DELIMITER + inning + KEY_DELIMITER + half.trim().toLowerCase(Locale.ROOT)
                + KEY_DELIMITER + batterOrder + KEY_DELIMITER + playerId
                + KEY_DELIMITER + outsBefore + KEY_DELIMITER + this.normalizeRunners(runnersBefore);
    }

    private String normalizeRunners(String runnersBefore) {
        if (runnersBefore == null) {
            return "";
        }
        return runnersBefore.trim();
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
