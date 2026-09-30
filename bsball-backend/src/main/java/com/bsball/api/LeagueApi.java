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
import com.bsball.model.dto.LeagueOwnerDto;
import com.bsball.model.entity.League;
import com.bsball.model.entity.LeagueCreateRequest;
import com.bsball.model.entity.LeagueOwner;
import com.bsball.service.LeagueOwnerAssignService;
import com.bsball.service.LeagueProvisionService;
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
    private final LeagueProvisionService leagueProvisionService;

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
        return Result.ok(this.leagueService.createForCurrentUser(body));
    }

    /* 账号权限重构（批次 3a，Task 3.3）：门户自助建联盟审核端点（管理员；服务内校验超管/租管与同租户）。 */

    @GetMapping(value={"/create-request/list"})
    public Result<PageResult<LeagueCreateRequest>> createRequestList(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize) {
        Long operatorId = this.requireOperator();
        return Result.ok(this.leagueProvisionService.listPending(operatorId, page, pageSize));
    }

    @PostMapping(value={"/create-request/{id}/approve"})
    public Result<League> approveCreateRequest(@PathVariable Long id) {
        Long operatorId = this.requireOperator();
        return Result.ok(this.leagueProvisionService.approve(operatorId, id));
    }

    @PostMapping(value={"/create-request/{id}/reject"})
    public Result<Object> rejectCreateRequest(@PathVariable Long id, @RequestBody(required=false) Map<String, Object> body) {
        Long operatorId = this.requireOperator();
        Object raw = body == null ? null : body.get("reason");
        String reason = raw == null ? null : String.valueOf(raw);
        this.leagueProvisionService.reject(operatorId, id, reason);
        return Result.ok(Map.of());
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
    public Result<List<LeagueOwnerDto>> owners(@PathVariable Long id) {
        // I2：新增操作者守卫 + 服务内 assertAdmin/租户校验；返回精简 DTO（不回传实体）。
        Long operatorId = this.requireOperator();
        return Result.ok(this.leagueOwnerAssignService.listOwners(operatorId, id));
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
        // TODO(批 3a 已裁定硬化项/M3)：宽松解析失败（非数字）静默返回 null → 上层报「userId 不能为空」，
        //  文案不够精确（应区分「非法」与「缺失」）；后续批次硬化。
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
            LeagueProvisionService leagueProvisionService) {
        this.leagueService = leagueService;
        this.leagueOwnerAssignService = leagueOwnerAssignService;
        this.leagueProvisionService = leagueProvisionService;
    }
}

