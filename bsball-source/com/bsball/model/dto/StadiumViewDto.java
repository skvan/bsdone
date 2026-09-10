/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.StadiumHomeTeamViewDto
 *  com.bsball.model.dto.StadiumViewDto
 *  com.bsball.model.enums.RoofType
 *  com.bsball.model.enums.StadiumLevel
 *  com.bsball.model.enums.StadiumOperatingStatus
 *  com.bsball.model.enums.TurfType
 *  com.fasterxml.jackson.annotation.JsonInclude
 *  com.fasterxml.jackson.annotation.JsonInclude$Include
 *  lombok.Generated
 */
package com.bsball.model.dto;

import com.bsball.model.dto.StadiumHomeTeamViewDto;
import com.bsball.model.enums.RoofType;
import com.bsball.model.enums.StadiumLevel;
import com.bsball.model.enums.StadiumOperatingStatus;
import com.bsball.model.enums.TurfType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Generated;

public class StadiumViewDto {
    private Long id;
    @JsonInclude(value=JsonInclude.Include.ALWAYS)
    private Long tenantId;
    private String name;
    private String shortName;
    private String shortNameEn;
    private String addrProvince;
    private String addrCity;
    private String addrDistrict;
    private String addrDistrictAdcode;
    private String addrProvinceAdcode;
    private String addrCityAdcode;
    private String postalCode;
    private String addressDetail;
    private Double longitude;
    private Double latitude;
    private Map<String, Object> geometry;
    private StadiumLevel level;
    private StadiumOperatingStatus operatingStatus;
    private Integer capacityTotal;
    private Integer seatingCapacity;
    private Integer recordAttendance;
    private Integer fieldDistanceLeftM;
    private Integer fieldDistanceCenterM;
    private Integer fieldDistanceRightM;
    private TurfType turfType;
    private RoofType roofType;
    private String contactPhone;
    private Boolean hasLargeScreen;
    private String introduction;
    private String transportationInfo;
    private String layoutDiagramUrl;
    private String introImageUrl;
    private String constructionEra;
    private LocalDate openedOn;
    private String importantDatesNote;
    private List<StadiumHomeTeamViewDto> homeTeams;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Generated
    public StadiumViewDto() {
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getShortName() {
        return this.shortName;
    }

    @Generated
    public String getShortNameEn() {
        return this.shortNameEn;
    }

    @Generated
    public String getAddrProvince() {
        return this.addrProvince;
    }

    @Generated
    public String getAddrCity() {
        return this.addrCity;
    }

    @Generated
    public String getAddrDistrict() {
        return this.addrDistrict;
    }

    @Generated
    public String getAddrDistrictAdcode() {
        return this.addrDistrictAdcode;
    }

    @Generated
    public String getAddrProvinceAdcode() {
        return this.addrProvinceAdcode;
    }

    @Generated
    public String getAddrCityAdcode() {
        return this.addrCityAdcode;
    }

    @Generated
    public String getPostalCode() {
        return this.postalCode;
    }

    @Generated
    public String getAddressDetail() {
        return this.addressDetail;
    }

    @Generated
    public Double getLongitude() {
        return this.longitude;
    }

    @Generated
    public Double getLatitude() {
        return this.latitude;
    }

    @Generated
    public Map<String, Object> getGeometry() {
        return this.geometry;
    }

    @Generated
    public StadiumLevel getLevel() {
        return this.level;
    }

    @Generated
    public StadiumOperatingStatus getOperatingStatus() {
        return this.operatingStatus;
    }

    @Generated
    public Integer getCapacityTotal() {
        return this.capacityTotal;
    }

    @Generated
    public Integer getSeatingCapacity() {
        return this.seatingCapacity;
    }

    @Generated
    public Integer getRecordAttendance() {
        return this.recordAttendance;
    }

    @Generated
    public Integer getFieldDistanceLeftM() {
        return this.fieldDistanceLeftM;
    }

    @Generated
    public Integer getFieldDistanceCenterM() {
        return this.fieldDistanceCenterM;
    }

    @Generated
    public Integer getFieldDistanceRightM() {
        return this.fieldDistanceRightM;
    }

    @Generated
    public TurfType getTurfType() {
        return this.turfType;
    }

    @Generated
    public RoofType getRoofType() {
        return this.roofType;
    }

    @Generated
    public String getContactPhone() {
        return this.contactPhone;
    }

    @Generated
    public Boolean getHasLargeScreen() {
        return this.hasLargeScreen;
    }

    @Generated
    public String getIntroduction() {
        return this.introduction;
    }

    @Generated
    public String getTransportationInfo() {
        return this.transportationInfo;
    }

    @Generated
    public String getLayoutDiagramUrl() {
        return this.layoutDiagramUrl;
    }

    @Generated
    public String getIntroImageUrl() {
        return this.introImageUrl;
    }

    @Generated
    public String getConstructionEra() {
        return this.constructionEra;
    }

    @Generated
    public LocalDate getOpenedOn() {
        return this.openedOn;
    }

    @Generated
    public String getImportantDatesNote() {
        return this.importantDatesNote;
    }

    @Generated
    public List<StadiumHomeTeamViewDto> getHomeTeams() {
        return this.homeTeams;
    }

    @Generated
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @Generated
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    @Generated
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    @Generated
    public void setShortNameEn(String shortNameEn) {
        this.shortNameEn = shortNameEn;
    }

    @Generated
    public void setAddrProvince(String addrProvince) {
        this.addrProvince = addrProvince;
    }

    @Generated
    public void setAddrCity(String addrCity) {
        this.addrCity = addrCity;
    }

    @Generated
    public void setAddrDistrict(String addrDistrict) {
        this.addrDistrict = addrDistrict;
    }

    @Generated
    public void setAddrDistrictAdcode(String addrDistrictAdcode) {
        this.addrDistrictAdcode = addrDistrictAdcode;
    }

    @Generated
    public void setAddrProvinceAdcode(String addrProvinceAdcode) {
        this.addrProvinceAdcode = addrProvinceAdcode;
    }

    @Generated
    public void setAddrCityAdcode(String addrCityAdcode) {
        this.addrCityAdcode = addrCityAdcode;
    }

    @Generated
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    @Generated
    public void setAddressDetail(String addressDetail) {
        this.addressDetail = addressDetail;
    }

    @Generated
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    @Generated
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    @Generated
    public void setGeometry(Map<String, Object> geometry) {
        this.geometry = geometry;
    }

    @Generated
    public void setLevel(StadiumLevel level) {
        this.level = level;
    }

    @Generated
    public void setOperatingStatus(StadiumOperatingStatus operatingStatus) {
        this.operatingStatus = operatingStatus;
    }

    @Generated
    public void setCapacityTotal(Integer capacityTotal) {
        this.capacityTotal = capacityTotal;
    }

    @Generated
    public void setSeatingCapacity(Integer seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }

    @Generated
    public void setRecordAttendance(Integer recordAttendance) {
        this.recordAttendance = recordAttendance;
    }

    @Generated
    public void setFieldDistanceLeftM(Integer fieldDistanceLeftM) {
        this.fieldDistanceLeftM = fieldDistanceLeftM;
    }

    @Generated
    public void setFieldDistanceCenterM(Integer fieldDistanceCenterM) {
        this.fieldDistanceCenterM = fieldDistanceCenterM;
    }

    @Generated
    public void setFieldDistanceRightM(Integer fieldDistanceRightM) {
        this.fieldDistanceRightM = fieldDistanceRightM;
    }

    @Generated
    public void setTurfType(TurfType turfType) {
        this.turfType = turfType;
    }

    @Generated
    public void setRoofType(RoofType roofType) {
        this.roofType = roofType;
    }

    @Generated
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    @Generated
    public void setHasLargeScreen(Boolean hasLargeScreen) {
        this.hasLargeScreen = hasLargeScreen;
    }

    @Generated
    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    @Generated
    public void setTransportationInfo(String transportationInfo) {
        this.transportationInfo = transportationInfo;
    }

    @Generated
    public void setLayoutDiagramUrl(String layoutDiagramUrl) {
        this.layoutDiagramUrl = layoutDiagramUrl;
    }

    @Generated
    public void setIntroImageUrl(String introImageUrl) {
        this.introImageUrl = introImageUrl;
    }

    @Generated
    public void setConstructionEra(String constructionEra) {
        this.constructionEra = constructionEra;
    }

    @Generated
    public void setOpenedOn(LocalDate openedOn) {
        this.openedOn = openedOn;
    }

    @Generated
    public void setImportantDatesNote(String importantDatesNote) {
        this.importantDatesNote = importantDatesNote;
    }

    @Generated
    public void setHomeTeams(List<StadiumHomeTeamViewDto> homeTeams) {
        this.homeTeams = homeTeams;
    }

    @Generated
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Generated
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StadiumViewDto)) {
            return false;
        }
        StadiumViewDto other = (StadiumViewDto)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        Double this$longitude = this.getLongitude();
        Double other$longitude = other.getLongitude();
        if (this$longitude == null ? other$longitude != null : !((Object)this$longitude).equals(other$longitude)) {
            return false;
        }
        Double this$latitude = this.getLatitude();
        Double other$latitude = other.getLatitude();
        if (this$latitude == null ? other$latitude != null : !((Object)this$latitude).equals(other$latitude)) {
            return false;
        }
        Integer this$capacityTotal = this.getCapacityTotal();
        Integer other$capacityTotal = other.getCapacityTotal();
        if (this$capacityTotal == null ? other$capacityTotal != null : !((Object)this$capacityTotal).equals(other$capacityTotal)) {
            return false;
        }
        Integer this$seatingCapacity = this.getSeatingCapacity();
        Integer other$seatingCapacity = other.getSeatingCapacity();
        if (this$seatingCapacity == null ? other$seatingCapacity != null : !((Object)this$seatingCapacity).equals(other$seatingCapacity)) {
            return false;
        }
        Integer this$recordAttendance = this.getRecordAttendance();
        Integer other$recordAttendance = other.getRecordAttendance();
        if (this$recordAttendance == null ? other$recordAttendance != null : !((Object)this$recordAttendance).equals(other$recordAttendance)) {
            return false;
        }
        Integer this$fieldDistanceLeftM = this.getFieldDistanceLeftM();
        Integer other$fieldDistanceLeftM = other.getFieldDistanceLeftM();
        if (this$fieldDistanceLeftM == null ? other$fieldDistanceLeftM != null : !((Object)this$fieldDistanceLeftM).equals(other$fieldDistanceLeftM)) {
            return false;
        }
        Integer this$fieldDistanceCenterM = this.getFieldDistanceCenterM();
        Integer other$fieldDistanceCenterM = other.getFieldDistanceCenterM();
        if (this$fieldDistanceCenterM == null ? other$fieldDistanceCenterM != null : !((Object)this$fieldDistanceCenterM).equals(other$fieldDistanceCenterM)) {
            return false;
        }
        Integer this$fieldDistanceRightM = this.getFieldDistanceRightM();
        Integer other$fieldDistanceRightM = other.getFieldDistanceRightM();
        if (this$fieldDistanceRightM == null ? other$fieldDistanceRightM != null : !((Object)this$fieldDistanceRightM).equals(other$fieldDistanceRightM)) {
            return false;
        }
        Boolean this$hasLargeScreen = this.getHasLargeScreen();
        Boolean other$hasLargeScreen = other.getHasLargeScreen();
        if (this$hasLargeScreen == null ? other$hasLargeScreen != null : !((Object)this$hasLargeScreen).equals(other$hasLargeScreen)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$shortName = this.getShortName();
        String other$shortName = other.getShortName();
        if (this$shortName == null ? other$shortName != null : !this$shortName.equals(other$shortName)) {
            return false;
        }
        String this$shortNameEn = this.getShortNameEn();
        String other$shortNameEn = other.getShortNameEn();
        if (this$shortNameEn == null ? other$shortNameEn != null : !this$shortNameEn.equals(other$shortNameEn)) {
            return false;
        }
        String this$addrProvince = this.getAddrProvince();
        String other$addrProvince = other.getAddrProvince();
        if (this$addrProvince == null ? other$addrProvince != null : !this$addrProvince.equals(other$addrProvince)) {
            return false;
        }
        String this$addrCity = this.getAddrCity();
        String other$addrCity = other.getAddrCity();
        if (this$addrCity == null ? other$addrCity != null : !this$addrCity.equals(other$addrCity)) {
            return false;
        }
        String this$addrDistrict = this.getAddrDistrict();
        String other$addrDistrict = other.getAddrDistrict();
        if (this$addrDistrict == null ? other$addrDistrict != null : !this$addrDistrict.equals(other$addrDistrict)) {
            return false;
        }
        String this$addrDistrictAdcode = this.getAddrDistrictAdcode();
        String other$addrDistrictAdcode = other.getAddrDistrictAdcode();
        if (this$addrDistrictAdcode == null ? other$addrDistrictAdcode != null : !this$addrDistrictAdcode.equals(other$addrDistrictAdcode)) {
            return false;
        }
        String this$addrProvinceAdcode = this.getAddrProvinceAdcode();
        String other$addrProvinceAdcode = other.getAddrProvinceAdcode();
        if (this$addrProvinceAdcode == null ? other$addrProvinceAdcode != null : !this$addrProvinceAdcode.equals(other$addrProvinceAdcode)) {
            return false;
        }
        String this$addrCityAdcode = this.getAddrCityAdcode();
        String other$addrCityAdcode = other.getAddrCityAdcode();
        if (this$addrCityAdcode == null ? other$addrCityAdcode != null : !this$addrCityAdcode.equals(other$addrCityAdcode)) {
            return false;
        }
        String this$postalCode = this.getPostalCode();
        String other$postalCode = other.getPostalCode();
        if (this$postalCode == null ? other$postalCode != null : !this$postalCode.equals(other$postalCode)) {
            return false;
        }
        String this$addressDetail = this.getAddressDetail();
        String other$addressDetail = other.getAddressDetail();
        if (this$addressDetail == null ? other$addressDetail != null : !this$addressDetail.equals(other$addressDetail)) {
            return false;
        }
        Map this$geometry = this.getGeometry();
        Map other$geometry = other.getGeometry();
        if (this$geometry == null ? other$geometry != null : !((Object)this$geometry).equals(other$geometry)) {
            return false;
        }
        StadiumLevel this$level = this.getLevel();
        StadiumLevel other$level = other.getLevel();
        if (this$level == null ? other$level != null : !this$level.equals(other$level)) {
            return false;
        }
        StadiumOperatingStatus this$operatingStatus = this.getOperatingStatus();
        StadiumOperatingStatus other$operatingStatus = other.getOperatingStatus();
        if (this$operatingStatus == null ? other$operatingStatus != null : !this$operatingStatus.equals(other$operatingStatus)) {
            return false;
        }
        TurfType this$turfType = this.getTurfType();
        TurfType other$turfType = other.getTurfType();
        if (this$turfType == null ? other$turfType != null : !this$turfType.equals(other$turfType)) {
            return false;
        }
        RoofType this$roofType = this.getRoofType();
        RoofType other$roofType = other.getRoofType();
        if (this$roofType == null ? other$roofType != null : !this$roofType.equals(other$roofType)) {
            return false;
        }
        String this$contactPhone = this.getContactPhone();
        String other$contactPhone = other.getContactPhone();
        if (this$contactPhone == null ? other$contactPhone != null : !this$contactPhone.equals(other$contactPhone)) {
            return false;
        }
        String this$introduction = this.getIntroduction();
        String other$introduction = other.getIntroduction();
        if (this$introduction == null ? other$introduction != null : !this$introduction.equals(other$introduction)) {
            return false;
        }
        String this$transportationInfo = this.getTransportationInfo();
        String other$transportationInfo = other.getTransportationInfo();
        if (this$transportationInfo == null ? other$transportationInfo != null : !this$transportationInfo.equals(other$transportationInfo)) {
            return false;
        }
        String this$layoutDiagramUrl = this.getLayoutDiagramUrl();
        String other$layoutDiagramUrl = other.getLayoutDiagramUrl();
        if (this$layoutDiagramUrl == null ? other$layoutDiagramUrl != null : !this$layoutDiagramUrl.equals(other$layoutDiagramUrl)) {
            return false;
        }
        String this$introImageUrl = this.getIntroImageUrl();
        String other$introImageUrl = other.getIntroImageUrl();
        if (this$introImageUrl == null ? other$introImageUrl != null : !this$introImageUrl.equals(other$introImageUrl)) {
            return false;
        }
        String this$constructionEra = this.getConstructionEra();
        String other$constructionEra = other.getConstructionEra();
        if (this$constructionEra == null ? other$constructionEra != null : !this$constructionEra.equals(other$constructionEra)) {
            return false;
        }
        LocalDate this$openedOn = this.getOpenedOn();
        LocalDate other$openedOn = other.getOpenedOn();
        if (this$openedOn == null ? other$openedOn != null : !((Object)this$openedOn).equals(other$openedOn)) {
            return false;
        }
        String this$importantDatesNote = this.getImportantDatesNote();
        String other$importantDatesNote = other.getImportantDatesNote();
        if (this$importantDatesNote == null ? other$importantDatesNote != null : !this$importantDatesNote.equals(other$importantDatesNote)) {
            return false;
        }
        List this$homeTeams = this.getHomeTeams();
        List other$homeTeams = other.getHomeTeams();
        if (this$homeTeams == null ? other$homeTeams != null : !((Object)this$homeTeams).equals(other$homeTeams)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt)) {
            return false;
        }
        LocalDateTime this$updatedAt = this.getUpdatedAt();
        LocalDateTime other$updatedAt = other.getUpdatedAt();
        return !(this$updatedAt == null ? other$updatedAt != null : !((Object)this$updatedAt).equals(other$updatedAt));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof StadiumViewDto;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Double $longitude = this.getLongitude();
        result = result * 59 + ($longitude == null ? 43 : ((Object)$longitude).hashCode());
        Double $latitude = this.getLatitude();
        result = result * 59 + ($latitude == null ? 43 : ((Object)$latitude).hashCode());
        Integer $capacityTotal = this.getCapacityTotal();
        result = result * 59 + ($capacityTotal == null ? 43 : ((Object)$capacityTotal).hashCode());
        Integer $seatingCapacity = this.getSeatingCapacity();
        result = result * 59 + ($seatingCapacity == null ? 43 : ((Object)$seatingCapacity).hashCode());
        Integer $recordAttendance = this.getRecordAttendance();
        result = result * 59 + ($recordAttendance == null ? 43 : ((Object)$recordAttendance).hashCode());
        Integer $fieldDistanceLeftM = this.getFieldDistanceLeftM();
        result = result * 59 + ($fieldDistanceLeftM == null ? 43 : ((Object)$fieldDistanceLeftM).hashCode());
        Integer $fieldDistanceCenterM = this.getFieldDistanceCenterM();
        result = result * 59 + ($fieldDistanceCenterM == null ? 43 : ((Object)$fieldDistanceCenterM).hashCode());
        Integer $fieldDistanceRightM = this.getFieldDistanceRightM();
        result = result * 59 + ($fieldDistanceRightM == null ? 43 : ((Object)$fieldDistanceRightM).hashCode());
        Boolean $hasLargeScreen = this.getHasLargeScreen();
        result = result * 59 + ($hasLargeScreen == null ? 43 : ((Object)$hasLargeScreen).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $shortName = this.getShortName();
        result = result * 59 + ($shortName == null ? 43 : $shortName.hashCode());
        String $shortNameEn = this.getShortNameEn();
        result = result * 59 + ($shortNameEn == null ? 43 : $shortNameEn.hashCode());
        String $addrProvince = this.getAddrProvince();
        result = result * 59 + ($addrProvince == null ? 43 : $addrProvince.hashCode());
        String $addrCity = this.getAddrCity();
        result = result * 59 + ($addrCity == null ? 43 : $addrCity.hashCode());
        String $addrDistrict = this.getAddrDistrict();
        result = result * 59 + ($addrDistrict == null ? 43 : $addrDistrict.hashCode());
        String $addrDistrictAdcode = this.getAddrDistrictAdcode();
        result = result * 59 + ($addrDistrictAdcode == null ? 43 : $addrDistrictAdcode.hashCode());
        String $addrProvinceAdcode = this.getAddrProvinceAdcode();
        result = result * 59 + ($addrProvinceAdcode == null ? 43 : $addrProvinceAdcode.hashCode());
        String $addrCityAdcode = this.getAddrCityAdcode();
        result = result * 59 + ($addrCityAdcode == null ? 43 : $addrCityAdcode.hashCode());
        String $postalCode = this.getPostalCode();
        result = result * 59 + ($postalCode == null ? 43 : $postalCode.hashCode());
        String $addressDetail = this.getAddressDetail();
        result = result * 59 + ($addressDetail == null ? 43 : $addressDetail.hashCode());
        Map $geometry = this.getGeometry();
        result = result * 59 + ($geometry == null ? 43 : ((Object)$geometry).hashCode());
        StadiumLevel $level = this.getLevel();
        result = result * 59 + ($level == null ? 43 : $level.hashCode());
        StadiumOperatingStatus $operatingStatus = this.getOperatingStatus();
        result = result * 59 + ($operatingStatus == null ? 43 : $operatingStatus.hashCode());
        TurfType $turfType = this.getTurfType();
        result = result * 59 + ($turfType == null ? 43 : $turfType.hashCode());
        RoofType $roofType = this.getRoofType();
        result = result * 59 + ($roofType == null ? 43 : $roofType.hashCode());
        String $contactPhone = this.getContactPhone();
        result = result * 59 + ($contactPhone == null ? 43 : $contactPhone.hashCode());
        String $introduction = this.getIntroduction();
        result = result * 59 + ($introduction == null ? 43 : $introduction.hashCode());
        String $transportationInfo = this.getTransportationInfo();
        result = result * 59 + ($transportationInfo == null ? 43 : $transportationInfo.hashCode());
        String $layoutDiagramUrl = this.getLayoutDiagramUrl();
        result = result * 59 + ($layoutDiagramUrl == null ? 43 : $layoutDiagramUrl.hashCode());
        String $introImageUrl = this.getIntroImageUrl();
        result = result * 59 + ($introImageUrl == null ? 43 : $introImageUrl.hashCode());
        String $constructionEra = this.getConstructionEra();
        result = result * 59 + ($constructionEra == null ? 43 : $constructionEra.hashCode());
        LocalDate $openedOn = this.getOpenedOn();
        result = result * 59 + ($openedOn == null ? 43 : ((Object)$openedOn).hashCode());
        String $importantDatesNote = this.getImportantDatesNote();
        result = result * 59 + ($importantDatesNote == null ? 43 : $importantDatesNote.hashCode());
        List $homeTeams = this.getHomeTeams();
        result = result * 59 + ($homeTeams == null ? 43 : ((Object)$homeTeams).hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "StadiumViewDto(id=" + this.getId() + ", tenantId=" + this.getTenantId() + ", name=" + this.getName() + ", shortName=" + this.getShortName() + ", shortNameEn=" + this.getShortNameEn() + ", addrProvince=" + this.getAddrProvince() + ", addrCity=" + this.getAddrCity() + ", addrDistrict=" + this.getAddrDistrict() + ", addrDistrictAdcode=" + this.getAddrDistrictAdcode() + ", addrProvinceAdcode=" + this.getAddrProvinceAdcode() + ", addrCityAdcode=" + this.getAddrCityAdcode() + ", postalCode=" + this.getPostalCode() + ", addressDetail=" + this.getAddressDetail() + ", longitude=" + this.getLongitude() + ", latitude=" + this.getLatitude() + ", geometry=" + String.valueOf(this.getGeometry()) + ", level=" + String.valueOf(this.getLevel()) + ", operatingStatus=" + String.valueOf(this.getOperatingStatus()) + ", capacityTotal=" + this.getCapacityTotal() + ", seatingCapacity=" + this.getSeatingCapacity() + ", recordAttendance=" + this.getRecordAttendance() + ", fieldDistanceLeftM=" + this.getFieldDistanceLeftM() + ", fieldDistanceCenterM=" + this.getFieldDistanceCenterM() + ", fieldDistanceRightM=" + this.getFieldDistanceRightM() + ", turfType=" + String.valueOf(this.getTurfType()) + ", roofType=" + String.valueOf(this.getRoofType()) + ", contactPhone=" + this.getContactPhone() + ", hasLargeScreen=" + this.getHasLargeScreen() + ", introduction=" + this.getIntroduction() + ", transportationInfo=" + this.getTransportationInfo() + ", layoutDiagramUrl=" + this.getLayoutDiagramUrl() + ", introImageUrl=" + this.getIntroImageUrl() + ", constructionEra=" + this.getConstructionEra() + ", openedOn=" + String.valueOf(this.getOpenedOn()) + ", importantDatesNote=" + this.getImportantDatesNote() + ", homeTeams=" + String.valueOf(this.getHomeTeams()) + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", updatedAt=" + String.valueOf(this.getUpdatedAt()) + ")";
    }
}

