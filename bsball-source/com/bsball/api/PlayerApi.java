/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.PlayerApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.dto.PlayerGameLogEntryDTO
 *  com.bsball.model.dto.PlayerImportRequest
 *  com.bsball.model.dto.PlayerOptionDto
 *  com.bsball.model.dto.PlayerStatsByEventDTO
 *  com.bsball.model.dto.TeamPlayerOptionDto
 *  com.bsball.model.entity.Player
 *  com.bsball.service.PlayerService
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
import com.bsball.model.dto.PlayerGameLogEntryDTO;
import com.bsball.model.dto.PlayerImportRequest;
import com.bsball.model.dto.PlayerOptionDto;
import com.bsball.model.dto.PlayerStatsByEventDTO;
import com.bsball.model.dto.TeamPlayerOptionDto;
import com.bsball.model.entity.Player;
import com.bsball.service.PlayerService;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
@RequestMapping(value={"/player"})
public class PlayerApi {
    private final PlayerService playerService;

    @GetMapping(value={"/select-options"})
    public Result<List<PlayerOptionDto>> selectOptions() {
        return Result.ok((Object)this.playerService.listForSelect());
    }

    @GetMapping(value={"/list"})
    public Result<PageResult<Player>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder, @RequestParam(required=false) Long teamId, @RequestParam(required=false) String ids, @RequestParam(required=false) String keyword, @RequestParam(required=false) String number, @RequestParam(required=false) String position, @RequestParam(required=false) String throwHand, @RequestParam(required=false) String batHand, @RequestParam(required=false) String status, @RequestParam(required=false) String joinDateFrom, @RequestParam(required=false) String joinDateTo) {
        List idList = null;
        if (ids != null && !ids.isBlank()) {
            idList = Arrays.stream(ids.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(Long::parseLong).toList();
        }
        PageResult data = this.playerService.list(page, pageSize, sortProp, sortOrder, teamId, idList, keyword, number, position, throwHand, batHand, status, joinDateFrom, joinDateTo);
        return Result.ok((Object)data);
    }

    @GetMapping(value={"/check-full-name"})
    public Result<Map<String, Object>> checkFullName(@RequestParam String name, @RequestParam(required=false) Long excludeId) {
        boolean dup = this.playerService.isFullNameDuplicate(name, excludeId);
        return Result.ok((Object)Map.of((Object)"duplicate", (Object)dup));
    }

    @GetMapping(value={"/team-options"})
    public Result<List<TeamPlayerOptionDto>> teamOptions(@RequestParam long teamId) {
        return Result.ok((Object)this.playerService.listTeamPlayerOptions(teamId));
    }

    @GetMapping(value={"/{id}"})
    public Result<Player> get(@PathVariable Long id) {
        Player data = this.playerService.get(id);
        return Result.ok((Object)data);
    }

    @GetMapping(value={"/{id}/stats"})
    public Result<Map<String, Object>> getStats(@PathVariable Long id, @RequestParam(required=false) String gameMode) {
        Map data = this.playerService.getStats(id, gameMode);
        return Result.ok((Object)data);
    }

    @GetMapping(value={"/{id}/stats/by-season"})
    public Result<List<PlayerStatsByEventDTO>> getStatsBySeason(@PathVariable Long id, @RequestParam(required=false) String gameMode) {
        return Result.ok((Object)this.playerService.getStatsBySeason(id, gameMode));
    }

    @GetMapping(value={"/{id}/stats/game-log"})
    public Result<List<PlayerGameLogEntryDTO>> getGameLog(@PathVariable Long id, @RequestParam(required=false, defaultValue="30") Integer limit, @RequestParam(required=false) String gameMode) {
        return Result.ok((Object)this.playerService.getGameLog(id, limit == null || limit <= 0 ? 30 : limit, gameMode));
    }

    @GetMapping(value={"/{id}/stats/drill-down/batting"})
    public Result<PageResult<Map<String, Object>>> drillDownBatting(@PathVariable Long id, @RequestParam String metric, @RequestParam(required=false, defaultValue="1") Integer page, @RequestParam(required=false, defaultValue="20") Integer pageSize, @RequestParam(required=false) Long eventId, @RequestParam(required=false) String season, @RequestParam(required=false) String gameMode) {
        return Result.ok((Object)this.playerService.drillDownBatting(id, metric, page, pageSize, eventId, season, gameMode));
    }

    @GetMapping(value={"/{id}/stats/drill-down/pitching"})
    public Result<PageResult<Map<String, Object>>> drillDownPitching(@PathVariable Long id, @RequestParam String metric, @RequestParam(required=false, defaultValue="1") Integer page, @RequestParam(required=false, defaultValue="20") Integer pageSize, @RequestParam(required=false) Long eventId, @RequestParam(required=false) String season, @RequestParam(required=false) String gameMode) {
        return Result.ok((Object)this.playerService.drillDownPitching(id, metric, page, pageSize, eventId, season, gameMode));
    }

    @GetMapping(value={"/{id}/stats/drill-down/fielding"})
    public Result<PageResult<Map<String, Object>>> drillDownFielding(@PathVariable Long id, @RequestParam String metric, @RequestParam(required=false, defaultValue="1") Integer page, @RequestParam(required=false, defaultValue="20") Integer pageSize, @RequestParam(required=false) Long eventId, @RequestParam(required=false) String season, @RequestParam(required=false) String gameMode) {
        return Result.ok((Object)this.playerService.drillDownFielding(id, metric, page, pageSize, eventId, season, gameMode));
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody Player body) {
        Player created = this.playerService.create(body);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(@PathVariable Long id, @RequestBody @Valid Player body) {
        this.playerService.update(id, body);
        return Result.ok((Object)Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        this.playerService.delete(id);
        return Result.ok((Object)Map.of());
    }

    @PostMapping(value={"/delete-batch"})
    public Result<Object> deleteBatch(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body != null && body.get("ids") != null ? body.get("ids") : List.of();
        this.playerService.deleteBatch((List)ids);
        return Result.ok((Object)Map.of());
    }

    @PostMapping(value={"/import"})
    public Result<Map<String, Object>> importBatch(@RequestBody PlayerImportRequest body) {
        List items = body != null && body.items() != null ? body.items() : List.of();
        String duplicateStrategy = body != null ? body.duplicateStrategy() : "skip";
        Map result = this.playerService.batchImport(items, duplicateStrategy);
        return Result.ok((Object)result);
    }

    @Generated
    public PlayerApi(PlayerService playerService) {
        this.playerService = playerService;
    }
}

