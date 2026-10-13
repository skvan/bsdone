package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.model.dto.EventRosterPlayerDto;
import com.bsball.model.dto.EventTeamDto;
import com.bsball.service.EventRegistrationService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 电子秩序册（Issue #310）API：赛事参赛登记（邀请/列表/移除/锁定）与球队侧报名（确认/谢绝/名单）。
 * 写守卫在服务层：赛事侧 assertCanManageEvent；球队侧 assertCanStewardOrManageTeam（含代管无主球队）。
 */
@RestController
public class EventRegistrationApi {
    private final EventRegistrationService eventRegistrationService;

    public EventRegistrationApi(EventRegistrationService eventRegistrationService) {
        this.eventRegistrationService = eventRegistrationService;
    }

    /** 邀请球队参赛（body: teamIds 必填非空、remark 可选）。 */
    @PostMapping(value = {"/event/{eventId}/teams/invite"})
    public Result<Map<String, Object>> invite(@PathVariable Long eventId, @RequestBody InviteTeamsRequest body) {
        if (body == null || body.teamIds() == null || body.teamIds().isEmpty()) {
            return Result.fail("请选择要邀请的球队");
        }
        return Result.ok(this.eventRegistrationService.inviteTeams(eventId, body.teamIds(), body.remark()));
    }

    /** 赛事参赛列表（球队名/徽、状态、名单人数、锁定态）。 */
    @GetMapping(value = {"/event/{eventId}/teams"})
    public Result<List<EventTeamDto>> listEventTeams(@PathVariable Long eventId) {
        return Result.ok(this.eventRegistrationService.listEventTeams(eventId));
    }

    /** 移除参赛（软删登记 + 名单；锁定后 400）。 */
    @PostMapping(value = {"/event/{eventId}/teams/{teamId}/remove"})
    public Result<Object> remove(@PathVariable Long eventId, @PathVariable Long teamId) {
        this.eventRegistrationService.removeEventTeam(eventId, teamId);
        return Result.ok(Map.of());
    }

    /** 锁定名单（办赛方）。 */
    @PostMapping(value = {"/event/{eventId}/teams/{teamId}/lock"})
    public Result<EventTeamDto> lock(@PathVariable Long eventId, @PathVariable Long teamId) {
        return Result.ok(this.eventRegistrationService.lockRoster(eventId, teamId, true));
    }

    /** 解锁名单（办赛方）。 */
    @PostMapping(value = {"/event/{eventId}/teams/{teamId}/unlock"})
    public Result<EventTeamDto> unlock(@PathVariable Long eventId, @PathVariable Long teamId) {
        return Result.ok(this.eventRegistrationService.lockRoster(eventId, teamId, false));
    }

    /** 查看报名名单明细。 */
    @GetMapping(value = {"/event/{eventId}/teams/{teamId}/roster"})
    public Result<List<EventRosterPlayerDto>> roster(@PathVariable Long eventId, @PathVariable Long teamId) {
        return Result.ok(this.eventRegistrationService.roster(eventId, teamId));
    }

    /** 球队侧「我的赛事报名」列表。 */
    @GetMapping(value = {"/team/{teamId}/event-registrations"})
    public Result<List<EventTeamDto>> myRegistrations(@PathVariable Long teamId) {
        return Result.ok(this.eventRegistrationService.listTeamRegistrations(teamId));
    }

    /** 确认参加。 */
    @PostMapping(value = {"/event/{eventId}/teams/{teamId}/confirm"})
    public Result<EventTeamDto> confirm(@PathVariable Long eventId, @PathVariable Long teamId) {
        return Result.ok(this.eventRegistrationService.confirm(eventId, teamId));
    }

    /** 谢绝参赛。 */
    @PostMapping(value = {"/event/{eventId}/teams/{teamId}/decline"})
    public Result<EventTeamDto> decline(@PathVariable Long eventId, @PathVariable Long teamId) {
        return Result.ok(this.eventRegistrationService.decline(eventId, teamId));
    }

    /** 提交/修改报名名单（body: playerIds 按展示顺序）。 */
    @PutMapping(value = {"/event/{eventId}/teams/{teamId}/roster"})
    public Result<EventTeamDto> submitRoster(@PathVariable Long eventId, @PathVariable Long teamId,
            @RequestBody SubmitRosterRequest body) {
        return Result.ok(this.eventRegistrationService.submitRoster(eventId, teamId,
                body == null ? null : body.playerIds()));
    }

    /** 邀请请求体。 */
    public record InviteTeamsRequest(List<Long> teamIds, String remark) {
    }

    /** 提交名单请求体。 */
    public record SubmitRosterRequest(List<Long> playerIds) {
    }
}
