/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.model.entity.IpLocationCache
 *  com.bsball.model.entity.PortalDevtoolsReport
 *  com.bsball.repository.PortalDevtoolsReportRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.IpLocationCacheService
 *  com.bsball.service.PortalDevtoolsReportService
 *  jakarta.persistence.criteria.Expression
 *  jakarta.persistence.criteria.Predicate
 *  jakarta.persistence.criteria.Root
 *  jakarta.persistence.criteria.Subquery
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.scheduling.annotation.Async
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.model.entity.IpLocationCache;
import com.bsball.model.entity.PortalDevtoolsReport;
import com.bsball.repository.PortalDevtoolsReportRepository;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.IpLocationCacheService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class PortalDevtoolsReportService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(PortalDevtoolsReportService.class);
    private final PortalDevtoolsReportRepository repository;
    private final TenantProperties tenantProperties;
    private final IpLocationCacheService ipLocationCacheService;
    private final ApiPermissionService apiPermissionService;

    @Async
    @Transactional
    public void recordAsync(String visitorId, long tenantId, String clientIp, String path, String routeName, String userAgent, String clientMetaJson) {
        if (visitorId == null || visitorId.isBlank()) {
            return;
        }
        try {
            PortalDevtoolsReport row = new PortalDevtoolsReport();
            row.setTenantId(Long.valueOf(tenantId));
            row.setVisitorId(PortalDevtoolsReportService.truncate((String)visitorId, (int)64));
            row.setIp(PortalDevtoolsReportService.truncate((String)clientIp, (int)128));
            row.setPath(PortalDevtoolsReportService.truncate((String)path, (int)512));
            row.setRouteName(PortalDevtoolsReportService.truncate((String)routeName, (int)256));
            row.setUserAgent(PortalDevtoolsReportService.truncate((String)userAgent, (int)512));
            row.setClientMeta(PortalDevtoolsReportService.truncate((String)clientMetaJson, (int)16000));
            row.setCreatedAt(LocalDateTime.now());
            this.repository.save((Object)row);
        }
        catch (Exception e) {
            log.warn("portal_devtools_report \u5199\u5165\u5931\u8d25: {}", (Object)e.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    @Transactional(readOnly=true)
    public PageResult<PortalDevtoolsReport> list(Integer page, Integer pageSize, String keyword, String excludeKeyword, String ip, String ipRegion) {
        Page result;
        boolean hasIpRegion;
        Long opId = CurrentUserHolder.get();
        int pg = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        PageRequest p = PageRequest.of((int)(pg - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdAt"}));
        boolean superUser = opId != null && this.apiPermissionService.isSuperAdmin(opId);
        long tid = CurrentUserHolder.getTenantId() != null ? CurrentUserHolder.getTenantId().longValue() : this.tenantProperties.getDefaultId();
        boolean hasKw = keyword != null && !keyword.isBlank();
        boolean hasEx = excludeKeyword != null && !excludeKeyword.isBlank();
        boolean hasIp = ip != null && !ip.isBlank();
        boolean bl = hasIpRegion = ipRegion != null && !ipRegion.isBlank();
        if (!(!superUser || hasKw || hasEx || hasIp || hasIpRegion)) {
            result = this.repository.findAll((Pageable)p);
        } else {
            Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
                ArrayList<Predicate> preds = new ArrayList<Predicate>();
                if (!superUser) {
                    preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
                }
                if (hasKw) {
                    String k = "%" + keyword.trim().toLowerCase() + "%";
                    ArrayList<Predicate> ors = new ArrayList<Predicate>();
                    ors.add(cb.like(cb.lower((Expression)root.get("visitorId")), k));
                    ors.add(cb.like(cb.lower((Expression)root.get("ip")), k));
                    ors.add(cb.like(cb.lower((Expression)root.get("path")), k));
                    ors.add(cb.like(cb.lower((Expression)root.get("routeName")), k));
                    ors.add(cb.like(cb.lower((Expression)root.get("userAgent")), k));
                    preds.add(cb.or(ors.toArray(new Predicate[0])));
                }
                if (hasEx) {
                    String ex = "%" + excludeKeyword.trim().toLowerCase() + "%";
                    ArrayList<Predicate> exPreds = new ArrayList<Predicate>();
                    exPreds.add(cb.like(cb.lower((Expression)root.get("visitorId")), ex));
                    exPreds.add(cb.like(cb.lower((Expression)root.get("ip")), ex));
                    exPreds.add(cb.like(cb.lower((Expression)root.get("path")), ex));
                    exPreds.add(cb.like(cb.lower((Expression)root.get("routeName")), ex));
                    exPreds.add(cb.like(cb.lower((Expression)root.get("userAgent")), ex));
                    Predicate anyMatch = cb.or(exPreds.toArray(new Predicate[0]));
                    preds.add(cb.not((Expression)anyMatch));
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
            result = this.repository.findAll((Specification)spec, (Pageable)p);
        }
        List rows = result.getContent();
        this.attachDevtoolsIpRegions(rows);
        return PageResult.of((List)rows, (long)result.getTotalElements());
    }

    private void attachDevtoolsIpRegions(List<PortalDevtoolsReport> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List ips = rows.stream().map(PortalDevtoolsReport::getIp).filter(ip -> ip != null && !ip.isBlank()).map(String::trim).distinct().toList();
        if (ips.isEmpty()) {
            return;
        }
        Map map = this.ipLocationCacheService.findRegionTextsForListDisplay((Collection)ips);
        for (PortalDevtoolsReport row : rows) {
            String ip2 = row.getIp();
            if (ip2 == null || ip2.isBlank()) {
                row.setIpRegion(null);
                continue;
            }
            row.setIpRegion((String)map.get(ip2.trim()));
        }
    }

    @Generated
    public PortalDevtoolsReportService(PortalDevtoolsReportRepository repository, TenantProperties tenantProperties, IpLocationCacheService ipLocationCacheService, ApiPermissionService apiPermissionService) {
        this.repository = repository;
        this.tenantProperties = tenantProperties;
        this.ipLocationCacheService = ipLocationCacheService;
        this.apiPermissionService = apiPermissionService;
    }
}

