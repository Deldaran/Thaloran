package com.deldaran.politics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.deldaran.politics.planet.Biome;

public final class Palette {
    public static final float TEMPERATURE_NEIGE = -5f;
    public static final Color LAC = new Color(0.29f, 0.56f, 0.85f, 1f);
    public static final Color RIVIERE = new Color(0.18f, 0.44f, 0.82f, 1f);

    private static final Color OCEAN_PROFOND = new Color(0.05f, 0.15f, 0.40f, 1f);
    private static final Color OCEAN_COTIER = new Color(0.20f, 0.45f, 0.80f, 1f);
    private static final Color TERRE_BASSE = new Color(0.30f, 0.60f, 0.25f, 1f);
    private static final Color TERRE_HAUTE = new Color(0.55f, 0.45f, 0.30f, 1f);
    private static final Color TERRE_ROCHE = new Color(0.54f, 0.52f, 0.50f, 1f);
    private static final Color NEIGE = new Color(0.96f, 0.98f, 0.99f, 1f);

    private static final Color[] BIOMES = {
        new Color(0x123a7aff), new Color(0xcfe3f2ff), new Color(0xf4f8fbff), new Color(0x9aa58cff), new Color(0x3f6b55ff),
        new Color(0xa9c060ff), new Color(0x3c8a3aff), new Color(0xe3cf8cff), new Color(0xc2b24cff), new Color(0x116b2aff),
        new Color(0x8a8580ff), new Color(0xf6f9fcff)
    };

    private Palette() {
    }

    public static void ocean(float profondeur, Color sortie) {
        float t = MathUtils.clamp(profondeur / 7f, 0f, 1f);
        sortie.set(OCEAN_COTIER).lerp(OCEAN_PROFOND, t);
    }

    public static void relief(float hauteur, float temperature, Color sortie) {
        if (hauteur < 0) {
            ocean(-hauteur, sortie);
        } else if (hauteur < 1.2f) {
            sortie.set(TERRE_BASSE).lerp(TERRE_HAUTE, hauteur / 1.2f);
        } else if (hauteur > 2f && temperature < TEMPERATURE_NEIGE) {
            sortie.set(NEIGE);
        } else {
            float t = MathUtils.clamp((hauteur - 1.2f) / 1.3f, 0f, 1f);
            sortie.set(TERRE_HAUTE).lerp(TERRE_ROCHE, t);
        }
    }

    public static void biome(byte biome, Color sortie) {
        sortie.set(BIOMES[biome]);
    }

    /**
     * Lot K2 : les rangées et colonnes de palettes/sol.png que prend chaque biome dans le rendu des tuiles (tuile.frag, materiau()).
     * Le globe en formation et le globe vu de loin prennent ainsi les mêmes tons que le monde vu de près.
     */
    private static final int[][] TONS_BIOMES = {
        { 13, 4 }, { 7, 5 }, { 7, 4 }, { 11, 4 }, { 10, 3 }, { 0, 4 }, { 9, 3 }, { 12, 4 }, { 1, 4 }, { 0, 3 }, { 5, 4 }, { 6, 4 }
    };
    private static int[][] sol;

    private static int[][] sol() {
        if (sol == null) {
            com.badlogic.gdx.graphics.Pixmap p = new com.badlogic.gdx.graphics.Pixmap(com.badlogic.gdx.Gdx.files.internal("palettes/sol.png"));
            sol = new int[p.getHeight()][p.getWidth()];
            for (int r = 0; r < p.getHeight(); r++) for (int c = 0; c < p.getWidth(); c++) sol[r][c] = p.getPixel(c, r);
            p.dispose();
        }
        return sol;
    }

    /**
     * Le ton d'une cellule du globe, pris dans palettes/sol.png comme le fait tuile.frag : la mer d'après sa profondeur
     * (hauteur négative, en km), la terre d'après son biome ; « grain » (0 à 1) fait varier d'une colonne pour que ça ne soit pas plat.
     */
    public static void ton(byte biome, float hauteur, float grain, Color sortie) {
        int[][] s = sol();
        int rangee, colonne;
        if (biome == Biome.OCEAN || hauteur < 0f && biome != Biome.BANQUISE) {
            float metres = Math.max(0f, -hauteur * 1000f);
            rangee = 13;
            // Vue de loin, tuile.frag ne montre presque pas les hauts-fonds : on reste dans les bleus sombres (colonnes 0 à 3).
            colonne = Math.min(3, Math.round(6.6f - (float) (Math.log(1.0 + metres) / Math.log(2.0)) * 0.75f));
        } else {
            rangee = TONS_BIOMES[biome][0];
            colonne = TONS_BIOMES[biome][1] + (grain < 0.2f ? -1 : grain > 0.8f ? 1 : 0);
        }
        colonne = MathUtils.clamp(colonne, 0, s[rangee].length - 1);
        Color.rgba8888ToColor(sortie, s[rangee][colonne]);
    }

    /** L'eau douce (lacs, fleuves) dans le ton de tuile.frag. */
    public static void tonEau(Color sortie) {
        Color.rgba8888ToColor(sortie, sol()[13][5]);
    }
}
