/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.TeamManagerApi
 *  com.bsball.common.Result
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.Team
 *  com.bsball.model.entity.TeamManager
 *  com.bsball.repository.TeamManagerRepository
 *  com.bsball.repository.TeamRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.JwtService
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestHeader
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.Team;
import com.bsball.model.entity.TeamManager;
import com.bsball.repository.TeamManagerRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.JwtService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TeamManagerApi {
    private final TeamManagerRepository teamManagerRepository;
    private final TeamRepository teamRepository;
    private final ApiPermissionService apiPermissionService;
    private final JwtService jwtService;

    @GetMapping(value={"/team/{teamId}/managers"})
    public Result<List<TeamManager>> list(@PathVariable Long teamId) {
        return Result.ok((Object)this.teamManagerRepository.findByTeamIdAndStatusAndDeletedAtIsNull(teamId, "active"));
    }

    @PostMapping(value={"/team/{teamId}/managers"})
    public Result<TeamManager> assign(@RequestHeader(value="Authorization", required=false) String auth, @PathVariable Long teamId, @RequestBody Map<String, Object> body) {
        Long userId;
        Object object;
        Long opId = this.requireUserId(auth);
        if (!this.apiPermissionService.isSuperAdmin(opId) && !this.apiPermissionService.isTenantAdmin(opId)) {
            throw new BusinessException(403, "\u4ec5\u7ba1\u7406\u5458\u53ef\u6307\u5b9a\u7403\u961f\u8d1f\u8d23\u4eba");
        }
        if (body != null && (object = body.get("userId")) instanceof Number) {
            Number n = (Number)object;
            v0 = n.longValue();
        } else {
            v0 = userId = null;
        }
        if (userId == null) {
            throw new BusinessException(400, "userId \u4e0d\u80fd\u4e3a\u7a7a");
        }
        Team team = (Team)this.teamRepository.findById((Object)teamId).orElseThrow(() -> new BusinessException(404, "\u7403\u961f\u4e0d\u5b58\u5728"));
        if (this.teamManagerRepository.findByTeamIdAndUserIdAndDeletedAtIsNull(teamId, userId).isPresent()) {
            throw new BusinessException(400, "\u8be5\u7528\u6237\u5df2\u662f\u7403\u961f\u8d1f\u8d23\u4eba");
        }
        LocalDateTime now = LocalDateTime.now();
        TeamManager tm = new TeamManager();
        tm.setTenantId(team.getTenantId());
        tm.setTeamId(teamId);
        tm.setUserId(userId);
        tm.setStatus("active");
        tm.setCreatedAt(now);
        tm.setUpdatedAt(now);
        return Result.ok((Object)((TeamManager)this.teamManagerRepository.save((Object)tm)));
    }

    @DeleteMapping(value={"/team/{teamId}/managers/{userId}"})
    public Result<Object> remove(@RequestHeader(value="Authorization", required=false) String auth, @PathVariable Long teamId, @PathVariable Long userId) {
        Long opId = this.requireUserId(auth);
        if (!this.apiPermissionService.isSuperAdmin(opId) && !this.apiPermissionService.isTenantAdmin(opId)) {
            throw new BusinessException(403, "\u4ec5\u7ba1\u7406\u5458\u53ef\u79fb\u9664\u7403\u961f\u8d1f\u8d23\u4eba");
        }
        TeamManager tm = (TeamManager)this.teamManagerRepository.findByTeamIdAndUserIdAndDeletedAtIsNull(teamId, userId).orElseThrow(() -> new BusinessException(404, "\u8d1f\u8d23\u4eba\u4e0d\u5b58\u5728"));
        tm.setStatus("inactive");
        tm.setDeletedAt(LocalDateTime.now());
        this.teamManagerRepository.save((Object)tm);
        return Result.ok((Object)Map.of());
    }

    private Long requireUserId(String auth) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7).trim() : null;
        Long userId = this.jwtService.parseUserId(token);
        if (userId == null) {
            throw new BusinessException(401, "\u8bf7\u5148\u767b\u5f55");
        }
        return userId;
    }

    @Generated
    public TeamManagerApi(TeamManagerRepository teamManagerRepository, TeamRepository teamRepository, ApiPermissionService apiPermissionService, JwtService jwtService) {
        this.teamManagerRepository = teamManagerRepository;
        this.teamRepository = teamRepository;
        this.apiPermissionService = apiPermissionService;
        this.jwtService = jwtService;
    }
}

