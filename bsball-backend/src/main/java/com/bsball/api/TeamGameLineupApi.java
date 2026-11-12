package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.model.dto.GameLineupSubmissionDto;
import com.bsball.model.dto.LineupSubmissionSaveRequest;
import com.bsball.model.dto.TeamLineupContextDto;
import com.bsball.model.dto.TeamLineupGameListDto;
import com.bsball.service.TeamGameLineupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 赛前阵容提交（电子打序单）API。
 * <p>球队侧：比赛列表 / 单场上下文 / 保存本队阵容；办赛方：读取双方提交。
 * 写守卫与状态/名单校验在 {@link TeamGameLineupService}。</p>
 */
@RestController
public class TeamGameLineupApi {
    private final TeamGameLineupService teamGameLineupService;

    public TeamGameLineupApi(TeamGameLineupService teamGameLineupService) {
        this.teamGameLineupService = teamGameLineupService;
    }

    /** 球队端：该赛事下本队所有未开始比赛及阵容保存状态。 */
    @GetMapping(value = {"/team/{teamId}/lineup-games"})
    public Result<TeamLineupGameListDto> listGames(@PathVariable Long teamId, @RequestParam Long eventId) {
        return Result.ok(this.teamGameLineupService.listGames(teamId, eventId));
    }

    /** 球队端：单场赛前阵容编辑上下文（只返回本队提交）。 */
    @GetMapping(value = {"/team/{teamId}/lineup-games/{gameId}"})
    public Result<TeamLineupContextDto> context(@PathVariable Long teamId, @PathVariable Long gameId) {
        return Result.ok(this.teamGameLineupService.getContext(teamId, gameId));
    }

    /** 球队端：保存本队赛前阵容（草稿允许不完整；开赛后拒绝）。 */
    @PutMapping(value = {"/team/{teamId}/lineup-games/{gameId}"})
    public Result<Object> save(@PathVariable Long teamId, @PathVariable Long gameId,
            @RequestBody @Valid LineupSubmissionSaveRequest body) {
        this.teamGameLineupService.save(teamId, gameId, body);
        return Result.ok(Map.of("saved", Boolean.TRUE));
    }

    /** 办赛方：读取一场比赛双方已提交的赛前阵容。 */
    @GetMapping(value = {"/game/{gameId}/lineup-submissions"})
    public Result<List<GameLineupSubmissionDto>> submissions(@PathVariable Long gameId) {
        return Result.ok(this.teamGameLineupService.listSubmissionsForOrganizer(gameId));
    }
}
