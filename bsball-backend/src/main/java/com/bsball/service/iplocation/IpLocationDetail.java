/*
 * Decompiled with CFR 0.152.
 */
package com.bsball.service.iplocation;

import com.bsball.model.enums.LbsProvider;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public record IpLocationDetail(String formatted, String province, String city, String adcode, Polygon rectangle, Point location, LbsProvider provider) {
}