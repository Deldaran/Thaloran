package com.deldaran.politics.planet;

public final class Bruit {
    private Bruit() {
    }

    private static int melanger(int x, int y, int z, int graine) {
        int h = x * 0x27d4eb2d ^ y * 0x165667b1 ^ z * 0x9e3779b1 ^ graine * 0x85ebca6b;
        h = (h ^ (h >>> 15)) * 0x2c1b3c6d;
        h = (h ^ (h >>> 12)) * 0x297a2d39;
        return h ^ (h >>> 15);
    }

    private static double gradient(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    private static double lisser(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double melange(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static float perlin(double x, double y, double z, int graine) {
        double bx = Math.floor(x), by = Math.floor(y), bz = Math.floor(z);
        int ix = (int) bx, iy = (int) by, iz = (int) bz;
        double fx = x - bx, fy = y - by, fz = z - bz;
        double u = lisser(fx), v = lisser(fy), w = lisser(fz);

        double a = gradient(melanger(ix, iy, iz, graine), fx, fy, fz);
        double b = gradient(melanger(ix + 1, iy, iz, graine), fx - 1, fy, fz);
        double c = gradient(melanger(ix, iy + 1, iz, graine), fx, fy - 1, fz);
        double d = gradient(melanger(ix + 1, iy + 1, iz, graine), fx - 1, fy - 1, fz);
        double e = gradient(melanger(ix, iy, iz + 1, graine), fx, fy, fz - 1);
        double f = gradient(melanger(ix + 1, iy, iz + 1, graine), fx - 1, fy, fz - 1);
        double g = gradient(melanger(ix, iy + 1, iz + 1, graine), fx, fy - 1, fz - 1);
        double h = gradient(melanger(ix + 1, iy + 1, iz + 1, graine), fx - 1, fy - 1, fz - 1);

        double bas = melange(melange(a, b, u), melange(c, d, u), v);
        double haut = melange(melange(e, f, u), melange(g, h, u), v);
        return (float) melange(bas, haut, w);
    }
}
