package com.deldaran.politics.planet;

public class Terrain {
    public static final double RAYON_KM = 6371.0;

    private static final int COTE = 256;
    private static final int LARGEUR = COTE + 1;
    private static final float COMPRESSION_MER = 0.05f;
    private static final double PORTEE = 1.6;

    private static final double FREQUENCE_BASE = 53.0;
    private static final float GAIN = 0.5f;
    private static final float RELIEF_MER = 0.15f;
    private static final float RELIEF_PLAINE = 0.3f;
    private static final float RELIEF_MONTAGNE = 1.2f;
    private static final float DEBUT_CRETES = 0.8f;
    private static final float ETENDUE_CRETES = 1.5f;

    private static final double FREQUENCE_CLIMAT = 40.0;
    private static final int OCTAVES_CLIMAT = 4;
    private static final float VARIATION_TEMPERATURE = 3f;
    private static final float VARIATION_PLUIE = 0.3f;
    private static final float REFROIDISSEMENT = 6.5f;

    public final float hauteurPlaines;

    private final int graine;
    private final float[][] altitude = new float[Cube.FACES][LARGEUR * LARGEUR];
    private final float[][] temperature = new float[Cube.FACES][LARGEUR * LARGEUR];
    private final float[][] pluie = new float[Cube.FACES][LARGEUR * LARGEUR];

    private final double rayon2;
    private final int[] marque;
    private int tour = 0;
    private double poids, sommeH, sommeT, sommeP;

    public Terrain(Monde monde, float hauteurPlaines) {
        this.hauteurPlaines = hauteurPlaines;
        graine = (int) monde.seed;

        Sphere s = monde.sphere;
        double espacement = Math.sqrt(4.0 * Math.PI / s.nombre);
        rayon2 = PORTEE * PORTEE * espacement * espacement;
        marque = new int[s.nombre];
        double[] d = new double[3];

        for (int face = 0; face < Cube.FACES; face++) {
            for (int y = 0; y < LARGEUR; y++) {
                for (int x = 0; x < LARGEUR; x++) {
                    double a = 2.0 * x / COTE - 1.0;
                    double b = 2.0 * y / COTE - 1.0;
                    Cube.direction(face, a, b, d);
                    remplir(monde, d, face, y * LARGEUR + x);
                }
            }
        }
    }

    private void remplir(Monde monde, double[] d, int face, int indice) {
        Sphere s = monde.sphere;
        int proche = s.plusProche((float) d[0], (float) d[1], (float) d[2]);
        poids = sommeH = sommeT = sommeP = 0.0;
        tour++;

        ajouter(monde, proche, d);
        for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
            int v = s.voisin(proche, k);
            ajouter(monde, v, d);
            for (int l = 0; l < Sphere.NOMBRE_VOISINS; l++) {
                ajouter(monde, s.voisin(v, l), d);
            }
        }
        altitude[face][indice] = (float) (sommeH / poids);
        temperature[face][indice] = (float) (sommeT / poids);
        pluie[face][indice] = (float) (sommeP / poids);
    }

    private void ajouter(Monde monde, int i, double[] d) {
        if (marque[i] == tour) return;
        marque[i] = tour;
        Sphere s = monde.sphere;
        double ex = s.x[i] - d[0], ey = s.y[i] - d[1], ez = s.z[i] - d[2];
        double reste = 1.0 - (ex * ex + ey * ey + ez * ez) / rayon2;
        if (reste <= 0.0) return;
        double w = reste * reste;
        poids += w;
        sommeH += w * compresser(monde.altitude[i] - monde.niveauMer);
        sommeT += w * monde.temperature[i];
        sommeP += w * monde.pluie[i];
    }

    private static float compresser(float h) {
        return h < 0f ? h * COMPRESSION_MER : h;
    }

    private static float decompresser(float h) {
        return h < 0f ? h / COMPRESSION_MER : h;
    }

    public void echantillonner(double x, double y, double z, int octaves, Point sortie) {
        int face = Cube.face(x, y, z);
        double gx = (Cube.coordonneeA(face, x, y, z) + 1.0) * 0.5 * COTE;
        double gy = (Cube.coordonneeB(face, x, y, z) + 1.0) * 0.5 * COTE;
        int ix = Math.max(0, Math.min(LARGEUR - 2, (int) gx));
        int iy = Math.max(0, Math.min(LARGEUR - 2, (int) gy));
        float fx = (float) (gx - ix), fy = (float) (gy - iy);
        int indice = iy * LARGEUR + ix;

        float base = decompresser(interpoler(altitude[face], indice, fx, fy));
        sortie.temperature = interpoler(temperature[face], indice, fx, fy);
        sortie.pluie = interpoler(pluie[face], indice, fx, fy);
        sortie.altitude = base + detail(x, y, z, octaves, base);
        sortie.base = base;
    }

    private float interpoler(float[] grille, int i, float fx, float fy) {
        float bas = grille[i] + (grille[i + 1] - grille[i]) * fx;
        float haut = grille[i + LARGEUR] + (grille[i + LARGEUR + 1] - grille[i + LARGEUR]) * fx;
        return bas + (haut - bas) * fy;
    }

    private float detail(double x, double y, double z, int octaves, float base) {
        double frequence = FREQUENCE_BASE;
        float amplitude = 1f, doux = 0f, cretes = 0f;
        for (int o = 0; o < octaves; o++) {
            float n = Bruit.perlin(x * frequence, y * frequence, z * frequence, graine + o);
            doux += amplitude * n;
            cretes += amplitude * (0.35f - Math.abs(n)) * 2f;
            frequence *= 2.0;
            amplitude *= GAIN;
        }
        float montagne = Math.max(0f, Math.min(1f, (base - DEBUT_CRETES) / ETENDUE_CRETES));
        float terre = Math.max(0f, Math.min(1f, (base + 0.5f) / 0.8f));
        float relief = RELIEF_MER + terre * (RELIEF_PLAINE - RELIEF_MER + montagne * RELIEF_MONTAGNE);
        return relief * (doux + (cretes - doux) * montagne);
    }

    public byte biome(double x, double y, double z, Point point) {
        float variation = 0f, amplitude = 1f;
        double frequence = FREQUENCE_CLIMAT;
        for (int o = 0; o < OCTAVES_CLIMAT; o++) {
            variation += amplitude * Bruit.perlin(x * frequence, y * frequence, z * frequence, graine + 100 + o);
            frequence *= 2.0;
            amplitude *= 0.5f;
        }
        boolean sousLEau = point.altitude < 0f;
        float ecart = sousLEau ? 0f : Math.max(point.altitude, 0f) - Math.max(point.base, 0f);
        point.temperature += VARIATION_TEMPERATURE * variation - REFROIDISSEMENT * ecart;
        float pluieLocale = point.pluie * (1f + VARIATION_PLUIE * variation);
        return Biome.choisir(point.temperature, pluieLocale, sousLEau, point.altitude - hauteurPlaines);
    }

    public static final class Point {
        public float altitude;
        public float base;
        public float temperature;
        public float pluie;
    }
}
