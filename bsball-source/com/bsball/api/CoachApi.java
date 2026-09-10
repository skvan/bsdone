/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.CoachApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.dto.CoachOptionDto
 *  com.bsball.model.entity.Coach
 *  com.bsball.service.CoachService
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
import com.bsball.model.dto.CoachOptionDto;
import com.bsball.model.entity.Coach;
import com.bsball.service.CoachService;
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
@RequestMapping(value={"/coach"})
public class CoachApi {
    private final CoachService coachService;

    @GetMapping(value={"/select-options"})
    public Result<List<CoachOptionDto>> selectOptions() {
        return Result.ok((Object)this.coachService.listForSelect());
    }

    @GetMapping(value={"/list"})
    public Result<PageResult<Coach>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword, @RequestParam(required=false) Long teamId) {
        PageResult data = this.coachService.list(page, pageSize, keyword, teamId);
        return Result.ok((Object)data);
    }

    @GetMapping(value={"/{id}"})
    public Result<Coach> get(@PathVariable Long id) {
        Coach data = this.coachService.get(id);
        return Result.ok((Object)data);
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody Coach body) {
        Coach created = this.coachService.create(body);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(@PathVariable Long id, @RequestBody Coach body) {
        this.coachService.update(id, body);
        return Result.ok((Object)Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        this.coachService.delete(id);
        return Result.ok((Object)Map.of());
    }

    @Generated
    public CoachApi(CoachService coachService) {
        this.coachService = coachService;
    }
}

