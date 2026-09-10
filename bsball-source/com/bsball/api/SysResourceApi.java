/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysResourceApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.entity.SysResource
 *  com.bsball.service.SysResourceService
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 *  org.springframework.web.multipart.MultipartFile
 */
package com.bsball.api;

import com.bsball.common.PageResult;
import com.bsball.common.Result;
import com.bsball.model.entity.SysResource;
import com.bsball.service.SysResourceService;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/sys/resource"})
public class SysResourceApi {
    private final SysResourceService sysResourceService;

    @GetMapping(value={"/list"})
    public Result<PageResult<SysResource>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword) {
        PageResult data = this.sysResourceService.list(page, pageSize, keyword);
        return Result.ok((Object)data);
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody SysResource body) {
        SysResource created = this.sysResourceService.create(body);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId()));
    }

    @PostMapping(value={"/upload"})
    public Result<Map<String, Object>> upload(@RequestParam(value="file") MultipartFile file) {
        try {
            SysResource created = this.sysResourceService.upload(file);
            return Result.ok((Object)Map.of((Object)"id", (Object)created.getId(), (Object)"url", (Object)(created.getUrl() != null ? created.getUrl() : ""), (Object)"path", (Object)(created.getPath() != null ? created.getPath() : ""), (Object)"name", (Object)(created.getName() != null ? created.getName() : file.getOriginalFilename())));
        }
        catch (IllegalArgumentException e) {
            return Result.fail((String)e.getMessage());
        }
        catch (Exception e) {
            return Result.fail((String)("\u4e0a\u4f20\u5931\u8d25: " + e.getMessage()));
        }
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        this.sysResourceService.delete(id);
        return Result.ok((Object)Map.of());
    }

    @Generated
    public SysResourceApi(SysResourceService sysResourceService) {
        this.sysResourceService = sysResourceService;
    }
}

