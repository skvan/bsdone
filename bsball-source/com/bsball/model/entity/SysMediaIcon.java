/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysMediaIcon
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_media_icon")
@Comment(value="SVG \u56fe\u6807\u5e93")
public class SysMediaIcon
extends BaseEntity {
    @Comment(value="\u6240\u5c5e\u79df\u6237")
    private Long tenantId;
    @Column(nullable=false, length=128)
    @Comment(value="\u552f\u4e00\u540d\u79f0")
    private String name;
    @Column(nullable=false, columnDefinition="text")
    @Comment(value="SVG \u6e90\u7801")
    private String svgContent;
    @Column(length=500)
    @Comment(value="\u6807\u7b7e\uff0c\u9017\u53f7\u5206\u9694")
    private String tags;
    @Comment(value="\u5c55\u793a\u7f29\u653e")
    private Double scale = 1.0;
    @Comment(value="\u5c55\u793a\u65cb\u8f6c\u89d2\u5ea6")
    @Column(name="rotate_deg")
    private Double rotateDeg = 0.0;
    @Column(length=128)
    @Comment(value="Font Class / \u7c7b\u540d\uff08\u79df\u6237\u5185\u552f\u4e00\uff09\uff0c\u5982 icon-home")
    private String fontSymbol;
    @Comment(value="\u5c55\u793a\u5e73\u79fb X\uff08px\uff09")
    @Column(name="offset_x")
    private Double offsetX = 0.0;
    @Comment(value="\u5c55\u793a\u5e73\u79fb Y\uff08px\uff09")
    @Column(name="offset_y")
    private Double offsetY = 0.0;
    @Column(length=32)
    @Comment(value="\u9ed8\u8ba4\u586b\u5145\u8272\uff0c\u5982 #333 \u6216 currentColor")
    private String fillColor = "currentColor";
    @Column(name="part_styles_json", columnDefinition="text")
    @Comment(value="\u5206\u6bb5\u6837\u5f0f JSON\uff1a[{index,scale,rotateDeg,translateX,translateY,fill}]")
    private String partStylesJson;
    @Column(length=500)
    @Comment(value="\u5907\u6ce8")
    private String remark;

    @Generated
    public SysMediaIcon() {
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
    public String getSvgContent() {
        return this.svgContent;
    }

    @Generated
    public String getTags() {
        return this.tags;
    }

    @Generated
    public Double getScale() {
        return this.scale;
    }

    @Generated
    public Double getRotateDeg() {
        return this.rotateDeg;
    }

    @Generated
    public String getFontSymbol() {
        return this.fontSymbol;
    }

    @Generated
    public Double getOffsetX() {
        return this.offsetX;
    }

    @Generated
    public Double getOffsetY() {
        return this.offsetY;
    }

    @Generated
    public String getFillColor() {
        return this.fillColor;
    }

    @Generated
    public String getPartStylesJson() {
        return this.partStylesJson;
    }

    @Generated
    public String getRemark() {
        return this.remark;
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
    public void setSvgContent(String svgContent) {
        this.svgContent = svgContent;
    }

    @Generated
    public void setTags(String tags) {
        this.tags = tags;
    }

    @Generated
    public void setScale(Double scale) {
        this.scale = scale;
    }

    @Generated
    public void setRotateDeg(Double rotateDeg) {
        this.rotateDeg = rotateDeg;
    }

    @Generated
    public void setFontSymbol(String fontSymbol) {
        this.fontSymbol = fontSymbol;
    }

    @Generated
    public void setOffsetX(Double offsetX) {
        this.offsetX = offsetX;
    }

    @Generated
    public void setOffsetY(Double offsetY) {
        this.offsetY = offsetY;
    }

    @Generated
    public void setFillColor(String fillColor) {
        this.fillColor = fillColor;
    }

    @Generated
    public void setPartStylesJson(String partStylesJson) {
        this.partStylesJson = partStylesJson;
    }

    @Generated
    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Generated
    public String toString() {
        return "SysMediaIcon(tenantId=" + this.getTenantId() + ", name=" + this.getName() + ", svgContent=" + this.getSvgContent() + ", tags=" + this.getTags() + ", scale=" + this.getScale() + ", rotateDeg=" + this.getRotateDeg() + ", fontSymbol=" + this.getFontSymbol() + ", offsetX=" + this.getOffsetX() + ", offsetY=" + this.getOffsetY() + ", fillColor=" + this.getFillColor() + ", partStylesJson=" + this.getPartStylesJson() + ", remark=" + this.getRemark() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysMediaIcon)) {
            return false;
        }
        SysMediaIcon other = (SysMediaIcon)o;
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
        Double this$scale = this.getScale();
        Double other$scale = other.getScale();
        if (this$scale == null ? other$scale != null : !((Object)this$scale).equals(other$scale)) {
            return false;
        }
        Double this$rotateDeg = this.getRotateDeg();
        Double other$rotateDeg = other.getRotateDeg();
        if (this$rotateDeg == null ? other$rotateDeg != null : !((Object)this$rotateDeg).equals(other$rotateDeg)) {
            return false;
        }
        Double this$offsetX = this.getOffsetX();
        Double other$offsetX = other.getOffsetX();
        if (this$offsetX == null ? other$offsetX != null : !((Object)this$offsetX).equals(other$offsetX)) {
            return false;
        }
        Double this$offsetY = this.getOffsetY();
        Double other$offsetY = other.getOffsetY();
        if (this$offsetY == null ? other$offsetY != null : !((Object)this$offsetY).equals(other$offsetY)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$svgContent = this.getSvgContent();
        String other$svgContent = other.getSvgContent();
        if (this$svgContent == null ? other$svgContent != null : !this$svgContent.equals(other$svgContent)) {
            return false;
        }
        String this$tags = this.getTags();
        String other$tags = other.getTags();
        if (this$tags == null ? other$tags != null : !this$tags.equals(other$tags)) {
            return false;
        }
        String this$fontSymbol = this.getFontSymbol();
        String other$fontSymbol = other.getFontSymbol();
        if (this$fontSymbol == null ? other$fontSymbol != null : !this$fontSymbol.equals(other$fontSymbol)) {
            return false;
        }
        String this$fillColor = this.getFillColor();
        String other$fillColor = other.getFillColor();
        if (this$fillColor == null ? other$fillColor != null : !this$fillColor.equals(other$fillColor)) {
            return false;
        }
        String this$partStylesJson = this.getPartStylesJson();
        String other$partStylesJson = other.getPartStylesJson();
        if (this$partStylesJson == null ? other$partStylesJson != null : !this$partStylesJson.equals(other$partStylesJson)) {
            return false;
        }
        String this$remark = this.getRemark();
        String other$remark = other.getRemark();
        return !(this$remark == null ? other$remark != null : !this$remark.equals(other$remark));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysMediaIcon;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Double $scale = this.getScale();
        result = result * 59 + ($scale == null ? 43 : ((Object)$scale).hashCode());
        Double $rotateDeg = this.getRotateDeg();
        result = result * 59 + ($rotateDeg == null ? 43 : ((Object)$rotateDeg).hashCode());
        Double $offsetX = this.getOffsetX();
        result = result * 59 + ($offsetX == null ? 43 : ((Object)$offsetX).hashCode());
        Double $offsetY = this.getOffsetY();
        result = result * 59 + ($offsetY == null ? 43 : ((Object)$offsetY).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $svgContent = this.getSvgContent();
        result = result * 59 + ($svgContent == null ? 43 : $svgContent.hashCode());
        String $tags = this.getTags();
        result = result * 59 + ($tags == null ? 43 : $tags.hashCode());
        String $fontSymbol = this.getFontSymbol();
        result = result * 59 + ($fontSymbol == null ? 43 : $fontSymbol.hashCode());
        String $fillColor = this.getFillColor();
        result = result * 59 + ($fillColor == null ? 43 : $fillColor.hashCode());
        String $partStylesJson = this.getPartStylesJson();
        result = result * 59 + ($partStylesJson == null ? 43 : $partStylesJson.hashCode());
        String $remark = this.getRemark();
        result = result * 59 + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }
}

