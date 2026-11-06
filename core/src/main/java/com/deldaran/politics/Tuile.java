package com.deldaran.politics;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.deldaran.politics.planet.Cube;
import java.util.concurrent.atomic.AtomicInteger;

public class Tuile {
    public static final int COTE = 256;
    public static final int SEGMENTS = 16;
    public static final int POINTS = SEGMENTS + 1;

    public static final int VIDE = 0;
    public static final int EN_COURS = 1;
    public static final int CALCULEE = 2;
    public static final int PRETE = 3;

    public final int face, niveau, x, y;
    public final boolean biomes;
    public final double[] grille = new double[POINTS * POINTS * 3];
    public final AtomicInteger etat = new AtomicInteger(VIDE);

    public Texture texture;
    public volatile Pixmap image;
    public long dernierUsage;

    public Tuile(int face, int niveau, int x, int y, boolean biomes) {
        this.face = face;
        this.niveau = niveau;
        this.x = x;
        this.y = y;
        this.biomes = biomes;

        double[] d = new double[3];
        for (int j = 0; j < POINTS; j++) {
            for (int i = 0; i < POINTS; i++) {
                Cube.direction(face, coordonnee(x, (double) i / SEGMENTS), coordonnee(y, (double) j / SEGMENTS), d);
                System.arraycopy(d, 0, grille, (j * POINTS + i) * 3, 3);
            }
        }
    }

    public double coordonnee(int position, double fraction) {
        return 2.0 * (position + fraction) / (1 << niveau) - 1.0;
    }

    public static long cle(int face, int niveau, int x, int y, boolean biomes) {
        return ((long) (biomes ? 1 : 0) << 52) | ((long) face << 49) | ((long) niveau << 44) | ((long) x << 22) | y;
    }

    public void liberer() {
        if (texture != null) texture.dispose();
        if (image != null) image.dispose();
        texture = null;
        image = null;
    }
}
