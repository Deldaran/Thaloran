package com.deldaran.politics;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;

/**
 * Thaloran, le jeu. Lot K1 : il s'ouvre sur le menu d'accueil (MenuScreen), en plein écran si les paramètres le veulent ; on entre
 * dans un monde (FirstScreen) et l'on en ressort par le menu du jeu. Les essais (SORTIE) vont tout droit au monde 42.
 */
public class Thaloran extends Game {
    /** Le dossier du joueur (dans son dossier personnel) : ses mondes, le modèle d'IA locale. */
    public static final String DOSSIER = "Thaloran/";
    /** L'ancien nom du jeu : son dossier est déménagé au premier lancement. */
    private static final String ANCIEN_DOSSIER = "Politics";
    private static Thaloran jeu;

    public static Thaloran jeu() {
        return jeu;
    }

    @Override
    public void create() {
        jeu = this;
        demenager();
        Parametres.charger();
        if (System.getenv("SORTIE") != null && System.getenv("MENU") == null) {
            setScreen(new FirstScreen());
            return;
        }
        if (System.getenv("SORTIE") == null) Parametres.appliquerEcran();
        setScreen(new MenuScreen());
    }

    /** Le jeu s'appelait Politics : ses mondes et son IA passent de ~/Politics à ~/Thaloran (une seule fois, sans rien copier). */
    private static void demenager() {
        java.io.File ancien = Gdx.files.external(ANCIEN_DOSSIER).file(), neuf = Gdx.files.external(DOSSIER).file();
        if (!ancien.isDirectory() || neuf.exists()) return;
        if (!ancien.renameTo(neuf)) Gdx.app.error("Thaloran", "Impossible de renommer " + ancien + " en " + neuf);
    }

    /** Entre dans un monde ; l'écran d'avant est libéré. */
    public void lancer(Mondes.Fiche f) {
        changer(new FirstScreen(f));
    }

    /** Revient au menu d'accueil. */
    public void menu() {
        changer(new MenuScreen());
    }

    private void changer(Screen nouveau) {
        Screen ancien = getScreen();
        setScreen(nouveau);
        if (ancien != null) ancien.dispose();
    }

    @Override
    public void render() {
        super.render();
        Sons.mettreAJour(Gdx.graphics.getDeltaTime());
    }

    @Override
    public void dispose() {
        Screen s = getScreen();
        super.dispose();
        if (s != null) s.dispose();
        Sons.liberer();
        Theme.liberer();
    }
}
