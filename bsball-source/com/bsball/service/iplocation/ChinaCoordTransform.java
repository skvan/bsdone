/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.service.iplocation.ChinaCoordTransform
 */
package com.bsball.service.iplocation;

public final class ChinaCoordTransform {
    private static final double X_PI = 52.35987755982988;

    private ChinaCoordTransform() {
    }

    public static double[] bd09ToGcj02(double bdLng, double bdLat) {
        double x = bdLng - 0.0065;
        double y = bdLat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 2.0E-5 * Math.sin(y * 52.35987755982988);
        double theta = Math.atan2(y, x) - 3.0E-6 * Math.cos(x * 52.35987755982988);
        double gcjLng = z * Math.cos(theta);
        double gcjLat = z * Math.sin(theta);
        return new double[]{gcjLng, gcjLat};
    }
}

