/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.dto.StadiumHomeTeamInputDto
 *  com.bsball.model.dto.StadiumHomeTeamViewDto
 *  com.bsball.model.dto.StadiumUpsertDto
 *  com.bsball.model.dto.StadiumViewDto
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.ChinaRegion
 *  com.bsball.model.entity.Stadium
 *  com.bsball.model.entity.StadiumHomeTeam
 *  com.bsball.model.entity.Team
 *  com.bsball.model.enums.StadiumLevel
 *  com.bsball.repository.ChinaRegionRepository
 *  com.bsball.repository.StadiumRepository
 *  com.bsball.repository.TeamRepository
 *  com.bsball.service.StadiumService
 *  com.bsball.service.TenantQueryPolicyService
 *  com.bsball.utils.ChinaAdcodeUtils
 *  jakarta.persistence.EntityManager
 *  jakarta.persistence.PersistenceContext
 *  jakarta.persistence.criteria.Expression
 *  jakarta.persistence.criteria.Predicate
 *  lombok.Generated
 *  org.locationtech.jts.geom.Coordinate
 *  org.locationtech.jts.geom.GeometryFactory
 *  org.locationtech.jts.geom.Point
 *  org.locationtech.jts.geom.PrecisionModel
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.StadiumHomeTeamInputDto;
import com.bsball.model.dto.StadiumHomeTeamViewDto;
import com.bsball.model.dto.StadiumUpsertDto;
import com.bsball.model.dto.StadiumViewDto;
import com.bsball.model.entity.BaseEntity;
import com.bsball.model.entity.ChinaRegion;
import com.bsball.model.entity.Stadium;
import com.bsball.model.entity.StadiumHomeTeam;
import com.bsball.model.entity.Team;
import com.bsball.model.enums.StadiumLevel;
import com.bsball.repository.ChinaRegionRepository;
import com.bsball.repository.StadiumRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.TenantQueryPolicyService;
import com.bsball.utils.ChinaAdcodeUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Generated;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
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
public class StadiumService {
    private static final GeometryFactory GEOMETRY_4326 = new GeometryFactory(new PrecisionModel(), 4326);
    private final StadiumRepository stadiumRepository;
    private final TeamRepository teamRepository;
    private final ChinaRegionRepository chinaRegionRepository;
    private final TenantQueryPolicyService tenantQueryPolicyService;
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly=true)
    public PageResult<StadiumViewDto> list(Integer page, Integer pageSize, String sortProp, String sortOrder, String keyword, StadiumLevel level) {
        Pageable p = this.buildPageable(page, pageSize, sortProp, sortOrder);
        Long tenantId = this.tenantQueryPolicyService.tenantIdOrNullForQuery();
        Specification spec = this.buildSpec(keyword, level, tenantId);
        Page result = this.stadiumRepository.findAll(spec, p);
        Map teamNames = this.loadTeamNames((Collection)result.getContent());
        List views = result.getContent().stream().map(s -> this.toView(s, teamNames)).toList();
        return PageResult.of((List)views, (long)result.getTotalElements());
    }

    @Transactional(readOnly=true)
    public StadiumViewDto get(Long id) {
        Stadium s = this.findActive(id);
        if (s == null) {
            return null;
        }
        Map teamNames = this.loadTeamNames((Collection)List.of((Object)s));
        return this.toView(s, teamNames);
    }

    @Transactional(rollbackFor={Exception.class})
    public StadiumViewDto create(StadiumUpsertDto dto) {
        Stadium s = new Stadium();
        s.setTenantId(Long.valueOf(this.tenantQueryPolicyService.requiredTenantId()));
        this.applyDto(s, dto);
        StadiumService.normalizeStrings((Stadium)s);
        Stadium saved = (Stadium)this.stadiumRepository.save((Object)s);
        Map teamNames = this.loadTeamNames((Collection)List.of((Object)saved));
        return this.toView(saved, teamNames);
    }

    @Transactional(rollbackFor={Exception.class})
    public StadiumViewDto update(Long id, StadiumUpsertDto dto) {
        Stadium existing = this.findActive(id);
        if (existing == null) {
            return null;
        }
        this.applyDto(existing, dto);
        StadiumService.normalizeStrings((Stadium)existing);
        Stadium saved = (Stadium)this.stadiumRepository.save((Object)existing);
        Map teamNames = this.loadTeamNames((Collection)List.of((Object)saved));
        return this.toView(saved, teamNames);
    }

    @Transactional(rollbackFor={Exception.class})
    public void delete(Long id) {
        Stadium s = this.findActive(id);
        if (s == null) {
            return;
        }
        Long uid = CurrentUserHolder.get();
        LocalDateTime now = LocalDateTime.now();
        s.setDeletedAt(now);
        s.setDeletedBy(uid);
        for (StadiumHomeTeam ht : s.getHomeTeams()) {
            ht.setDeletedAt(now);
            ht.setDeletedBy(uid);
        }
        this.stadiumRepository.save((Object)s);
    }

    @Transactional(readOnly=true)
    public Map<String, Object> buildGeoJsonFeatureCollection() {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> cb.and(new Predicate[]{cb.isNull((Expression)root.get("deletedAt")), cb.isNotNull((Expression)root.get("location")), cb.equal((Expression)root.get("tenantId"), (Object)tid)});
        List list = this.stadiumRepository.findAll((Specification)spec);
        Map teamNames = this.loadTeamNames((Collection)list);
        ArrayList features = new ArrayList();
        for (Stadium s : list) {
            Point loc = s.getLocation();
            if (loc == null) continue;
            LinkedHashMap<String, Object> props = new LinkedHashMap<String, Object>();
            props.put("id", s.getId());
            props.put("name", Optional.ofNullable(s.getName()).orElse(""));
            props.put("shortName", s.getShortName());
            props.put("level", s.getLevel() != null ? s.getLevel().name() : null);
            props.put("operatingStatus", s.getOperatingStatus() != null ? s.getOperatingStatus().name() : null);
            props.put("addrCity", s.getAddrCity());
            props.put("coordSys", "GCJ02");
            props.put("homeTeamLabels", StadiumService.homeTeamLabels((Stadium)s, (Map)teamNames));
            LinkedHashMap<String, Object> feature = new LinkedHashMap<String, Object>();
            feature.put("type", "Feature");
            feature.put("id", s.getId());
            feature.put("geometry", StadiumService.pointToGeoJson((Point)loc));
            feature.put("properties", props);
            features.add(feature);
        }
        LinkedHashMap<String, Object> root2 = new LinkedHashMap<String, Object>();
        root2.put("type", "FeatureCollection");
        root2.put("features", features);
        return root2;
    }

    @Transactional(readOnly=true)
    public List<StadiumViewDto> listNearby(double longitude, double latitude, double radiusMeters) {
        if (radiusMeters <= 0.0 || radiusMeters > 500000.0) {
            throw new BusinessException(400, "radiusMeters \u9700\u5728 (0, 500000] \u5185");
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        List raw = this.entityManager.createNativeQuery("SELECT s.id FROM bs_stadium s\nWHERE s.deleted_at IS NULL AND s.location IS NOT NULL\n  AND s.tenant_id = :tid\n  AND ST_DWithin(\n    s.location::geography,\n    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,\n    :radius\n  )\nORDER BY ST_Distance(\n    s.location::geography,\n    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography\n)\n").setParameter("tid", (Object)tid).setParameter("lng", (Object)longitude).setParameter("lat", (Object)latitude).setParameter("radius", (Object)radiusMeters).getResultList();
        List ids = raw.stream().map(o -> ((Number)o).longValue()).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        List list = this.stadiumRepository.findAllById((Iterable)ids);
        Map<Long, Stadium> byId = list.stream().collect(Collectors.toMap(BaseEntity::getId, x -> x));
        Map teamNames = this.loadTeamNames((Collection)list);
        ArrayList<StadiumViewDto> out = new ArrayList<StadiumViewDto>();
        for (Long sid : ids) {
            Stadium s = byId.get(sid);
            if (s == null || s.getDeletedAt() != null) continue;
            out.add(this.toView(s, teamNames));
        }
        return out;
    }

    private Stadium findActive(Long id) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        return this.stadiumRepository.findById((Object)id).filter(s -> s.getDeletedAt() == null && Objects.equals(s.getTenantId(), tid)).orElse(null);
    }

    private void applyDto(Stadium s, StadiumUpsertDto dto) {
        this.setLocationFromDto(s, dto.getLongitude(), dto.getLatitude());
        s.setName(dto.getName());
        s.setShortName(dto.getShortName());
        s.setShortNameEn(dto.getShortNameEn());
        boolean updating = s.getId() != null;
        String adcodeIn = dto.getAddrDistrictAdcode();
        if (adcodeIn != null && !adcodeIn.isBlank()) {
            this.applyDistrictAdcode(s, adcodeIn);
        } else if (!updating) {
            throw new BusinessException(400, "\u8bf7\u9009\u62e9\u7701/\u5e02/\u533a\u53bf");
        }
        s.setPostalCode(dto.getPostalCode());
        s.setAddressDetail(dto.getAddressDetail());
        s.setLevel(dto.getLevel());
        s.setOperatingStatus(dto.getOperatingStatus());
        s.setCapacityTotal(dto.getCapacityTotal());
        s.setSeatingCapacity(dto.getSeatingCapacity());
        s.setRecordAttendance(dto.getRecordAttendance());
        s.setFieldDistanceLeftM(dto.getFieldDistanceLeftM());
        s.setFieldDistanceCenterM(dto.getFieldDistanceCenterM());
        s.setFieldDistanceRightM(dto.getFieldDistanceRightM());
        s.setTurfType(dto.getTurfType());
        s.setRoofType(dto.getRoofType());
        s.setContactPhone(dto.getContactPhone());
        s.setHasLargeScreen(dto.getHasLargeScreen());
        s.setIntroduction(dto.getIntroduction());
        s.setTransportationInfo(dto.getTransportationInfo());
        s.setLayoutDiagramUrl(dto.getLayoutDiagramUrl());
        s.setIntroImageUrl(dto.getIntroImageUrl());
        s.setConstructionEra(dto.getConstructionEra());
        s.setOpenedOn(dto.getOpenedOn());
        s.setImportantDatesNote(dto.getImportantDatesNote());
        if (dto.getHomeTeams() != null) {
            this.validateTeamIdsForTenant(dto.getHomeTeams().stream().map(StadiumHomeTeamInputDto::getTeamId).toList(), s.getTenantId().longValue());
            s.getHomeTeams().clear();
            int order = 0;
            for (StadiumHomeTeamInputDto row : dto.getHomeTeams()) {
                StadiumHomeTeam link = new StadiumHomeTeam();
                link.setStadium(s);
                link.setTeamId(row.getTeamId());
                link.setEffectiveFrom(row.getEffectiveFrom());
                link.setEffectiveTo(row.getEffectiveTo());
                link.setSortOrder(Integer.valueOf(row.getSortOrder() != null ? row.getSortOrder() : order++));
                s.getHomeTeams().add(link);
            }
        }
    }

    private void applyDistrictAdcode(Stadium s, String rawDistrictAdcode) {
        String dNorm = ChinaAdcodeUtils.normalize((String)rawDistrictAdcode);
        if (dNorm == null || dNorm.length() < 6) {
            throw new BusinessException(400, "\u533a\u53bf\u533a\u5212\u4ee3\u7801\u65e0\u6548");
        }
        ChinaRegion dist = (ChinaRegion)this.chinaRegionRepository.findByAdcode(dNorm).orElseThrow(() -> new BusinessException(400, "\u65e0\u6548\u7684\u533a\u53bf\u4ee3\u7801: " + dNorm));
        if (dist.getLevel() != 3) {
            throw new BusinessException(400, "\u8bf7\u9009\u62e9\u533a\u53bf\u4e00\u7ea7\u533a\u5212");
        }
        ChinaRegion city = (ChinaRegion)this.chinaRegionRepository.findByAdcode(dist.getParentAdcode()).orElseThrow(() -> new BusinessException(400, "\u533a\u53bf\u5bf9\u5e94\u4e0a\u7ea7\u5730\u5e02\u4e0d\u5b58\u5728\uff0c\u8bf7\u786e\u8ba4\u533a\u5212\u6570\u636e\u5df2\u5bfc\u5165"));
        ChinaRegion prov = (ChinaRegion)this.chinaRegionRepository.findByAdcode(city.getParentAdcode()).orElseThrow(() -> new BusinessException(400, "\u5730\u5e02\u5bf9\u5e94\u4e0a\u7ea7\u7701\u4e0d\u5b58\u5728\uff0c\u8bf7\u786e\u8ba4\u533a\u5212\u6570\u636e\u5df2\u5bfc\u5165"));
        s.setAddrDistrictAdcode(dNorm);
        s.setAddrProvince(prov.getName());
        s.setAddrCity(city.getName());
        s.setAddrDistrict(dist.getName());
    }

    private void validateTeamIdsForTenant(List<Long> teamIds, long stadiumTenantId) {
        if (teamIds == null) {
            return;
        }
        for (Long teamId : teamIds) {
            if (teamId == null || teamId <= 0L) {
                throw new BusinessException(400, "\u4e3b\u573a\u7403\u961f ID \u65e0\u6548");
            }
            Team t = this.teamRepository.findById((Object)teamId).orElse(null);
            if (t == null) {
                throw new BusinessException(400, "\u4e3b\u573a\u7403\u961f\u4e0d\u5b58\u5728: " + teamId);
            }
            if (Objects.equals(t.getTenantId(), stadiumTenantId)) continue;
            throw new BusinessException(400, "\u4e3b\u573a\u7403\u961f\u4e0d\u5c5e\u4e8e\u5f53\u524d\u79df\u6237");
        }
    }

    private void setLocationFromDto(Stadium s, Double longitude, Double latitude) {
        if (longitude == null && latitude == null) {
            s.setLocation(null);
            return;
        }
        if (longitude == null || latitude == null) {
            throw new BusinessException(400, "\u7ecf\u5ea6\u4e0e\u7eac\u5ea6\u9700\u540c\u65f6\u586b\u5199\u6216\u540c\u65f6\u7559\u7a7a");
        }
        s.setLocation(GEOMETRY_4326.createPoint(new Coordinate(longitude.doubleValue(), latitude.doubleValue())));
    }

    private static void normalizeStrings(Stadium s) {
        if (s.getShortName() != null && s.getShortName().isBlank()) {
            s.setShortName(null);
        }
        if (s.getShortNameEn() != null && s.getShortNameEn().isBlank()) {
            s.setShortNameEn(null);
        }
        if (s.getAddrProvince() != null && s.getAddrProvince().isBlank()) {
            s.setAddrProvince(null);
        }
        if (s.getAddrCity() != null && s.getAddrCity().isBlank()) {
            s.setAddrCity(null);
        }
        if (s.getAddrDistrict() != null && s.getAddrDistrict().isBlank()) {
            s.setAddrDistrict(null);
        }
        if (s.getAddrDistrictAdcode() != null && s.getAddrDistrictAdcode().isBlank()) {
            s.setAddrDistrictAdcode(null);
        }
        if (s.getPostalCode() != null && s.getPostalCode().isBlank()) {
            s.setPostalCode(null);
        }
        if (s.getAddressDetail() != null && s.getAddressDetail().isBlank()) {
            s.setAddressDetail(null);
        }
        if (s.getContactPhone() != null && s.getContactPhone().isBlank()) {
            s.setContactPhone(null);
        }
        if (s.getIntroduction() != null && s.getIntroduction().isBlank()) {
            s.setIntroduction(null);
        }
        if (s.getTransportationInfo() != null && s.getTransportationInfo().isBlank()) {
            s.setTransportationInfo(null);
        }
        if (s.getLayoutDiagramUrl() != null && s.getLayoutDiagramUrl().isBlank()) {
            s.setLayoutDiagramUrl(null);
        }
        if (s.getIntroImageUrl() != null && s.getIntroImageUrl().isBlank()) {
            s.setIntroImageUrl(null);
        }
        if (s.getConstructionEra() != null && s.getConstructionEra().isBlank()) {
            s.setConstructionEra(null);
        }
        if (s.getImportantDatesNote() != null && s.getImportantDatesNote().isBlank()) {
            s.setImportantDatesNote(null);
        }
    }

    private Specification<Stadium> buildSpec(String keyword, StadiumLevel level, Long tenantId) {
        return (Specification & Serializable)(root, q, cb) -> {
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            preds.add(cb.isNull((Expression)root.get("deletedAt")));
            if (tenantId != null) {
                preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tenantId));
            }
            if (level != null) {
                preds.add(cb.equal((Expression)root.get("level"), (Object)level));
            }
            if (keyword != null && !keyword.isBlank()) {
                String k = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                preds.add(cb.or(new Predicate[]{cb.like(cb.lower((Expression)root.get("name")), k), cb.like(cb.lower(cb.coalesce((Expression)root.get("shortName"), cb.literal((Object)""))), k), cb.like(cb.lower(cb.coalesce((Expression)root.get("addrCity"), cb.literal((Object)""))), k), cb.like(cb.lower(cb.coalesce((Expression)root.get("addressDetail"), cb.literal((Object)""))), k)}));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }

    private Pageable buildPageable(Integer page, Integer pageSize, String sortProp, String sortOrder) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        if (sortProp != null && !sortProp.isEmpty()) {
            Sort.Direction dir = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)dir, (String[])new String[]{sortProp}));
        }
        return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.ASC, (String[])new String[]{"id"}));
    }

    private Map<Long, String> loadTeamNames(Collection<Stadium> stadiums) {
        HashSet<Long> ids = new HashSet<Long>();
        for (Stadium s : stadiums) {
            if (s.getHomeTeams() == null) continue;
            for (StadiumHomeTeam ht : s.getHomeTeams()) {
                if (ht.getDeletedAt() != null || ht.getTeamId() == null) continue;
                ids.add(ht.getTeamId());
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        return this.teamRepository.findAllById(ids).stream().collect(Collectors.toMap(t -> t.getId(), t -> t.getName() != null ? t.getName() : ""));
    }

    private StadiumViewDto toView(Stadium s, Map<Long, String> teamNames) {
        StadiumViewDto v = new StadiumViewDto();
        v.setId(s.getId());
        v.setTenantId(s.getTenantId());
        v.setName(s.getName());
        v.setShortName(s.getShortName());
        v.setShortNameEn(s.getShortNameEn());
        v.setAddrProvince(s.getAddrProvince());
        v.setAddrCity(s.getAddrCity());
        v.setAddrDistrict(s.getAddrDistrict());
        v.setAddrDistrictAdcode(s.getAddrDistrictAdcode());
        StadiumService.fillRegionAdcodesForView((StadiumViewDto)v, (String)s.getAddrDistrictAdcode());
        v.setPostalCode(s.getPostalCode());
        v.setAddressDetail(s.getAddressDetail());
        Point loc = s.getLocation();
        if (loc != null) {
            v.setLongitude(Double.valueOf(loc.getX()));
            v.setLatitude(Double.valueOf(loc.getY()));
            v.setGeometry(StadiumService.pointToGeoJson((Point)loc));
        }
        v.setLevel(s.getLevel());
        v.setOperatingStatus(s.getOperatingStatus());
        v.setCapacityTotal(s.getCapacityTotal());
        v.setSeatingCapacity(s.getSeatingCapacity());
        v.setRecordAttendance(s.getRecordAttendance());
        v.setFieldDistanceLeftM(s.getFieldDistanceLeftM());
        v.setFieldDistanceCenterM(s.getFieldDistanceCenterM());
        v.setFieldDistanceRightM(s.getFieldDistanceRightM());
        v.setTurfType(s.getTurfType());
        v.setRoofType(s.getRoofType());
        v.setContactPhone(s.getContactPhone());
        v.setHasLargeScreen(s.getHasLargeScreen());
        v.setIntroduction(s.getIntroduction());
        v.setTransportationInfo(s.getTransportationInfo());
        v.setLayoutDiagramUrl(s.getLayoutDiagramUrl());
        v.setIntroImageUrl(s.getIntroImageUrl());
        v.setConstructionEra(s.getConstructionEra());
        v.setOpenedOn(s.getOpenedOn());
        v.setImportantDatesNote(s.getImportantDatesNote());
        v.setCreatedAt(s.getCreatedAt());
        v.setUpdatedAt(s.getUpdatedAt());
        ArrayList<StadiumHomeTeamViewDto> htDtos = new ArrayList<StadiumHomeTeamViewDto>();
        if (s.getHomeTeams() != null) {
            for (StadiumHomeTeam ht : s.getHomeTeams()) {
                if (ht.getDeletedAt() != null) continue;
                StadiumHomeTeamViewDto d = new StadiumHomeTeamViewDto();
                d.setId(ht.getId());
                d.setTeamId(ht.getTeamId());
                d.setTeamName(teamNames.get(ht.getTeamId()));
                d.setEffectiveFrom(ht.getEffectiveFrom());
                d.setEffectiveTo(ht.getEffectiveTo());
                d.setSortOrder(ht.getSortOrder());
                htDtos.add(d);
            }
        }
        v.setHomeTeams(htDtos);
        return v;
    }

    private static void fillRegionAdcodesForView(StadiumViewDto v, String districtAdcode) {
        if (districtAdcode == null || districtAdcode.length() < 6) {
            return;
        }
        v.setAddrProvinceAdcode(districtAdcode.substring(0, 2) + "0000");
        v.setAddrCityAdcode(districtAdcode.substring(0, 4) + "00");
    }

    private static List<String> homeTeamLabels(Stadium s, Map<Long, String> teamNames) {
        if (s.getHomeTeams() == null) {
            return List.of();
        }
        return s.getHomeTeams().stream().filter(ht -> ht.getDeletedAt() == null).map(ht -> teamNames.getOrDefault(ht.getTeamId(), String.valueOf(ht.getTeamId()))).toList();
    }

    private static Map<String, Object> pointToGeoJson(Point p) {
        LinkedHashMap<String, Object> g = new LinkedHashMap<String, Object>();
        g.put("type", "Point");
        g.put("coordinates", List.of((Object)p.getX(), (Object)p.getY()));
        return g;
    }

    @Generated
    public StadiumService(StadiumRepository stadiumRepository, TeamRepository teamRepository, ChinaRegionRepository chinaRegionRepository, TenantQueryPolicyService tenantQueryPolicyService) {
        this.stadiumRepository = stadiumRepository;
        this.teamRepository = teamRepository;
        this.chinaRegionRepository = chinaRegionRepository;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

