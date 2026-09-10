/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.StadiumApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.dto.StadiumUpsertDto
 *  com.bsball.model.dto.StadiumViewDto
 *  com.bsball.model.enums.StadiumLevel
 *  com.bsball.service.StadiumService
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
import com.bsball.model.dto.StadiumUpsertDto;
import com.bsball.model.dto.StadiumViewDto;
import com.bsball.model.enums.StadiumLevel;
import com.bsball.service.StadiumService;
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
@RequestMapping(value={"/stadium"})
public class StadiumApi {
    private final StadiumService stadiumService;

    @GetMapping(value={"/list"})
    public Result<PageResult<StadiumViewDto>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder, @RequestParam(required=false) String keyword, @RequestParam(required=false) StadiumLevel level) {
        return Result.ok((Object)this.stadiumService.list(page, pageSize, sortProp, sortOrder, keyword, level));
    }

    @GetMapping(value={"/geojson"})
    public Result<Map<String, Object>> geoJson() {
        return Result.ok((Object)this.stadiumService.buildGeoJsonFeatureCollection());
    }

    @GetMapping(value={"/nearby"})
    public Result<List<StadiumViewDto>> nearby(@RequestParam double lng, @RequestParam double lat, @RequestParam(defaultValue="50000") double radiusMeters) {
        return Result.ok((Object)this.stadiumService.listNearby(lng, lat, radiusMeters));
    }

    @GetMapping(value={"/{id}"})
    public Result<StadiumViewDto> get(@PathVariable Long id) {
        return Result.ok((Object)this.stadiumService.get(id));
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody @Valid StadiumUpsertDto body) {
        StadiumViewDto v = this.stadiumService.create(body);
        return Result.ok((Object)Map.of((Object)"id", (Object)v.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(@PathVariable Long id, @RequestBody @Valid StadiumUpsertDto body) {
        StadiumViewDto v = this.stadiumService.update(id, body);
        if (v == null) {
            return Result.fail((int)404, (String)"\u7403\u573a\u4e0d\u5b58\u5728");
        }
        return Result.ok((Object)Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        this.stadiumService.delete(id);
        return Result.ok((Object)Map.of());
    }

    @Generated
    public StadiumApi(StadiumService stadiumService) {
        this.stadiumService = stadiumService;
    }
}

