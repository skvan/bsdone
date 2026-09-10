/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.HistoryRecordApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.entity.HistoryRecord
 *  com.bsball.service.HistoryRecordService
 *  jakarta.validation.Valid
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.PageResult;
import com.bsball.common.Result;
import com.bsball.model.entity.HistoryRecord;
import com.bsball.service.HistoryRecordService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/history-record", "/personnel-change"})
public class HistoryRecordApi {
    private final HistoryRecordService historyRecordService;

    @GetMapping(value={"/list"})
    public Result<PageResult<HistoryRecord>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder, @RequestParam(required=false) String targetType, @RequestParam(required=false) Long targetId, @RequestParam(required=false) String relatedObjectType, @RequestParam(required=false) Long relatedObjectId, @RequestParam(required=false) String type, @RequestParam(required=false) String recordType, @RequestParam(required=false) String dateFrom, @RequestParam(required=false) String dateTo) {
        PageResult data = this.historyRecordService.list(page, pageSize, sortProp, sortOrder, targetType, targetId, relatedObjectType, relatedObjectId, type, recordType, dateFrom, dateTo);
        return Result.ok((Object)data);
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody @Valid HistoryRecord body) {
        HistoryRecord created = this.historyRecordService.create(body);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId()));
    }

    @Generated
    public HistoryRecordApi(HistoryRecordService historyRecordService) {
        this.historyRecordService = historyRecordService;
    }
}

