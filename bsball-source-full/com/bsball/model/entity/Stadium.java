/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.Stadium
 *  com.bsball.model.entity.StadiumHomeTeam
 *  com.bsball.model.enums.RoofType
 *  com.bsball.model.enums.StadiumLevel
 *  com.bsball.model.enums.StadiumOperatingStatus
 *  com.bsball.model.enums.TurfType
 *  jakarta.persistence.CascadeType
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.EnumType
 *  jakarta.persistence.Enumerated
 *  jakarta.persistence.OneToMany
 *  jakarta.persistence.OrderBy
 *  jakarta.persistence.Table
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.NotNull
 *  jakarta.validation.constraints.Size
 *  lombok.Generated
 *  org.hibernate.annotations.BatchSize
 *  org.hibernate.annotations.Comment
 *  org.hibernate.annotations.JdbcTypeCode
 *  org.locationtech.jts.geom.Point
 */
package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import com.bsball.model.entity.StadiumHomeTeam;
import com.bsball.model.enums.RoofType;
import com.bsball.model.enums.StadiumLevel;
import com.bsball.model.enums.StadiumOperatingStatus;
import com.bsball.model.enums.TurfType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name="bs_stadium")
@Comment(value="\u7403\u573a")
public class Stadium
extends BaseEntity {
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @NotBlank(message="\u7403\u573a\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a")
    @Size(max=200)
    @Comment(value="\u7403\u573a\u540d\u79f0")
    private @NotBlank(message="\u7403\u573a\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a") @Size(max=200) String name;
    @Size(max=50)
    @Comment(value="\u7b80\u79f0")
    private @Size(max=50) String shortName;
    @Size(max=80)
    @Comment(value="\u82f1\u6587\u7b80\u79f0")
    private @Size(max=80) String shortNameEn;
    @Size(max=50)
    @Comment(value="\u7701")
    private @Size(max=50) String addrProvince;
    @Size(max=80)
    @Comment(value="\u53bf/\u5e02")
    private @Size(max=80) String addrCity;
    @Size(max=80)
    @Comment(value="\u4e61/\u9547/\u533a")
    private @Size(max=80) String addrDistrict;
    @Size(max=12)
    @Comment(value="\u672b\u7ea7\u533a\u5212\u4ee3\u7801\uff08\u89c1 sys_china_region\uff0c\u9547\u8857\u53ef\u4e3a9\uff5e12\u4f4d\uff09")
    private @Size(max=12) String addrDistrictAdcode;
    @Size(max=20)
    @Comment(value="\u90ae\u9012\u533a\u53f7")
    private @Size(max=20) String postalCode;
    @Size(max=500)
    @Comment(value="\u8be6\u7ec6\u5730\u5740")
    private @Size(max=500) String addressDetail;
    @JdbcTypeCode(value=3200)
    @Comment(value="\u5750\u6807 GCJ-02\uff0c\u4e0e\u9ad8\u5fb7\u4e00\u81f4\uff08\u51e0\u4f55\u7c7b\u578b\u4e3a Point,4326\uff09")
    private Point location;
    @NotNull
    @Enumerated(value=EnumType.STRING)
    @Column(nullable=false, length=16)
    @Comment(value="\u7403\u573a\u7ea7\u522b")
    private StadiumLevel level;
    @NotNull
    @Enumerated(value=EnumType.STRING)
    @Column(nullable=false, length=32)
    @Comment(value="\u8fd0\u8425\u72b6\u6001")
    private StadiumOperatingStatus operatingStatus;
    @Comment(value="\u5bb9\u7eb3\u4eba\u6570\u4e0a\u9650\uff08\u542b\u7ad9\u5e2d\uff09")
    private Integer capacityTotal;
    @Comment(value="\u7eaf\u5ea7\u5e2d\u6570")
    private Integer seatingCapacity;
    @Comment(value="\u7eaa\u5f55\u4e0a\u5ea7\uff08\u5c55\u793a\uff09")
    private Integer recordAttendance;
    @Comment(value="\u5de6\u5916\u91ce\u8ddd\u79bb(\u7c73)")
    @Column(name="field_distance_left_m")
    private Integer fieldDistanceLeftM;
    @Comment(value="\u4e2d\u5916\u91ce\u8ddd\u79bb(\u7c73)")
    @Column(name="field_distance_center_m")
    private Integer fieldDistanceCenterM;
    @Comment(value="\u53f3\u5916\u91ce\u8ddd\u79bb(\u7c73)")
    @Column(name="field_distance_right_m")
    private Integer fieldDistanceRightM;
    @Enumerated(value=EnumType.STRING)
    @Column(length=32)
    @Comment(value="\u8349\u76ae\u7c7b\u578b")
    private TurfType turfType;
    @Enumerated(value=EnumType.STRING)
    @Column(length=32)
    @Comment(value="\u5c4b\u9876\u7c7b\u578b")
    private RoofType roofType;
    @Size(max=50)
    @Comment(value="\u8054\u7cfb\u7535\u8bdd")
    private @Size(max=50) String contactPhone;
    @Comment(value="\u662f\u5426\u5177\u5907\u5927\u578b\u8ba1\u5206\u5c4f/\u5927\u5c4f\u5e55\uff08\u7a7a=\u672a\u586b\uff09")
    private Boolean hasLargeScreen;
    @Column(columnDefinition="TEXT")
    @Comment(value="\u7403\u573a\u7b80\u4ecb")
    private String introduction;
    @Column(columnDefinition="TEXT")
    @Comment(value="\u4ea4\u901a\u8d44\u8baf\uff08\u591a\u884c\u6587\u672c\uff09")
    private String transportationInfo;
    @Size(max=500)
    @Comment(value="\u7403\u573a\u5e03\u5c40\u56fe\uff08\u4e0a\u4f20\u56fe\u7247 URL\uff09")
    private @Size(max=500) String layoutDiagramUrl;
    @Size(max=500)
    @Comment(value="\u7403\u573a\u56fe\u7247\uff08\u95e8\u6237\u4ecb\u7ecd\u9875\u4e3b\u56fe\uff0c\u5efa\u8bae 940\u00d7380\uff1b\u4e0a\u4f20 URL\uff0c\u5217\u540d intro_image_url\uff09")
    private @Size(max=500) String introImageUrl;
    @Size(max=120)
    @Comment(value="\u5174\u5efa\u5e74\u4ee3\uff08\u81ea\u7531\u6587\u672c\uff0c\u5982 2004~2006\uff09")
    private @Size(max=120) String constructionEra;
    @Comment(value="\u5f00\u653e\u65e5\u671f")
    private LocalDate openedOn;
    @Column(columnDefinition="TEXT")
    @Comment(value="\u91cd\u8981\u65e5\u671f\u8bb0\u4e8b\uff08\u6269\u5efa\u3001\u804c\u68d2\u9996\u6218\u3001\u660e\u661f\u8d5b\u3001\u603b\u51a0\u519b\u8d5b\u7b49\uff0c\u591a\u884c\uff09")
    private String importantDatesNote;
    @BatchSize(size=32)
    @OneToMany(mappedBy="stadium", cascade={CascadeType.ALL}, orphanRemoval=true)
    @OrderBy(value="sortOrder ASC, id ASC")
    private List<StadiumHomeTeam> homeTeams = new ArrayList();

    @Generated
    public Stadium() {
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
    public String getPostalCode() {
        return this.postalCode;
    }

    @Generated
    public String getAddressDetail() {
        return this.addressDetail;
    }

    @Generated
    public Point getLocation() {
        return this.location;
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
    public List<StadiumHomeTeam> getHomeTeams() {
        return this.homeTeams;
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
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    @Generated
    public void setAddressDetail(String addressDetail) {
        this.addressDetail = addressDetail;
    }

    @Generated
    public void setLocation(Point location) {
        this.location = location;
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
    public void setHomeTeams(List<StadiumHomeTeam> homeTeams) {
        this.homeTeams = homeTeams;
    }

    @Generated
    public String toString() {
        return "Stadium(tenantId=" + this.getTenantId() + ", name=" + this.getName() + ", shortName=" + this.getShortName() + ", shortNameEn=" + this.getShortNameEn() + ", addrProvince=" + this.getAddrProvince() + ", addrCity=" + this.getAddrCity() + ", addrDistrict=" + this.getAddrDistrict() + ", addrDistrictAdcode=" + this.getAddrDistrictAdcode() + ", postalCode=" + this.getPostalCode() + ", addressDetail=" + this.getAddressDetail() + ", location=" + String.valueOf(this.getLocation()) + ", level=" + String.valueOf(this.getLevel()) + ", operatingStatus=" + String.valueOf(this.getOperatingStatus()) + ", capacityTotal=" + this.getCapacityTotal() + ", seatingCapacity=" + this.getSeatingCapacity() + ", recordAttendance=" + this.getRecordAttendance() + ", fieldDistanceLeftM=" + this.getFieldDistanceLeftM() + ", fieldDistanceCenterM=" + this.getFieldDistanceCenterM() + ", fieldDistanceRightM=" + this.getFieldDistanceRightM() + ", turfType=" + String.valueOf(this.getTurfType()) + ", roofType=" + String.valueOf(this.getRoofType()) + ", contactPhone=" + this.getContactPhone() + ", hasLargeScreen=" + this.getHasLargeScreen() + ", introduction=" + this.getIntroduction() + ", transportationInfo=" + this.getTransportationInfo() + ", layoutDiagramUrl=" + this.getLayoutDiagramUrl() + ", introImageUrl=" + this.getIntroImageUrl() + ", constructionEra=" + this.getConstructionEra() + ", openedOn=" + String.valueOf(this.getOpenedOn()) + ", importantDatesNote=" + this.getImportantDatesNote() + ", homeTeams=" + String.valueOf(this.getHomeTeams()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Stadium)) {
            return false;
        }
        Stadium other = (Stadium)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
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
        Point this$location = this.getLocation();
        Point other$location = other.getLocation();
        if (this$location == null ? other$location != null : !this$location.equals(other$location)) {
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
        return !(this$homeTeams == null ? other$homeTeams != null : !((Object)this$homeTeams).equals(other$homeTeams));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof Stadium;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
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
        String $postalCode = this.getPostalCode();
        result = result * 59 + ($postalCode == null ? 43 : $postalCode.hashCode());
        String $addressDetail = this.getAddressDetail();
        result = result * 59 + ($addressDetail == null ? 43 : $addressDetail.hashCode());
        Point $location = this.getLocation();
        result = result * 59 + ($location == null ? 43 : $location.hashCode());
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
        return result;
    }
}

