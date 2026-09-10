/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.model.entity.IpLocationCache
 *  com.bsball.model.entity.PortalVisitHit
 *  com.bsball.repository.PortalVisitHitRepository
 *  com.bsball.repository.PortalVisitHitRepository$MonthlyMetricsProjection
 *  com.bsball.repository.PortalVisitHitRepository$ProvinceCityBucketCountProjection
 *  com.bsball.repository.PortalVisitHitRepository$VisitDailyProjection
 *  com.bsball.repository.PortalVisitHitRepository$VisitHourlyProjection
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.IpLocationCacheService
 *  com.bsball.service.PortalVisitService
 *  com.bsball.service.PortalVisitService$DateRange
 *  com.bsball.service.PortalVisitService$PortalVisitCityDto
 *  com.bsball.service.PortalVisitService$PortalVisitDailyDto
 *  com.bsball.service.PortalVisitService$PortalVisitProvinceCitiesDto
 *  com.bsball.service.PortalVisitService$PortalVisitProvinceDto
 *  com.bsball.service.PortalVisitService$PortalVisitProvinceSummaryDto
 *  com.bsball.service.PortalVisitService$PortalVisitSummaryDto
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
import com.bsball.model.entity.PortalVisitHit;
import com.bsball.repository.PortalVisitHitRepository;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.IpLocationCacheService;
import com.bsball.service.PortalVisitService;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Generated;
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
public class PortalVisitService {
    private final PortalVisitHitRepository portalVisitHitRepository;
    private final TenantProperties tenantProperties;
    private final IpLocationCacheService ipLocationCacheService;
    private final ApiPermissionService apiPermissionService;
    private static final int SUMMARY_MONTHLY_SERIES_THRESHOLD_DAYS = 62;

    @Async
    @Transactional
    public void recordAsync(String visitorId, String path, String clientIp, long tenantId, String userAgent) {
        if (visitorId == null || visitorId.isBlank()) {
            return;
        }
        PortalVisitHit hit = new PortalVisitHit();
        hit.setTenantId(Long.valueOf(tenantId));
        hit.setVisitorId(visitorId);
        hit.setPath(PortalVisitService.truncate((String)path, (int)512));
        hit.setUserAgent(PortalVisitService.truncate((String)userAgent, (int)512));
        hit.setIp(PortalVisitService.truncate((String)clientIp, (int)128));
        LocalDateTime now = LocalDateTime.now();
        hit.setCreatedAt(now);
        hit.setHitDate(now.toLocalDate());
        this.portalVisitHitRepository.save((Object)hit);
    }

    static DateRange resolveVisitRange(Integer days, LocalDate from, LocalDate to) {
        int d;
        if (from != null && to != null) {
            long span;
            LocalDate f = from;
            LocalDate t = to;
            if (f.isAfter(t)) {
                LocalDate x = f;
                f = t;
                t = x;
            }
            if ((span = ChronoUnit.DAYS.between(f, t) + 1L) > 366L) {
                f = t.minusDays(365L);
            }
            int inclusive = (int)ChronoUnit.DAYS.between(f, t) + 1;
            return new DateRange(f, t, inclusive);
        }
        int n = d = days == null ? 14 : days;
        if (d < 1) {
            d = 1;
        }
        if (d > 366) {
            d = 366;
        }
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays((long)d - 1L);
        return new DateRange(start, end, d);
    }

    @Transactional(readOnly=true)
    public PortalVisitSummaryDto summary(Integer days, LocalDate fromParam, LocalDate toParam, long tenantId) {
        DateRange range = PortalVisitService.resolveVisitRange((Integer)days, (LocalDate)fromParam, (LocalDate)toParam);
        LocalDate from = range.from();
        LocalDate to = range.to();
        if (range.inclusiveDays() >= 62) {
            return this.buildMonthlyVisitSummary(tenantId, from, to, range.inclusiveDays());
        }
        if (range.inclusiveDays() == 1) {
            return this.buildHourlyVisitSummary(tenantId, from);
        }
        List rows = this.portalVisitHitRepository.aggregateByDateRange(tenantId, from, to);
        LinkedHashMap<LocalDate, PortalVisitHitRepository.VisitDailyProjection> byDate = new LinkedHashMap<LocalDate, PortalVisitHitRepository.VisitDailyProjection>();
        for (PortalVisitHitRepository.VisitDailyProjection r : rows) {
            byDate.put(r.getHitDate(), r);
        }
        ArrayList<PortalVisitDailyDto> series = new ArrayList<PortalVisitDailyDto>();
        long totalPv = 0L;
        long sumDailyUv = 0L;
        LocalDate d = from;
        while (!d.isAfter(to)) {
            PortalVisitHitRepository.VisitDailyProjection r = (PortalVisitHitRepository.VisitDailyProjection)byDate.get(d);
            long pv = r == null ? 0L : r.getPv();
            long uv = r == null ? 0L : r.getUv();
            series.add(new PortalVisitDailyDto(d, null, pv, uv));
            totalPv += pv;
            sumDailyUv += uv;
            d = d.plusDays(1L);
        }
        long uniqueVisitors = this.portalVisitHitRepository.countDistinctVisitorsBetween(tenantId, from, to);
        return new PortalVisitSummaryDto(series, totalPv, uniqueVisitors, sumDailyUv, range.inclusiveDays(), "daily");
    }

    private PortalVisitSummaryDto buildHourlyVisitSummary(long tenantId, LocalDate day) {
        List rows = this.portalVisitHitRepository.aggregateByHourForDay(tenantId, day);
        HashMap<Integer, PortalVisitHitRepository.VisitHourlyProjection> byHour = new HashMap<Integer, PortalVisitHitRepository.VisitHourlyProjection>();
        for (PortalVisitHitRepository.VisitHourlyProjection r : rows) {
            if (r.getHr() == null || r.getHr() < 0 || r.getHr() > 23) continue;
            byHour.put(r.getHr(), r);
        }
        ArrayList<PortalVisitDailyDto> series = new ArrayList<PortalVisitDailyDto>();
        long totalPv = 0L;
        long sumHourlyUv = 0L;
        for (int h = 0; h < 24; ++h) {
            PortalVisitHitRepository.VisitHourlyProjection r = (PortalVisitHitRepository.VisitHourlyProjection)byHour.get(h);
            long pv = r == null ? 0L : r.getPv();
            long uv = r == null ? 0L : r.getUv();
            series.add(new PortalVisitDailyDto(day, Integer.valueOf(h), pv, uv));
            totalPv += pv;
            sumHourlyUv += uv;
        }
        long uniqueVisitors = this.portalVisitHitRepository.countDistinctVisitorsBetween(tenantId, day, day);
        return new PortalVisitSummaryDto(series, totalPv, uniqueVisitors, sumHourlyUv, 1, "hourly");
    }

    private PortalVisitSummaryDto buildMonthlyVisitSummary(long tenantId, LocalDate from, LocalDate to, int inclusiveDays) {
        List rows = this.portalVisitHitRepository.aggregateByMonthRange(tenantId, from, to);
        HashMap<YearMonth, PortalVisitHitRepository.MonthlyMetricsProjection> byYm = new HashMap<YearMonth, PortalVisitHitRepository.MonthlyMetricsProjection>();
        for (PortalVisitHitRepository.MonthlyMetricsProjection r : rows) {
            byYm.put(YearMonth.of(r.getY(), r.getM()), r);
        }
        ArrayList<PortalVisitDailyDto> series = new ArrayList<PortalVisitDailyDto>();
        long totalPv = 0L;
        YearMonth cursor = YearMonth.from(from);
        YearMonth endYm = YearMonth.from(to);
        while (!cursor.isAfter(endYm)) {
            PortalVisitHitRepository.MonthlyMetricsProjection r = (PortalVisitHitRepository.MonthlyMetricsProjection)byYm.get(cursor);
            long pv = r == null ? 0L : r.getPv();
            long uv = r == null ? 0L : r.getUv();
            series.add(new PortalVisitDailyDto(cursor.atDay(1), null, pv, uv));
            totalPv += pv;
            cursor = cursor.plusMonths(1L);
        }
        long uniqueVisitors = this.portalVisitHitRepository.countDistinctVisitorsBetween(tenantId, from, to);
        return new PortalVisitSummaryDto(series, totalPv, uniqueVisitors, 0L, inclusiveDays, "monthly");
    }

    @Transactional(readOnly=true)
    public PortalVisitProvinceSummaryDto summaryByProvince(Integer days, LocalDate fromParam, LocalDate toParam, long tenantId) {
        DateRange range = PortalVisitService.resolveVisitRange((Integer)days, (LocalDate)fromParam, (LocalDate)toParam);
        LocalDate f = range.from();
        LocalDate t = range.to();
        List rows = this.portalVisitHitRepository.aggregateMetricsByProvince(tenantId, f, t);
        Map<String, Long> cityBuckets = this.portalVisitHitRepository.countDistinctCityBucketsByProvince(tenantId, f, t).stream().collect(Collectors.toMap(PortalVisitHitRepository.ProvinceCityBucketCountProjection::getProvinceName, PortalVisitHitRepository.ProvinceCityBucketCountProjection::getCityBucketCount, (a, b) -> a));
        List items = rows.stream().map(r -> new PortalVisitProvinceDto(r.getProvinceName(), r.getPv(), r.getUv(), cityBuckets.getOrDefault(r.getProvinceName(), 0L).longValue())).collect(Collectors.toList());
        return new PortalVisitProvinceSummaryDto(items, range.inclusiveDays());
    }

    @Transactional(readOnly=true)
    public PortalVisitProvinceCitiesDto summaryByProvinceCities(String province, Integer days, LocalDate fromParam, LocalDate toParam, long tenantId) {
        DateRange range = PortalVisitService.resolveVisitRange((Integer)days, (LocalDate)fromParam, (LocalDate)toParam);
        if (province == null || province.isBlank()) {
            return new PortalVisitProvinceCitiesDto("", List.of(), range.inclusiveDays());
        }
        LocalDate f = range.from();
        LocalDate t = range.to();
        String prov = province.trim();
        List rows = this.portalVisitHitRepository.aggregateMetricsByProvinceAndCity(tenantId, f, t, prov);
        List items = rows.stream().map(r -> new PortalVisitCityDto(r.getCityName(), r.getPv(), r.getUv())).collect(Collectors.toList());
        return new PortalVisitProvinceCitiesDto(prov, items, range.inclusiveDays());
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
    public PageResult<PortalVisitHit> listHits(Integer page, Integer pageSize, String keyword, String excludeKeyword, String ip, String ipRegion, LocalDate from, LocalDate to) {
        Page result;
        boolean hasDate;
        Long opId = CurrentUserHolder.get();
        int pg = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        PageRequest p = PageRequest.of((int)(pg - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdAt"}));
        boolean superUser = opId != null && this.apiPermissionService.isSuperAdmin(opId);
        long tid = CurrentUserHolder.getTenantId() != null ? CurrentUserHolder.getTenantId().longValue() : this.tenantProperties.getDefaultId();
        boolean hasKw = keyword != null && !keyword.isBlank();
        boolean hasEx = excludeKeyword != null && !excludeKeyword.isBlank();
        boolean hasIp = ip != null && !ip.isBlank();
        boolean hasIpRegion = ipRegion != null && !ipRegion.isBlank();
        boolean bl = hasDate = from != null || to != null;
        if (!(!superUser || hasDate || hasKw || hasEx || hasIp || hasIpRegion)) {
            result = this.portalVisitHitRepository.findAll((Pageable)p);
        } else {
            Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
                ArrayList<Predicate> preds = new ArrayList<Predicate>();
                if (!superUser) {
                    preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
                }
                if (from != null) {
                    preds.add(cb.greaterThanOrEqualTo((Expression)root.get("hitDate"), (Comparable)from));
                }
                if (to != null) {
                    preds.add(cb.lessThanOrEqualTo((Expression)root.get("hitDate"), (Comparable)to));
                }
                if (hasKw) {
                    String k = "%" + keyword.trim().toLowerCase() + "%";
                    ArrayList<Predicate> ors = new ArrayList<Predicate>();
                    ors.add(cb.like(cb.lower((Expression)root.get("visitorId")), k));
                    ors.add(cb.like(cb.lower((Expression)root.get("ip")), k));
                    ors.add(cb.like(cb.lower((Expression)root.get("path")), k));
                    ors.add(cb.like(cb.lower((Expression)root.get("userAgent")), k));
                    Subquery sqKwRegion = q.subquery(String.class);
                    Root cacheKw = sqKwRegion.from(IpLocationCache.class);
                    sqKwRegion.select((Expression)cacheKw.get("ip"));
                    sqKwRegion.where(new Predicate[]{cb.equal((Expression)cacheKw.get("ip"), (Expression)root.get("ip")), cb.like(cb.lower((Expression)cacheKw.get("regionText")), k)});
                    ors.add(cb.exists(sqKwRegion));
                    preds.add(cb.or(ors.toArray(new Predicate[0])));
                }
                if (hasEx) {
                    String ex = "%" + excludeKeyword.trim().toLowerCase() + "%";
                    ArrayList<Predicate> exPreds = new ArrayList<Predicate>();
                    exPreds.add(cb.like(cb.lower((Expression)root.get("visitorId")), ex));
                    exPreds.add(cb.like(cb.lower((Expression)root.get("ip")), ex));
                    exPreds.add(cb.like(cb.lower((Expression)root.get("path")), ex));
                    exPreds.add(cb.like(cb.lower((Expression)root.get("userAgent")), ex));
                    Subquery sqExRegion = q.subquery(String.class);
                    Root cacheEx = sqExRegion.from(IpLocationCache.class);
                    sqExRegion.select((Expression)cacheEx.get("ip"));
                    sqExRegion.where(new Predicate[]{cb.equal((Expression)cacheEx.get("ip"), (Expression)root.get("ip")), cb.like(cb.lower((Expression)cacheEx.get("regionText")), ex)});
                    exPreds.add(cb.exists(sqExRegion));
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
            result = this.portalVisitHitRepository.findAll((Specification)spec, (Pageable)p);
        }
        List rows = result.getContent();
        this.attachVisitHitIpRegions(rows);
        return PageResult.of((List)rows, (long)result.getTotalElements());
    }

    private void attachVisitHitIpRegions(List<PortalVisitHit> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List ips = rows.stream().map(PortalVisitHit::getIp).filter(ip -> ip != null && !ip.isBlank()).map(String::trim).distinct().toList();
        if (ips.isEmpty()) {
            return;
        }
        Map map = this.ipLocationCacheService.findRegionTextsForListDisplay((Collection)ips);
        for (PortalVisitHit row : rows) {
            String ip2 = row.getIp();
            if (ip2 == null || ip2.isBlank()) {
                row.setIpRegion(null);
                continue;
            }
            row.setIpRegion((String)map.get(ip2.trim()));
        }
    }

    @Generated
    public PortalVisitService(PortalVisitHitRepository portalVisitHitRepository, TenantProperties tenantProperties, IpLocationCacheService ipLocationCacheService, ApiPermissionService apiPermissionService) {
        this.portalVisitHitRepository = portalVisitHitRepository;
        this.tenantProperties = tenantProperties;
        this.ipLocationCacheService = ipLocationCacheService;
        this.apiPermissionService = apiPermissionService;
    }
}

