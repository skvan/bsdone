/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysMediaIconApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.entity.SysMediaIcon
 *  com.bsball.service.SysMediaIconService
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
 *  org.springframework.web.multipart.MultipartFile
 */
package com.bsball.api;

import com.bsball.common.PageResult;
import com.bsball.common.Result;
import com.bsball.model.entity.SysMediaIcon;
import com.bsball.service.SysMediaIconService;
import java.io.IOException;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/sys/media-icon"})
public class SysMediaIconApi {
    private final SysMediaIconService sysMediaIconService;

    @GetMapping(value={"/list"})
    public Result<PageResult<SysMediaIcon>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword, @RequestParam(required=false) String tag) {
        return Result.ok((Object)this.sysMediaIconService.list(page, pageSize, keyword, tag));
    }

    @GetMapping(value={"/{id}"})
    public Result<SysMediaIcon> get(@PathVariable Long id) {
        return Result.ok((Object)this.sysMediaIconService.get(id));
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody SysMediaIcon body) {
        SysMediaIcon created = this.sysMediaIconService.create(body);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody SysMediaIcon body) {
        SysMediaIcon updated = this.sysMediaIconService.update(id, body);
        return Result.ok((Object)Map.of((Object)"id", (Object)updated.getId()));
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        this.sysMediaIconService.delete(id);
        return Result.ok((Object)Map.of());
    }

    @PostMapping(value={"/upload"})
    public Result<Map<String, Object>> upload(@RequestParam(value="file") MultipartFile file, @RequestParam(required=false) String name, @RequestParam(required=false) String tags) throws IOException {
        SysMediaIcon created = this.sysMediaIconService.upload(file, name, tags);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId(), (Object)"name", (Object)created.getName()));
    }

    @Generated
    public SysMediaIconApi(SysMediaIconService sysMediaIconService) {
        this.sysMediaIconService = sysMediaIconService;
    }
}

