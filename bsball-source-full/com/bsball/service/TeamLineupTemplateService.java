/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.dto.EffectiveDataScope
 *  com.bsball.model.dto.TeamLineupTemplateCopyFromGameDto
 *  com.bsball.model.dto.TeamLineupTemplateJsonPayload
 *  com.bsball.model.dto.TeamLineupTemplateSaveDto
 *  com.bsball.model.dto.TeamLineupTemplateSlotDto
 *  com.bsball.model.entity.Game
 *  com.bsball.model.entity.GamePlayerStat
 *  com.bsball.model.entity.Team
 *  com.bsball.model.entity.TeamLineupTemplate
 *  com.bsball.repository.GamePlayerStatRepository
 *  com.bsball.repository.GameRepository
 *  com.bsball.repository.TeamLineupTemplateRepository
 *  com.bsball.service.DataScopeService
 *  com.bsball.service.TeamLineupTemplateService
 *  com.bsball.service.TeamLineupTemplateService$ExtractedLineupFromGame
 *  com.bsball.service.TeamService
 *  com.bsball.service.TenantQueryPolicyService
 *  com.fasterxml.jackson.core.type.TypeReference
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
import com.bsball.model.dto.TeamLineupTemplateCopyFromGameDto;
import com.bsball.model.dto.TeamLineupTemplateJsonPayload;
import com.bsball.model.dto.TeamLineupTemplateSaveDto;
import com.bsball.model.dto.TeamLineupTemplateSlotDto;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.Team;
import com.bsball.model.entity.TeamLineupTemplate;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.TeamLineupTemplateRepository;
import com.bsball.service.DataScopeService;
import com.bsball.service.TeamLineupTemplateService;
import com.bsball.service.TeamService;
import com.bsball.service.TenantQueryPolicyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
public class TeamLineupTemplateService {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> REQUIRED_STARTER_POSITIONS = Set.of((Object)"C", (Object)"1B", (Object)"2B", (Object)"3B", (Object)"SS", (Object)"LF", (Object)"CF", (Object)"RF");
    private static final Set<String> ALLOWED_STARTER_POSITIONS;
    private final TeamLineupTemplateRepository templateRepository;
    private final TeamService teamService;
    private final GameRepository gameRepository;
    private final GamePlayerStatRepository gamePlayerStatRepository;
    private final DataScopeService dataScopeService;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    private Team assertTeam(long teamId) {
        Team t = this.teamService.get(Long.valueOf(teamId));
        if (t == null) {
            throw new BusinessException(404, "\u7403\u961f\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u8bbf\u95ee");
        }
        return t;
    }

    public List<TeamLineupTemplate> list(long teamId) {
        this.assertTeam(teamId);
        return this.templateRepository.findByTeamIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long.valueOf(teamId));
    }

    public PageResult<TeamLineupTemplate> listPage(Integer page, Integer pageSize, Long teamId) {
        Long tid = this.tenantQueryPolicyService.tenantIdOrNullForQuery();
        long scopeTid = this.tenantQueryPolicyService.requiredTenantId();
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), scopeTid);
        if (!scope.isUnrestrictedInTenant() && scope.getTeamIds().isEmpty()) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        if (teamId != null) {
            this.assertTeam(teamId.longValue());
        }
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        PageRequest pageable = PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"updatedAt"}));
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            preds.add(cb.isNull((Expression)root.get("deletedAt")));
            if (tid != null) {
                preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
            }
            if (teamId != null) {
                preds.add(cb.equal((Expression)root.get("teamId"), (Object)teamId));
            } else if (!scope.isUnrestrictedInTenant()) {
                preds.add(root.get("teamId").in((Collection)scope.getTeamIds()));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Page result = this.templateRepository.findAll((Specification)spec, (Pageable)pageable);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public TeamLineupTemplate get(long teamId, long id) {
        this.assertTeam(teamId);
        return (TeamLineupTemplate)this.templateRepository.findByIdAndTeamIdAndDeletedAtIsNull(Long.valueOf(id), Long.valueOf(teamId)).orElseThrow(() -> new BusinessException(404, "\u6a21\u677f\u4e0d\u5b58\u5728"));
    }

    @Transactional(rollbackFor={Exception.class})
    public TeamLineupTemplate create(long teamId, TeamLineupTemplateSaveDto dto) {
        Team team = this.assertTeam(teamId);
        List slots = this.normalizeAndValidateSlots(dto.getSlots());
        List bench = TeamLineupTemplateService.normalizeBenchPlayerIds((List)dto.getBenchPlayerIds());
        this.validateBenchVersusStarters(slots, bench);
        Long sp = this.normalizeStartingPitcherPlayerId(dto.getStartingPitcherPlayerId(), slots);
        return this.persistNew(team.getTenantId().longValue(), teamId, dto.getName(), dto.getDescription(), slots, bench, sp);
    }

    @Transactional(rollbackFor={Exception.class})
    public TeamLineupTemplate update(long teamId, long id, TeamLineupTemplateSaveDto dto) {
        this.assertTeam(teamId);
        TeamLineupTemplate existing = (TeamLineupTemplate)this.templateRepository.findByIdAndTeamIdAndDeletedAtIsNull(Long.valueOf(id), Long.valueOf(teamId)).orElseThrow(() -> new BusinessException(404, "\u6a21\u677f\u4e0d\u5b58\u5728"));
        List slots = this.normalizeAndValidateSlots(dto.getSlots());
        List bench = TeamLineupTemplateService.normalizeBenchPlayerIds((List)dto.getBenchPlayerIds());
        this.validateBenchVersusStarters(slots, bench);
        Long sp = this.normalizeStartingPitcherPlayerId(dto.getStartingPitcherPlayerId(), slots);
        existing.setName(dto.getName().trim());
        existing.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        existing.setSlotsJson(TeamLineupTemplateService.writePayload((List)slots, (List)bench, (Long)sp));
        return (TeamLineupTemplate)this.templateRepository.save((Object)existing);
    }

    @Transactional(rollbackFor={Exception.class})
    public void delete(long teamId, long id) {
        this.assertTeam(teamId);
        TeamLineupTemplate existing = (TeamLineupTemplate)this.templateRepository.findByIdAndTeamIdAndDeletedAtIsNull(Long.valueOf(id), Long.valueOf(teamId)).orElseThrow(() -> new BusinessException(404, "\u6a21\u677f\u4e0d\u5b58\u5728"));
        existing.setDeletedAt(LocalDateTime.now());
        existing.setDeletedBy(CurrentUserHolder.get());
        this.templateRepository.save((Object)existing);
    }

    @Transactional(rollbackFor={Exception.class})
    public TeamLineupTemplate copyFromGame(long teamId, TeamLineupTemplateCopyFromGameDto dto) {
        String side;
        Team team = this.assertTeam(teamId);
        String string = side = dto.getTeamSide() == null ? "" : dto.getTeamSide().trim().toLowerCase(Locale.ROOT);
        if (!"home".equals(side) && !"away".equals(side)) {
            throw new BusinessException(400, "teamSide \u987b\u4e3a home \u6216 away");
        }
        Game game = this.gameRepository.findById((Object)dto.getGameId()).orElse(null);
        if (game == null || game.getDeletedAt() != null) {
            throw new BusinessException(404, "\u6bd4\u8d5b\u4e0d\u5b58\u5728");
        }
        if (!Objects.equals(game.getTenantId(), team.getTenantId())) {
            throw new BusinessException(400, "\u6bd4\u8d5b\u4e0e\u7403\u961f\u4e0d\u5c5e\u4e8e\u540c\u4e00\u79df\u6237");
        }
        long expectTeam = "home".equals(side) ? game.getHomeTeamId() : game.getAwayTeamId();
        if (expectTeam != teamId) {
            throw new BusinessException(400, "\u8be5\u573a\u6bd4\u8d5b\u7684" + ("home".equals(side) ? "\u4e3b\u961f" : "\u5ba2\u961f") + "\u4e0e\u5f53\u524d\u7403\u961f\u4e0d\u4e00\u81f4");
        }
        List stats = this.gamePlayerStatRepository.findByGameId(game.getId());
        ExtractedLineupFromGame extracted = this.extractSlotsFromGameStats(teamId, stats);
        String name = dto.getName() != null && !dto.getName().isBlank() ? dto.getName().trim() : "\u6765\u81ea\u6bd4\u8d5b#" + game.getId();
        String desc = dto.getDescription() != null ? dto.getDescription().trim() : null;
        return this.persistNew(team.getTenantId().longValue(), teamId, name, desc, extracted.slots(), List.of(), extracted.startingPitcherPlayerId());
    }

    public List<TeamLineupTemplateSlotDto> readSlots(TeamLineupTemplate entity) {
        return this.readPayload(entity).getSlots();
    }

    public TeamLineupTemplateJsonPayload readPayload(TeamLineupTemplate entity) {
        return this.parseSlotsJson(entity.getSlotsJson());
    }

    public List<Long> readBenchPlayerIds(TeamLineupTemplate entity) {
        List b = this.readPayload(entity).getBenchPlayerIds();
        return b == null ? List.of() : b;
    }

    private TeamLineupTemplate persistNew(long tenantId, long teamId, String name, String description, List<TeamLineupTemplateSlotDto> slots, List<Long> benchPlayerIds, Long startingPitcherPlayerId) {
        TeamLineupTemplate t = new TeamLineupTemplate();
        t.setTenantId(Long.valueOf(tenantId));
        t.setTeamId(Long.valueOf(teamId));
        t.setName(name.trim());
        t.setDescription(description != null && !description.isBlank() ? description : null);
        t.setSlotsJson(TeamLineupTemplateService.writePayload(slots, benchPlayerIds, (Long)startingPitcherPlayerId));
        return (TeamLineupTemplate)this.templateRepository.save((Object)t);
    }

    private TeamLineupTemplateJsonPayload parseSlotsJson(String json) {
        if (json == null || json.isBlank()) {
            throw new BusinessException(500, "\u6a21\u677f\u6570\u636e\u635f\u574f");
        }
        String trim = json.trim();
        try {
            if (trim.startsWith("[")) {
                List slots = (List)MAPPER.readValue(trim, (TypeReference)new /* Unavailable Anonymous Inner Class!! */);
                TeamLineupTemplateJsonPayload p = new TeamLineupTemplateJsonPayload();
                p.setSlots(slots != null ? slots : new ArrayList());
                p.setBenchPlayerIds(new ArrayList());
                return p;
            }
            TeamLineupTemplateJsonPayload p = (TeamLineupTemplateJsonPayload)MAPPER.readValue(trim, TeamLineupTemplateJsonPayload.class);
            if (p.getSlots() == null) {
                p.setSlots(new ArrayList());
            }
            if (p.getBenchPlayerIds() == null) {
                p.setBenchPlayerIds(new ArrayList());
            }
            return p;
        }
        catch (Exception e) {
            throw new BusinessException(500, "\u6a21\u677f\u6570\u636e\u635f\u574f");
        }
    }

    private static String writePayload(List<TeamLineupTemplateSlotDto> slots, List<Long> benchPlayerIds, Long startingPitcherPlayerId) {
        try {
            TeamLineupTemplateJsonPayload p = new TeamLineupTemplateJsonPayload();
            p.setSlots(slots);
            p.setBenchPlayerIds((List)(benchPlayerIds != null ? benchPlayerIds : new ArrayList()));
            p.setStartingPitcherPlayerId(startingPitcherPlayerId);
            return MAPPER.writeValueAsString((Object)p);
        }
        catch (Exception e) {
            throw new BusinessException(500, "\u5e8f\u5217\u5316\u6a21\u677f\u5931\u8d25");
        }
    }

    private static List<Long> normalizeBenchPlayerIds(List<Long> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        ArrayList<Long> out = new ArrayList<Long>();
        HashSet<Long> seen = new HashSet<Long>();
        for (Long id : raw) {
            if (id == null || !seen.add(id)) continue;
            out.add(id);
        }
        return out;
    }

    private void validateBenchVersusStarters(List<TeamLineupTemplateSlotDto> slots, List<Long> bench) {
        if (bench.isEmpty()) {
            return;
        }
        if (bench.size() > 60) {
            throw new BusinessException(400, "\u66ff\u8865\u540d\u5355\u6700\u591a 60 \u4eba");
        }
        Set starters = slots.stream().map(TeamLineupTemplateSlotDto::getPlayerId).filter(Objects::nonNull).collect(Collectors.toSet());
        for (Long bid : bench) {
            if (!starters.contains(bid)) continue;
            throw new BusinessException(400, "\u66ff\u8865\u7403\u5458\u4e0d\u80fd\u4e0e\u5148\u53d1\u91cd\u590d");
        }
    }

    private Long normalizeStartingPitcherPlayerId(Long raw, List<TeamLineupTemplateSlotDto> slots) {
        if (raw == null || slots == null || slots.isEmpty()) {
            return null;
        }
        List firstNine = slots.stream().filter(s -> s.getBattingOrder() != null && s.getBattingOrder() >= 1 && s.getBattingOrder() <= 9).sorted(Comparator.comparing(TeamLineupTemplateSlotDto::getBattingOrder)).toList();
        if (firstNine.size() != 9) {
            return null;
        }
        return raw;
    }

    private ExtractedLineupFromGame extractSlotsFromGameStats(long teamId, List<GamePlayerStat> all) {
        TeamLineupTemplateSlotDto d;
        List teamRows = all.stream().filter(Objects::nonNull).filter(s -> Objects.equals(s.getTeamId(), teamId)).toList();
        Map byOrder = TeamLineupTemplateService.pickStarterBattingOrderStats((List)teamRows);
        if (byOrder.size() != 9) {
            throw new BusinessException(400, "\u8be5\u573a\u6bd4\u8d5b\u8bb0\u5f55\u4e2d\u65e0\u6cd5\u89e3\u6790\u51fa\u5b8c\u6574\u7684 9 \u4eba\u5148\u53d1\u6253\u5e8f\uff08\u6253\u5e8f 1\u20139 \u5404\u987b\u5bf9\u5e94\u4e00\u540d\u7403\u5458\uff09");
        }
        ArrayList<TeamLineupTemplateSlotDto> slots = new ArrayList<TeamLineupTemplateSlotDto>();
        for (int bo = 1; bo <= 9; ++bo) {
            String pos;
            GamePlayerStat s2 = (GamePlayerStat)byOrder.get(bo);
            if (s2 == null || s2.getPlayerId() == null) {
                throw new BusinessException(400, "\u5148\u53d1\u6253\u5e8f " + bo + " \u7f3a\u5c11\u7403\u5458");
            }
            String string = pos = s2.getPosition() == null ? "" : s2.getPosition().trim().toUpperCase(Locale.ROOT);
            if (pos.isEmpty() || "PH".equals(pos)) {
                throw new BusinessException(400, "\u5148\u53d1\u6253\u5e8f " + bo + " \u7f3a\u5c11\u6709\u6548\u5b88\u5907\u4f4d\u7f6e");
            }
            d = new TeamLineupTemplateSlotDto();
            d.setPlayerId(s2.getPlayerId());
            d.setBattingOrder(Integer.valueOf(bo));
            d.setPosition(pos);
            d.setNumber(s2.getNumber());
            d.setBatHand(s2.getBatHand());
            d.setFieldingGs(Integer.valueOf(1));
            slots.add(d);
        }
        Long startingPitcherPlayerId = TeamLineupTemplateService.resolveStartingPitcherPlayerId((List)teamRows);
        Optional tenthDh = TeamLineupTemplateService.findTenthDesignatedHitter((List)teamRows);
        this.normalizeExtractedSlotsForTemplate(slots, teamRows, startingPitcherPlayerId, tenthDh);
        if (tenthDh.isPresent()) {
            GamePlayerStat dh = (GamePlayerStat)tenthDh.get();
            d = new TeamLineupTemplateSlotDto();
            d.setPlayerId(dh.getPlayerId());
            d.setBattingOrder(Integer.valueOf(10));
            d.setPosition("DH");
            d.setNumber(dh.getNumber());
            d.setBatHand(dh.getBatHand());
            d.setFieldingGs(Integer.valueOf(1));
            slots.add(d);
        }
        this.validateSlotsCore(slots);
        Long spForPayload = null;
        if (slots.size() == 9 && TeamLineupTemplateService.hasPositionInSlots(slots, (String)"DH") && !TeamLineupTemplateService.hasPositionInSlots(slots, (String)"P") && startingPitcherPlayerId != null) {
            spForPayload = startingPitcherPlayerId;
        }
        return new ExtractedLineupFromGame(slots, spForPayload);
    }

    private static Map<Integer, GamePlayerStat> pickStarterBattingOrderStats(List<GamePlayerStat> teamRows) {
        LinkedHashMap<Integer, GamePlayerStat> byOrder = new LinkedHashMap<Integer, GamePlayerStat>();
        List inRange = teamRows.stream().filter(s -> s.getBattingOrder() != null && s.getBattingOrder() >= 1 && s.getBattingOrder() <= 9).sorted(Comparator.comparing(GamePlayerStat::getBattingOrder).thenComparing(s -> s.getIsPitcher() != null && s.getIsPitcher() == 1 ? 1 : 0).thenComparing(s -> s.getListOrder() != null ? s.getListOrder() : 0)).toList();
        for (GamePlayerStat s2 : inRange) {
            boolean incomingPitcher;
            int bo = s2.getBattingOrder();
            GamePlayerStat existing = (GamePlayerStat)byOrder.get(bo);
            if (existing == null) {
                byOrder.put(bo, s2);
                continue;
            }
            boolean existingPitcher = existing.getIsPitcher() != null && existing.getIsPitcher() == 1;
            boolean bl = incomingPitcher = s2.getIsPitcher() != null && s2.getIsPitcher() == 1;
            if (!existingPitcher || incomingPitcher) continue;
            byOrder.put(bo, s2);
        }
        return byOrder;
    }

    private static Optional<GamePlayerStat> findTenthDesignatedHitter(List<GamePlayerStat> teamRows) {
        return teamRows.stream().filter(s -> s.getBattingOrder() != null && s.getBattingOrder() == 10).filter(s -> s.getIsPitcher() == null || s.getIsPitcher() == 0).filter(s -> s.getPlayerId() != null).filter(s -> "DH".equals(TeamLineupTemplateService.normalizePosRaw((String)s.getPosition()))).min(Comparator.comparing(s -> s.getListOrder() != null ? s.getListOrder() : 0));
    }

    private static Long resolveStartingPitcherPlayerId(List<GamePlayerStat> teamRows) {
        List pitchers = teamRows.stream().filter(s -> s.getIsPitcher() != null && s.getIsPitcher() == 1).filter(s -> s.getPlayerId() != null).toList();
        if (pitchers.isEmpty()) {
            return null;
        }
        Comparator<GamePlayerStat> pitcherPickOrder = Comparator.comparing(s -> s.getPitcherOrder() != null ? s.getPitcherOrder() : Integer.MAX_VALUE).thenComparing(s -> s.getListOrder() != null ? s.getListOrder() : 0);
        Optional<GamePlayerStat> gs = pitchers.stream().filter(s -> s.getGs() != null && s.getGs() == 1).min(pitcherPickOrder);
        if (gs.isPresent()) {
            return gs.get().getPlayerId();
        }
        return pitchers.stream().min(pitcherPickOrder).map(GamePlayerStat::getPlayerId).orElse(null);
    }

    private void normalizeExtractedSlotsForTemplate(List<TeamLineupTemplateSlotDto> slots, List<GamePlayerStat> teamRows, Long startingPitcherPlayerId, Optional<GamePlayerStat> tenthDh) {
        if (tenthDh.isPresent()) {
            return;
        }
        if (TeamLineupTemplateService.hasPositionInSlots(slots, (String)"P") || TeamLineupTemplateService.hasPositionInSlots(slots, (String)"DH")) {
            return;
        }
        if (startingPitcherPlayerId != null) {
            for (TeamLineupTemplateSlotDto slot : slots) {
                if (!Objects.equals(slot.getPlayerId(), startingPitcherPlayerId)) continue;
                slot.setPosition("P");
                return;
            }
        }
        for (GamePlayerStat row : teamRows) {
            int bo;
            TeamLineupTemplateSlotDto slot;
            if (row.getBattingOrder() == null || row.getBattingOrder() < 1 || row.getBattingOrder() > 9 || row.getIsPitcher() == null || row.getIsPitcher() == 0 || row.getPlayerId() == null || !Objects.equals((slot = slots.get((bo = row.getBattingOrder().intValue()) - 1)).getPlayerId(), row.getPlayerId())) continue;
            slot.setPosition("P");
            return;
        }
    }

    private static boolean hasPositionInSlots(List<TeamLineupTemplateSlotDto> slots, String pos) {
        return slots.stream().anyMatch(s -> pos.equals(TeamLineupTemplateService.normalizePos((TeamLineupTemplateSlotDto)s)));
    }

    private static String normalizePosRaw(String position) {
        if (position == null) {
            return "";
        }
        return position.trim().toUpperCase(Locale.ROOT);
    }

    private List<TeamLineupTemplateSlotDto> normalizeAndValidateSlots(List<TeamLineupTemplateSlotDto> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new BusinessException(400, "\u8bf7\u5148\u914d\u7f6e\u5148\u53d1\u6253\u5e8f");
        }
        if (raw.size() != 9 && raw.size() != 10) {
            throw new BusinessException(400, "\u5148\u53d1\u6a21\u677f\u987b\u4e3a 9 \u4eba\uff0c\u6216 9 \u4eba\u5148\u53d1\u53e6\u52a0 1 \u540d\u6307\u5b9a\u6253\u51fb\uff08\u5171 10 \u6761\uff09");
        }
        ArrayList<TeamLineupTemplateSlotDto> slots = new ArrayList<TeamLineupTemplateSlotDto>();
        for (TeamLineupTemplateSlotDto s : raw) {
            if (s == null || s.getPlayerId() == null || s.getBattingOrder() == null) {
                throw new BusinessException(400, "\u6bcf\u6761\u6a21\u677f\u987b\u5305\u542b playerId \u4e0e battingOrder");
            }
            TeamLineupTemplateSlotDto d = new TeamLineupTemplateSlotDto();
            d.setPlayerId(s.getPlayerId());
            d.setBattingOrder(s.getBattingOrder());
            String pos = s.getPosition() == null ? "" : s.getPosition().trim().toUpperCase(Locale.ROOT);
            d.setPosition(pos);
            d.setNumber(s.getNumber());
            d.setBatHand(s.getBatHand());
            d.setFieldingGs(Integer.valueOf(1));
            slots.add(d);
        }
        this.validateSlotsCore(slots);
        return slots;
    }

    private static String normalizePos(TeamLineupTemplateSlotDto s) {
        if (s.getPosition() == null) {
            return "";
        }
        return s.getPosition().trim().toUpperCase(Locale.ROOT);
    }

    private void validateSlotsCore(List<TeamLineupTemplateSlotDto> slots) {
        TreeMap<Integer, TeamLineupTemplateSlotDto> byBo = new TreeMap<Integer, TeamLineupTemplateSlotDto>();
        for (TeamLineupTemplateSlotDto s : slots) {
            Integer bo = s.getBattingOrder();
            if (bo == null) {
                throw new BusinessException(400, "\u6bcf\u6761\u6a21\u677f\u987b\u5305\u542b battingOrder");
            }
            if (byBo.put(bo, s) == null) continue;
            throw new BusinessException(400, "\u6253\u5e8f\u91cd\u590d\uff1a" + bo);
        }
        if (slots.size() == 9) {
            if (!byBo.keySet().equals(Set.of((Object)1, (Object)2, (Object)3, (Object)4, (Object)5, (Object)6, (Object)7, (Object)8, (Object)9))) {
                throw new BusinessException(400, "\u6253\u5e8f\u987b\u4e3a 1\u20139 \u5404\u51fa\u73b0\u4e00\u6b21");
            }
            List ordered = IntStream.rangeClosed(1, 9).mapToObj(byBo::get).toList();
            this.validateClassicNineOrdered(ordered);
            return;
        }
        if (!byBo.keySet().equals(Set.of((Object)1, (Object)2, (Object)3, (Object)4, (Object)5, (Object)6, (Object)7, (Object)8, (Object)9, (Object)10))) {
            throw new BusinessException(400, "\u5171 10 \u6761\u65f6\u6253\u5e8f\u987b\u4e3a 1\u201310 \u5404\u51fa\u73b0\u4e00\u6b21");
        }
        List firstNine = IntStream.rangeClosed(1, 9).mapToObj(byBo::get).toList();
        this.validateNineWithPitcherOnlyNoDh(firstNine);
        TeamLineupTemplateSlotDto tenth = (TeamLineupTemplateSlotDto)byBo.get(10);
        if (!"DH".equals(TeamLineupTemplateService.normalizePos((TeamLineupTemplateSlotDto)tenth))) {
            throw new BusinessException(400, "\u7b2c 10 \u6761\u987b\u4e3a\u6307\u5b9a\u6253\u51fb\uff08DH\uff09");
        }
        HashSet<Long> pids = new HashSet<Long>();
        for (int i = 1; i <= 10; ++i) {
            if (pids.add(((TeamLineupTemplateSlotDto)byBo.get(i)).getPlayerId())) continue;
            throw new BusinessException(400, "\u540c\u4e00\u7403\u5458\u4e0d\u80fd\u51fa\u73b0\u591a\u6b21");
        }
    }

    private void validateClassicNineOrdered(List<TeamLineupTemplateSlotDto> ordered) {
        this.validateNineCommonFielding(ordered);
        Map<String, Long> posCount = ordered.stream().collect(Collectors.groupingBy(TeamLineupTemplateService::normalizePos, Collectors.counting()));
        boolean hasP = posCount.containsKey("P");
        boolean hasDh = posCount.containsKey("DH");
        if (hasP == hasDh) {
            throw new BusinessException(400, "9 \u4eba\u5148\u53d1\u987b\u4e8c\u9009\u4e00\uff1a\u8981\u4e48\u542b\u6295\u624b\u4f4d\u7f6e P\uff08\u56fd\u8054\u5e38\u89c1\uff09\uff0c\u8981\u4e48\u542b\u6307\u5b9a\u6253\u51fb DH\uff08\u7f8e\u8054\u5e38\u89c1\uff09\uff0c\u4e0d\u53ef\u540c\u65f6\u6709\u4e24\u8005\u6216\u4e24\u8005\u90fd\u6ca1\u6709");
        }
    }

    private void validateNineWithPitcherOnlyNoDh(List<TeamLineupTemplateSlotDto> firstNine) {
        this.validateNineCommonFielding(firstNine);
        Map<String, Long> posCount = firstNine.stream().collect(Collectors.groupingBy(TeamLineupTemplateService::normalizePos, Collectors.counting()));
        if (!posCount.containsKey("P")) {
            throw new BusinessException(400, "\u4f7f\u7528\u7b2c 10 \u6761\u6307\u5b9a\u6253\u51fb\u65f6\uff0c\u6253\u5e8f 1\u20139 \u4e2d\u987b\u5305\u542b\u6295\u624b\uff08P\uff09");
        }
        if (posCount.containsKey("DH")) {
            throw new BusinessException(400, "\u4f7f\u7528\u7b2c 10 \u6761\u6307\u5b9a\u6253\u51fb\u65f6\uff0c\u6253\u5e8f 1\u20139 \u4e2d\u4e0d\u53ef\u518d\u8bbe DH");
        }
    }

    private void validateNineCommonFielding(List<TeamLineupTemplateSlotDto> ordered) {
        if (ordered.size() != 9) {
            throw new BusinessException(500, "\u5185\u90e8\u9519\u8bef\uff1a\u5148\u53d1\u6821\u9a8c\u6761\u76ee\u6570\u4e0d\u4e3a 9");
        }
        HashSet<Long> pids = new HashSet<Long>();
        for (TeamLineupTemplateSlotDto s : ordered) {
            if (!pids.add(s.getPlayerId())) {
                throw new BusinessException(400, "\u540c\u4e00\u7403\u5458\u4e0d\u80fd\u51fa\u73b0\u591a\u6b21");
            }
            String pos = TeamLineupTemplateService.normalizePos((TeamLineupTemplateSlotDto)s);
            if (!pos.isEmpty() && ALLOWED_STARTER_POSITIONS.contains(pos)) continue;
            throw new BusinessException(400, "\u5b58\u5728\u65e0\u6548\u5b88\u5907\u4f4d\u7f6e\uff1a" + pos);
        }
        Map<String, Long> posCount = ordered.stream().collect(Collectors.groupingBy(TeamLineupTemplateService::normalizePos, Collectors.counting()));
        for (Map.Entry<String, Long> e : posCount.entrySet()) {
            if (e.getValue() <= 1L) continue;
            throw new BusinessException(400, "\u5b88\u5907\u4f4d\u7f6e\u91cd\u590d\uff1a" + e.getKey());
        }
        for (String req : REQUIRED_STARTER_POSITIONS) {
            if (posCount.containsKey(req)) continue;
            throw new BusinessException(400, "\u7f3a\u5c11\u5b88\u5907\u4f4d\u7f6e\uff1a" + req);
        }
    }

    @Generated
    public TeamLineupTemplateService(TeamLineupTemplateRepository templateRepository, TeamService teamService, GameRepository gameRepository, GamePlayerStatRepository gamePlayerStatRepository, DataScopeService dataScopeService, TenantQueryPolicyService tenantQueryPolicyService) {
        this.templateRepository = templateRepository;
        this.teamService = teamService;
        this.gameRepository = gameRepository;
        this.gamePlayerStatRepository = gamePlayerStatRepository;
        this.dataScopeService = dataScopeService;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }

    static {
        HashSet<String> s = new HashSet<String>(REQUIRED_STARTER_POSITIONS);
        s.add("P");
        s.add("DH");
        ALLOWED_STARTER_POSITIONS = Collections.unmodifiableSet(s);
    }
}

