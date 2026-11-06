package com.deldaran.politics.planet;

import com.badlogic.gdx.utils.Array;

public class Monde {
    public final int largeur, hauteur;

    public final int[] plaque;
    public final float[] epaisseur;
    public final boolean[] isContinentale;
    public final float[] age;
    public final float[] altitude;
    public final Sphere sphere;
    private final int[] caseCarte;

    public final float[] fosse;
    public final float[] volcan;


    public final Array<Plaque> plaques = new Array<>();
    public float volumeEau;
    public float niveauMer;
    public float temps;
    public final long seed;

    public final float[] temperature;
    public final float[] pluie;
    public final byte[] biome;
    public final float[] debit;
    public final boolean[] lac;


    public Monde(int largeur, int hauteur, long seed) {
        this.largeur = largeur;
        this.hauteur = hauteur;
        this.seed = seed;
        int n = largeur * hauteur;
        sphere = new Sphere(n);
        caseCarte = new int[n];
        remplirCarte();

        fosse = new float[n];
        volcan = new float[n];
        
        temperature = new float[n];
        pluie = new float[n];
        biome = new byte[n];
        
        debit = new float[n];
        lac = new boolean[n];

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
        x = ((x % largeur) + largeur) % largeur;
        return caseCarte[y * largeur + x];
    }
    
    private void remplirCarte() {
        for (int y = 0; y < hauteur; y++) {
            double latitude = ((y + 0.5) / hauteur - 0.5) * Math.PI;
            for (int x = 0; x < largeur; x++) {
                double longitude = ((x + 0.5) / largeur - 0.5) * 2.0 * Math.PI;
                float px = (float) (Math.cos(latitude) * Math.cos(longitude));
                float py = (float) (Math.cos(latitude) * Math.sin(longitude));
                float pz = (float) Math.sin(latitude);
                caseCarte[y * largeur + x] = sphere.plusProche(px, py, pz);
            }
        }
    }

    public void calculerAltitude(int i) { 
        if (isContinentale[i]) {
            altitude[i] = epaisseur[i] * (1 - Croute.DENSITE_CONTINENTALE / Croute.DENSITE_MANTEAU) + volcan[i];
        } else {
            float enfoncement = Math.min(Croute.SUBSIDENCE * (float) Math.sqrt(age[i]), Croute.SUBSIDENCE_MAX);
            altitude[i] = epaisseur[i] * (1 - Croute.DENSITE_OCEANIQUE / Croute.DENSITE_MANTEAU)
                + Croute.SOULEVEMENT_DORSALE - enfoncement - fosse[i] + volcan[i];
        }
    }

    public void calculerToutesAltitudes() {
        for (int i = 0; i < plaque.length; i++) {
            calculerAltitude(i);
        }
    }

    public boolean estGele(int i) {
        return biome[i] == Biome.GLACE || biome[i] == Biome.NEIGE;
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
