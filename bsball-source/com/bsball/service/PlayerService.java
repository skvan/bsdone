/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.common.json.PositionsJsonUtil
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.dto.EffectiveDataScope
 *  com.bsball.model.dto.PlayerGameLogEntryDTO
 *  com.bsball.model.dto.PlayerOptionDto
 *  com.bsball.model.dto.PlayerStatsByEventDTO
 *  com.bsball.model.dto.TeamPlayerOptionDto
 *  com.bsball.model.entity.HistoryRecord
 *  com.bsball.model.entity.Player
 *  com.bsball.model.entity.Team
 *  com.bsball.repository.PlayerRepository
 *  com.bsball.repository.TeamRepository
 *  com.bsball.service.DataScopeService
 *  com.bsball.service.PersonnelHistoryRecorder
 *  com.bsball.service.PlayerService
 *  com.bsball.service.StatsService
 *  com.bsball.service.TenantQueryPolicyService
 *  jakarta.persistence.criteria.Expression
 *  jakarta.persistence.criteria.Predicate
 *  jakarta.persistence.criteria.Root
 *  jakarta.persistence.criteria.Subquery
 *  lombok.Generated
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.common.json.PositionsJsonUtil;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveDataScope;
import com.bsball.model.dto.PlayerGameLogEntryDTO;
import com.bsball.model.dto.PlayerOptionDto;
import com.bsball.model.dto.PlayerStatsByEventDTO;
import com.bsball.model.dto.TeamPlayerOptionDto;
import com.bsball.model.entity.HistoryRecord;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.Team;
import com.bsball.repository.PlayerRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.DataScopeService;
import com.bsball.service.PersonnelHistoryRecorder;
import com.bsball.service.StatsService;
import com.bsball.service.TenantQueryPolicyService;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final StatsService statsService;
    private final DataScopeService dataScopeService;
    private final PersonnelHistoryRecorder personnelHistoryRecorder;
    private final TenantQueryPolicyService tenantQueryPolicyService;
    private static final int PLAYER_BG_IMAGES_MAX = 5;

    public List<PlayerOptionDto> listForSelect() {
        if (this.tenantQueryPolicyService.isGlobalQueryMode()) {
            return this.playerRepository.findAllForSelect();
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (scope.isUnrestrictedInTenant()) {
            return this.playerRepository.findAllForSelectByTenantId(tid);
        }
        if (scope.getTeamIds().isEmpty()) {
            return List.of();
        }
        return this.playerRepository.findAllForSelectByTenantIdAndTeamIdIn(tid, (Collection)scope.getTeamIds());
    }

    public List<TeamPlayerOptionDto> listTeamPlayerOptions(long teamId) {
        if (teamId <= 0L) {
            throw new BusinessException(400, "\u5fc5\u987b\u4f20\u5165\u7403\u961f ID\uff08teamId\uff09");
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!scope.isUnrestrictedInTenant() && !scope.getTeamIds().contains(teamId)) {
            throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u7403\u961f\u7684\u7403\u5458");
        }
        List rows = this.playerRepository.findTeamPlayerOptionFields(tid, teamId);
        ArrayList<TeamPlayerOptionDto> out = new ArrayList<TeamPlayerOptionDto>(rows.size());
        for (Object[] r : rows) {
            Long id = r[0] != null ? Long.valueOf(((Number)r[0]).longValue()) : null;
            String name = r[1] != null ? String.valueOf(r[1]) : null;
            String number = r[2] != null ? String.valueOf(r[2]) : null;
            String positionsRaw = r[3] != null ? String.valueOf(r[3]) : null;
            String batHand = r[4] != null ? String.valueOf(r[4]) : null;
            String throwHand = r[5] != null ? String.valueOf(r[5]) : null;
            String status = r[6] != null ? String.valueOf(r[6]) : null;
            out.add(new TeamPlayerOptionDto(id, name, number, PlayerService.parsePositionsList((String)positionsRaw), batHand, throwHand, status));
        }
        return out;
    }

    private static List<String> parsePositionsList(String raw) {
        return PositionsJsonUtil.parseList((String)raw);
    }

    public PageResult<Player> list(Integer page, Integer pageSize, String sortProp, String sortOrder, Long teamId, List<Long> ids, String keyword, String number, String position, String throwHand, String batHand, String status, String joinDateFrom, String joinDateTo) {
        boolean global = this.tenantQueryPolicyService.isGlobalQueryMode();
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!global && !scope.isUnrestrictedInTenant() && scope.getTeamIds().isEmpty()) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        if (!(global || scope.isUnrestrictedInTenant() || teamId == null || teamId == 0L || scope.getTeamIds().contains(teamId))) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        if (ids != null && !ids.isEmpty()) {
            List list = this.playerRepository.findByDeletedAtIsNullAndIdIn(ids).stream().filter(p -> global || Objects.equals(p.getTenantId(), tid)).filter(p -> scope.isUnrestrictedInTenant() || p.getTeamId() != null && p.getTeamId() != 0L && scope.getTeamIds().contains(p.getTeamId()) || p.getTeamId() == null || p.getTeamId() == 0L).toList();
            return PageResult.of((List)list, (long)list.size());
        }
        boolean hasFilter = keyword != null && !keyword.isBlank() || number != null && !number.isBlank() || position != null && !position.isBlank() || throwHand != null && !throwHand.isBlank() || batHand != null && !batHand.isBlank() || status != null && !status.isBlank() || joinDateFrom != null && !joinDateFrom.isBlank() || joinDateTo != null && !joinDateTo.isBlank() || teamId != null;
        Pageable p2 = this.buildPageable(page, pageSize, sortProp, sortOrder);
        Specification spec = this.buildListSpec(teamId, keyword, number, position, throwHand, batHand, status, joinDateFrom, joinDateTo, tid, scope, hasFilter, global);
        Page result = this.playerRepository.findAll(spec, p2);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    private Specification<Player> buildListSpec(Long teamId, String keyword, String number, String position, String throwHand, String batHand, String status, String joinDateFrom, String joinDateTo, long tid, EffectiveDataScope scope, boolean applyExtraFilters, boolean global) {
        return (Specification & Serializable)(root, q, cb) -> {
            boolean freeAgentOnly;
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            preds.add(cb.isNull((Expression)root.get("deletedAt")));
            if (!global) {
                preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
            }
            boolean bl = freeAgentOnly = teamId != null && teamId == 0L;
            if (!(global || scope.isUnrestrictedInTenant() || freeAgentOnly)) {
                preds.add(root.get("teamId").in((Collection)scope.getTeamIds()));
            }
            if (teamId != null && teamId != 0L) {
                preds.add(cb.equal((Expression)root.get("teamId"), (Object)teamId));
            } else if (freeAgentOnly) {
                preds.add(cb.or((Expression)cb.isNull((Expression)root.get("teamId")), (Expression)cb.equal((Expression)root.get("teamId"), (Object)0L)));
            }
            if (applyExtraFilters) {
                if (keyword != null && !keyword.isBlank()) {
                    String k = "%" + keyword.toLowerCase() + "%";
                    preds.add(cb.or(new Predicate[]{cb.like(cb.lower((Expression)root.get("name")), k), cb.like(cb.lower((Expression)root.get("nickname")), k), cb.like(cb.lower((Expression)root.get("shortName")), k)}));
                }
                if (number != null && !number.isBlank()) {
                    preds.add(cb.like((Expression)root.get("number"), "%" + number + "%"));
                }
                if (position != null && !position.isBlank()) {
                    preds.add(cb.like((Expression)root.get("positions"), "%\"" + position + "\"%"));
                }
                if (throwHand != null && !throwHand.isBlank()) {
                    preds.add(cb.equal((Expression)root.get("throwHand"), (Object)throwHand));
                }
                if (batHand != null && !batHand.isBlank()) {
                    preds.add(cb.equal((Expression)root.get("batHand"), (Object)batHand));
                }
                if (status != null && !status.isBlank()) {
                    preds.add(cb.equal((Expression)root.get("status"), (Object)status));
                }
                if (joinDateFrom != null && !joinDateFrom.isBlank() || joinDateTo != null && !joinDateTo.isBlank()) {
                    Subquery sq = q.subquery(Long.class);
                    Root pcRoot = sq.from(HistoryRecord.class);
                    sq.select((Expression)pcRoot.get("id"));
                    ArrayList<Predicate> sqPreds = new ArrayList<Predicate>();
                    if (joinDateFrom != null && !joinDateFrom.isBlank()) {
                        sqPreds.add(cb.greaterThanOrEqualTo((Expression)pcRoot.get("changeDate"), (Comparable)((Object)joinDateFrom)));
                    }
                    if (joinDateTo != null && !joinDateTo.isBlank()) {
                        sqPreds.add(cb.lessThanOrEqualTo((Expression)pcRoot.get("changeDate"), (Comparable)((Object)joinDateTo)));
                    }
                    sq.where((Expression)cb.and(sqPreds.toArray(new Predicate[0])));
                    preds.add(root.get("currentJoinRecordId").in(new Expression[]{sq}));
                }
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }

    public Player get(Long id) {
        Player p = this.playerRepository.findById((Object)id).orElse(null);
        if (p == null || p.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(p.getTenantId(), tid)) {
            return null;
        }
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!(scope.isUnrestrictedInTenant() || p.getTeamId() != null && scope.getTeamIds().contains(p.getTeamId()))) {
            throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u7403\u5458");
        }
        return p;
    }

    public Map<String, Object> getStats(Long id, String gameMode) {
        if (id == null) {
            return this.statsService.emptyPlayerStats();
        }
        Player p = this.get(id);
        if (p == null) {
            return this.statsService.emptyPlayerStats();
        }
        return this.statsService.getPlayerStats(id, gameMode);
    }

    public List<PlayerStatsByEventDTO> getStatsBySeason(Long id, String gameMode) {
        if (id == null || this.get(id) == null) {
            return List.of();
        }
        return this.statsService.getPlayerStatsBySeason(id, gameMode);
    }

    public List<PlayerGameLogEntryDTO> getGameLog(Long id, int limit, String gameMode) {
        if (id == null || this.get(id) == null) {
            return List.of();
        }
        return this.statsService.getPlayerGameLog(id, limit, gameMode);
    }

    public PageResult<Map<String, Object>> drillDownBatting(Long id, String metric, Integer page, Integer pageSize, Long eventId, String season, String gameMode) {
        if (id == null || this.get(id) == null) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        return this.statsService.drillDownBatting(id, metric, page, pageSize, eventId, season, gameMode);
    }

    public PageResult<Map<String, Object>> drillDownPitching(Long id, String metric, Integer page, Integer pageSize, Long eventId, String season, String gameMode) {
        if (id == null || this.get(id) == null) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        return this.statsService.drillDownPitching(id, metric, page, pageSize, eventId, season, gameMode);
    }

    public PageResult<Map<String, Object>> drillDownFielding(Long id, String metric, Integer page, Integer pageSize, Long eventId, String season, String gameMode) {
        if (id == null || this.get(id) == null) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        return this.statsService.drillDownFielding(id, metric, page, pageSize, eventId, season, gameMode);
    }

    public Player create(Player entity) {
        if (entity.getName() != null) {
            String n = entity.getName().trim();
            entity.setName(n.isEmpty() ? null : n);
        }
        this.applyTenantFromTeam(entity);
        this.validateTeamId(entity.getTeamId());
        PlayerService.normalizeBlankStringsToNull((Player)entity);
        PlayerService.normalizePlayerBackgroundFields((Player)entity);
        Player saved = (Player)this.playerRepository.save((Object)entity);
        this.personnelHistoryRecorder.afterPlayerCreate(saved);
        return saved;
    }

    public boolean isFullNameDuplicate(String name, Long excludeId) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String n = name.trim();
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (excludeId != null) {
            return this.playerRepository.countActiveByTenantIdAndFullNameExcludingId(tid, n, excludeId.longValue()) > 0L;
        }
        return this.playerRepository.countActiveByTenantIdAndFullName(tid, n) > 0L;
    }

    public Player update(Long id, Player entity) {
        Player existing = this.playerRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u7403\u5458");
        }
        this.applyTenantFromTeam(entity);
        this.validateTeamId(entity.getTeamId());
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        if (entity.getName() != null) {
            String n = entity.getName().trim();
            entity.setName(n.isEmpty() ? null : n);
        }
        PlayerService.normalizeBlankStringsToNull((Player)entity);
        PlayerService.normalizePlayerBackgroundFields((Player)entity);
        Player before = PersonnelHistoryRecorder.snapshotPlayer((Player)existing);
        Player saved = (Player)this.playerRepository.save((Object)entity);
        this.personnelHistoryRecorder.afterPlayerUpdate(before, saved);
        return saved;
    }

    private static void normalizeBlankStringsToNull(Player p) {
        if (p == null) {
            return;
        }
        if (p.getName() != null && p.getName().isBlank()) {
            p.setName(null);
        }
        if (p.getShortName() != null && p.getShortName().isBlank()) {
            p.setShortName(null);
        }
        if (p.getNameEn() != null && p.getNameEn().isBlank()) {
            p.setNameEn(null);
        }
        if (p.getNickname() != null && p.getNickname().isBlank()) {
            p.setNickname(null);
        }
        if (p.getNumber() != null && p.getNumber().isBlank()) {
            p.setNumber(null);
        }
        if (p.getPositions() != null && p.getPositions().isBlank()) {
            p.setPositions(null);
        }
        if (p.getAvatar() != null && p.getAvatar().isBlank()) {
            p.setAvatar(null);
        }
        if (p.getBgImage() != null && p.getBgImage().isBlank()) {
            p.setBgImage(null);
        }
        if (p.getBgFocusConfig() != null && p.getBgFocusConfig().isEmpty()) {
            p.setBgFocusConfig(null);
        }
        if (p.getBgImages() != null && p.getBgImages().isEmpty()) {
            p.setBgImages(null);
        }
        if (p.getBirthDate() != null && p.getBirthDate().isBlank()) {
            p.setBirthDate(null);
        }
        if (p.getBirthPlace() != null && p.getBirthPlace().isBlank()) {
            p.setBirthPlace(null);
        }
        if (p.getHeight() != null && p.getHeight().isBlank()) {
            p.setHeight(null);
        }
        if (p.getWeight() != null && p.getWeight().isBlank()) {
            p.setWeight(null);
        }
        if (p.getThrowHand() != null && p.getThrowHand().isBlank()) {
            p.setThrowHand(null);
        }
        if (p.getBatHand() != null && p.getBatHand().isBlank()) {
            p.setBatHand(null);
        }
        if (p.getDraft() != null && p.getDraft().isBlank()) {
            p.setDraft(null);
        }
        if (p.getDebut() != null && p.getDebut().isBlank()) {
            p.setDebut(null);
        }
        if (p.getEducation() != null && p.getEducation().isBlank()) {
            p.setEducation(null);
        }
        if (p.getStatus() != null && p.getStatus().isBlank()) {
            p.setStatus(null);
        }
        if (p.getContactPhone() != null && p.getContactPhone().isBlank()) {
            p.setContactPhone(null);
        }
        if (p.getContactEmail() != null && p.getContactEmail().isBlank()) {
            p.setContactEmail(null);
        }
        if (p.getIntro() != null && p.getIntro().isBlank()) {
            p.setIntro(null);
        }
    }

    private static void normalizePlayerBackgroundFields(Player p) {
        if (p == null) {
            return;
        }
        if (p.getBgImages() != null && !p.getBgImages().isEmpty()) {
            ArrayList<String> normalized = new ArrayList<String>();
            HashSet<String> seen = new HashSet<String>();
            for (String u : p.getBgImages()) {
                String t;
                if (u == null || u.isBlank() || !seen.add(t = u.trim())) continue;
                normalized.add(t);
                if (normalized.size() < 5) continue;
                break;
            }
            if (normalized.isEmpty()) {
                p.setBgImages(null);
                p.setBgImage(null);
                return;
            }
            p.setBgImages(normalized);
            p.setBgImage((String)normalized.get(0));
            return;
        }
        if (p.getBgImage() != null && !p.getBgImage().isBlank()) {
            p.setBgImages(List.of((Object)p.getBgImage().trim()));
        }
    }

    @Transactional(rollbackFor={Exception.class})
    public void delete(Long id) {
        Player existing = this.playerRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return;
        }
        if (!Objects.equals(existing.getTenantId(), this.tenantQueryPolicyService.requiredTenantId())) {
            throw new BusinessException(403, "\u65e0\u6743\u5220\u9664\u8be5\u7403\u5458");
        }
        Long uid = CurrentUserHolder.get();
        LocalDateTime now = LocalDateTime.now();
        existing.setDeletedAt(now);
        existing.setDeletedBy(uid);
        this.playerRepository.save((Object)existing);
    }

    @Transactional(rollbackFor={Exception.class})
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List validIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (validIds.isEmpty()) {
            return;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Long uid = CurrentUserHolder.get();
        LocalDateTime now = LocalDateTime.now();
        List toSoftDelete = this.playerRepository.findAllById((Iterable)validIds).stream().filter(p -> Objects.equals(p.getTenantId(), tid)).filter(p -> p.getDeletedAt() == null).toList();
        for (Player p2 : toSoftDelete) {
            p2.setDeletedAt(now);
            p2.setDeletedBy(uid);
        }
        this.playerRepository.saveAll((Iterable)toSoftDelete);
    }

    private void validateTeamId(Long teamId) {
        if (teamId != null && teamId > 0L && !this.teamRepository.existsById((Object)teamId)) {
            throw new BusinessException(400, "\u7403\u961f\u4e0d\u5b58\u5728\uff0c\u8bf7\u5148\u521b\u5efa\u7403\u961f");
        }
    }

    private void applyTenantFromTeam(Player entity) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (entity.getTeamId() != null && entity.getTeamId() > 0L) {
            Team team = this.teamRepository.findById((Object)entity.getTeamId()).orElse(null);
            if (team == null) {
                throw new BusinessException(400, "\u7403\u961f\u4e0d\u5b58\u5728\uff0c\u8bf7\u5148\u521b\u5efa\u7403\u961f");
            }
            if (!Objects.equals(team.getTenantId(), tid)) {
                throw new BusinessException(400, "\u7403\u961f\u4e0e\u5f53\u524d\u79df\u6237\u4e0d\u4e00\u81f4");
            }
            entity.setTenantId(team.getTenantId());
        } else {
            entity.setTenantId(Long.valueOf(tid));
        }
    }

    @Transactional(rollbackFor={Exception.class})
    public Map<String, Object> batchImport(List<Player> items, String duplicateStrategy) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Set teamIds = items.stream().map(Player::getTeamId).filter(Objects::nonNull).filter(id -> id > 0L).collect(Collectors.toSet());
        HashSet<Object> invalidTeamIds = new HashSet<Object>();
        for (Object x : teamIds) {
            Team t = this.teamRepository.findById(x).orElse(null);
            if (t != null && Objects.equals(t.getTenantId(), tid)) continue;
            invalidTeamIds.add(x);
        }
        if (!invalidTeamIds.isEmpty()) {
            throw new BusinessException(400, "\u4ee5\u4e0b\u7403\u961f\u4e0d\u5b58\u5728\u6216\u4e0d\u5c5e\u4e8e\u5f53\u524d\u79df\u6237\uff1aID " + String.valueOf(invalidTeamIds));
        }
        HashMap<String, Player> existingByKey = new HashMap<String, Player>();
        for (Player p : this.playerRepository.findByDeletedAtIsNullAndTenantId(tid)) {
            existingByKey.put(this.dupKey(p), p);
        }
        boolean overwrite = "overwrite".equalsIgnoreCase(duplicateStrategy);
        int created = 0;
        int updated = 0;
        int skipped = 0;
        for (Player p : items) {
            if (p.getName() == null || p.getName().isBlank()) {
                ++skipped;
                continue;
            }
            this.applyTenantFromTeam(p);
            Player existing = (Player)existingByKey.get(this.dupKey(p));
            if (existing != null) {
                if (overwrite) {
                    p.setId(existing.getId());
                    p.setCreatedAt(existing.getCreatedAt());
                    this.playerRepository.save((Object)p);
                    ++updated;
                    continue;
                }
                ++skipped;
                continue;
            }
            this.playerRepository.save((Object)p);
            existingByKey.put(this.dupKey(p), p);
            ++created;
        }
        return Map.of((Object)"created", (Object)created, (Object)"updated", (Object)updated, (Object)"skipped", (Object)skipped);
    }

    private String dupKey(Player p) {
        return (p.getName() != null ? p.getName() : "") + "|" + (p.getNumber() != null ? p.getNumber() : "");
    }

    private Pageable buildPageable(Integer page, Integer pageSize, String sortProp, String sortOrder) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        if (sortProp != null && !sortProp.isEmpty()) {
            Sort.Direction dir = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)dir, (String[])new String[]{sortProp}));
        }
        return PageRequest.of((int)(p - 1), (int)ps);
    }

    @Generated
    public PlayerService(PlayerRepository playerRepository, TeamRepository teamRepository, StatsService statsService, DataScopeService dataScopeService, PersonnelHistoryRecorder personnelHistoryRecorder, TenantQueryPolicyService tenantQueryPolicyService) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.statsService = statsService;
        this.dataScopeService = dataScopeService;
        this.personnelHistoryRecorder = personnelHistoryRecorder;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

