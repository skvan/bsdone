/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.ChinaRegion
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_china_region")
@Comment(value="\u4e2d\u56fd\u884c\u653f\u533a\u5212\uff08\u7cfb\u7edf\u57fa\u7840\u6570\u636e\uff09")
public class ChinaRegion {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Comment(value="\u4e3b\u952e")
    private Long id;
    @Column(nullable=false, unique=true, length=12)
    @Comment(value="\u533a\u5212\u4ee3\u7801\uff08\u7701\u5730\u5e02\u591a\u4e3a6\u4f4d\uff1b\u9547\u8857\u7b49\u53ef\u523012\u4f4d\uff09")
    private String adcode;
    @Column(nullable=false, length=64)
    @Comment(value="\u540d\u79f0")
    private String name;
    @Column(nullable=false)
    @Comment(value="\u5c42\u7ea7\uff1a1\u7701 2\u5e02 3\u533a\u53bf\uff08\u4e0e Flyway smallint \u4e00\u81f4\uff0c\u907f\u514d Hibernate \u6539\u7c7b\u578b\u89e6\u53d1 Druid Wall \u8bef\u62e6\uff09")
    private short level;
    @Column(length=12)
    @Comment(value="\u4e0a\u7ea7\u533a\u5212\u4ee3\u7801")
    private String parentAdcode;

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public String getAdcode() {
        return this.adcode;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public short getLevel() {
        return this.level;
    }

    @Generated
    public String getParentAdcode() {
        return this.parentAdcode;
    }

    @Generated
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setAdcode(String adcode) {
        this.adcode = adcode;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setLevel(short level) {
        this.level = level;
    }

    @Generated
    public void setParentAdcode(String parentAdcode) {
        this.parentAdcode = parentAdcode;
    }
}

