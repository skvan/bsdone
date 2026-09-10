/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.enums.LbsProvider
 *  com.bsball.service.iplocation.IpLocationDetail
 *  org.locationtech.jts.geom.Point
 *  org.locationtech.jts.geom.Polygon
 */
package com.bsball.service.iplocation;

import com.bsball.model.enums.LbsProvider;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public record IpLocationDetail(String formatted, String province, String city, String adcode, Polygon rectangle, Point location, LbsProvider provider) {
    private final String formatted;
    private final String province;
    private final String city;
    private final String adcode;
    private final Polygon rectangle;
    private final Point location;
    private final LbsProvider provider;

    public IpLocationDetail(String formatted, String province, String city, String adcode, Polygon rectangle, Point location, LbsProvider provider) {
        this.formatted = formatted;
        this.province = province;
        this.city = city;
        this.adcode = adcode;
        this.rectangle = rectangle;
        this.location = location;
        this.provider = provider;
    }

    public String formatted() {
        return this.formatted;
    }

    public String province() {
        return this.province;
    }

    public String city() {
        return this.city;
    }

    public String adcode() {
        return this.adcode;
    }

    public Polygon rectangle() {
        return this.rectangle;
    }

    public Point location() {
        return this.location;
    }

    public LbsProvider provider() {
        return this.provider;
    }
}

