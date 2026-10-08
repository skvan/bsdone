/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.PlayerClaimApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.exception.UnauthorizedException
 *  com.bsball.model.entity.PlayerClaim
 *  com.bsball.service.JwtService
 *  com.bsball.service.PlayerClaimService
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestHeader
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.PageResult;
import com.bsball.common.Result;
import com.bsball.exception.BusinessException;
import com.bsball.exception.UnauthorizedException;
import com.bsball.model.entity.Player;
import com.bsball.model.entity.PlayerClaim;
import com.bsball.service.JwtService;
import com.bsball.service.PlayerClaimService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * Exception performing whole class analysis ignored.
 */
@RestController
public class PlayerClaimApi {
    private final PlayerClaimService playerClaimService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @PostMapping(value={"/account/player-claim"})
    public Result<PlayerClaim> submit(@RequestHeader(value="Authorization", required=false) String auth, @RequestBody Map<String, Object> body) {
        Long userId = this.requireUserId(auth);
        Long playerId = PlayerClaimApi.parseLong(body != null ? body.get("playerId") : null);
        if (playerId == null) {
            return Result.fail("playerId \u4e0d\u80fd\u4e3a\u7a7a");
        }
        String remark = body != null && body.get("remark") != null ? body.get("remark").toString() : null;
        return Result.ok(this.playerClaimService.submitClaim(userId, playerId, remark, null));
    }

    @GetMapping(value={"/account/player-claims"})
    public Result<List<PlayerClaim>> myClaims(@RequestHeader(value="Authorization", required=false) String auth) {
        return Result.ok(this.playerClaimService.myClaims(this.requireUserId(auth)));
    }

    @GetMapping(value={"/account/player-claims/pending"})
    public Result<PageResult<Map<String, Object>>> pending(@RequestHeader(value="Authorization", required=false) String auth, @RequestParam(defaultValue="1") Integer page, @RequestParam(defaultValue="20") Integer pageSize, @RequestParam(required=false) String keyword, @RequestParam(required=false) String reviewerType) {
        return Result.ok(this.playerClaimService.pendingForReviewer(this.requireUserId(auth), page, pageSize, keyword, reviewerType));
    }

    @PostMapping(value={"/account/player-claims/{id}/cancel"})
    public Result<PlayerClaim> cancel(@RequestHeader(value="Authorization", required=false) String auth, @PathVariable Long id) {
        return Result.ok(this.playerClaimService.cancelClaim(this.requireUserId(auth), id));
    }

    @PostMapping(value={"/account/player-claims/{id}/approve"})
    public Result<PlayerClaim> approve(@RequestHeader(value="Authorization", required=false) String auth, @PathVariable Long id) {
        return Result.ok(this.playerClaimService.approve(id, this.requireUserId(auth), null));
    }

    @PostMapping(value={"/account/player-claims/{id}/reject"})
    public Result<PlayerClaim> reject(@RequestHeader(value="Authorization", required=false) String auth, @PathVariable Long id, @RequestBody(required=false) Map<String, Object> body) {
        String reason = body != null && body.get("reason") != null ? body.get("reason").toString() : null;
        return Result.ok(this.playerClaimService.reject(id, this.requireUserId(auth), reason));
    }

    @PostMapping(value={"/account/player-profile/{playerId}/release-claim"})
    public Result<Map<String, Object>> releaseClaim(@RequestHeader(value="Authorization", required=false) String auth, @PathVariable Long playerId, @RequestBody(required=false) Map<String, Object> body) {
        Long userId = this.requireUserId(auth);
        String remark = body != null && body.get("remark") != null ? body.get("remark").toString() : null;
        return Result.ok(this.playerClaimService.releaseClaim(playerId, userId, remark));
    }

    @PostMapping(value={"/team/{teamId}/player-claim-invite"})
    public Result<Map<String, Object>> createInvite(@RequestHeader(value="Authorization", required=false) String auth, @PathVariable Long teamId, @RequestBody(required=false) Map<String, Object> body) {
        Integer n;
        Object object;
        Integer expireHours;
        Object object2;
        Long playerId;
        Long userId = this.requireUserId(auth);
        Long l = playerId = body != null ? PlayerClaimApi.parseLong((Object)body.get("playerId")) : null;
        if (body != null && (object2 = body.get("expireHours")) instanceof Number) {
            Number n2 = (Number)object2;
            expireHours = n2.intValue();
        } else {
            expireHours = null;
        }
        if (body != null && (object = body.get("maxUses")) instanceof Number) {
            Number n3 = (Number)object;
            n = n3.intValue();
        } else {
            n = null;
        }
        Integer maxUses = n;
        String remark = body != null && body.get("remark") != null ? body.get("remark").toString() : null;
        return Result.ok(this.playerClaimService.createInvite(userId, teamId, playerId, expireHours, maxUses, remark));
    }

    @GetMapping(value={"/portal/player-claim-invite/{token}"})
    public Result<Map<String, Object>> getInvite(@PathVariable String token) {
        return Result.ok(this.playerClaimService.getInvitePublic(token));
    }

    @PostMapping(value={"/portal/player-claim-invite/{token}/claim"})
    public Result<Map<String, Object>> claimViaInvite(@RequestHeader(value="Authorization", required=false) String auth, @PathVariable String token, @RequestBody(required=false) Map<String, Object> body) {
        Long userId = this.requireUserId(auth);
        Long playerId = body != null ? PlayerClaimApi.parseLong((Object)body.get("playerId")) : null;
        Player draft = this.parseDraft(body);
        return Result.ok(this.playerClaimService.claimOrRegisterViaInvite(userId, token, playerId, draft));
    }

    /** 从 body.draft 解析自助建档草稿（可选）；缺省或非对象时返回 null，交由服务层判定分支。 */
    private Player parseDraft(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object raw = body.get("draft");
        if (raw == null) {
            return null;
        }
        try {
            return (Player)this.objectMapper.convertValue(raw, Player.class);
        }
        catch (IllegalArgumentException e) {
            throw new BusinessException(400, "\u5efa\u6863\u8349\u7a3f\u683c\u5f0f\u4e0d\u6b63\u786e");
        }
    }

    private Long requireUserId(String auth) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7).trim() : null;
        Long userId = this.jwtService.parseUserId(token);
        if (userId == null) {
            throw new UnauthorizedException("\u8bf7\u5148\u767b\u5f55");
        }
        return userId;
    }

    private static Long parseLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            Number n = (Number)v;
            return n.longValue();
        }
        try {
            return Long.parseLong(v.toString().trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    @Generated
    public PlayerClaimApi(PlayerClaimService playerClaimService, JwtService jwtService, ObjectMapper objectMapper) {
        this.playerClaimService = playerClaimService;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }
}

