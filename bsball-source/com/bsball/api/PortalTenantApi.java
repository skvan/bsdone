/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.PortalTenantApi
 *  com.bsball.common.Result
 *  com.bsball.model.entity.SysTenant
 *  com.bsball.repository.SysTenantRepository
 *  lombok.Generated
 *  org.springframework.util.StringUtils
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.model.entity.SysTenant;
import com.bsball.repository.SysTenantRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Generated;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/portal/tenant"})
public class PortalTenantApi {
    private final SysTenantRepository sysTenantRepository;

    @GetMapping(value={"/check"})
    public Result<Map<String, Object>> check(@RequestParam(value="code") String code) {
        HashMap<String, Object> data = new HashMap<String, Object>(4);
        if (!StringUtils.hasText((String)code)) {
            data.put("exists", false);
            return Result.ok(data);
        }
        Optional<SysTenant> opt = this.sysTenantRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(code.trim()).filter(SysTenant::isActive);
        if (opt.isEmpty()) {
            data.put("exists", false);
            return Result.ok(data);
        }
        SysTenant t = opt.get();
        data.put("exists", true);
        data.put("id", t.getId());
        data.put("name", t.getName());
        return Result.ok(data);
    }

    @Generated
    public PortalTenantApi(SysTenantRepository sysTenantRepository) {
        this.sysTenantRepository = sysTenantRepository;
    }
}

