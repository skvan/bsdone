/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.json.PositionsJsonUtil
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.Player
 *  com.fasterxml.jackson.annotation.JsonIgnore
 *  com.fasterxml.jackson.annotation.JsonProperty
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  jakarta.validation.constraints.Size
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 *  org.hibernate.annotations.JdbcTypeCode
 */
package com.bsball.model.entity;

import com.bsball.common.json.PositionsJsonUtil;
import com.bsball.model.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name="bs_player")
@Comment(value="\u7403\u5458")
public class Player
extends BaseEntity {
    @Comment(value="\u5df2\u8ba4\u8bc1\u8ba4\u9886\u7684\u7528\u6237ID")
    private Long userId;
    @Comment(value="\u7403\u961fID")
    private Long teamId;
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @Comment(value="\u59d3\u540d")
    private String name;
    @Comment(value="\u7b80\u79f0")
    private String shortName;
    @Comment(value="\u82f1\u6587\u540d")
    private String nameEn;
    @Comment(value="\u6635\u79f0")
    private String nickname;
    @Comment(value="\u80cc\u53f7")
    private String number;
    @Column(columnDefinition="TEXT")
    @Comment(value="\u5b88\u5907\u4f4d\u7f6e")
    private String positions;
    @Comment(value="\u5934\u50cf")
    private String avatar;
    @Size(max=500)
    @Comment(value="\u80cc\u666f\u56fe")
    private @Size(max=500) String bgImage;
    @JdbcTypeCode(value=3001)
    @Column(columnDefinition="jsonb")
    @Comment(value="\u80cc\u666f\u56fe\u914d\u7f6e\uff08JSON\uff09")
    private Map<String, Object> bgFocusConfig;
    @JdbcTypeCode(value=3001)
    @Column(columnDefinition="jsonb")
    @Comment(value="\u80cc\u666f\u56fe\u5217\u8868\uff08JSON \u6570\u7ec4\uff09")
    private List<String> bgImages;
    @Comment(value="\u51fa\u751f\u65e5\u671f")
    private String birthDate;
    @Comment(value="\u51fa\u751f\u5730")
    private String birthPlace;
    @Comment(value="\u8eab\u9ad8")
    private String height;
    @Comment(value="\u4f53\u91cd")
    private String weight;
    @Comment(value="\u6295\u7403\u624b")
    private String throwHand;
    @Comment(value="\u6253\u51fb\u624b")
    private String batHand;
    @Comment(value="\u9009\u79c0")
    private String draft;
    @Comment(value="\u9996\u79c0")
    private String debut;
    @Comment(value="\u5b66\u5386")
    private String education;
    @Comment(value="\u5f53\u524d\u52a0\u5165\u8bb0\u5f55ID")
    private Long currentJoinRecordId;
    @Comment(value="\u72b6\u6001")
    private String status = "active";
    @Comment(value="\u8054\u7cfb\u7535\u8bdd")
    private String contactPhone;
    @Comment(value="\u8054\u7cfb\u90ae\u7bb1")
    private String contactEmail;
    @Column(length=2000)
    @Size(max=2000, message="\u7b80\u4ecb\u4e0d\u80fd\u8d85\u8fc72000\u5b57")
    @Comment(value="\u7b80\u4ecb")
    private @Size(max=2000, message="\u7b80\u4ecb\u4e0d\u80fd\u8d85\u8fc72000\u5b57") String intro;
    @Comment(value="\u6392\u5e8f")
    private Integer sort = 0;

    @JsonIgnore
    public String getPositions() {
        return this.positions;
    }

    @JsonIgnore
    public void setPositions(String positions) {
        this.positions = positions;
    }

    @JsonProperty(value="positions")
    public List<String> getPositionsList() {
        return PositionsJsonUtil.parseList((String)this.positions);
    }

    @JsonProperty(value="positions")
    public void setPositionsList(List<String> list) {
        this.positions = PositionsJsonUtil.toStorage(list);
    }

    @Generated
    public Player() {
    }

    @Generated
    public Long getUserId() {
        return this.userId;
    }

    @Generated
    public Long getTeamId() {
        return this.teamId;
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
    public String getNameEn() {
        return this.nameEn;
    }

    @Generated
    public String getNickname() {
        return this.nickname;
    }

    @Generated
    public String getNumber() {
        return this.number;
    }

    @Generated
    public String getAvatar() {
        return this.avatar;
    }

    @Generated
    public String getBgImage() {
        return this.bgImage;
    }

    @Generated
    public Map<String, Object> getBgFocusConfig() {
        return this.bgFocusConfig;
    }

    @Generated
    public List<String> getBgImages() {
        return this.bgImages;
    }

    @Generated
    public String getBirthDate() {
        return this.birthDate;
    }

    @Generated
    public String getBirthPlace() {
        return this.birthPlace;
    }

    @Generated
    public String getHeight() {
        return this.height;
    }

    @Generated
    public String getWeight() {
        return this.weight;
    }

    @Generated
    public String getThrowHand() {
        return this.throwHand;
    }

    @Generated
    public String getBatHand() {
        return this.batHand;
    }

    @Generated
    public String getDraft() {
        return this.draft;
    }

    @Generated
    public String getDebut() {
        return this.debut;
    }

    @Generated
    public String getEducation() {
        return this.education;
    }

    @Generated
    public Long getCurrentJoinRecordId() {
        return this.currentJoinRecordId;
    }

    @Generated
    public String getStatus() {
        return this.status;
    }

    @Generated
    public String getContactPhone() {
        return this.contactPhone;
    }

    @Generated
    public String getContactEmail() {
        return this.contactEmail;
    }

    @Generated
    public String getIntro() {
        return this.intro;
    }

    @Generated
    public Integer getSort() {
        return this.sort;
    }

    @Generated
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Generated
    public void setTeamId(Long teamId) {
        this.teamId = teamId;
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
    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    @Generated
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Generated
    public void setNumber(String number) {
        this.number = number;
    }

    @Generated
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Generated
    public void setBgImage(String bgImage) {
        this.bgImage = bgImage;
    }

    @Generated
    public void setBgFocusConfig(Map<String, Object> bgFocusConfig) {
        this.bgFocusConfig = bgFocusConfig;
    }

    @Generated
    public void setBgImages(List<String> bgImages) {
        this.bgImages = bgImages;
    }

    @Generated
    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    @Generated
    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    @Generated
    public void setHeight(String height) {
        this.height = height;
    }

    @Generated
    public void setWeight(String weight) {
        this.weight = weight;
    }

    @Generated
    public void setThrowHand(String throwHand) {
        this.throwHand = throwHand;
    }

    @Generated
    public void setBatHand(String batHand) {
        this.batHand = batHand;
    }

    @Generated
    public void setDraft(String draft) {
        this.draft = draft;
    }

    @Generated
    public void setDebut(String debut) {
        this.debut = debut;
    }

    @Generated
    public void setEducation(String education) {
        this.education = education;
    }

    @Generated
    public void setCurrentJoinRecordId(Long currentJoinRecordId) {
        this.currentJoinRecordId = currentJoinRecordId;
    }

    @Generated
    public void setStatus(String status) {
        this.status = status;
    }

    @Generated
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    @Generated
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    @Generated
    public void setIntro(String intro) {
        this.intro = intro;
    }

    @Generated
    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Generated
    public String toString() {
        return "Player(userId=" + this.getUserId() + ", teamId=" + this.getTeamId() + ", tenantId=" + this.getTenantId() + ", name=" + this.getName() + ", shortName=" + this.getShortName() + ", nameEn=" + this.getNameEn() + ", nickname=" + this.getNickname() + ", number=" + this.getNumber() + ", positions=" + this.getPositions() + ", avatar=" + this.getAvatar() + ", bgImage=" + this.getBgImage() + ", bgFocusConfig=" + String.valueOf(this.getBgFocusConfig()) + ", bgImages=" + String.valueOf(this.getBgImages()) + ", birthDate=" + this.getBirthDate() + ", birthPlace=" + this.getBirthPlace() + ", height=" + this.getHeight() + ", weight=" + this.getWeight() + ", throwHand=" + this.getThrowHand() + ", batHand=" + this.getBatHand() + ", draft=" + this.getDraft() + ", debut=" + this.getDebut() + ", education=" + this.getEducation() + ", currentJoinRecordId=" + this.getCurrentJoinRecordId() + ", status=" + this.getStatus() + ", contactPhone=" + this.getContactPhone() + ", contactEmail=" + this.getContactEmail() + ", intro=" + this.getIntro() + ", sort=" + this.getSort() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Player)) {
            return false;
        }
        Player other = (Player)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$userId = this.getUserId();
        Long other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !((Object)this$userId).equals(other$userId)) {
            return false;
        }
        Long this$teamId = this.getTeamId();
        Long other$teamId = other.getTeamId();
        if (this$teamId == null ? other$teamId != null : !((Object)this$teamId).equals(other$teamId)) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        Long this$currentJoinRecordId = this.getCurrentJoinRecordId();
        Long other$currentJoinRecordId = other.getCurrentJoinRecordId();
        if (this$currentJoinRecordId == null ? other$currentJoinRecordId != null : !((Object)this$currentJoinRecordId).equals(other$currentJoinRecordId)) {
            return false;
        }
        Integer this$sort = this.getSort();
        Integer other$sort = other.getSort();
        if (this$sort == null ? other$sort != null : !((Object)this$sort).equals(other$sort)) {
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
        String this$nameEn = this.getNameEn();
        String other$nameEn = other.getNameEn();
        if (this$nameEn == null ? other$nameEn != null : !this$nameEn.equals(other$nameEn)) {
            return false;
        }
        String this$nickname = this.getNickname();
        String other$nickname = other.getNickname();
        if (this$nickname == null ? other$nickname != null : !this$nickname.equals(other$nickname)) {
            return false;
        }
        String this$number = this.getNumber();
        String other$number = other.getNumber();
        if (this$number == null ? other$number != null : !this$number.equals(other$number)) {
            return false;
        }
        String this$positions = this.getPositions();
        String other$positions = other.getPositions();
        if (this$positions == null ? other$positions != null : !this$positions.equals(other$positions)) {
            return false;
        }
        String this$avatar = this.getAvatar();
        String other$avatar = other.getAvatar();
        if (this$avatar == null ? other$avatar != null : !this$avatar.equals(other$avatar)) {
            return false;
        }
        String this$bgImage = this.getBgImage();
        String other$bgImage = other.getBgImage();
        if (this$bgImage == null ? other$bgImage != null : !this$bgImage.equals(other$bgImage)) {
            return false;
        }
        Map this$bgFocusConfig = this.getBgFocusConfig();
        Map other$bgFocusConfig = other.getBgFocusConfig();
        if (this$bgFocusConfig == null ? other$bgFocusConfig != null : !((Object)this$bgFocusConfig).equals(other$bgFocusConfig)) {
            return false;
        }
        List this$bgImages = this.getBgImages();
        List other$bgImages = other.getBgImages();
        if (this$bgImages == null ? other$bgImages != null : !((Object)this$bgImages).equals(other$bgImages)) {
            return false;
        }
        String this$birthDate = this.getBirthDate();
        String other$birthDate = other.getBirthDate();
        if (this$birthDate == null ? other$birthDate != null : !this$birthDate.equals(other$birthDate)) {
            return false;
        }
        String this$birthPlace = this.getBirthPlace();
        String other$birthPlace = other.getBirthPlace();
        if (this$birthPlace == null ? other$birthPlace != null : !this$birthPlace.equals(other$birthPlace)) {
            return false;
        }
        String this$height = this.getHeight();
        String other$height = other.getHeight();
        if (this$height == null ? other$height != null : !this$height.equals(other$height)) {
            return false;
        }
        String this$weight = this.getWeight();
        String other$weight = other.getWeight();
        if (this$weight == null ? other$weight != null : !this$weight.equals(other$weight)) {
            return false;
        }
        String this$throwHand = this.getThrowHand();
        String other$throwHand = other.getThrowHand();
        if (this$throwHand == null ? other$throwHand != null : !this$throwHand.equals(other$throwHand)) {
            return false;
        }
        String this$batHand = this.getBatHand();
        String other$batHand = other.getBatHand();
        if (this$batHand == null ? other$batHand != null : !this$batHand.equals(other$batHand)) {
            return false;
        }
        String this$draft = this.getDraft();
        String other$draft = other.getDraft();
        if (this$draft == null ? other$draft != null : !this$draft.equals(other$draft)) {
            return false;
        }
        String this$debut = this.getDebut();
        String other$debut = other.getDebut();
        if (this$debut == null ? other$debut != null : !this$debut.equals(other$debut)) {
            return false;
        }
        String this$education = this.getEducation();
        String other$education = other.getEducation();
        if (this$education == null ? other$education != null : !this$education.equals(other$education)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$contactPhone = this.getContactPhone();
        String other$contactPhone = other.getContactPhone();
        if (this$contactPhone == null ? other$contactPhone != null : !this$contactPhone.equals(other$contactPhone)) {
            return false;
        }
        String this$contactEmail = this.getContactEmail();
        String other$contactEmail = other.getContactEmail();
        if (this$contactEmail == null ? other$contactEmail != null : !this$contactEmail.equals(other$contactEmail)) {
            return false;
        }
        String this$intro = this.getIntro();
        String other$intro = other.getIntro();
        return !(this$intro == null ? other$intro != null : !this$intro.equals(other$intro));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof Player;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : ((Object)$userId).hashCode());
        Long $teamId = this.getTeamId();
        result = result * 59 + ($teamId == null ? 43 : ((Object)$teamId).hashCode());
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Long $currentJoinRecordId = this.getCurrentJoinRecordId();
        result = result * 59 + ($currentJoinRecordId == null ? 43 : ((Object)$currentJoinRecordId).hashCode());
        Integer $sort = this.getSort();
        result = result * 59 + ($sort == null ? 43 : ((Object)$sort).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $shortName = this.getShortName();
        result = result * 59 + ($shortName == null ? 43 : $shortName.hashCode());
        String $nameEn = this.getNameEn();
        result = result * 59 + ($nameEn == null ? 43 : $nameEn.hashCode());
        String $nickname = this.getNickname();
        result = result * 59 + ($nickname == null ? 43 : $nickname.hashCode());
        String $number = this.getNumber();
        result = result * 59 + ($number == null ? 43 : $number.hashCode());
        String $positions = this.getPositions();
        result = result * 59 + ($positions == null ? 43 : $positions.hashCode());
        String $avatar = this.getAvatar();
        result = result * 59 + ($avatar == null ? 43 : $avatar.hashCode());
        String $bgImage = this.getBgImage();
        result = result * 59 + ($bgImage == null ? 43 : $bgImage.hashCode());
        Map $bgFocusConfig = this.getBgFocusConfig();
        result = result * 59 + ($bgFocusConfig == null ? 43 : ((Object)$bgFocusConfig).hashCode());
        List $bgImages = this.getBgImages();
        result = result * 59 + ($bgImages == null ? 43 : ((Object)$bgImages).hashCode());
        String $birthDate = this.getBirthDate();
        result = result * 59 + ($birthDate == null ? 43 : $birthDate.hashCode());
        String $birthPlace = this.getBirthPlace();
        result = result * 59 + ($birthPlace == null ? 43 : $birthPlace.hashCode());
        String $height = this.getHeight();
        result = result * 59 + ($height == null ? 43 : $height.hashCode());
        String $weight = this.getWeight();
        result = result * 59 + ($weight == null ? 43 : $weight.hashCode());
        String $throwHand = this.getThrowHand();
        result = result * 59 + ($throwHand == null ? 43 : $throwHand.hashCode());
        String $batHand = this.getBatHand();
        result = result * 59 + ($batHand == null ? 43 : $batHand.hashCode());
        String $draft = this.getDraft();
        result = result * 59 + ($draft == null ? 43 : $draft.hashCode());
        String $debut = this.getDebut();
        result = result * 59 + ($debut == null ? 43 : $debut.hashCode());
        String $education = this.getEducation();
        result = result * 59 + ($education == null ? 43 : $education.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $contactPhone = this.getContactPhone();
        result = result * 59 + ($contactPhone == null ? 43 : $contactPhone.hashCode());
        String $contactEmail = this.getContactEmail();
        result = result * 59 + ($contactEmail == null ? 43 : $contactEmail.hashCode());
        String $intro = this.getIntro();
        result = result * 59 + ($intro == null ? 43 : $intro.hashCode());
        return result;
    }
}

