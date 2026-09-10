/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.HitSprayApi
 *  com.bsball.common.Result
 *  com.bsball.model.dto.HitSprayDTO
 *  com.bsball.model.entity.HitSpray
 *  com.bsball.service.HitSprayService
 *  jakarta.validation.Valid
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.model.dto.HitSprayDTO;
import com.bsball.model.entity.HitSpray;
import com.bsball.service.HitSprayService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/hit-spray"})
public class HitSprayApi {
    private final HitSprayService hitSprayService;

    @PostMapping
    public Result<Void> save(@RequestBody @Valid HitSprayDTO dto) {
        this.hitSprayService.save(dto);
        return Result.ok(null);
    }

    @PostMapping(value={"/batch"})
    public Result<Void> saveBatch(@RequestBody List<HitSprayDTO> dtos) {
        this.hitSprayService.saveAll(dtos);
        return Result.ok(null);
    }

    @GetMapping(value={"/game/{gameId}"})
    public Result<List<HitSpray>> getByGame(@PathVariable Long gameId) {
        return Result.ok((Object)this.hitSprayService.getByGameId(gameId));
    }

    @GetMapping(value={"/player/{playerId}/game/{gameId}"})
    public Result<List<HitSpray>> getByPlayerAndGame(@PathVariable Long playerId, @PathVariable Long gameId) {
        return Result.ok((Object)this.hitSprayService.getByPlayerAndGame(playerId, gameId));
    }

    @GetMapping(value={"/team/{teamId}/game/{gameId}"})
    public Result<List<HitSpray>> getByTeamAndGame(@PathVariable Long teamId, @PathVariable Long gameId) {
        return Result.ok((Object)this.hitSprayService.getByTeamAndGame(teamId, gameId));
    }

    @Generated
    public HitSprayApi(HitSprayService hitSprayService) {
        this.hitSprayService = hitSprayService;
    }
}

