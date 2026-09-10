/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.LineupTemplateListApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.service.TeamLineupTemplateService
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.PageResult;
import com.bsball.common.Result;
import com.bsball.service.TeamLineupTemplateService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/lineup-template"})
public class LineupTemplateListApi {
    private final TeamLineupTemplateService teamLineupTemplateService;

    @GetMapping(value={"/list"})
    public Result<PageResult<Map<String, Object>>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) Long teamId) {
        PageResult raw = this.teamLineupTemplateService.listPage(page, pageSize, teamId);
        List items = raw.getList().stream().map(t -> {
            LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", t.getId());
            m.put("teamId", t.getTeamId());
            m.put("name", t.getName());
            m.put("description", t.getDescription());
            m.put("updatedAt", t.getUpdatedAt());
            return m;
        }).collect(Collectors.toList());
        return Result.ok((Object)PageResult.of(items, (long)raw.getTotal()));
    }

    @Generated
    public LineupTemplateListApi(TeamLineupTemplateService teamLineupTemplateService) {
        this.teamLineupTemplateService = teamLineupTemplateService;
    }
}

