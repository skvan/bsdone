/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.config.TenantProperties
 *  com.bsball.model.entity.IpLocationCache
 *  com.bsball.model.entity.SysLoginLog
 *  com.bsball.repository.SysLoginLogRepository
 *  com.bsball.service.IpLocationCacheService
 *  com.bsball.service.SysLoginLogService
 *  com.bsball.service.TenantQueryPolicyService
 *  jakarta.persistence.criteria.Expression
 *  jakarta.persistence.criteria.Predicate
 *  jakarta.persistence.criteria.Root
 *  jakarta.persistence.criteria.Subquery
 *  lombok.Generated
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.config.TenantProperties;
import com.bsball.model.entity.IpLocationCache;
import com.bsball.model.entity.SysLoginLog;
import com.bsball.repository.SysLoginLogRepository;
import com.bsball.service.IpLocationCacheService;
import com.bsball.service.TenantQueryPolicyService;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class SysLoginLogService {
    private final SysLoginLogRepository sysLoginLogRepository;
    private final TenantProperties tenantProperties;
    private final IpLocationCacheService ipLocationCacheService;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    public void recordLogin(Long userId, String username, String ip, String deviceInfo, String status, String failReason, String tokenPrefix, Long tenantId) {
        SysLoginLog log = new SysLoginLog();
        long tid = tenantId != null ? tenantId.longValue() : this.tenantProperties.getDefaultId();
        log.setTenantId(Long.valueOf(tid));
        log.setUserId(userId);
        log.setUsername(username != null ? username : "");
        log.setLoginTime(LocalDateTime.now());
        log.setCreatedAt(LocalDateTime.now());
        log.setIp(ip != null ? ip : "");
        log.setDeviceInfo(deviceInfo != null ? (deviceInfo.length() > 500 ? deviceInfo.substring(0, 500) : deviceInfo) : null);
        log.setStatus(status != null ? status : "success");
        log.setFailReason(failReason);
        log.setTokenPrefix(tokenPrefix);
        this.sysLoginLogRepository.save((Object)log);
    }

    public PageResult<SysLoginLog> list(Integer page, Integer pageSize, String keyword, String status, String ip, String ipRegion) {
        Page result;
        boolean hasIpRegion;
        Pageable p = this.buildPageable(page, pageSize);
        Long tid = this.tenantQueryPolicyService.tenantIdOrNullForQuery();
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasIp = ip != null && !ip.isBlank();
        boolean bl = hasIpRegion = ipRegion != null && !ipRegion.isBlank();
        if (!(tid != null || hasKeyword || hasStatus || hasIp || hasIpRegion)) {
            result = this.sysLoginLogRepository.findAll(p);
        } else {
            Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
                ArrayList<Predicate> preds = new ArrayList<Predicate>();
                if (tid != null) {
                    preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
                }
                if (hasKeyword) {
                    String k = "%" + keyword.trim().toLowerCase() + "%";
                    preds.add(cb.or((Expression)cb.like(cb.lower((Expression)root.get("username")), k), (Expression)cb.like(cb.lower((Expression)root.get("ip")), k)));
                }
                if (hasStatus) {
                    preds.add(cb.equal((Expression)root.get("status"), (Object)status.trim()));
                }
                if (hasIp) {
                    String kIp = "%" + ip.trim().toLowerCase() + "%";
                    preds.add(cb.like(cb.lower((Expression)root.get("ip")), kIp));
                }
                if (hasIpRegion) {
                    String kRegion = "%" + ipRegion.trim().toLowerCase() + "%";
                    Subquery sq = q.subquery(String.class);
                    Root cache = sq.from(IpLocationCache.class);
                    sq.select((Expression)cache.get("ip"));
                    sq.where(new Predicate[]{cb.equal((Expression)cache.get("ip"), (Expression)root.get("ip")), cb.like(cb.lower((Expression)cache.get("regionText")), kRegion)});
                    preds.add(cb.exists(sq));
                }
                return cb.and(preds.toArray(new Predicate[0]));
            };
            result = this.sysLoginLogRepository.findAll((Specification)spec, p);
        }
        List rows = result.getContent();
        this.attachLoginIpRegions(rows);
        return PageResult.of((List)rows, (long)result.getTotalElements());
    }

    private void attachLoginIpRegions(List<SysLoginLog> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List ips = rows.stream().map(SysLoginLog::getIp).filter(ip -> ip != null && !ip.isBlank()).map(String::trim).distinct().toList();
        if (ips.isEmpty()) {
            return;
        }
        Map map = this.ipLocationCacheService.findRegionTextsForListDisplay((Collection)ips);
        for (SysLoginLog row : rows) {
            String region;
            String ip2 = row.getIp();
            if (ip2 == null || ip2.isBlank() || (region = (String)map.get(ip2.trim())) == null || region.isBlank()) continue;
            row.setLocation(region);
        }
    }

    private Pageable buildPageable(Integer page, Integer pageSize) {
        int pg = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        return PageRequest.of((int)(pg - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"loginTime"}));
    }

    @Generated
    public SysLoginLogService(SysLoginLogRepository sysLoginLogRepository, TenantProperties tenantProperties, IpLocationCacheService ipLocationCacheService, TenantQueryPolicyService tenantQueryPolicyService) {
        this.sysLoginLogRepository = sysLoginLogRepository;
        this.tenantProperties = tenantProperties;
        this.ipLocationCacheService = ipLocationCacheService;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

