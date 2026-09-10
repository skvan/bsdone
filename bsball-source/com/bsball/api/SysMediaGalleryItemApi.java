/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysMediaGalleryItemApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.entity.SysMediaGalleryItem
 *  com.bsball.service.SysMediaGalleryItemService
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
import com.bsball.model.entity.SysMediaGalleryItem;
import com.bsball.service.SysMediaGalleryItemService;
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
@RequestMapping(value={"/sys/media-gallery"})
public class SysMediaGalleryItemApi {
    private final SysMediaGalleryItemService sysMediaGalleryItemService;

    @GetMapping(value={"/list"})
    public Result<PageResult<SysMediaGalleryItem>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword, @RequestParam(required=false) String tag) {
        return Result.ok((Object)this.sysMediaGalleryItemService.list(page, pageSize, keyword, tag));
    }

    @GetMapping(value={"/{id}"})
    public Result<SysMediaGalleryItem> get(@PathVariable Long id) {
        return Result.ok((Object)this.sysMediaGalleryItemService.get(id));
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody SysMediaGalleryItem body) {
        SysMediaGalleryItem created = this.sysMediaGalleryItemService.create(body);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody SysMediaGalleryItem body) {
        SysMediaGalleryItem updated = this.sysMediaGalleryItemService.update(id, body);
        return Result.ok((Object)Map.of((Object)"id", (Object)updated.getId()));
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        this.sysMediaGalleryItemService.delete(id);
        return Result.ok((Object)Map.of());
    }

    @Generated
    public SysMediaGalleryItemApi(SysMediaGalleryItemService sysMediaGalleryItemService) {
        this.sysMediaGalleryItemService = sysMediaGalleryItemService;
    }
}

