/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.vo.IpLocationCacheRowVo
 *  lombok.Generated
 */
package com.bsball.model.vo;

import lombok.Generated;

public class IpLocationCacheRowVo {
    private String ip;
    private String regionText;
    private String province;
    private String city;
    private String adcode;
    private String lbsProvider;
    private String fetchedAt;
    private String createdAt;
    private Integer resolveCount;
    private String createdAtDisplay;

    @Generated
    public IpLocationCacheRowVo() {
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
    public String getLbsProvider() {
        return this.lbsProvider;
    }

    @Generated
    public String getFetchedAt() {
        return this.fetchedAt;
    }

    @Generated
    public String getCreatedAt() {
        return this.createdAt;
    }

    @Generated
    public Integer getResolveCount() {
        return this.resolveCount;
    }

    @Generated
    public String getCreatedAtDisplay() {
        return this.createdAtDisplay;
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
    public void setLbsProvider(String lbsProvider) {
        this.lbsProvider = lbsProvider;
    }

    @Generated
    public void setFetchedAt(String fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    @Generated
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Generated
    public void setResolveCount(Integer resolveCount) {
        this.resolveCount = resolveCount;
    }

    @Generated
    public void setCreatedAtDisplay(String createdAtDisplay) {
        this.createdAtDisplay = createdAtDisplay;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof IpLocationCacheRowVo)) {
            return false;
        }
        IpLocationCacheRowVo other = (IpLocationCacheRowVo)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$resolveCount = this.getResolveCount();
        Integer other$resolveCount = other.getResolveCount();
        if (this$resolveCount == null ? other$resolveCount != null : !((Object)this$resolveCount).equals(other$resolveCount)) {
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
        String this$lbsProvider = this.getLbsProvider();
        String other$lbsProvider = other.getLbsProvider();
        if (this$lbsProvider == null ? other$lbsProvider != null : !this$lbsProvider.equals(other$lbsProvider)) {
            return false;
        }
        String this$fetchedAt = this.getFetchedAt();
        String other$fetchedAt = other.getFetchedAt();
        if (this$fetchedAt == null ? other$fetchedAt != null : !this$fetchedAt.equals(other$fetchedAt)) {
            return false;
        }
        String this$createdAt = this.getCreatedAt();
        String other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) {
            return false;
        }
        String this$createdAtDisplay = this.getCreatedAtDisplay();
        String other$createdAtDisplay = other.getCreatedAtDisplay();
        return !(this$createdAtDisplay == null ? other$createdAtDisplay != null : !this$createdAtDisplay.equals(other$createdAtDisplay));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof IpLocationCacheRowVo;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $resolveCount = this.getResolveCount();
        result = result * 59 + ($resolveCount == null ? 43 : ((Object)$resolveCount).hashCode());
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
        String $lbsProvider = this.getLbsProvider();
        result = result * 59 + ($lbsProvider == null ? 43 : $lbsProvider.hashCode());
        String $fetchedAt = this.getFetchedAt();
        result = result * 59 + ($fetchedAt == null ? 43 : $fetchedAt.hashCode());
        String $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : $createdAt.hashCode());
        String $createdAtDisplay = this.getCreatedAtDisplay();
        result = result * 59 + ($createdAtDisplay == null ? 43 : $createdAtDisplay.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "IpLocationCacheRowVo(ip=" + this.getIp() + ", regionText=" + this.getRegionText() + ", province=" + this.getProvince() + ", city=" + this.getCity() + ", adcode=" + this.getAdcode() + ", lbsProvider=" + this.getLbsProvider() + ", fetchedAt=" + this.getFetchedAt() + ", createdAt=" + this.getCreatedAt() + ", resolveCount=" + this.getResolveCount() + ", createdAtDisplay=" + this.getCreatedAtDisplay() + ")";
    }
}

