/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysArticleApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.entity.SysArticle
 *  com.bsball.service.LbsIpLocationService
 *  com.bsball.service.SysArticleService
 *  com.bsball.utils.HttpClientIpUtil
 *  jakarta.servlet.http.HttpServletRequest
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
import com.bsball.model.entity.SysArticle;
import com.bsball.service.LbsIpLocationService;
import com.bsball.service.SysArticleService;
import com.bsball.utils.HttpClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
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
@RequestMapping(value={"/sys/article"})
public class SysArticleApi {
    private final SysArticleService sysArticleService;
    private final LbsIpLocationService lbsIpLocationService;

    @GetMapping(value={"/list"})
    public Result<PageResult<SysArticle>> list(@RequestParam(required=false) Boolean platformOnly, @RequestParam(required=false) Boolean forPortal, @RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder, @RequestParam(required=false) String publishTarget, @RequestParam(required=false) String type, @RequestParam(required=false) String keyword, @RequestParam(required=false) Integer showInCarousel, @RequestParam(required=false) String adminScope) {
        PageResult data = Boolean.TRUE.equals(platformOnly) ? this.sysArticleService.listPlatformPortal(page, pageSize, sortProp, sortOrder, publishTarget, type, keyword, showInCarousel) : (Boolean.TRUE.equals(forPortal) ? this.sysArticleService.listPortal(page, pageSize, sortProp, sortOrder, publishTarget, type, keyword, showInCarousel) : this.sysArticleService.list(page, pageSize, sortProp, sortOrder, publishTarget, type, keyword, showInCarousel, adminScope));
        return Result.ok((Object)data);
    }

    @GetMapping(value={"/platform/list"})
    public Result<PageResult<SysArticle>> platformList(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder, @RequestParam(required=false) String publishTarget, @RequestParam(required=false) String type, @RequestParam(required=false) String keyword, @RequestParam(required=false) Integer showInCarousel) {
        PageResult data = this.sysArticleService.listPlatformPortal(page, pageSize, sortProp, sortOrder, publishTarget, type, keyword, showInCarousel);
        return Result.ok((Object)data);
    }

    @GetMapping(value={"/{id}"})
    public Result<SysArticle> get(@PathVariable Long id) {
        SysArticle data = this.sysArticleService.get(id);
        return Result.ok((Object)data);
    }

    @PostMapping(value={"/increment-view/{id}"})
    public Result<Map<String, Long>> incrementView(HttpServletRequest request, @PathVariable Long id) {
        String ip = HttpClientIpUtil.getClientIp((HttpServletRequest)request);
        Optional count = this.sysArticleService.incrementViewCount(id, ip);
        if (count.isEmpty()) {
            return Result.fail((int)404, (String)"\u5185\u5bb9\u4e0d\u5b58\u5728");
        }
        return Result.ok((Object)Map.of((Object)"viewCount", (Object)((Long)count.get())));
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(HttpServletRequest request, @RequestBody @Valid SysArticle body) {
        String ip = HttpClientIpUtil.getClientIp((HttpServletRequest)request);
        String region = this.lbsIpLocationService.provinceForPersist(ip);
        SysArticle created = this.sysArticleService.create(body, ip, region);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(HttpServletRequest request, @PathVariable Long id, @RequestBody @Valid SysArticle body) {
        String ip = HttpClientIpUtil.getClientIp((HttpServletRequest)request);
        String region = this.lbsIpLocationService.provinceForPersist(ip);
        this.sysArticleService.update(id, body, ip, region);
        return Result.ok((Object)Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        this.sysArticleService.delete(id);
        return Result.ok((Object)Map.of());
    }

    @Generated
    public SysArticleApi(SysArticleService sysArticleService, LbsIpLocationService lbsIpLocationService) {
        this.sysArticleService = sysArticleService;
        this.lbsIpLocationService = lbsIpLocationService;
    }
}

