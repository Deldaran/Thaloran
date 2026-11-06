package com.deldaran.politics.planet;

public final class Biome {
    public static final byte OCEAN = 0;
    public static final byte BANQUISE = 1;
    public static final byte GLACE = 2;
    public static final byte TOUNDRA = 3;
    public static final byte TAIGA = 4;
    public static final byte PRAIRIE = 5;
    public static final byte FORET = 6;
    public static final byte DESERT = 7;
    public static final byte SAVANE = 8;
    public static final byte JUNGLE = 9;
    public static final byte MONTAGNE = 10;
    public static final byte NEIGE = 11;
    public static final float HAUTEUR_MONTAGNE = 1.2f;
    public static final float TEMPERATURE_NEIGE = -5f;

    private Biome() {
    }

    public static byte choisir(float t, float p, boolean sousLEau, float relief) {
        if (sousLEau) return t < -9f ? BANQUISE : OCEAN;
        if (relief > HAUTEUR_MONTAGNE) return t < TEMPERATURE_NEIGE ? NEIGE : MONTAGNE;
        if (t < -8f) return GLACE;
        if (t < 0f) return TOUNDRA;
        if (t < 8f) return p < 200f ? TOUNDRA : TAIGA;
        if (t < 20f) {
            if (p < 250f) return DESERT;
            if (p < 550f) return PRAIRIE;
            return FORET;
        }
        if (p < 250f) return DESERT;
        if (p < 600f) return SAVANE;
        return JUNGLE;
    }
    
    public static boolean estGele(byte biome) {
        return biome == GLACE || biome == NEIGE;
    }

}
