package com.deldaran.politics;

import com.badlogic.gdx.math.Matrix3;

public class GameCamera {
    private static final double LATITUDE_MAX = Math.toRadians(89.9);
    private static final double COSINUS_MIN = 0.1;

    public double vitessePixels = 500.0;
    public double rayonMin = 150.0;
    public double rayonMax = 3200000.0;


    public double longitude = 0.0;
    public double latitude = 0.0;
    public double rayonPixels = 330.0;

    private final Matrix3 repere = new Matrix3();

    public void deplacerPixels(double pixelsX, double pixelsY) {
        double cosinus = Math.max(COSINUS_MIN, Math.cos(latitude));
        longitude += pixelsX / (rayonPixels * cosinus);
        latitude += pixelsY / rayonPixels;
        latitude = Math.max(-LATITUDE_MAX, Math.min(LATITUDE_MAX, latitude));
        longitude = Math.IEEEremainder(longitude, 2.0 * Math.PI);
    }

    public void zoomer(double facteur) {
        rayonPixels = Math.max(rayonMin, Math.min(rayonMax, rayonPixels * facteur));
    }

    public Matrix3 repere() {
        double cosLon = Math.cos(longitude), sinLon = Math.sin(longitude);
        double cosLat = Math.cos(latitude), sinLat = Math.sin(latitude);
        float[] m = repere.val;
        m[0] = (float) -sinLon;
        m[1] = (float) cosLon;
        m[2] = 0f;
        m[3] = (float) (-sinLat * cosLon);
        m[4] = (float) (-sinLat * sinLon);
        m[5] = (float) cosLat;
        m[6] = (float) (cosLat * cosLon);
        m[7] = (float) (cosLat * sinLon);
        m[8] = (float) sinLat;
        return repere;
    }
}
