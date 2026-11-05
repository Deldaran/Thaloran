package com.deldaran.politics.planet;

import com.badlogic.gdx.utils.Array;

public class Monde {
    public final int largeur, hauteur;

    public final int[] plaque;
    public final float[] epaisseur;
    public final boolean[] isContinentale;
    public final float[] age;
    public final float[] altitude;

    public final Array<Plaque> plaques = new Array<>();
    public float volumeEau;
    public float niveauMer;
    public float temps;
    public final long seed;
    public Monde(int largeur, int hauteur, long seed) {
        this.largeur = largeur;
        this.hauteur = hauteur;
        this.seed = seed;
        int n = largeur * hauteur;

        plaque = new int[n];
        epaisseur = new float[n];
        isContinentale = new boolean[n];
        age = new float[n];
        altitude = new float[n];
    }

    public int index(int x, int y) {
        if (y < 0 || y >= hauteur) {
            return -1;
        }
        x = ((x%largeur) + largeur) % largeur;
        return y * largeur + x;
    }

    public void calculerAltitude(int i) { 
        float densité = isContinentale[i] ? Croute.DENSITE_CONTINENTALE : Croute.DENSITE_OCEANIQUE;
        altitude[i] = epaisseur[i] * (1 - densité / Croute.DENSITE_MANTEAU);

    }

    public void calculerToutesAltitudes() {
        for (int i = 0; i < plaque.length; i++) {
            calculerAltitude(i);
        }
    }

    public boolean estSousLEau(int i) {
        return altitude[i] < niveauMer;
    }

    public float epaisseurMax() {
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < epaisseur.length; i++) {
            if (epaisseur[i] > max) {
                max = epaisseur[i];
            }
        }
        return max;
    }
    public void calculerNiveauMer() {
        float bas = Float.MAX_VALUE;
        float haut = -Float.MAX_VALUE;
        for (float a : altitude) {
            bas = Math.min(bas, a);
            haut = Math.max(haut, a);
        }
        haut += volumeEau / altitude.length;

        for (int essai = 0; essai < 25; essai++) {
            float milieu = (bas + haut) / 2f;
            float volume = 0f;
            for (float a : altitude) {
                if (a < milieu) volume += milieu - a;
            }
            if (volume < volumeEau) {
                bas = milieu;
            } else {
                haut = milieu;
            }
        }
        niveauMer = (bas + haut) / 2f;
    }


}
