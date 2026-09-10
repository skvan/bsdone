/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysNoticeApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.dto.SysNoticeCreateRequest
 *  com.bsball.model.dto.SysNoticeCreateResult
 *  com.bsball.model.entity.SysNotice
 *  com.bsball.service.SysNoticeService
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
import com.bsball.model.dto.SysNoticeCreateRequest;
import com.bsball.model.dto.SysNoticeCreateResult;
import com.bsball.model.entity.SysNotice;
import com.bsball.service.SysNoticeService;
import jakarta.validation.Valid;
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
@RequestMapping(value={"/sys/notice"})
public class SysNoticeApi {
    private final SysNoticeService sysNoticeService;

    @GetMapping(value={"/list"})
    public Result<PageResult<SysNotice>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder, @RequestParam(required=false) String target, @RequestParam(required=false) String keyword) {
        PageResult data = this.sysNoticeService.list(page, pageSize, sortProp, sortOrder, target, keyword);
        return Result.ok((Object)data);
    }

    @GetMapping(value={"/{id}"})
    public Result<SysNotice> get(@PathVariable Long id) {
        SysNotice data = this.sysNoticeService.get(id);
        return Result.ok((Object)data);
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody @Valid SysNoticeCreateRequest body) {
        SysNoticeCreateResult created = this.sysNoticeService.create(body);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.firstId(), (Object)"createdCount", (Object)created.createdCount()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(@PathVariable Long id, @RequestBody @Valid SysNotice body) {
        this.sysNoticeService.update(id, body);
        return Result.ok((Object)Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        this.sysNoticeService.delete(id);
        return Result.ok((Object)Map.of());
    }

    @Generated
    public SysNoticeApi(SysNoticeService sysNoticeService) {
        this.sysNoticeService = sysNoticeService;
    }
}

