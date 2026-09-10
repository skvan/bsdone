/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysDictTypeApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.entity.SysDictType
 *  com.bsball.service.SysDictTypeService
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
import com.bsball.model.entity.SysDictType;
import com.bsball.service.SysDictTypeService;
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
@RequestMapping(value={"/sys/dict/type"})
public class SysDictTypeApi {
    private final SysDictTypeService sysDictTypeService;

    @GetMapping(value={"/list"})
    public Result<PageResult<SysDictType>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize) {
        PageResult data = this.sysDictTypeService.list(page, pageSize);
        return Result.ok((Object)data);
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody SysDictType body) {
        SysDictType created = this.sysDictTypeService.create(body);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(@PathVariable Long id, @RequestBody @Valid SysDictType body) {
        this.sysDictTypeService.update(id, body);
        return Result.ok((Object)Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        this.sysDictTypeService.delete(id);
        return Result.ok((Object)Map.of());
    }

    @Generated
    public SysDictTypeApi(SysDictTypeService sysDictTypeService) {
        this.sysDictTypeService = sysDictTypeService;
    }
}

