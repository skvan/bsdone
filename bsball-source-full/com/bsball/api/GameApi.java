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
import com.bsball.model.dto.GameSaveLiveDTO;
import com.bsball.model.dto.LiveSnapshotSaveDTO;
import com.bsball.model.dto.SaveGameResultDTO;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.service.GamePlayerStatService;
import com.bsball.service.GameService;
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
        List eids = this.parseLongList(eventIds);
        List yrs = this.parseIntegerList(years);
        PageResult raw = this.gameService.list(page, pageSize, sortProp, sortOrder, eventId, eids, yrs, teamId);
        List items = raw.getList().stream().map(GameResponseDTO::from).collect(Collectors.toList());
        return Result.ok((Object)PageResult.of(items, (long)raw.getTotal()));
    }

    @GetMapping(value={"/{id}"})
    public Result<GameResponseDTO> get(@PathVariable Long id) {
        Game data = this.gameService.get(id);
        return Result.ok((Object)GameResponseDTO.from((Game)data));
    }

    @GetMapping(value={"/{id}/live-snapshot"})
    public Result<Map<String, String>> getLiveSnapshot(@PathVariable Long id) {
        String s = this.gameService.getLiveSnapshot(id);
        return Result.ok((Object)Map.of((Object)"snapshotJson", (Object)(s != null ? s : "")));
    }

    @PostMapping(value={"/{id}/live-snapshot"})
    public Result<Object> saveLiveSnapshot(@PathVariable Long id, @RequestBody LiveSnapshotSaveDTO body) {
        this.gameService.saveLiveSnapshot(id, body != null ? body.getSnapshotJson() : null);
        return Result.ok((Object)Map.of());
    }

    @GetMapping(value={"/{gameId}/stats"})
    public Result<PageResult<GamePlayerStat>> listByGame(@PathVariable Long gameId) {
        List data = this.gamePlayerStatService.listByGame(gameId);
        return Result.ok((Object)PageResult.of((List)data, (long)data.size()));
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody @Valid Game body) {
        Game created = this.gameService.create(body);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(@PathVariable Long id, @RequestBody @Valid Game body) {
        this.gameService.update(id, body);
        return Result.ok((Object)Map.of());
    }

    @PostMapping(value={"/{id}/save-live"})
    public Result<Object> saveLive(@PathVariable Long id, @RequestBody GameSaveLiveDTO body) {
        this.gameService.saveLive(id, body);
        return Result.ok((Object)Map.of());
    }

    @PostMapping(value={"/{id}/save-result"})
    public Result<Object> saveResult(@PathVariable Long id, @RequestBody SaveGameResultDTO body) {
        this.gameService.saveResult(id, body);
        return Result.ok((Object)Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        this.gameService.delete(id);
        return Result.ok((Object)Map.of());
    }

    @PostMapping(value={"/stats/create"})
    public Result<Map<String, Object>> createStat(@RequestBody GamePlayerStat body) {
        GamePlayerStat created = this.gamePlayerStatService.create(body);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId()));
    }

    @PutMapping(value={"/stats/update/{id}"})
    public Result<Object> updateStat(@PathVariable Long id, @RequestBody GamePlayerStat body) {
        this.gamePlayerStatService.update(id, body);
        return Result.ok((Object)Map.of());
    }

    @DeleteMapping(value={"/stats/delete/{id}"})
    public Result<Object> deleteStat(@PathVariable Long id) {
        this.gamePlayerStatService.delete(id);
        return Result.ok((Object)Map.of());
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

