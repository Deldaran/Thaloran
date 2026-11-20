package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/** Lot K1 : les réglages du joueur, gardés d'une partie à l'autre (plein écran, volumes). */
public final class Parametres {
    public static boolean pleinEcran = true;
    public static float musique = 0.6f, sons = 0.8f;
    private static Preferences p;

    private Parametres() {
    }

    public static void charger() {
        p = Gdx.app.getPreferences("thaloran-parametres");
        // Le jeu s'appelait Politics : on reprend ses réglages une fois.
        if (p.get().isEmpty()) {
            Preferences ancien = Gdx.app.getPreferences("politics-parametres");
            if (!ancien.get().isEmpty()) {
                p.put(ancien.get());
                p.flush();
            }
        }
        pleinEcran = p.getBoolean("pleinEcran", true);
        musique = p.getFloat("musique", 0.6f);
        sons = p.getFloat("sons", 0.8f);
    }

    public static void enregistrer() {
        if (p == null) return;
        p.putBoolean("pleinEcran", pleinEcran);
        p.putFloat("musique", musique);
        p.putFloat("sons", sons);
        p.flush();
    }

    /** Passe en plein écran (à la définition de l'écran) ou en fenêtre de 1280 × 720. */
    public static void appliquerEcran() {
        if (pleinEcran) Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        else Gdx.graphics.setWindowedMode(1280, 720);
    }
}
