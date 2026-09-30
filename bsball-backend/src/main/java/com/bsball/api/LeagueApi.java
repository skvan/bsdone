/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.LeagueApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.entity.League
 *  com.bsball.service.LeagueService
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
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.League;
import com.bsball.model.entity.LeagueOwner;
import com.bsball.repository.LeagueOwnerRepository;
import com.bsball.service.LeagueOwnerAssignService;
import com.bsball.service.LeagueService;
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
@RequestMapping(value={"/league"})
public class LeagueApi {
    private final LeagueService leagueService;
    private final LeagueOwnerAssignService leagueOwnerAssignService;
    private final LeagueOwnerRepository leagueOwnerRepository;

    @GetMapping(value={"/list"})
    public Result<PageResult<League>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder) {
        PageResult data = this.leagueService.list(page, pageSize, sortProp, sortOrder);
        return Result.ok(data);
    }

    @GetMapping(value={"/{id}"})
    public Result<League> get(@PathVariable Long id) {
        League data = this.leagueService.get(id);
        return Result.ok(data);
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody @Valid League body) {
        League created = this.leagueService.create(body);
        return Result.ok(Map.of("id",created.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(@PathVariable Long id, @RequestBody @Valid League body) {
        this.leagueService.update(id, body);
        return Result.ok(Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        this.leagueService.delete(id);
        return Result.ok(Map.of());
    }

    /* 账号权限重构（批次 3a）：联盟主办方管理端点（操作者须超管 / 租户管理员，服务内校验）。 */

    @GetMapping(value={"/{id}/owners"})
    public Result<List<LeagueOwner>> owners(@PathVariable Long id) {
        return Result.ok(this.leagueOwnerRepository.findByLeagueIdAndStatusAndDeletedAtIsNull(id, LeagueOwner.STATUS_ACTIVE));
    }

    @PostMapping(value={"/{id}/owner/assign"})
    public Result<LeagueOwner> assignOwner(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long operatorId = this.requireOperator();
        Long userId = LeagueApi.parseUserId(body);
        if (userId == null) {
            throw new BusinessException(400, "userId 不能为空");
        }
        return Result.ok(this.leagueOwnerAssignService.assign(operatorId, id, userId));
    }

    @DeleteMapping(value={"/{id}/owner/{userId}"})
    public Result<Object> revokeOwner(@PathVariable Long id, @PathVariable Long userId) {
        Long operatorId = this.requireOperator();
        this.leagueOwnerAssignService.revoke(operatorId, id, userId);
        return Result.ok(Map.of());
    }

    /** 取当前登录用户作为操作者（未登录 401）。 */
    private Long requireOperator() {
        Long userId = CurrentUserHolder.get();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }

    private static Long parseUserId(Map<String, Object> body) {
        Object raw = body == null ? null : body.get("userId");
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number)raw).longValue();
        }
        try {
            return Long.parseLong(raw.toString().trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    @Generated
    public LeagueApi(LeagueService leagueService, LeagueOwnerAssignService leagueOwnerAssignService,
            LeagueOwnerRepository leagueOwnerRepository) {
        this.leagueService = leagueService;
        this.leagueOwnerAssignService = leagueOwnerAssignService;
        this.leagueOwnerRepository = leagueOwnerRepository;
    }
}

