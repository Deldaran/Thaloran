package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.Random;

public class Chargement {
    private static final float FONDU = 0.7f, DUREE_MAX = 25f;
    private static final int STABLE = 4, BRAISE_L = 11, BRAISE_H = 16, PIXEL = 4, BARRE = 260;
    private static final String[] PHRASES = {
        "Les cendres se souviennent", "Rien ne dure", "La guerre ne finit jamais", "Les morts attendent leur tour",
        "Chaque royaume tombe", "Il n'y a pas de matin ici", "La terre boit le sang des anciens"
    };

    private final SpriteBatch pinceau = new SpriteBatch();
    private final BitmapFont police = new BitmapFont();
    private final GlyphLayout mise = new GlyphLayout();
    private final Texture blanc;
    private final Random hasard = new Random();
    private boolean actif;
    private float duree, fondu, progression, affiche, temps;
    private int restantMax, stable;
    private String phrase = PHRASES[0];

    public Chargement() {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(1f, 1f, 1f, 1f);
        p.fill();
        blanc = new Texture(p);
        p.dispose();
        police.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        police.setUseIntegerPositions(true);
    }

    public void commencer() {
        if (!actif) {
            phrase = PHRASES[hasard.nextInt(PHRASES.length)];
            progression = 0f;
            affiche = 0f;
            restantMax = 0;
        }
        actif = true;
        duree = 0f;
        fondu = 1f;
        stable = 0;
    }

    public boolean actif() {
        return actif;
    }

    public void dessiner(int restant, String etape, float delta) {
        temps += delta;
        if (actif) {
            duree += delta;
            restantMax = Math.max(restantMax, restant);
            float p = restantMax == 0 ? 1f : 1f - (float) restant / restantMax;
            progression = Math.max(progression, p * 0.97f);
            stable = restant == 0 ? stable + 1 : 0;
            if (stable >= STABLE || duree > DUREE_MAX) {
                actif = false;
                progression = 1f;
            }
        } else {
            fondu -= delta / FONDU;
        }
        if (fondu <= 0f) return;
        affiche += (progression - affiche) * Math.min(1f, delta * 8f);
        float a = fondu * fondu * (3f - 2f * fondu);

        int largeur = Gdx.graphics.getBackBufferWidth(), hauteur = Gdx.graphics.getBackBufferHeight();
        Gdx.gl.glViewport(0, 0, largeur, hauteur);
        pinceau.getProjectionMatrix().setToOrtho2D(0f, 0f, largeur, hauteur);
        pinceau.enableBlending();
        pinceau.begin();
        pinceau.setColor(0.02f, 0.018f, 0.016f, a);
        pinceau.draw(blanc, 0f, 0f, largeur, hauteur);

        int cx = largeur / 2, cy = hauteur / 2;
        braise(cx, cy + 40, a);

        int bx = cx - BARRE / 2, by = cy - 10;
        pinceau.setColor(0.16f, 0.14f, 0.12f, a);
        pinceau.draw(blanc, bx - 1, by - 1, BARRE + 2, 4);
        pinceau.setColor(0.05f, 0.045f, 0.04f, a);
        pinceau.draw(blanc, bx, by, BARRE, 2);
        pinceau.setColor(0.62f, 0.38f, 0.18f, a);
        pinceau.draw(blanc, bx, by, Math.round(BARRE * affiche), 2);

        police.getData().setScale(2f);
        police.setColor(0.6f, 0.55f, 0.48f, a);
        mise.setText(police, phrase);
        police.draw(pinceau, phrase, cx - mise.width / 2f, cy - 30);
        police.getData().setScale(1f);
        police.setColor(0.36f, 0.33f, 0.3f, a);
        mise.setText(police, etape);
        police.draw(pinceau, etape, cx - mise.width / 2f, cy - 76);
        pinceau.end();
    }

    private void braise(int cx, int bas, float a) {
        for (int j = 0; j < BRAISE_H; j++) {
            float v = (float) j / BRAISE_H;
            float demi = (1f - v) * (1f - v) * BRAISE_L * 0.5f + 0.6f;
            for (int i = 0; i < BRAISE_L; i++) {
                float u = i - (BRAISE_L - 1) / 2f + (float) Math.sin(temps * 3.1f + j * 0.7f) * v * 1.4f;
                float bord = 1f - Math.abs(u) / demi;
                if (bord <= 0f) continue;
                float f = bord * (1f - v) * 1.6f + 0.55f * bruit(i, j - (int) (temps * 9f)) - 0.35f;
                f *= 0.85f + 0.15f * (float) Math.sin(temps * 11f + i);
                if (f < 0.15f) continue;
                if (f > 0.85f) pinceau.setColor(0.95f, 0.78f, 0.45f, a);
                else if (f > 0.55f) pinceau.setColor(0.82f, 0.42f, 0.14f, a);
                else if (f > 0.3f) pinceau.setColor(0.5f, 0.17f, 0.07f, a);
                else pinceau.setColor(0.24f, 0.07f, 0.04f, a);
                pinceau.draw(blanc, cx + (i - BRAISE_L / 2) * PIXEL, bas + j * PIXEL, PIXEL, PIXEL);
            }
        }
        pinceau.setColor(0.2f, 0.17f, 0.15f, a);
        for (int k = -2; k <= 2; k++) pinceau.draw(blanc, cx + k * 3 * PIXEL - PIXEL, bas - PIXEL, PIXEL * 3, PIXEL);
    }

    private static float bruit(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 1023) / 1023f;
    }

    public void dispose() {
        pinceau.dispose();
        police.dispose();
        blanc.dispose();
    }
}
