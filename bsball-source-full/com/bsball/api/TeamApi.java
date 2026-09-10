/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.TeamApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.dto.TeamOptionDto
 *  com.bsball.model.entity.Team
 *  com.bsball.service.TeamService
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
import com.bsball.model.dto.TeamOptionDto;
import com.bsball.model.entity.Team;
import com.bsball.service.TeamService;
import jakarta.validation.Valid;
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
@RequestMapping(value={"/team"})
public class TeamApi {
    private final TeamService teamService;

    @GetMapping(value={"/select-options"})
    public Result<List<TeamOptionDto>> selectOptions() {
        return Result.ok((Object)this.teamService.listForSelect());
    }

    @GetMapping(value={"/list"})
    public Result<PageResult<Team>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder) {
        PageResult data = this.teamService.list(page, pageSize, sortProp, sortOrder);
        return Result.ok((Object)data);
    }

    @GetMapping(value={"/{id}"})
    public Result<Team> get(@PathVariable Long id) {
        Team data = this.teamService.get(id);
        return Result.ok((Object)data);
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody @Valid Team body) {
        Team created = this.teamService.create(body);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(@PathVariable Long id, @RequestBody @Valid Team body) {
        this.teamService.update(id, body);
        return Result.ok((Object)Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        this.teamService.delete(id);
        return Result.ok((Object)Map.of());
    }

    @Generated
    public TeamApi(TeamService teamService) {
        this.teamService = teamService;
    }
}

