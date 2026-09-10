/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.service.iplocation.IpGcj02
 *  org.locationtech.jts.geom.Coordinate
 *  org.locationtech.jts.geom.GeometryFactory
 *  org.locationtech.jts.geom.Point
 *  org.locationtech.jts.geom.Polygon
 *  org.locationtech.jts.geom.PrecisionModel
 */
package com.bsball.service.iplocation;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

public final class IpGcj02 {
    private static final GeometryFactory GEOMETRY_4326 = new GeometryFactory(new PrecisionModel(), 4326);

    private IpGcj02() {
    }

    public static Point point(double lng, double lat) {
        return GEOMETRY_4326.createPoint(new Coordinate(lng, lat));
    }

    public static Point polygonCentroid(Polygon poly) {
        if (poly == null) {
            return null;
        }
        Point c = poly.getCentroid();
        Coordinate coord = c.getCoordinate();
        return GEOMETRY_4326.createPoint(new Coordinate(coord.x, coord.y));
    }
}

