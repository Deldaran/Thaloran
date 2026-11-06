package com.deldaran.politics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

public final class Palette {
    public static final float TEMPERATURE_NEIGE = -5f;

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
}
