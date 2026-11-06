package com.deldaran.politics.histoire;

import com.deldaran.politics.planet.Biome;

public final class Peuple {
    public static final int ELFE = 0;
    public static final int NAIN = 1;
    public static final int HUMAIN = 2;
    public static final int ORK = 3;
    public static final int NOMBRE = 4;

    public final String nom;
    public float rythme = 1f;
    public float mobilite = 1f;
    public int eveil = 0;
    public int berceaux = 2;
    public int fondateurs = 1000;
    public float peche = 1f;
    public float loi = 0f;
    public float bien = 0f;
    public final float[] attrait = new float[12];

    private Peuple(String nom) {
        this.nom = nom;
    }

    public static Peuple[] tous() {
        Peuple elfe = new Peuple("Elfes");
        elfe.rythme = 1f / 12f;
        elfe.mobilite = 0.3f;
        elfe.berceaux = 2;
        elfe.fondateurs = 1500;
        elfe.peche = 0.3f;
        elfe.loi = 20f;
        elfe.bien = 50f;
        elfe.attrait[Biome.FORET] = 1f;
        elfe.attrait[Biome.JUNGLE] = 0.7f;
        elfe.attrait[Biome.TAIGA] = 0.5f;
        elfe.attrait[Biome.PRAIRIE] = 0.2f;

        Peuple nain = new Peuple("Nains");
        nain.rythme = 1f / 3f;
        nain.mobilite = 0.5f;
        nain.berceaux = 3;
        nain.fondateurs = 1000;
        nain.peche = 0f;
        nain.loi = 50f;
        nain.bien = 10f;
        nain.attrait[Biome.MONTAGNE] = 1f;
        nain.attrait[Biome.NEIGE] = 0.3f;
        nain.attrait[Biome.TAIGA] = 0.1f;
        nain.attrait[Biome.TOUNDRA] = 0.1f;

        Peuple humain = new Peuple("Humains");
        humain.mobilite = 1.3f;
        humain.eveil = 1500;
        humain.berceaux = 2;
        humain.fondateurs = 2000;
        humain.attrait[Biome.PRAIRIE] = 1f;
        humain.attrait[Biome.FORET] = 0.7f;
        humain.attrait[Biome.SAVANE] = 0.6f;
        humain.attrait[Biome.JUNGLE] = 0.4f;
        humain.attrait[Biome.TAIGA] = 0.2f;
        humain.attrait[Biome.MONTAGNE] = 0.1f;
        humain.attrait[Biome.TOUNDRA] = 0.05f;
        humain.attrait[Biome.DESERT] = 0.03f;

        Peuple ork = new Peuple("Orks");
        ork.rythme = 1.5f;
        ork.mobilite = 1.5f;
        ork.eveil = 1500;
        ork.berceaux = 2;
        ork.fondateurs = 2000;
        ork.peche = 0.5f;
        ork.loi = -40f;
        ork.bien = -50f;
        ork.attrait[Biome.TOUNDRA] = 0.5f;
        ork.attrait[Biome.DESERT] = 0.4f;
        ork.attrait[Biome.MONTAGNE] = 0.5f;
        ork.attrait[Biome.TAIGA] = 0.4f;
        ork.attrait[Biome.SAVANE] = 0.3f;
        ork.attrait[Biome.PRAIRIE] = 0.3f;

        return new Peuple[] { elfe, nain, humain, ork };
    }
}
