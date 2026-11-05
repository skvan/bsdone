/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.GameApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.dto.GameResponseDTO
 *  com.bsball.model.dto.GameSaveLiveDTO
 *  com.bsball.model.dto.LiveSnapshotSaveDTO
 *  com.bsball.model.dto.SaveGameResultDTO
 *  com.bsball.model.entity.Game
 *  com.bsball.model.entity.GamePlayerStat
 *  com.bsball.service.GamePlayerStatService
 *  com.bsball.service.GameService
 *  jakarta.validation.Valid
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.PageResult;
import com.bsball.common.Result;
import com.bsball.model.dto.GameResponseDTO;
import com.bsball.model.dto.EarnedRunDecisionAdjustRequest;
import com.bsball.model.dto.EarnedRunDecisionListDTO;
import com.bsball.model.dto.EarnedRunHalfInningRequest;
import com.bsball.model.dto.EarnedRunDecisionOverrideRequest;
import com.bsball.model.dto.FieldingAdjustmentCandidateDTO;
import com.bsball.model.dto.FieldingAdjustmentCreateRequest;
import com.bsball.model.dto.FieldingAdjustmentListDTO;
import com.bsball.model.dto.GameRevisionAuditDTO;
import com.bsball.model.dto.GameSaveLiveDTO;
import com.bsball.model.dto.LiveSnapshotSaveDTO;
import com.bsball.model.dto.PitcherRunOverrideRequest;
import com.bsball.model.dto.PitcherRunTotalsDTO;
import com.bsball.model.dto.PitcherTransferRequest;
import com.bsball.model.dto.PitcherTransferResultDTO;
import com.bsball.model.dto.SaveGameResultDTO;
import com.bsball.model.dto.StatBaseCompareDTO;
import com.bsball.model.dto.StatRowDeleteCheckDTO;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.GameSubstitutionEvent;
import com.bsball.model.entity.GameFieldingPosStat;
import com.bsball.model.entity.EarnedRunDecisionEntity;
import com.bsball.service.GamePlayerStatService;
import com.bsball.service.GameService;
import com.bsball.stats.earnedrun.EarnedRunReconstructionResult;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Generated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/game"})
public class GameApi {
    private final GameService gameService;
    private final GamePlayerStatService gamePlayerStatService;

    @GetMapping(value={"/list"})
    public Result<PageResult<GameResponseDTO>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder, @RequestParam(required=false) Long eventId, @RequestParam(required=false) String eventIds, @RequestParam(required=false) String years, @RequestParam(required=false) Long teamId) {
        List<Long> eids = this.parseLongList(eventIds);
        List<Integer> yrs = this.parseIntegerList(years);
        PageResult<Game> raw = this.gameService.list(page, pageSize, sortProp, sortOrder, eventId, eids, yrs, teamId);
        List<GameResponseDTO> items = raw.getList().stream().map(GameResponseDTO::from).collect(Collectors.toList());
        return Result.ok(PageResult.of(items, (long)raw.getTotal()));
    }

    @GetMapping(value={"/{id}"})
    public Result<GameResponseDTO> get(@PathVariable Long id) {
        Game data = this.gameService.get(id);
        GameResponseDTO dto = GameResponseDTO.from((Game)data);
        if (dto != null) {
            dto.setEngineManaged(Boolean.valueOf(this.gameService.isEngineManaged(data)));
        }
        return Result.ok(dto);
    }

    @GetMapping(value={"/{id}/live-snapshot"})
    public Result<Map<String, String>> getLiveSnapshot(@PathVariable Long id) {
        String s = this.gameService.getLiveSnapshot(id);
        return Result.ok(Map.of("snapshotJson", s != null ? s : ""));
    }

    @PostMapping(value={"/{id}/live-snapshot"})
    public Result<Object> saveLiveSnapshot(@PathVariable Long id, @RequestBody LiveSnapshotSaveDTO body) {
        this.gameService.saveLiveSnapshot(id, body != null ? body.getSnapshotJson() : null);
        return Result.ok(Map.of());
    }

    /** 阵容专用保存（门①，Issue #310）：受限赛事校验阵容球员 ⊆ 报名名单后落快照。 */
    @PostMapping(value={"/{id}/lineup-snapshot"})
    public Result<Object> saveLineupSnapshot(@PathVariable Long id, @RequestBody LiveSnapshotSaveDTO body) {
        this.gameService.saveLineupSnapshot(id, body != null ? body.getSnapshotJson() : null);
        return Result.ok(Map.of());
    }

    @GetMapping(value={"/{gameId}/stats"})
    public Result<PageResult<GamePlayerStat>> listByGame(@PathVariable Long gameId) {
        List<GamePlayerStat> data = this.gamePlayerStatService.listByGame(gameId);
        return Result.ok(PageResult.of(data, (long)data.size()));
    }

    @GetMapping(value={"/{gameId}/fielding-positions"})
    public Result<PageResult<GameFieldingPosStat>> listFieldingPositions(@PathVariable Long gameId) {
        List<GameFieldingPosStat> data = this.gameService.listFieldingPositions(gameId);
        return Result.ok(PageResult.of(data, (long)data.size()));
    }

    @GetMapping(value={"/{gameId}/substitutions"})
    public Result<PageResult<GameSubstitutionEvent>> listSubstitutions(@PathVariable Long gameId) {
        List<GameSubstitutionEvent> data = this.gameService.listSubstitutions(gameId);
        return Result.ok(PageResult.of(data, (long)data.size()));
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody @Valid Game body) {
        Game created = this.gameService.create(body);
        return Result.ok(Map.of("id",created.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(@PathVariable Long id, @RequestBody @Valid Game body) {
        this.gameService.update(id, body);
        return Result.ok(Map.of());
    }

    @PostMapping(value={"/{id}/save-live"})
    public Result<Object> saveLive(@PathVariable Long id, @RequestBody GameSaveLiveDTO body) {
        this.gameService.saveLive(id, body);
        return Result.ok(Map.of());
    }

    @PostMapping(value={"/{id}/earned-runs/reconstruct"})
    public Result<EarnedRunReconstructionResult> reconstructEarnedRuns(
            @PathVariable Long id,
            @RequestBody EarnedRunHalfInningRequest body) {
        return Result.ok(this.gameService.reconstructEarnedRuns(id, body));
    }


    @GetMapping(value={"/{id}/earned-runs/decisions"})
    public Result<List<EarnedRunDecisionListDTO>> listEarnedRunDecisions(@PathVariable Long id) {
        return Result.ok(this.gameService.listEarnedRunDecisions(id));
    }

    @PutMapping(value={"/{id}/earned-runs/decisions/{decisionId}"})
    public Result<EarnedRunDecisionEntity> overrideEarnedRunDecision(
            @PathVariable Long id,
            @PathVariable Long decisionId,
            @RequestBody EarnedRunDecisionOverrideRequest body) {
        return Result.ok(this.gameService.overrideEarnedRunDecision(id, decisionId, body));
    }

    @PostMapping(value={"/{id}/earned-runs/decisions/{decisionId}/reassign"})
    public Result<EarnedRunDecisionListDTO> reassignEarnedRunDecision(
            @PathVariable Long id,
            @PathVariable Long decisionId,
            @RequestBody EarnedRunDecisionAdjustRequest body) {
        return Result.ok(this.gameService.reassignEarnedRunDecision(id, decisionId, body));
    }

    @PostMapping(value={"/{id}/earned-runs/decisions/{decisionId}/void"})
    public Result<EarnedRunDecisionListDTO> voidEarnedRunDecision(
            @PathVariable Long id,
            @PathVariable Long decisionId,
            @RequestBody(required=false) EarnedRunDecisionAdjustRequest body) {
        return Result.ok(this.gameService.voidEarnedRunDecision(id, decisionId, body));
    }

    @PostMapping(value={"/{id}/earned-runs/decision-adjustments/{adjustmentId}/revoke"})
    public Result<EarnedRunDecisionListDTO> revokeEarnedRunDecisionAdjustment(
            @PathVariable Long id,
            @PathVariable Long adjustmentId) {
        return Result.ok(
                this.gameService.revokeEarnedRunDecisionAdjustment(id, adjustmentId));
    }

    @GetMapping(value={"/{id}/earned-runs/pitchers"})
    public Result<List<PitcherRunTotalsDTO>> listPitcherRunTotals(@PathVariable Long id) {
        return Result.ok(this.gameService.listPitcherRunTotals(id));
    }

    @PutMapping(value={"/{id}/earned-runs/pitchers/{playerId}/override"})
    public Result<PitcherRunTotalsDTO> overridePitcherRuns(
            @PathVariable Long id,
            @PathVariable Long playerId,
            @RequestBody PitcherRunOverrideRequest body) {
        return Result.ok(this.gameService.overridePitcherRuns(id, playerId, body));
    }

    @PostMapping(value={"/{id}/earned-runs/overrides/{overrideId}/revoke"})
    public Result<PitcherRunTotalsDTO> revokePitcherRunOverride(
            @PathVariable Long id, @PathVariable Long overrideId) {
        return Result.ok(this.gameService.revokePitcherRunOverride(id, overrideId));
    }

    @PostMapping(value={"/{id}/pitcher-transfers"})
    public Result<PitcherTransferResultDTO> transferPitcher(
            @PathVariable Long id, @RequestBody PitcherTransferRequest body) {
        return Result.ok(this.gameService.transferPitcher(id, body));
    }

    @PostMapping(value={"/{id}/pitcher-transfers/{transferId}/delete-source"})
    public Result<Object> deletePitcherTransferSource(
            @PathVariable Long id, @PathVariable Long transferId) {
        this.gameService.deletePitcherTransferSource(id, transferId);
        return Result.ok(Map.of());
    }

    @GetMapping(value={"/{id}/fielding-adjustments"})
    public Result<List<FieldingAdjustmentListDTO>> listFieldingAdjustments(@PathVariable Long id) {
        return Result.ok(this.gameService.listFieldingAdjustments(id));
    }

    @GetMapping(value={"/{id}/fielding-adjustments/candidates"})
    public Result<List<FieldingAdjustmentCandidateDTO>> listFieldingAdjustmentCandidates(
            @PathVariable Long id) {
        return Result.ok(this.gameService.listFieldingAdjustmentCandidates(id));
    }

    @GetMapping(value={"/{id}/stat-rows/{statId}/delete-check"})
    public Result<StatRowDeleteCheckDTO> checkStatRowDelete(
            @PathVariable Long id, @PathVariable Long statId) {
        return Result.ok(this.gameService.checkStatRowDelete(id, statId));
    }

    @GetMapping(value={"/{id}/stat-base/compare"})
    public Result<StatBaseCompareDTO> compareStatBase(@PathVariable Long id) {
        return Result.ok(this.gameService.compareStatBase(id));
    }

    @GetMapping(value={"/{id}/revision-audit"})
    public Result<List<GameRevisionAuditDTO>> listRevisionAudit(@PathVariable Long id) {
        return Result.ok(this.gameService.listRevisionAudit(id));
    }

    @PostMapping(value={"/{id}/fielding-adjustments"})
    public Result<FieldingAdjustmentListDTO> createFieldingAdjustment(
            @PathVariable Long id,
            @RequestBody FieldingAdjustmentCreateRequest body) {
        return Result.ok(this.gameService.createFieldingAdjustment(id, body));
    }

    @PostMapping(value={"/{id}/fielding-adjustments/{adjustmentId}/revoke"})
    public Result<FieldingAdjustmentListDTO> revokeFieldingAdjustment(
            @PathVariable Long id,
            @PathVariable Long adjustmentId) {
        return Result.ok(this.gameService.revokeFieldingAdjustment(id, adjustmentId));
    }

    @PostMapping(value={"/{id}/save-result"})
    public Result<Object> saveResult(@PathVariable Long id, @RequestBody SaveGameResultDTO body) {
        this.gameService.saveResult(id, body);
        return Result.ok(Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        this.gameService.delete(id);
        return Result.ok(Map.of());
    }

    @PostMapping(value={"/stats/create"})
    public Result<Map<String, Object>> createStat(@RequestBody GamePlayerStat body) {
        GamePlayerStat created = this.gamePlayerStatService.create(body);
        return Result.ok(Map.of("id",created.getId()));
    }

    @PutMapping(value={"/stats/update/{id}"})
    public Result<Object> updateStat(@PathVariable Long id, @RequestBody GamePlayerStat body) {
        this.gamePlayerStatService.update(id, body);
        return Result.ok(Map.of());
    }

    @DeleteMapping(value={"/stats/delete/{id}"})
    public Result<Object> deleteStat(@PathVariable Long id) {
        this.gamePlayerStatService.delete(id);
        return Result.ok(Map.of());
    }

    private List<Long> parseLongList(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).map(Long::parseLong).collect(Collectors.toList());
    }

    private List<Integer> parseIntegerList(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).map(Integer::parseInt).collect(Collectors.toList());
    }

    @Generated
    public GameApi(GameService gameService, GamePlayerStatService gamePlayerStatService) {
        this.gameService = gameService;
        this.gamePlayerStatService = gamePlayerStatService;
    }
}

