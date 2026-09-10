/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.IpLocationCache
 *  com.bsball.model.enums.LbsProvider
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.EnumType
 *  jakarta.persistence.Enumerated
 *  jakarta.persistence.Id
 *  jakarta.persistence.PostLoad
 *  jakarta.persistence.PostPersist
 *  jakarta.persistence.Table
 *  jakarta.persistence.Transient
 *  lombok.Generated
 *  org.hibernate.annotations.ColumnDefault
 *  org.hibernate.annotations.Comment
 *  org.hibernate.annotations.JdbcTypeCode
 *  org.locationtech.jts.geom.Point
 *  org.locationtech.jts.geom.Polygon
 *  org.springframework.data.domain.Persistable
 */
package com.bsball.model.entity;

import com.bsball.model.enums.LbsProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import lombok.Generated;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name="ip_location_cache")
@Comment(value="IP \u5f52\u5c5e\u5730\u7f13\u5b58")
public class IpLocationCache
implements Persistable<String> {
    @Id
    @Column(length=128)
    @Comment(value="IP")
    private String ip;
    @Column(name="region_text", length=512, nullable=false)
    @Comment(value="\u5f52\u5c5e\u5730\u5c55\u793a\u6587\u6848")
    private String regionText;
    @Column(length=64)
    @Comment(value="\u7701\u7ea7\u884c\u653f\u533a\u540d\u79f0")
    private String province;
    @Column(length=128)
    @Comment(value="\u5e02\u7ea7\u884c\u653f\u533a\u540d\u79f0")
    private String city;
    @Column(length=12)
    @Comment(value="\u884c\u653f\u533a\u5212\u4ee3\u7801")
    private String adcode;
    @Enumerated(value=EnumType.STRING)
    @Column(name="lbs_provider", length=16)
    @Comment(value="\u6570\u636e\u6765\u6e90 LBS")
    private LbsProvider lbsProvider;
    @JdbcTypeCode(value=3200)
    @Comment(value="IP \u53ef\u80fd\u4f4d\u7f6e\u77e9\u5f62\uff08\u9ad8\u5fb7\u6709\uff1b\u9876\u70b9\u4e3a GCJ-02\uff0cgeometry Polygon,4326\uff09")
    private Polygon rectangle;
    @JdbcTypeCode(value=3200)
    @Comment(value="IP \u8fd1\u4f3c\u4f4d\u7f6e\u70b9\uff08GCJ-02\uff09\uff1b\u767e\u5ea6 BD-09 \u7ecf\u8f6c\u6362\u540e\u5199\u5165\uff1b\u9ad8\u5fb7\u4e3a\u77e9\u5f62\u8d28\u5fc3\uff1b\u817e\u8baf\u4e3a\u63a5\u53e3\u70b9")
    private Point location;
    @Column(name="fetched_at", nullable=false)
    @Comment(value="\u6700\u8fd1\u4e00\u6b21\u89e3\u6790\u6210\u529f\u65f6\u95f4")
    private LocalDateTime fetchedAt;
    @Column(name="created_at")
    @Comment(value="\u9996\u6b21\u5199\u5165\u7f13\u5b58\u7684\u65f6\u95f4\uff08\u5386\u53f2\u884c\u53ef\u7531\u8fc1\u79fb\u4ece fetched_at \u56de\u586b\uff09")
    private LocalDateTime createdAt;
    @ColumnDefault(value="0")
    @Column(name="resolve_count", nullable=false)
    @Comment(value="\u7ba1\u7406\u7aef\u300c\u91cd\u65b0\u89e3\u6790\u300d\u6210\u529f\u7d2f\u8ba1\u6b21\u6570")
    private int resolveCount;
    @Transient
    private boolean newEntity = true;

    public String getId() {
        return this.ip;
    }

    public boolean isNew() {
        return this.newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }

    @Generated
    public IpLocationCache() {
    }

    @Generated
    public String getIp() {
        return this.ip;
    }

    @Generated
    public String getRegionText() {
        return this.regionText;
    }

    @Generated
    public String getProvince() {
        return this.province;
    }

    @Generated
    public String getCity() {
        return this.city;
    }

    @Generated
    public String getAdcode() {
        return this.adcode;
    }

    @Generated
    public LbsProvider getLbsProvider() {
        return this.lbsProvider;
    }

    @Generated
    public Polygon getRectangle() {
        return this.rectangle;
    }

    @Generated
    public Point getLocation() {
        return this.location;
    }

    @Generated
    public LocalDateTime getFetchedAt() {
        return this.fetchedAt;
    }

    @Generated
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @Generated
    public int getResolveCount() {
        return this.resolveCount;
    }

    @Generated
    public boolean isNewEntity() {
        return this.newEntity;
    }

    @Generated
    public void setIp(String ip) {
        this.ip = ip;
    }

    @Generated
    public void setRegionText(String regionText) {
        this.regionText = regionText;
    }

    @Generated
    public void setProvince(String province) {
        this.province = province;
    }

    @Generated
    public void setCity(String city) {
        this.city = city;
    }

    @Generated
    public void setAdcode(String adcode) {
        this.adcode = adcode;
    }

    @Generated
    public void setLbsProvider(LbsProvider lbsProvider) {
        this.lbsProvider = lbsProvider;
    }

    @Generated
    public void setRectangle(Polygon rectangle) {
        this.rectangle = rectangle;
    }

    @Generated
    public void setLocation(Point location) {
        this.location = location;
    }

    @Generated
    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    @Generated
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Generated
    public void setResolveCount(int resolveCount) {
        this.resolveCount = resolveCount;
    }

    @Generated
    public void setNewEntity(boolean newEntity) {
        this.newEntity = newEntity;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof IpLocationCache)) {
            return false;
        }
        IpLocationCache other = (IpLocationCache)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getResolveCount() != other.getResolveCount()) {
            return false;
        }
        if (this.isNewEntity() != other.isNewEntity()) {
            return false;
        }
        String this$ip = this.getIp();
        String other$ip = other.getIp();
        if (this$ip == null ? other$ip != null : !this$ip.equals(other$ip)) {
            return false;
        }
        String this$regionText = this.getRegionText();
        String other$regionText = other.getRegionText();
        if (this$regionText == null ? other$regionText != null : !this$regionText.equals(other$regionText)) {
            return false;
        }
        String this$province = this.getProvince();
        String other$province = other.getProvince();
        if (this$province == null ? other$province != null : !this$province.equals(other$province)) {
            return false;
        }
        String this$city = this.getCity();
        String other$city = other.getCity();
        if (this$city == null ? other$city != null : !this$city.equals(other$city)) {
            return false;
        }
        String this$adcode = this.getAdcode();
        String other$adcode = other.getAdcode();
        if (this$adcode == null ? other$adcode != null : !this$adcode.equals(other$adcode)) {
            return false;
        }
        LbsProvider this$lbsProvider = this.getLbsProvider();
        LbsProvider other$lbsProvider = other.getLbsProvider();
        if (this$lbsProvider == null ? other$lbsProvider != null : !this$lbsProvider.equals(other$lbsProvider)) {
            return false;
        }
        Polygon this$rectangle = this.getRectangle();
        Polygon other$rectangle = other.getRectangle();
        if (this$rectangle == null ? other$rectangle != null : !this$rectangle.equals(other$rectangle)) {
            return false;
        }
        Point this$location = this.getLocation();
        Point other$location = other.getLocation();
        if (this$location == null ? other$location != null : !this$location.equals(other$location)) {
            return false;
        }
        LocalDateTime this$fetchedAt = this.getFetchedAt();
        LocalDateTime other$fetchedAt = other.getFetchedAt();
        if (this$fetchedAt == null ? other$fetchedAt != null : !((Object)this$fetchedAt).equals(other$fetchedAt)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof IpLocationCache;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getResolveCount();
        result = result * 59 + (this.isNewEntity() ? 79 : 97);
        String $ip = this.getIp();
        result = result * 59 + ($ip == null ? 43 : $ip.hashCode());
        String $regionText = this.getRegionText();
        result = result * 59 + ($regionText == null ? 43 : $regionText.hashCode());
        String $province = this.getProvince();
        result = result * 59 + ($province == null ? 43 : $province.hashCode());
        String $city = this.getCity();
        result = result * 59 + ($city == null ? 43 : $city.hashCode());
        String $adcode = this.getAdcode();
        result = result * 59 + ($adcode == null ? 43 : $adcode.hashCode());
        LbsProvider $lbsProvider = this.getLbsProvider();
        result = result * 59 + ($lbsProvider == null ? 43 : $lbsProvider.hashCode());
        Polygon $rectangle = this.getRectangle();
        result = result * 59 + ($rectangle == null ? 43 : $rectangle.hashCode());
        Point $location = this.getLocation();
        result = result * 59 + ($location == null ? 43 : $location.hashCode());
        LocalDateTime $fetchedAt = this.getFetchedAt();
        result = result * 59 + ($fetchedAt == null ? 43 : ((Object)$fetchedAt).hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "IpLocationCache(ip=" + this.getIp() + ", regionText=" + this.getRegionText() + ", province=" + this.getProvince() + ", city=" + this.getCity() + ", adcode=" + this.getAdcode() + ", lbsProvider=" + String.valueOf(this.getLbsProvider()) + ", rectangle=" + String.valueOf(this.getRectangle()) + ", location=" + String.valueOf(this.getLocation()) + ", fetchedAt=" + String.valueOf(this.getFetchedAt()) + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", resolveCount=" + this.getResolveCount() + ", newEntity=" + this.isNewEntity() + ")";
    }
}

