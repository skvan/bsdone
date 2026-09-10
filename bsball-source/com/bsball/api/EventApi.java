/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.EventApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.dto.SaveGameResultDTO
 *  com.bsball.model.entity.Event
 *  com.bsball.service.EventService
 *  com.bsball.service.GameService
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
import com.bsball.model.dto.SaveGameResultDTO;
import com.bsball.model.entity.Event;
import com.bsball.service.EventService;
import com.bsball.service.GameService;
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
@RequestMapping(value={"/event"})
public class EventApi {
    private final EventService eventService;
    private final GameService gameService;

    @GetMapping(value={"/list"})
    public Result<PageResult<Event>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String sortProp, @RequestParam(required=false) String sortOrder) {
        PageResult data = this.eventService.list(page, pageSize, sortProp, sortOrder);
        return Result.ok((Object)data);
    }

    @GetMapping(value={"/{id}"})
    public Result<Event> get(@PathVariable Long id) {
        Event data = this.eventService.get(id);
        if (data == null) {
            return Result.fail((int)404, (String)"\u8d5b\u4e8b\u4e0d\u5b58\u5728\u6216\u5df2\u5220\u9664");
        }
        return Result.ok((Object)data);
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody Event body) {
        Event created = this.eventService.create(body);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId()));
    }

    @PostMapping(value={"/{eventId}/import-game-result"})
    public Result<Map<String, Object>> importGameResult(@PathVariable Long eventId, @RequestBody SaveGameResultDTO body) {
        Long gameId = this.gameService.importGameResult(eventId, body);
        return Result.ok((Object)Map.of((Object)"id", (Object)gameId));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(@PathVariable Long id, @RequestBody @Valid Event body) {
        this.eventService.update(id, body);
        return Result.ok((Object)Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        this.eventService.delete(id);
        return Result.ok((Object)Map.of());
    }

    @Generated
    public EventApi(EventService eventService, GameService gameService) {
        this.eventService = eventService;
        this.gameService = gameService;
    }
}

