/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.model.entity.IpLocationCache
 *  com.bsball.model.entity.SysOperationLog
 *  com.bsball.repository.SysOperationLogRepository
 *  com.bsball.service.IpLocationCacheService
 *  com.bsball.service.SysOperationLogService
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
import com.bsball.model.entity.IpLocationCache;
import com.bsball.model.entity.SysOperationLog;
import com.bsball.repository.SysOperationLogRepository;
import com.bsball.service.IpLocationCacheService;
import com.bsball.service.TenantQueryPolicyService;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.io.Serializable;
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
public class SysOperationLogService {
    private final SysOperationLogRepository sysOperationLogRepository;
    private final IpLocationCacheService ipLocationCacheService;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    public PageResult<SysOperationLog> list(Integer page, Integer pageSize, String keyword, String module, String action, String ip, String ipRegion) {
        Page result;
        boolean hasIpRegion;
        Pageable p = this.buildPageable(page, pageSize);
        Long tid = this.tenantQueryPolicyService.tenantIdOrNullForQuery();
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasModule = module != null && !module.isBlank();
        boolean hasAction = action != null && !action.isBlank();
        boolean hasIp = ip != null && !ip.isBlank();
        boolean bl = hasIpRegion = ipRegion != null && !ipRegion.isBlank();
        if (!(tid != null || hasKeyword || hasModule || hasAction || hasIp || hasIpRegion)) {
            result = this.sysOperationLogRepository.findAll(p);
        } else {
            Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
                ArrayList<Predicate> preds = new ArrayList<Predicate>();
                if (tid != null) {
                    preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
                }
                if (hasKeyword) {
                    String k = "%" + keyword.trim().toLowerCase() + "%";
                    preds.add(cb.or(new Predicate[]{cb.like(cb.lower((Expression)root.get("username")), k), cb.like(cb.lower((Expression)root.get("module")), k), cb.like(cb.lower((Expression)root.get("description")), k)}));
                }
                if (hasModule) {
                    preds.add(cb.equal((Expression)root.get("module"), (Object)module.trim()));
                }
                if (hasAction) {
                    preds.add(cb.equal((Expression)root.get("action"), (Object)action.trim()));
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
            result = this.sysOperationLogRepository.findAll((Specification)spec, p);
        }
        List rows = result.getContent();
        this.attachOperationIpRegions(rows);
        return PageResult.of((List)rows, (long)result.getTotalElements());
    }

    private void attachOperationIpRegions(List<SysOperationLog> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List ips = rows.stream().map(SysOperationLog::getIp).filter(ip -> ip != null && !ip.isBlank()).map(String::trim).distinct().toList();
        if (ips.isEmpty()) {
            return;
        }
        Map map = this.ipLocationCacheService.findRegionTextsForListDisplay((Collection)ips);
        for (SysOperationLog row : rows) {
            String ip2 = row.getIp();
            if (ip2 == null || ip2.isBlank()) {
                row.setIpRegion(null);
                continue;
            }
            row.setIpRegion((String)map.get(ip2.trim()));
        }
    }

    private Pageable buildPageable(Integer page, Integer pageSize) {
        int pg = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        return PageRequest.of((int)(pg - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdAt"}));
    }

    @Generated
    public SysOperationLogService(SysOperationLogRepository sysOperationLogRepository, IpLocationCacheService ipLocationCacheService, TenantQueryPolicyService tenantQueryPolicyService) {
        this.sysOperationLogRepository = sysOperationLogRepository;
        this.ipLocationCacheService = ipLocationCacheService;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

