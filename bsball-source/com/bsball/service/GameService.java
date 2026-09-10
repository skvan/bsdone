/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.dto.EffectiveDataScope
 *  com.bsball.model.dto.GameSaveLiveDTO
 *  com.bsball.model.dto.GameSaveLiveDTO$GamePlayerStatPart
 *  com.bsball.model.dto.GameSaveLiveDTO$GameUpdatePart
 *  com.bsball.model.dto.SaveGameResultDTO
 *  com.bsball.model.dto.SaveGameResultDTO$GamePart
 *  com.bsball.model.dto.SaveGameResultDTO$StatPart
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.Event
 *  com.bsball.model.entity.Game
 *  com.bsball.model.entity.GamePlayerStat
 *  com.bsball.model.entity.Stadium
 *  com.bsball.repository.EventRepository
 *  com.bsball.repository.GamePlayerStatRepository
 *  com.bsball.repository.GameRepository
 *  com.bsball.repository.StadiumRepository
 *  com.bsball.service.DataScopeService
 *  com.bsball.service.GameService
 *  com.bsball.service.TenantQueryPolicyService
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  jakarta.persistence.criteria.Expression
 *  jakarta.persistence.criteria.Predicate
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
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveDataScope;
import com.bsball.model.dto.GameSaveLiveDTO;
import com.bsball.model.dto.SaveGameResultDTO;
import com.bsball.model.entity.BaseEntity;
import com.bsball.model.entity.Event;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.Stadium;
import com.bsball.repository.EventRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.StadiumRepository;
import com.bsball.service.DataScopeService;
import com.bsball.service.TenantQueryPolicyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
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

@Service
public class GameService {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final GameRepository gameRepository;
    private final GamePlayerStatRepository gamePlayerStatRepository;
    private final EventRepository eventRepository;
    private final StadiumRepository stadiumRepository;
    private final DataScopeService dataScopeService;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    public PageResult<Game> list(Integer page, Integer pageSize, String sortProp, String sortOrder, Long eventId, List<Long> eventIds, List<Integer> years, Long teamId) {
        boolean hasFilter;
        EffectiveDataScope scope;
        Long tid = this.tenantQueryPolicyService.tenantIdOrNullForQuery();
        long scopeTid = this.tenantQueryPolicyService.requiredTenantId();
        List allowedEventIds = this.resolveAllowedEventIds(scopeTid, scope = this.dataScopeService.resolve(CurrentUserHolder.get(), scopeTid));
        if (allowedEventIds != null && allowedEventIds.isEmpty()) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        Pageable p = this.buildPageable(page, pageSize, sortProp, sortOrder);
        boolean bl = hasFilter = eventId != null || eventIds != null && !eventIds.isEmpty() || years != null && !years.isEmpty();
        if (!hasFilter) {
            Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
                ArrayList<Predicate> preds = new ArrayList<Predicate>();
                preds.add(cb.isNull((Expression)root.get("deletedAt")));
                if (tid != null) {
                    preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
                }
                if (allowedEventIds != null) {
                    preds.add(root.get("eventId").in((Collection)allowedEventIds));
                }
                if (teamId != null) {
                    preds.add(cb.or((Expression)cb.equal((Expression)root.get("homeTeamId"), (Object)teamId), (Expression)cb.equal((Expression)root.get("awayTeamId"), (Object)teamId)));
                }
                return cb.and(preds.toArray(new Predicate[0]));
            };
            Page result = this.gameRepository.findAll((Specification)spec, p);
            return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
        }
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            preds.add(cb.isNull((Expression)root.get("deletedAt")));
            if (tid != null) {
                preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
            }
            if (allowedEventIds != null) {
                preds.add(root.get("eventId").in((Collection)allowedEventIds));
            }
            if (teamId != null) {
                preds.add(cb.or((Expression)cb.equal((Expression)root.get("homeTeamId"), (Object)teamId), (Expression)cb.equal((Expression)root.get("awayTeamId"), (Object)teamId)));
            }
            if (eventId != null) {
                preds.add(cb.equal((Expression)root.get("eventId"), (Object)eventId));
            }
            if (eventIds != null && !eventIds.isEmpty()) {
                preds.add(root.get("eventId").in((Collection)eventIds));
            }
            if (years != null && !years.isEmpty()) {
                Predicate[] yearPreds = (Predicate[])years.stream().map(y -> cb.like((Expression)root.get("gameday"), y + "%")).toArray(Predicate[]::new);
                preds.add(cb.or(yearPreds));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Page result = this.gameRepository.findAll((Specification)spec, p);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    private List<Long> resolveAllowedEventIds(long tid, EffectiveDataScope scope) {
        if (scope.isUnrestrictedInTenant()) {
            return null;
        }
        if (scope.getLeagueIds().isEmpty()) {
            return Collections.emptyList();
        }
        return this.eventRepository.findByTenantIdAndLeagueIdInAndDeletedAtIsNull(Long.valueOf(tid), (Collection)scope.getLeagueIds()).stream().map(BaseEntity::getId).toList();
    }

    public Game get(Long id) {
        Game g = this.gameRepository.findById((Object)id).orElse(null);
        if (g == null || g.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!this.tenantQueryPolicyService.isGlobalQueryMode() && !Objects.equals(g.getTenantId(), tid)) {
            return null;
        }
        if (g.getEventId() == null) {
            return null;
        }
        Event ev = this.eventRepository.findById((Object)g.getEventId()).orElse(null);
        if (ev == null || ev.getDeletedAt() != null || !this.tenantQueryPolicyService.isGlobalQueryMode() && !Objects.equals(ev.getTenantId(), tid)) {
            return null;
        }
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!scope.isUnrestrictedInTenant() && !scope.canReadLeague(ev.getLeagueId().longValue())) {
            throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u6bd4\u8d5b");
        }
        return g;
    }

    public String getLiveSnapshot(Long gameId) {
        Game g = this.get(gameId);
        return g == null ? null : g.getLiveSnapshotJson();
    }

    @Transactional
    public void saveLiveSnapshot(Long gameId, String json) {
        Game g = this.gameRepository.findById((Object)gameId).orElse(null);
        if (g == null) {
            return;
        }
        this.assertGameWritable(g);
        g.setLiveSnapshotJson(json);
        this.gameRepository.save((Object)g);
    }

    public Game create(Game entity) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Event ev = this.eventRepository.findById((Object)entity.getEventId()).orElse(null);
        if (ev == null || ev.getDeletedAt() != null || !Objects.equals(ev.getTenantId(), tid)) {
            throw new BusinessException(400, "\u8d5b\u4e8b\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u4f7f\u7528");
        }
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!scope.isUnrestrictedInTenant() && !scope.canReadLeague(ev.getLeagueId().longValue())) {
            throw new BusinessException(403, "\u65e0\u6743\u5728\u8be5\u8d5b\u4e8b\u4e0b\u521b\u5efa\u6bd4\u8d5b");
        }
        entity.setTenantId(Long.valueOf(tid));
        if (entity.getStadiumId() != null) {
            this.assertStadiumBelongsToTenant(entity.getStadiumId(), tid);
        }
        return (Game)this.gameRepository.save((Object)entity);
    }

    public Game update(Long id, Game entity) {
        Game existing = this.gameRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u6bd4\u8d5b");
        }
        long eid = entity.getEventId() != null ? entity.getEventId() : existing.getEventId();
        Event ev = this.eventRepository.findById((Object)eid).orElse(null);
        if (ev == null || !Objects.equals(ev.getTenantId(), tid)) {
            throw new BusinessException(400, "\u8d5b\u4e8b\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u4f7f\u7528");
        }
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!scope.isUnrestrictedInTenant()) {
            Event oldEv;
            if (!(existing.getEventId() == null || (oldEv = (Event)this.eventRepository.findById((Object)existing.getEventId()).orElse(null)) != null && scope.canReadLeague(oldEv.getLeagueId().longValue()))) {
                throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u6bd4\u8d5b");
            }
            if (!scope.canReadLeague(ev.getLeagueId().longValue())) {
                throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u6bd4\u8d5b");
            }
        }
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        entity.setTenantId(Long.valueOf(tid));
        if (entity.getStadiumId() != null) {
            this.assertStadiumBelongsToTenant(entity.getStadiumId(), tid);
        }
        return (Game)this.gameRepository.save((Object)entity);
    }

    private void assertStadiumBelongsToTenant(Long stadiumId, long tenantId) {
        Stadium s = this.stadiumRepository.findById((Object)stadiumId).orElse(null);
        if (s == null || s.getDeletedAt() != null || !Objects.equals(s.getTenantId(), tenantId)) {
            throw new BusinessException(400, "\u7403\u573a\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u4f7f\u7528");
        }
    }

    @Transactional(rollbackFor={Exception.class})
    public void delete(Long id) {
        Game existing = this.gameRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return;
        }
        this.assertGameWritable(existing);
        existing.setDeletedAt(LocalDateTime.now());
        existing.setDeletedBy(CurrentUserHolder.get());
        this.gameRepository.save((Object)existing);
    }

    @Transactional
    public void saveLive(Long gameId, GameSaveLiveDTO dto) {
        Game game = this.gameRepository.findById((Object)gameId).orElse(null);
        if (game == null) {
            return;
        }
        this.assertGameWritable(game);
        if (dto.getGame() != null) {
            GameSaveLiveDTO.GameUpdatePart g = dto.getGame();
            if (g.getHomeScore() != null) {
                game.setHomeScore(g.getHomeScore());
            }
            if (g.getAwayScore() != null) {
                game.setAwayScore(g.getAwayScore());
            }
            if (g.getHomeScoreByInning() != null) {
                game.setHomeScoreByInning((Object)this.toInningJson(g.getHomeScoreByInning()));
            }
            if (g.getAwayScoreByInning() != null) {
                game.setAwayScoreByInning((Object)this.toInningJson(g.getAwayScoreByInning()));
            }
            if (g.getHomeH() != null) {
                game.setHomeH(g.getHomeH());
            }
            if (g.getAwayH() != null) {
                game.setAwayH(g.getAwayH());
            }
            if (g.getHomeE() != null) {
                game.setHomeE(g.getHomeE());
            }
            if (g.getAwayE() != null) {
                game.setAwayE(g.getAwayE());
            }
            if (g.getStatus() != null) {
                game.setStatus(g.getStatus());
            }
            if (g.getGameEndTime() != null) {
                game.setGameEndTime(g.getGameEndTime());
            }
            if (g.getInning() != null) {
                game.setInning(g.getInning());
            }
            if (g.getTopBottom() != null) {
                game.setTopBottom(g.getTopBottom());
            }
            this.gameRepository.save((Object)game);
        }
        if (dto.getStats() != null && !dto.getStats().isEmpty()) {
            List existing = this.gamePlayerStatRepository.findByGameId(gameId);
            Map<String, GamePlayerStat> existingMap = existing.stream().collect(Collectors.toMap(s -> s.getTeamId() + "_" + s.getPlayerId(), s -> s));
            for (GameSaveLiveDTO.GamePlayerStatPart part : dto.getStats()) {
                if (part.getTeamId() == null || part.getPlayerId() == null) continue;
                String key = part.getTeamId() + "_" + part.getPlayerId();
                GamePlayerStat stat = existingMap.get(key);
                Long statTid = game.getTenantId();
                if (stat != null) {
                    this.applyPartToStat(stat, part, gameId, statTid);
                    this.gamePlayerStatRepository.save((Object)stat);
                    continue;
                }
                GamePlayerStat newStat = new GamePlayerStat();
                newStat.setGameId(gameId);
                this.applyPartToStat(newStat, part, gameId, statTid);
                this.gamePlayerStatRepository.save((Object)newStat);
            }
        }
    }

    @Transactional(rollbackFor={Exception.class})
    public Long importGameResult(Long eventId, SaveGameResultDTO dto) {
        if (dto == null || dto.getGame() == null || dto.getStats() == null) {
            throw new IllegalArgumentException("\u5bfc\u5165\u6570\u636e\u7f3a\u5c11 game \u6216 stats");
        }
        if (dto.getGame() != null && !Boolean.TRUE.equals(dto.getGame().getIsSpecialResult())) {
            this.assertStarterFieldingPositions(dto.getStats(), "\u5bfc\u5165\u6bd4\u8d5b");
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Event ev = this.eventRepository.findById((Object)eventId).orElse(null);
        if (ev == null || !Objects.equals(ev.getTenantId(), tid)) {
            throw new BusinessException(400, "\u8d5b\u4e8b\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u4f7f\u7528");
        }
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!scope.isUnrestrictedInTenant() && !scope.canReadLeague(ev.getLeagueId().longValue())) {
            throw new BusinessException(403, "\u65e0\u6743\u5728\u8be5\u8d5b\u4e8b\u4e0b\u5bfc\u5165\u6bd4\u8d5b");
        }
        SaveGameResultDTO.GamePart g = dto.getGame();
        Game game = new Game();
        game.setEventId(eventId);
        game.setTenantId(Long.valueOf(tid));
        game.setHomeTeamId(g.getHomeTeamId());
        game.setAwayTeamId(g.getAwayTeamId());
        game.setGameTime(g.getGameTime());
        game.setGameEndTime(g.getGameEndTime());
        game.setGameday(g.getGameday());
        game.setGameNumber(g.getGameNumber());
        if (g.getStadiumId() != null) {
            this.assertStadiumBelongsToTenant(g.getStadiumId(), tid);
        }
        game.setStadiumId(g.getStadiumId());
        game.setVenue(g.getVenue());
        game.setStatus(g.getStatus() != null ? g.getStatus() : "final");
        game.setTotalInnings(g.getTotalInnings());
        game.setHomeScore(Integer.valueOf(g.getHomeScore() != null ? g.getHomeScore() : 0));
        game.setAwayScore(Integer.valueOf(g.getAwayScore() != null ? g.getAwayScore() : 0));
        game.setHomeScoreByInning((Object)this.toInningJson(g.getHomeScoreByInning()));
        game.setAwayScoreByInning((Object)this.toInningJson(g.getAwayScoreByInning()));
        game.setHomeH(g.getHomeH());
        game.setAwayH(g.getAwayH());
        game.setHomeE(g.getHomeE());
        game.setAwayE(g.getAwayE());
        game.setSpectatorCount(g.getSpectatorCount());
        game.setUmpireHp(g.getUmpireHp());
        game.setUmpire1b(g.getUmpire1b());
        game.setUmpire2b(g.getUmpire2b());
        game.setUmpire3b(g.getUmpire3b());
        game.setRecorders(g.getRecorders());
        if (g.getGameTag() != null) {
            game.setGameTag(g.getGameTag());
        }
        if (g.getWeatherSummary() != null) {
            game.setWeatherSummary(g.getWeatherSummary());
        }
        if (g.getWeatherTempC() != null) {
            game.setWeatherTempC(g.getWeatherTempC());
        }
        if (g.getWeatherWind() != null) {
            game.setWeatherWind(g.getWeatherWind());
        }
        if (g.getWeatherRainProbPct() != null) {
            game.setWeatherRainProbPct(g.getWeatherRainProbPct());
        }
        if (g.getRemark() != null) {
            game.setRemark(g.getRemark());
        }
        game = (Game)this.gameRepository.save((Object)game);
        Long gameId = game.getId();
        for (SaveGameResultDTO.StatPart part : dto.getStats()) {
            if (part.getTeamId() == null || part.getPlayerId() == null) continue;
            GamePlayerStat stat = new GamePlayerStat();
            this.applyResultPartToStat(stat, part, gameId, Long.valueOf(tid));
            this.gamePlayerStatRepository.save((Object)stat);
        }
        return gameId;
    }

    @Transactional
    public void saveResult(Long gameId, SaveGameResultDTO dto) {
        Game game = this.gameRepository.findById((Object)gameId).orElse(null);
        if (game == null) {
            return;
        }
        if (dto != null && dto.getStats() != null && !Boolean.TRUE.equals(game.getIsSpecialResult())) {
            this.assertStarterFieldingPositions(dto.getStats(), "\u4fdd\u5b58\u6bd4\u8d5b");
        }
        if (dto.getGame() != null) {
            SaveGameResultDTO.GamePart g = dto.getGame();
            if (g.getEventId() != null) {
                game.setEventId(g.getEventId());
            }
            if (g.getHomeTeamId() != null) {
                game.setHomeTeamId(g.getHomeTeamId());
            }
            if (g.getAwayTeamId() != null) {
                game.setAwayTeamId(g.getAwayTeamId());
            }
            if (g.getGameTime() != null) {
                game.setGameTime(g.getGameTime());
            }
            if (g.getGameEndTime() != null) {
                game.setGameEndTime(g.getGameEndTime());
            }
            if (g.getGameday() != null) {
                game.setGameday(g.getGameday());
            }
            if (g.getGameNumber() != null) {
                game.setGameNumber(g.getGameNumber());
            }
            if (g.getVenue() != null) {
                game.setVenue(g.getVenue());
            }
            if (g.getStadiumId() != null) {
                this.assertStadiumBelongsToTenant(g.getStadiumId(), game.getTenantId().longValue());
            }
            game.setStadiumId(g.getStadiumId());
            if (g.getStatus() != null) {
                game.setStatus(g.getStatus());
            }
            if (g.getGameMode() != null) {
                game.setGameMode(g.getGameMode());
            }
            if (g.getTotalInnings() != null) {
                game.setTotalInnings(g.getTotalInnings());
            }
            if (g.getHomeScore() != null) {
                game.setHomeScore(g.getHomeScore());
            }
            if (g.getAwayScore() != null) {
                game.setAwayScore(g.getAwayScore());
            }
            if (g.getHomeScoreByInning() != null) {
                game.setHomeScoreByInning((Object)this.toInningJson(g.getHomeScoreByInning()));
            }
            if (g.getAwayScoreByInning() != null) {
                game.setAwayScoreByInning((Object)this.toInningJson(g.getAwayScoreByInning()));
            }
            if (g.getHomeH() != null) {
                game.setHomeH(g.getHomeH());
            }
            if (g.getAwayH() != null) {
                game.setAwayH(g.getAwayH());
            }
            if (g.getHomeE() != null) {
                game.setHomeE(g.getHomeE());
            }
            if (g.getAwayE() != null) {
                game.setAwayE(g.getAwayE());
            }
            game.setSpectatorCount(g.getSpectatorCount());
            game.setUmpireHp(g.getUmpireHp());
            game.setUmpire1b(g.getUmpire1b());
            game.setUmpire2b(g.getUmpire2b());
            game.setUmpire3b(g.getUmpire3b());
            game.setRecorders(g.getRecorders());
            if (g.getGameTag() != null) {
                game.setGameTag(g.getGameTag());
            }
            if (g.getWeatherSummary() != null) {
                game.setWeatherSummary(g.getWeatherSummary());
            }
            if (g.getWeatherTempC() != null) {
                game.setWeatherTempC(g.getWeatherTempC());
            }
            if (g.getWeatherWind() != null) {
                game.setWeatherWind(g.getWeatherWind());
            }
            if (g.getWeatherRainProbPct() != null) {
                game.setWeatherRainProbPct(g.getWeatherRainProbPct());
            }
            if (g.getRemark() != null) {
                game.setRemark(g.getRemark());
            }
            game.setIsSpecialResult(g.getIsSpecialResult());
            game.setShowRemarkInCard(g.getShowRemarkInCard());
            game.setIncludeStatsInRanking(g.getIncludeStatsInRanking());
            Event evSync = this.eventRepository.findById((Object)game.getEventId()).orElse(null);
            if (evSync != null) {
                game.setTenantId(evSync.getTenantId());
            }
            this.gameRepository.save((Object)game);
        }
        this.assertGameWritable(game);
        Long statTenantId = game.getTenantId();
        if (dto.getStats() != null) {
            List existing = this.gamePlayerStatRepository.findByGameId(gameId);
            Set keepIds = dto.getStats().stream().map(SaveGameResultDTO.StatPart::getId).filter(Objects::nonNull).collect(Collectors.toSet());
            for (GamePlayerStat s : existing) {
                if (keepIds.contains(s.getId())) continue;
                this.gamePlayerStatRepository.delete((Object)s);
            }
            for (SaveGameResultDTO.StatPart part : dto.getStats()) {
                GamePlayerStat stat;
                if (part.getTeamId() == null || part.getPlayerId() == null) continue;
                if (part.getId() != null && this.gamePlayerStatRepository.existsById((Object)part.getId())) {
                    stat = this.gamePlayerStatRepository.findById((Object)part.getId()).orElse(null);
                    if (stat == null) {
                        continue;
                    }
                } else {
                    stat = new GamePlayerStat();
                }
                this.applyResultPartToStat(stat, part, gameId, statTenantId);
                this.gamePlayerStatRepository.save((Object)stat);
            }
        }
    }

    private void applyResultPartToStat(GamePlayerStat stat, SaveGameResultDTO.StatPart part, Long gameId, Long tenantId) {
        stat.setTenantId(tenantId);
        stat.setGameId(gameId);
        stat.setTeamId(part.getTeamId());
        stat.setPlayerId(part.getPlayerId());
        stat.setBattingOrder(part.getBattingOrder());
        stat.setListOrder(part.getListOrder());
        stat.setPosition(part.getPosition());
        stat.setNumber(part.getNumber());
        stat.setBatHand(part.getBatHand());
        stat.setThrowHand(part.getThrowHand());
        stat.setPa(Integer.valueOf(part.getPa() != null ? part.getPa() : 0));
        stat.setAb(Integer.valueOf(part.getAb() != null ? part.getAb() : 0));
        stat.setR(Integer.valueOf(part.getR() != null ? part.getR() : 0));
        stat.setH(Integer.valueOf(part.getH() != null ? part.getH() : 0));
        stat.setE(Integer.valueOf(part.getE() != null ? part.getE() : 0));
        stat.setBbHp(Integer.valueOf(part.getBbHp() != null ? part.getBbHp() : 0));
        stat.setSb(Integer.valueOf(part.getSb() != null ? part.getSb() : 0));
        stat.setSo(Integer.valueOf(part.getSo() != null ? part.getSo() : 0));
        stat.setSoSwing(Integer.valueOf(part.getSoSwing() != null ? part.getSoSwing() : 0));
        stat.setSoLooking(Integer.valueOf(part.getSoLooking() != null ? part.getSoLooking() : 0));
        stat.setRbi(Integer.valueOf(part.getRbi() != null ? part.getRbi() : 0));
        stat.setHr(Integer.valueOf(part.getHr() != null ? part.getHr() : 0));
        stat.setInsideParkHr(part.getInsideParkHr());
        stat.setDoubles(part.getDoubles());
        stat.setTriples(part.getTriples());
        stat.setGdp(part.getGdp());
        stat.setSh(part.getSh());
        stat.setSf(part.getSf());
        stat.setBb(part.getBb());
        stat.setIbb(part.getIbb());
        stat.setHbp(part.getHbp());
        stat.setCs(part.getCs());
        stat.setIsPitcher(Integer.valueOf(part.getIsPitcher() != null ? part.getIsPitcher() : 0));
        stat.setPitcherOrder(part.getPitcherOrder());
        stat.setIp(part.getIp());
        stat.setEr(part.getEr());
        stat.setPitchH(part.getPitchH());
        stat.setPitchBbHp(part.getPitchBbHp());
        stat.setPitchSo(part.getPitchSo());
        stat.setPitchHr(part.getPitchHr());
        stat.setPitchInsideParkHr(Integer.valueOf(part.getPitchInsideParkHr() != null ? part.getPitchInsideParkHr() : 0));
        stat.setPitchR(part.getPitchR());
        stat.setPitchPa(part.getPitchPa());
        stat.setPitchBf(part.getPitchBf());
        stat.setPitchAb(part.getPitchAb());
        stat.setNp(part.getNp());
        stat.setPitchBb(part.getPitchBb());
        stat.setPitchIbb(part.getPitchIbb());
        stat.setPitchHbp(part.getPitchHbp());
        stat.setWp(part.getWp());
        stat.setBk(part.getBk());
        stat.setGo(part.getGo());
        stat.setFo(part.getFo());
        stat.setGs(part.getGs());
        stat.setSvo(part.getSvo());
        stat.setCg(part.getCg());
        stat.setPg(part.getPg());
        stat.setW(part.getW());
        stat.setL(part.getL());
        stat.setSv(part.getSv());
        stat.setHld(part.getHld());
        stat.setPo(part.getPo());
        stat.setA(part.getA());
        stat.setTc(part.getTc());
        stat.setFieldingGs(part.getFieldingGs());
        stat.setDefInn(part.getDefInn());
        stat.setDp(part.getDp());
        stat.setPb(Integer.valueOf(part.getPb() != null ? part.getPb() : 0));
        stat.setCatcherSb(Integer.valueOf(part.getCatcherSb() != null ? part.getCatcherSb() : 0));
        stat.setCatcherCs(Integer.valueOf(part.getCatcherCs() != null ? part.getCatcherCs() : 0));
    }

    private void assertStarterFieldingPositions(List<SaveGameResultDTO.StatPart> stats, String sceneLabel) {
        if (stats == null || stats.isEmpty()) {
            return;
        }
        Map<Long, List<SaveGameResultDTO.StatPart>> battersByTeam = stats.stream().filter(Objects::nonNull).filter(s -> s.getTeamId() != null).filter(s -> !Boolean.TRUE.equals(s.getIsPitcher() != null && s.getIsPitcher() == 1)).collect(Collectors.groupingBy(SaveGameResultDTO.StatPart::getTeamId));
        for (Map.Entry<Long, List<SaveGameResultDTO.StatPart>> entry : battersByTeam.entrySet()) {
            Long teamId = entry.getKey();
            List<SaveGameResultDTO.StatPart> rows = entry.getValue();
            List battingOrderRows = rows.stream().filter(s -> s.getBattingOrder() != null && s.getBattingOrder() > 0).sorted((a, b) -> Integer.compare(a.getBattingOrder() != null ? a.getBattingOrder() : Integer.MAX_VALUE, b.getBattingOrder() != null ? b.getBattingOrder() : Integer.MAX_VALUE)).toList();
            if (battingOrderRows.size() < 9) {
                throw new BusinessException(400, sceneLabel + "\u5931\u8d25\uff1a\u7403\u961f(" + teamId + ")\u6709\u6253\u5e8f\u7684\u5148\u53d1\u4e0d\u8db3 9 \u4eba\uff0c\u65e0\u6cd5\u6821\u9a8c\u5b88\u5907\u4f4d\u7f6e");
            }
            List starters = battingOrderRows.stream().limit(9L).toList();
            List positions = starters.stream().map(s -> s.getPosition() == null ? "" : s.getPosition().trim().toUpperCase()).filter(p -> !p.isBlank()).filter(p -> !"PH".equals(p)).toList();
            LinkedHashSet unique = new LinkedHashSet(positions);
            if (unique.size() == positions.size()) continue;
            throw new BusinessException(400, sceneLabel + "\u5931\u8d25\uff1a\u7403\u961f(" + teamId + ")\u6709\u6253\u5e8f\u7684\u5148\u53d1\u5b88\u5907\u4f4d\u7f6e\u91cd\u590d");
        }
    }

    private String toInningJson(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String)obj;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        }
        catch (JsonProcessingException e) {
            return null;
        }
    }

    private void applyPartToStat(GamePlayerStat stat, GameSaveLiveDTO.GamePlayerStatPart part, Long gameId, Long tenantId) {
        stat.setTenantId(tenantId);
        stat.setGameId(gameId);
        stat.setTeamId(part.getTeamId());
        stat.setPlayerId(part.getPlayerId());
        stat.setBattingOrder(part.getBattingOrder());
        stat.setListOrder(part.getListOrder());
        stat.setPosition(part.getPosition());
        stat.setPa(part.getPa());
        stat.setAb(part.getAb());
        stat.setR(part.getR());
        stat.setH(part.getH());
        stat.setBbHp(part.getBbHp());
        stat.setSb(part.getSb());
        stat.setSo(part.getSo());
        stat.setSoSwing(part.getSoSwing());
        stat.setSoLooking(part.getSoLooking());
        stat.setE(part.getE());
        stat.setRbi(part.getRbi());
        stat.setDoubles(part.getDoubles());
        stat.setTriples(part.getTriples());
        stat.setHr(part.getHr());
        stat.setInsideParkHr(part.getInsideParkHr());
        stat.setSh(part.getSh());
        stat.setSf(part.getSf());
        stat.setBb(part.getBb());
        stat.setHbp(part.getHbp());
        stat.setCs(part.getCs());
        stat.setIsPitcher(Integer.valueOf(part.getIsPitcher() != null ? part.getIsPitcher() : 0));
        stat.setPitcherOrder(part.getPitcherOrder());
        stat.setIp(part.getIp());
        stat.setEr(part.getEr());
        stat.setPitchH(part.getPitchH());
        stat.setPitchBbHp(part.getPitchBbHp());
        stat.setPitchSo(part.getPitchSo());
        stat.setPitchHr(part.getPitchHr());
        stat.setPitchInsideParkHr(part.getPitchInsideParkHr());
        stat.setNp(part.getNp());
        stat.setWp(part.getWp());
        stat.setBk(part.getBk());
        stat.setPo(part.getPo());
        stat.setA(part.getA());
        stat.setTc(part.getTc());
    }

    private void assertGameWritable(Game game) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(game.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u64cd\u4f5c\u8be5\u6bd4\u8d5b");
        }
        if (game.getDeletedAt() != null) {
            throw new BusinessException(400, "\u6bd4\u8d5b\u5df2\u5220\u9664");
        }
        if (game.getEventId() == null) {
            throw new BusinessException(400, "\u6bd4\u8d5b\u672a\u5173\u8054\u8d5b\u4e8b");
        }
        Event ev = this.eventRepository.findById((Object)game.getEventId()).orElse(null);
        if (ev == null || ev.getDeletedAt() != null || !Objects.equals(ev.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u64cd\u4f5c\u8be5\u6bd4\u8d5b");
        }
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!scope.isUnrestrictedInTenant() && !scope.canReadLeague(ev.getLeagueId().longValue())) {
            throw new BusinessException(403, "\u65e0\u6743\u64cd\u4f5c\u8be5\u6bd4\u8d5b");
        }
    }

    private Pageable buildPageable(Integer page, Integer pageSize, String sortProp, String sortOrder) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        if (sortProp != null && !sortProp.isEmpty()) {
            Sort.Direction dir = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)dir, (String[])new String[]{sortProp}));
        }
        return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"gameday", "gameTime", "id"}));
    }

    @Generated
    public GameService(GameRepository gameRepository, GamePlayerStatRepository gamePlayerStatRepository, EventRepository eventRepository, StadiumRepository stadiumRepository, DataScopeService dataScopeService, TenantQueryPolicyService tenantQueryPolicyService) {
        this.gameRepository = gameRepository;
        this.gamePlayerStatRepository = gamePlayerStatRepository;
        this.eventRepository = eventRepository;
        this.stadiumRepository = stadiumRepository;
        this.dataScopeService = dataScopeService;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

