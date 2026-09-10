/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.service.iplocation.IpLocationRectangleParser
 *  org.locationtech.jts.geom.Coordinate
 *  org.locationtech.jts.geom.GeometryFactory
 *  org.locationtech.jts.geom.Polygon
 *  org.locationtech.jts.geom.PrecisionModel
 */
package com.bsball.service.iplocation;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

/*
 * Exception performing whole class analysis ignored.
 */
public final class IpLocationRectangleParser {
    private static final GeometryFactory GEOMETRY_4326 = new GeometryFactory(new PrecisionModel(), 4326);

    private IpLocationRectangleParser() {
    }

    public static Polygon parseAmapRectangle(String rectangle) {
        if (rectangle == null || rectangle.isBlank()) {
            return null;
        }
        String[] parts = rectangle.split(";");
        if (parts.length < 2) {
            return null;
        }
        double[] a = IpLocationRectangleParser.parseLngLat((String)parts[0].trim());
        double[] b = IpLocationRectangleParser.parseLngLat((String)parts[1].trim());
        if (a == null || b == null) {
            return null;
        }
        double minLng = Math.min(a[0], b[0]);
        double maxLng = Math.max(a[0], b[0]);
        double minLat = Math.min(a[1], b[1]);
        double maxLat = Math.max(a[1], b[1]);
        Coordinate[] ring = new Coordinate[]{new Coordinate(minLng, minLat), new Coordinate(maxLng, minLat), new Coordinate(maxLng, maxLat), new Coordinate(minLng, maxLat), new Coordinate(minLng, minLat)};
        return GEOMETRY_4326.createPolygon(ring);
    }

    private static double[] parseLngLat(String one) {
        int comma = one.indexOf(44);
        if (comma <= 0 || comma >= one.length() - 1) {
            return null;
        }
        try {
            double lng = Double.parseDouble(one.substring(0, comma).trim());
            double lat = Double.parseDouble(one.substring(comma + 1).trim());
            return new double[]{lng, lat};
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}

