package com.deldaran.politics;

import com.deldaran.politics.planet.Terrain;

public final class MicroRelief {
    public static final double CELLULE = 12.0;
    public static final float AMPLITUDE = 4f;
    public static final float DEBUT = 0.5f;
    public static final float PLEIN = 1.1f;
    private static final double RAYON_METRES = Terrain.RAYON_KM * 1000.0;

    private MicroRelief() {
    }

    public static float facteur(float altitude, float plaines) {
        float t = Math.max(0f, Math.min(1f, (altitude - plaines - DEBUT) / (PLEIN - DEBUT)));
        return t * t * (3f - 2f * t);
    }

    public static float hauteur(double x, double y, double z, float altitude, float plaines) {
        float f = facteur(altitude, plaines);
        if (f <= 0f) return 0f;
        double k = RAYON_METRES / CELLULE;
        return AMPLITUDE * f * (0.72f * bruit(x * k, y * k, z * k) + 0.28f * bruit(x * k * 3.0, y * k * 3.0, z * k * 3.0));
    }

    public static void base(double x, double y, double z, float[] sortie) {
        double k = RAYON_METRES / CELLULE;
        double cx = x * k, cy = y * k, cz = z * k;
        double bx = Math.floor(cx), by = Math.floor(cy), bz = Math.floor(cz);
        sortie[0] = (float) (cx - bx);
        sortie[1] = (float) (cy - by);
        sortie[2] = (float) (cz - bz);
        sortie[3] = (float) modulo(bx);
        sortie[4] = (float) modulo(by);
        sortie[5] = (float) modulo(bz);
        sortie[6] = (float) modulo(bx * 3.0);
        sortie[7] = (float) modulo(by * 3.0);
        sortie[8] = (float) modulo(bz * 3.0);
    }

    private static double modulo(double v) {
        return v - Math.floor(v / 289.0) * 289.0;
    }

    private static float bruit(double cx, double cy, double cz) {
        double ix = Math.floor(cx), iy = Math.floor(cy), iz = Math.floor(cz);
        float fx = (float) (cx - ix), fy = (float) (cy - iy), fz = (float) (cz - iz);
        float ax = (float) modulo(ix), ay = (float) modulo(iy), az = (float) modulo(iz);
        float bx = mod289(ax + 1f), by = mod289(ay + 1f), bz = mod289(az + 1f);
        float ux = lisse(fx), uy = lisse(fy), uz = lisse(fz);
        float x00 = melange(hasard3(ax, ay, az), hasard3(bx, ay, az), ux);
        float x10 = melange(hasard3(ax, by, az), hasard3(bx, by, az), ux);
        float x01 = melange(hasard3(ax, ay, bz), hasard3(bx, ay, bz), ux);
        float x11 = melange(hasard3(ax, by, bz), hasard3(bx, by, bz), ux);
        return melange(melange(x00, x10, uy), melange(x01, x11, uy), uz);
    }

    private static float lisse(float f) {
        return f * f * f * (f * (f * 6f - 15f) + 10f);
    }

    private static float melange(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float mod289(float x) {
        return x - (float) Math.floor(x / 289f) * 289f;
    }

    private static float permute(float x) {
        return mod289((x * 34f + 1f) * x);
    }

    private static float hasard3(float x, float y, float z) {
        return permute(permute(permute(mod289(x)) + mod289(y)) + mod289(z)) / 288f;
    }
}
