/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.model.entity.IpLocationCache
 *  com.bsball.model.vo.IpLocationCacheRowVo
 *  com.bsball.repository.IpLocationCacheRepository
 *  com.bsball.service.IpLocationCacheService
 *  com.bsball.service.PortalVisitProvinceNormalizer
 *  com.bsball.service.iplocation.IpLocationDetail
 *  jakarta.persistence.criteria.Expression
 *  jakarta.persistence.criteria.Predicate
 *  lombok.Generated
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.domain.Sort$Order
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.model.entity.IpLocationCache;
import com.bsball.model.vo.IpLocationCacheRowVo;
import com.bsball.repository.IpLocationCacheRepository;
import com.bsball.service.PortalVisitProvinceNormalizer;
import com.bsball.service.iplocation.IpLocationDetail;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class IpLocationCacheService {
    private final IpLocationCacheRepository repository;

    @Transactional(readOnly=true)
    public Optional<String> findValidRegion(String ip, int staleDays) {
        return this.repository.findById((Object)ip).filter(row -> !IpLocationCacheService.isStale((LocalDateTime)row.getFetchedAt(), (int)staleDays)).map(IpLocationCache::getRegionText);
    }

    @Transactional(readOnly=true)
    public Optional<String> findRegionTextRow(String ip) {
        if (ip == null || ip.isBlank()) {
            return Optional.empty();
        }
        return this.repository.findById((Object)ip.trim()).map(IpLocationCache::getRegionText).filter(s -> s != null && !s.isBlank());
    }

    @Transactional(readOnly=true)
    public Optional<String> findProvinceRow(String ip) {
        if (ip == null || ip.isBlank()) {
            return Optional.empty();
        }
        return this.repository.findById((Object)ip.trim()).map(IpLocationCache::getProvince).filter(s -> s != null && !s.isBlank());
    }

    @Transactional(readOnly=true)
    public Map<String, String> findValidRegionsByIps(Collection<String> ips, int staleDays) {
        if (ips == null || ips.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        for (IpLocationCache row : this.repository.findAllById(ips)) {
            if (IpLocationCacheService.isStale((LocalDateTime)row.getFetchedAt(), (int)staleDays)) continue;
            out.put(row.getIp(), row.getRegionText());
        }
        return out;
    }

    @Transactional(readOnly=true)
    public Map<String, String> findRegionTextsForListDisplay(Collection<String> ips) {
        if (ips == null || ips.isEmpty()) {
            return Map.of();
        }
        LinkedHashSet<String> uniq = new LinkedHashSet<String>();
        for (String ip : ips) {
            if (ip == null || ip.isBlank()) continue;
            uniq.add(ip.trim());
        }
        if (uniq.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        for (IpLocationCache row : this.repository.findAllById(uniq)) {
            String text = row.getRegionText();
            if (text == null || text.isBlank()) continue;
            out.put(row.getIp(), text);
        }
        return out;
    }

    @Transactional(readOnly=true)
    public PageResult<IpLocationCacheRowVo> page(Integer page, Integer pageSize, String keyword, String sortField, String sortOrder) {
        int pg = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        String sf = IpLocationCacheService.normalizeSortField((String)sortField);
        boolean asc = "asc".equalsIgnoreCase(sortOrder == null ? "" : sortOrder.trim());
        Sort sort = IpLocationCacheService.buildListSort((String)sf, (boolean)asc);
        PageRequest p = PageRequest.of((int)(pg - 1), (int)ps, (Sort)sort);
        Page result = this.repository.findAll(IpLocationCacheService.keywordSpecification((String)keyword), (Pageable)p);
        List rows = result.getContent().stream().map(arg_0 -> this.toRowVo(arg_0)).toList();
        return PageResult.of((List)rows, (long)result.getTotalElements());
    }

    private static String normalizeSortField(String raw) {
        if (raw == null || raw.isBlank()) {
            return "createdAt";
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "fetchedat" -> "fetchedAt";
            case "resolvecount" -> "resolveCount";
            case "createdat" -> "createdAt";
            default -> "createdAt";
        };
    }

    private static Sort buildListSort(String sortField, boolean asc) {
        Sort.Direction d = asc ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort tieBreak = Sort.by((Sort.Direction)Sort.Direction.ASC, (String[])new String[]{"ip"});
        if ("fetchedAt".equals(sortField)) {
            return Sort.by((Sort.Direction)d, (String[])new String[]{"fetchedAt"}).and(tieBreak);
        }
        if ("resolveCount".equals(sortField)) {
            return Sort.by((Sort.Direction)d, (String[])new String[]{"resolveCount"}).and(tieBreak);
        }
        Sort.Order byCreated = asc ? Sort.Order.asc((String)"createdAt").nullsLast() : Sort.Order.desc((String)"createdAt").nullsLast();
        Sort.Order byFetched = asc ? Sort.Order.asc((String)"fetchedAt") : Sort.Order.desc((String)"fetchedAt");
        return Sort.by((Sort.Order[])new Sort.Order[]{byCreated, byFetched}).and(tieBreak);
    }

    private static Specification<IpLocationCache> keywordSpecification(String keyword) {
        return (Specification & Serializable)(root, q, cb) -> {
            boolean hasKw;
            boolean bl = hasKw = keyword != null && !keyword.isBlank();
            if (!hasKw) {
                return cb.isNotNull((Expression)root.get("ip"));
            }
            String k = "%" + keyword.trim().toLowerCase() + "%";
            ArrayList ors = new ArrayList();
            ors.add(cb.like(cb.lower((Expression)root.get("ip")), k));
            ors.add(cb.like(cb.lower((Expression)root.get("regionText")), k));
            ors.add(cb.like(cb.lower((Expression)root.get("province")), k));
            ors.add(cb.like(cb.lower((Expression)root.get("city")), k));
            ors.add(cb.like(cb.lower((Expression)root.get("adcode")), k));
            ors.add(cb.like(cb.lower(root.get("lbsProvider").as(String.class)), k));
            return cb.or((Predicate[])ors.toArray(Predicate[]::new));
        };
    }

    private IpLocationCacheRowVo toRowVo(IpLocationCache e) {
        IpLocationCacheRowVo v = new IpLocationCacheRowVo();
        v.setIp(e.getIp());
        v.setRegionText(e.getRegionText());
        v.setProvince(e.getProvince());
        v.setCity(e.getCity());
        v.setAdcode(e.getAdcode());
        v.setLbsProvider(e.getLbsProvider() != null ? e.getLbsProvider().name() : null);
        v.setFetchedAt(e.getFetchedAt() != null ? e.getFetchedAt().toString() : null);
        v.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        v.setResolveCount(Integer.valueOf(e.getResolveCount()));
        LocalDateTime effCreated = e.getCreatedAt() != null ? e.getCreatedAt() : e.getFetchedAt();
        v.setCreatedAtDisplay(effCreated != null ? effCreated.toString() : null);
        return v;
    }

    @Transactional
    public void upsert(String ip, IpLocationDetail detail) {
        this.upsert(ip, detail, false);
    }

    @Transactional
    public void upsert(String ip, IpLocationDetail detail, boolean adminTriggeredRefresh) {
        Optional existing = this.repository.findById((Object)ip);
        IpLocationCache row = existing.orElseGet(IpLocationCache::new);
        boolean isNew = existing.isEmpty();
        LocalDateTime now = LocalDateTime.now();
        row.setIp(ip);
        row.setRegionText(detail.formatted());
        String mapProv = PortalVisitProvinceNormalizer.normalizeForChinaMap((String)detail.formatted());
        row.setProvince(mapProv != null ? mapProv : detail.province());
        row.setCity(detail.city());
        row.setAdcode(detail.adcode());
        row.setLbsProvider(detail.provider());
        row.setRectangle(detail.rectangle());
        row.setLocation(detail.location());
        row.setFetchedAt(now);
        if (isNew) {
            row.setCreatedAt(now);
            row.setResolveCount(adminTriggeredRefresh ? 1 : 0);
        } else if (adminTriggeredRefresh) {
            row.setResolveCount(row.getResolveCount() + 1);
        }
        this.repository.save((Object)row);
    }

    @Transactional
    public void upsertBatch(Map<String, IpLocationDetail> detailsByIp) {
        if (detailsByIp == null || detailsByIp.isEmpty()) {
            return;
        }
        LinkedHashSet<String> ips = new LinkedHashSet<String>(detailsByIp.keySet());
        LinkedHashMap<String, IpLocationCache> existing = new LinkedHashMap<String, IpLocationCache>();
        for (IpLocationCache row : this.repository.findAllById(ips)) {
            existing.put(row.getIp(), row);
        }
        LocalDateTime now = LocalDateTime.now();
        ArrayList<IpLocationCache> rows = new ArrayList<IpLocationCache>(detailsByIp.size());
        for (Map.Entry<String, IpLocationDetail> e : detailsByIp.entrySet()) {
            String ip = e.getKey();
            IpLocationDetail detail = e.getValue();
            if (ip == null || ip.isBlank() || detail == null) continue;
            boolean isNew = !existing.containsKey(ip);
            IpLocationCache row = existing.getOrDefault(ip, new IpLocationCache());
            row.setIp(ip);
            row.setRegionText(detail.formatted());
            String mapProv = PortalVisitProvinceNormalizer.normalizeForChinaMap((String)detail.formatted());
            row.setProvince(mapProv != null ? mapProv : detail.province());
            row.setCity(detail.city());
            row.setAdcode(detail.adcode());
            row.setLbsProvider(detail.provider());
            row.setRectangle(detail.rectangle());
            row.setLocation(detail.location());
            row.setFetchedAt(now);
            if (isNew) {
                row.setCreatedAt(now);
                row.setResolveCount(0);
            }
            rows.add(row);
        }
        if (!rows.isEmpty()) {
            this.repository.saveAll(rows);
        }
    }

    private static boolean isStale(LocalDateTime fetchedAt, int staleDays) {
        if (staleDays <= 0 || fetchedAt == null) {
            return false;
        }
        return fetchedAt.plusDays(staleDays).isBefore(LocalDateTime.now());
    }

    @Generated
    public IpLocationCacheService(IpLocationCacheRepository repository) {
        this.repository = repository;
    }
}

