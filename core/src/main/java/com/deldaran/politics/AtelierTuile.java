package com.deldaran.politics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.deldaran.politics.planet.Biome;
import com.deldaran.politics.planet.Cube;
import com.deldaran.politics.planet.Terrain;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class AtelierTuile {
    private static final int LARGEUR = Tuile.COTE + 2;
    private static final float FORCE_OMBRE = 5f;
    private static final float OMBRE_MIN = 0.55f;
    private static final float OMBRE_MAX = 1.3f;
    private static final float RACINE_DEUX = 0.70710678f;

    private final Terrain terrain;
    private final Terrain.Point point = new Terrain.Point();
    private final Color couleur = new Color();
    private final double[] d = new double[3];
    private final double[] px = new double[LARGEUR * LARGEUR];
    private final double[] py = new double[LARGEUR * LARGEUR];
    private final double[] pz = new double[LARGEUR * LARGEUR];
    private final float[] altitude = new float[LARGEUR * LARGEUR];
    private final float[] temperature = new float[LARGEUR * LARGEUR];
    private final byte[] biome = new byte[LARGEUR * LARGEUR];
    private final byte[] eau = new byte[LARGEUR * LARGEUR];

    private static final byte RIVIERE = 1;
    private static final byte LAC = 2;

    public AtelierTuile(Terrain terrain) {
        this.terrain = terrain;
    }

    public Pixmap generer(Tuile tuile) {
        for (int y = 0; y < LARGEUR; y++) {
            double b = tuile.coordonnee(tuile.y, (y - 0.5) / Tuile.COTE);
            for (int x = 0; x < LARGEUR; x++) {
                double a = tuile.coordonnee(tuile.x, (x - 0.5) / Tuile.COTE);
                Cube.direction(tuile.face, a, b, d);
                int i = y * LARGEUR + x;
                px[i] = d[0];
                py[i] = d[1];
                pz[i] = d[2];
                terrain.echantillonner(d[0], d[1], d[2], tuile.niveau, point);
                biome[i] = terrain.biome(d[0], d[1], d[2], point);
                altitude[i] = point.altitude;
                temperature[i] = point.temperature;
                eau[i] = Biome.estGele(biome[i]) ? 0 : (byte) (point.lac ? LAC : (point.riviere ? RIVIERE : 0));

            }
        }

        Pixmap image = new Pixmap(Tuile.COTE, Tuile.COTE, Pixmap.Format.RGBA8888);
        IntBuffer pixels = image.getPixels().order(ByteOrder.BIG_ENDIAN).asIntBuffer();
        for (int y = 0; y < Tuile.COTE; y++) {
            for (int x = 0; x < Tuile.COTE; x++) {
                int i = (y + 1) * LARGEUR + x + 1;
                choisirCouleur(i, tuile.biomes);
                if (altitude[i] >= 0f && eau[i] == 0) {
                    float ombre = calculerOmbre(i);
                    couleur.mul(ombre, ombre, ombre, 1f).clamp();
                }
                pixels.put(y * Tuile.COTE + x, Color.rgba8888(couleur));
            }
        }
        return image;
    }

    private void choisirCouleur(int i, boolean biomes) {
        if (altitude[i] >= 0f && eau[i] == LAC) {
            couleur.set(Palette.LAC);
        } else if (altitude[i] >= 0f && eau[i] == RIVIERE) {
            couleur.set(Palette.RIVIERE);
        } else if (!biomes) {
            Palette.relief(altitude[i], temperature[i], couleur);
        } else if (biome[i] == Biome.OCEAN) {
            Palette.ocean(-altitude[i], couleur);
        } else {
            Palette.biome(biome[i], couleur);
        }
    }

    private float calculerOmbre(int i) {
        int e = i + 1, w = i - 1, n = i + LARGEUR, s = i - LARGEUR;
        double ax = px[e] - px[w], ay = py[e] - py[w], az = pz[e] - pz[w];
        double bx = px[n] - px[s], by = py[n] - py[s], bz = pz[n] - pz[s];
        double longueurA = Math.sqrt(ax * ax + ay * ay + az * az);
        double longueurB = Math.sqrt(bx * bx + by * by + bz * bz);

        double x = px[i], y = py[i], z = pz[i];
        double horizontale = Math.max(1e-6, Math.sqrt(x * x + y * y));
        double estX = -y / horizontale, estY = x / horizontale;
        double nordX = -z * x / horizontale, nordY = -z * y / horizontale, nordZ = horizontale;
        double lumiereX = (nordX - estX) * RACINE_DEUX;
        double lumiereY = (nordY - estY) * RACINE_DEUX;
        double lumiereZ = nordZ * RACINE_DEUX;

        double penteA = (altitude[e] - altitude[w]) / (longueurA * Terrain.RAYON_KM);
        double penteB = (altitude[n] - altitude[s]) / (longueurB * Terrain.RAYON_KM);
        double versLumiereA = (ax * lumiereX + ay * lumiereY + az * lumiereZ) / longueurA;
        double versLumiereB = (bx * lumiereX + by * lumiereY + bz * lumiereZ) / longueurB;

        float ombre = 1f - FORCE_OMBRE * (float) (penteA * versLumiereA + penteB * versLumiereB);
        return MathUtils.clamp(ombre, OMBRE_MIN, OMBRE_MAX);
    }
}
