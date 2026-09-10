/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.TeamLineupTemplateApi
 *  com.bsball.common.Result
 *  com.bsball.model.dto.TeamLineupTemplateCopyFromGameDto
 *  com.bsball.model.dto.TeamLineupTemplateJsonPayload
 *  com.bsball.model.dto.TeamLineupTemplateSaveDto
 *  com.bsball.model.entity.TeamLineupTemplate
 *  com.bsball.service.TeamLineupTemplateService
 *  jakarta.validation.Valid
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.model.dto.TeamLineupTemplateCopyFromGameDto;
import com.bsball.model.dto.TeamLineupTemplateJsonPayload;
import com.bsball.model.dto.TeamLineupTemplateSaveDto;
import com.bsball.model.entity.TeamLineupTemplate;
import com.bsball.service.TeamLineupTemplateService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/team/{teamId}/lineup-template"})
public class TeamLineupTemplateApi {
    private final TeamLineupTemplateService teamLineupTemplateService;

    @GetMapping(value={"/list"})
    public Result<List<Map<String, Object>>> list(@PathVariable Long teamId) {
        List list = this.teamLineupTemplateService.list(teamId.longValue());
        List out = list.stream().map(t -> {
            LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", t.getId());
            m.put("teamId", t.getTeamId());
            m.put("name", t.getName());
            m.put("description", t.getDescription());
            m.put("updatedAt", t.getUpdatedAt());
            return m;
        }).collect(Collectors.toList());
        return Result.ok(out);
    }

    @GetMapping(value={"/{id}"})
    public Result<Map<String, Object>> detail(@PathVariable Long teamId, @PathVariable Long id) {
        TeamLineupTemplate t = this.teamLineupTemplateService.get(teamId.longValue(), id.longValue());
        TeamLineupTemplateJsonPayload payload = this.teamLineupTemplateService.readPayload(t);
        LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", t.getId());
        m.put("teamId", t.getTeamId());
        m.put("name", t.getName());
        m.put("description", t.getDescription());
        m.put("slots", payload.getSlots());
        m.put("benchPlayerIds", payload.getBenchPlayerIds() != null ? payload.getBenchPlayerIds() : List.of());
        m.put("startingPitcherPlayerId", payload.getStartingPitcherPlayerId());
        m.put("updatedAt", t.getUpdatedAt());
        return Result.ok(m);
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@PathVariable Long teamId, @RequestBody @Valid TeamLineupTemplateSaveDto body) {
        TeamLineupTemplate t = this.teamLineupTemplateService.create(teamId.longValue(), body);
        return Result.ok((Object)Map.of((Object)"id", (Object)t.getId()));
    }

    @PostMapping(value={"/copy-from-game"})
    public Result<Map<String, Object>> copyFromGame(@PathVariable Long teamId, @RequestBody @Valid TeamLineupTemplateCopyFromGameDto body) {
        TeamLineupTemplate t = this.teamLineupTemplateService.copyFromGame(teamId.longValue(), body);
        return Result.ok((Object)Map.of((Object)"id", (Object)t.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(@PathVariable Long teamId, @PathVariable Long id, @RequestBody @Valid TeamLineupTemplateSaveDto body) {
        this.teamLineupTemplateService.update(teamId.longValue(), id.longValue(), body);
        return Result.ok((Object)Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long teamId, @PathVariable Long id) {
        this.teamLineupTemplateService.delete(teamId.longValue(), id.longValue());
        return Result.ok((Object)Map.of());
    }

    @Generated
    public TeamLineupTemplateApi(TeamLineupTemplateService teamLineupTemplateService) {
        this.teamLineupTemplateService = teamLineupTemplateService;
    }
}

