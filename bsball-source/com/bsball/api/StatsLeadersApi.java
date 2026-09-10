/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.StatsLeadersApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.service.StatsService
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.PageResult;
import com.bsball.common.Result;
import com.bsball.service.StatsService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/stats"})
public class StatsLeadersApi {
    private final StatsService statsService;

    @GetMapping(value={"/leaders/batting"})
    public Result<PageResult<Map<String, Object>>> leadersBatting(@RequestParam(required=false) Long eventId, @RequestParam(required=false) String eventIds, @RequestParam(required=false) String years, @RequestParam(required=false) String teamIds, @RequestParam(required=false) Long teamId, @RequestParam(required=false) String playerName, @RequestParam(required=false) String position, @RequestParam(required=false) String homeAway, @RequestParam(required=false) String batterHand, @RequestParam(required=false) String pitcherHand, @RequestParam(required=false) String gameMode, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder) {
        List tids;
        List yrs;
        List eids = this.parseLongList(eventIds);
        PageResult pr = this.statsService.getBattingLeaders(eventId, eids, yrs = this.parseStringList(years), tids = this.resolveTeamIds(teamIds, teamId), playerName, position, homeAway, batterHand, pitcherHand, gameMode, page, pageSize, sortProp, sortOrder);
        return pr.getTotal() == 0L ? Result.okEmpty((Object)pr) : Result.ok((Object)pr);
    }

    @GetMapping(value={"/leaders/pitching"})
    public Result<PageResult<Map<String, Object>>> leadersPitching(@RequestParam(required=false) Long eventId, @RequestParam(required=false) String eventIds, @RequestParam(required=false) String years, @RequestParam(required=false) String teamIds, @RequestParam(required=false) Long teamId, @RequestParam(required=false) String playerName, @RequestParam(required=false) String position, @RequestParam(required=false) String homeAway, @RequestParam(required=false) String batterHand, @RequestParam(required=false) String pitcherHand, @RequestParam(required=false) String gameMode, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder) {
        List tids;
        List yrs;
        List eids = this.parseLongList(eventIds);
        PageResult pr = this.statsService.getPitchingLeaders(eventId, eids, yrs = this.parseStringList(years), tids = this.resolveTeamIds(teamIds, teamId), playerName, position, homeAway, batterHand, pitcherHand, gameMode, page, pageSize, sortProp, sortOrder);
        return pr.getTotal() == 0L ? Result.okEmpty((Object)pr) : Result.ok((Object)pr);
    }

    @GetMapping(value={"/leaders/fielding"})
    public Result<PageResult<Map<String, Object>>> leadersFielding(@RequestParam(required=false) Long eventId, @RequestParam(required=false) String eventIds, @RequestParam(required=false) String years, @RequestParam(required=false) String teamIds, @RequestParam(required=false) Long teamId, @RequestParam(required=false) String playerName, @RequestParam(required=false) String position, @RequestParam(required=false) String homeAway, @RequestParam(required=false) String batterHand, @RequestParam(required=false) String pitcherHand, @RequestParam(required=false) String gameMode, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder) {
        List tids;
        List yrs;
        List eids = this.parseLongList(eventIds);
        PageResult pr = this.statsService.getFieldingLeaders(eventId, eids, yrs = this.parseStringList(years), tids = this.resolveTeamIds(teamIds, teamId), playerName, position, homeAway, batterHand, pitcherHand, gameMode, page, pageSize, sortProp, sortOrder);
        return pr.getTotal() == 0L ? Result.okEmpty((Object)pr) : Result.ok((Object)pr);
    }

    @GetMapping(value={"/leaders/team-batting"})
    public Result<PageResult<Map<String, Object>>> leadersTeamBatting(@RequestParam(required=false) Long eventId, @RequestParam(required=false) String eventIds, @RequestParam(required=false) String years, @RequestParam(required=false) String teamIds, @RequestParam(required=false) Long teamId, @RequestParam(required=false) String homeAway, @RequestParam(required=false) String gameMode, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder) {
        List tids;
        List yrs;
        List eids = this.parseLongList(eventIds);
        PageResult pr = this.statsService.getTeamBattingLeaders(eventId, eids, yrs = this.parseStringList(years), tids = this.resolveTeamIds(teamIds, teamId), homeAway, gameMode, page, pageSize, sortProp, sortOrder);
        return pr.getTotal() == 0L ? Result.okEmpty((Object)pr) : Result.ok((Object)pr);
    }

    @GetMapping(value={"/leaders/team-pitching"})
    public Result<PageResult<Map<String, Object>>> leadersTeamPitching(@RequestParam(required=false) Long eventId, @RequestParam(required=false) String eventIds, @RequestParam(required=false) String years, @RequestParam(required=false) String teamIds, @RequestParam(required=false) Long teamId, @RequestParam(required=false) String homeAway, @RequestParam(required=false) String gameMode, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder) {
        List tids;
        List yrs;
        List eids = this.parseLongList(eventIds);
        PageResult pr = this.statsService.getTeamPitchingLeaders(eventId, eids, yrs = this.parseStringList(years), tids = this.resolveTeamIds(teamIds, teamId), homeAway, gameMode, page, pageSize, sortProp, sortOrder);
        return pr.getTotal() == 0L ? Result.okEmpty((Object)pr) : Result.ok((Object)pr);
    }

    @GetMapping(value={"/leaders/team-fielding"})
    public Result<PageResult<Map<String, Object>>> leadersTeamFielding(@RequestParam(required=false) Long eventId, @RequestParam(required=false) String eventIds, @RequestParam(required=false) String years, @RequestParam(required=false) String teamIds, @RequestParam(required=false) Long teamId, @RequestParam(required=false) String homeAway, @RequestParam(required=false) String gameMode, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder) {
        List tids;
        List yrs;
        List eids = this.parseLongList(eventIds);
        PageResult pr = this.statsService.getTeamFieldingLeaders(eventId, eids, yrs = this.parseStringList(years), tids = this.resolveTeamIds(teamIds, teamId), homeAway, gameMode, page, pageSize, sortProp, sortOrder);
        return pr.getTotal() == 0L ? Result.okEmpty((Object)pr) : Result.ok((Object)pr);
    }

    @GetMapping(value={"/standings"})
    public Result<PageResult<Map<String, Object>>> standings(@RequestParam(required=false) Long eventId, @RequestParam(required=false) String eventIds, @RequestParam(required=false) String years, @RequestParam(required=false) String gameMode, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize) {
        List yrs;
        List eids = this.parseLongList(eventIds);
        PageResult pr = this.statsService.getStandings(eventId, eids, yrs = this.parseStringList(years), gameMode, page, pageSize);
        return pr.getTotal() == 0L ? Result.okEmpty((Object)pr) : Result.ok((Object)pr);
    }

    @GetMapping(value={"/star/toplist"})
    public Result<Map<String, Object>> starTopList(@RequestParam(required=false) Long eventId, @RequestParam(required=false) String eventIds, @RequestParam(required=false) String years, @RequestParam(required=false) String gameMode, @RequestParam(required=false) Integer limit, @RequestParam(required=false) String includeMetrics) {
        List eids = this.parseLongList(eventIds);
        List yrs = this.parseStringList(years);
        List metrics = this.parseStringList(includeMetrics);
        Map data = this.statsService.getStarTopList(eventId, eids, yrs, gameMode, limit, metrics);
        return Result.ok((Object)data);
    }

    private List<Long> resolveTeamIds(String teamIds, Long teamId) {
        List fromParam = this.parseLongList(teamIds);
        if (fromParam != null && !fromParam.isEmpty()) {
            return fromParam;
        }
        if (teamId != null) {
            return List.of((Object)teamId);
        }
        return null;
    }

    private List<Long> parseLongList(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).map(Long::parseLong).toList();
    }

    private List<String> parseStringList(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    @Generated
    public StatsLeadersApi(StatsService statsService) {
        this.statsService = statsService;
    }
}

