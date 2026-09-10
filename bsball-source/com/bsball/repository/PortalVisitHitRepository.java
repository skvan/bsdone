/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.PortalVisitHit
 *  com.bsball.repository.PortalVisitHitRepository
 *  com.bsball.repository.PortalVisitHitRepository$CityMetricsProjection
 *  com.bsball.repository.PortalVisitHitRepository$MonthlyMetricsProjection
 *  com.bsball.repository.PortalVisitHitRepository$ProvinceCityBucketCountProjection
 *  com.bsball.repository.PortalVisitHitRepository$ProvinceMetricsProjection
 *  com.bsball.repository.PortalVisitHitRepository$VisitDailyProjection
 *  com.bsball.repository.PortalVisitHitRepository$VisitHourlyProjection
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package com.bsball.repository;

import com.bsball.model.entity.PortalVisitHit;
import com.bsball.repository.PortalVisitHitRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PortalVisitHitRepository
extends JpaRepository<PortalVisitHit, Long>,
JpaSpecificationExecutor<PortalVisitHit> {
    @Query(value="SELECT DISTINCT h.ip FROM PortalVisitHit h WHERE h.ip IS NOT NULL AND h.ip <> ''")
    public List<String> findDistinctNonBlankIps();

    @Query(value="SELECT DISTINCT h.ip FROM PortalVisitHit h WHERE h.ip IS NOT NULL AND h.ip <> '' AND h.hitDate >= :fromDate")
    public List<String> findDistinctNonBlankIpsSince(@Param(value="fromDate") LocalDate var1);

    @Query(value="SELECT DISTINCT h.ip\nFROM PortalVisitHit h\nLEFT JOIN IpLocationCache c ON h.ip = c.ip\nWHERE h.ip IS NOT NULL AND h.ip <> ''\n  AND h.hitDate >= :fromDate\n  AND (c.ip IS NULL OR c.fetchedAt IS NULL OR c.fetchedAt < :staleThreshold)\n")
    public List<String> findDistinctIpsWithoutValidCache(@Param(value="fromDate") LocalDate var1, @Param(value="staleThreshold") LocalDateTime var2);

    @Query(value="SELECT h.hitDate AS hitDate, COUNT(h) AS pv, COUNT(DISTINCT h.visitorId) AS uv\nFROM PortalVisitHit h\nWHERE (:tenantId = 0 OR h.tenantId = :tenantId) AND h.hitDate >= :from AND h.hitDate <= :to\nGROUP BY h.hitDate\nORDER BY h.hitDate\n")
    public List<VisitDailyProjection> aggregateByDateRange(@Param(value="tenantId") long var1, @Param(value="from") LocalDate var3, @Param(value="to") LocalDate var4);

    @Query(value="SELECT CAST(EXTRACT(HOUR FROM created_at) AS integer) AS hr,\n       COUNT(*) AS pv,\n       COUNT(DISTINCT visitor_id) AS uv\nFROM portal_visit_hit\nWHERE (:tenantId = 0 OR tenant_id = :tenantId) AND hit_date = :day\nGROUP BY CAST(EXTRACT(HOUR FROM created_at) AS integer)\nORDER BY hr\n", nativeQuery=true)
    public List<VisitHourlyProjection> aggregateByHourForDay(@Param(value="tenantId") long var1, @Param(value="day") LocalDate var3);

    @Query(value="SELECT CAST(date_part('year', hit_date) AS int) AS y,\n       CAST(date_part('month', hit_date) AS int) AS m,\n       COUNT(*) AS pv,\n       COUNT(DISTINCT visitor_id) AS uv\nFROM portal_visit_hit\nWHERE (:tenantId = 0 OR tenant_id = :tenantId) AND hit_date >= :from AND hit_date <= :to\nGROUP BY date_part('year', hit_date), date_part('month', hit_date)\nORDER BY y, m\n", nativeQuery=true)
    public List<MonthlyMetricsProjection> aggregateByMonthRange(@Param(value="tenantId") long var1, @Param(value="from") LocalDate var3, @Param(value="to") LocalDate var4);

    @Query(value="SELECT COUNT(DISTINCT h.visitorId) FROM PortalVisitHit h WHERE (:tenantId = 0 OR h.tenantId = :tenantId) AND h.hitDate >= :from AND h.hitDate <= :to")
    public long countDistinctVisitorsBetween(@Param(value="tenantId") long var1, @Param(value="from") LocalDate var3, @Param(value="to") LocalDate var4);

    @Query(value="SELECT c.province AS provinceName, COUNT(h) AS pv, COUNT(DISTINCT h.visitorId) AS uv\nFROM PortalVisitHit h\nINNER JOIN IpLocationCache c ON h.ip = c.ip\nWHERE (:tenantId = 0 OR h.tenantId = :tenantId) AND h.hitDate >= :from AND h.hitDate <= :to\n  AND c.province IS NOT NULL AND c.province <> ''\nGROUP BY c.province\nORDER BY uv DESC\n")
    public List<ProvinceMetricsProjection> aggregateMetricsByProvince(@Param(value="tenantId") long var1, @Param(value="from") LocalDate var3, @Param(value="to") LocalDate var4);

    @Query(value="SELECT c.province AS provinceName,\n       COUNT(DISTINCT COALESCE(NULLIF(TRIM(BOTH FROM c.city), ''), '\u5176\u4ed6')) AS cityBucketCount\nFROM portal_visit_hit h\nINNER JOIN ip_location_cache c ON h.ip = c.ip\nWHERE (:tenantId = 0 OR h.tenant_id = :tenantId) AND h.hit_date >= :from AND h.hit_date <= :to\n  AND c.province IS NOT NULL AND c.province <> ''\nGROUP BY c.province\n", nativeQuery=true)
    public List<ProvinceCityBucketCountProjection> countDistinctCityBucketsByProvince(@Param(value="tenantId") long var1, @Param(value="from") LocalDate var3, @Param(value="to") LocalDate var4);

    @Query(value="SELECT CASE WHEN c.city IS NULL OR TRIM(c.city) = '' THEN '\u5176\u4ed6' ELSE TRIM(c.city) END AS cityName,\n       COUNT(h) AS pv, COUNT(DISTINCT h.visitorId) AS uv\nFROM PortalVisitHit h\nINNER JOIN IpLocationCache c ON h.ip = c.ip\nWHERE (:tenantId = 0 OR h.tenantId = :tenantId) AND h.hitDate >= :from AND h.hitDate <= :to\n  AND c.province = :province\nGROUP BY CASE WHEN c.city IS NULL OR TRIM(c.city) = '' THEN '\u5176\u4ed6' ELSE TRIM(c.city) END\nORDER BY uv DESC\n")
    public List<CityMetricsProjection> aggregateMetricsByProvinceAndCity(@Param(value="tenantId") long var1, @Param(value="from") LocalDate var3, @Param(value="to") LocalDate var4, @Param(value="province") String var5);
}

