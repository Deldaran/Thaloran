package com.deldaran.politics;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FloatTextureData;
import com.deldaran.politics.planet.Biome;
import com.deldaran.politics.planet.Bruit;
import com.deldaran.politics.planet.Cube;
import com.deldaran.politics.planet.Terrain;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class AtelierTuile {
    public static final int LARGEUR = Tuile.COTE + 2;
    private static final double RAYON_METRES = Terrain.RAYON_KM * 1000.0;
    private static final double TEXELS_PAR_ONDE = 4.0;
    private static final double[] ONDES_CLIMAT = { 160.0, 60.0 };
    private static final double[] ONDES_TACHES = { 70.0, 28.0 };

    private final Terrain terrain;
    private final Terrain.Point point = new Terrain.Point();
    private final double[] d = new double[3];
    private final float[] courant = new float[2];

    public AtelierTuile(Terrain terrain) {
        this.terrain = terrain;
    }

    public void generer(Tuile tuile) {
        FloatTextureData donnees = new FloatTextureData(LARGEUR, LARGEUR, GL30.GL_RGBA32F, GL20.GL_RGBA, GL20.GL_FLOAT, false);
        donnees.prepare();
        FloatBuffer valeurs = donnees.getBuffer();
        Pixmap couverture = new Pixmap(LARGEUR, LARGEUR, Pixmap.Format.RGBA8888);
        ByteBuffer octets = couverture.getPixels();
        double espacement = Tuile.metresParTexel(tuile.niveau);
        for (int y = 0; y < LARGEUR; y++) {
            double b = tuile.coordonnee(tuile.y, (y - 0.5) / Tuile.COTE);
            for (int x = 0; x < LARGEUR; x++) {
                double a = tuile.coordonnee(tuile.x, (x - 0.5) / Tuile.COTE);
                Cube.direction(tuile.face, a, b, d);
                terrain.echantillonner(d[0], d[1], d[2], tuile.niveau, point);
                byte biome = terrain.biome(d[0], d[1], d[2], point);
                boolean eau = !Biome.estGele(biome) && (point.lac || point.riviere);
                int i = (y * LARGEUR + x) * 4;
                valeurs.put(i, point.altitude);
                valeurs.put(i + 1, point.temperature);
                valeurs.put(i + 2, point.pluie);
                valeurs.put(i + 3, eau ? 1f : 0f);
                courant(tuile.face, eau);
                octets.put(i + 2, (byte) Math.round(127.5f + 127f * courant[0]));
                octets.put(i + 3, (byte) Math.round(127.5f + 127f * courant[1]));

            }
        }
        tuile.donneesPretes = donnees;
        tuile.couverturePrete = couverture;
    }
    
    private void courant(int face, boolean eau) {
        courant[0] = 0f;
        courant[1] = 0f;
        if (!eau || !point.riviere) return;
        double pas = 1e-5;
        double x = d[0] + point.courantX * pas, y = d[1] + point.courantY * pas, z = d[2] + point.courantZ * pas;
        double da = Cube.coordonneeA(face, x, y, z) - Cube.coordonneeA(face, d[0], d[1], d[2]);
        double db = Cube.coordonneeB(face, x, y, z) - Cube.coordonneeB(face, d[0], d[1], d[2]);
        double norme = Math.sqrt(da * da + db * db);
        if (norme < 1e-12) return;
        courant[0] = (float) (da / norme);
        courant[1] = (float) (db / norme);
    }


    private float bruit(double[] ondes, double espacement, int graine) {
        float somme = 0f, poids = 0f, amplitude = 1f;
        for (int o = 0; o < ondes.length; o++) {
            if (ondes[o] >= TEXELS_PAR_ONDE * espacement) {
                double f = RAYON_METRES / ondes[o];
                somme += amplitude * Bruit.perlin(d[0] * f, d[1] * f, d[2] * f, graine + o);
                poids += amplitude;
            }
            amplitude *= 0.5f;
        }
        if (poids == 0f) return 0.5f;
        return Math.max(0f, Math.min(1f, 0.5f + 0.7f * somme / poids));
    }
}
