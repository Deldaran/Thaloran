package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Lot K2 : une petite plaque en haut à droite qui dit quelle carte on regarde (biomes, plaques, température…) et que Tab en
 * change. Elle s'éclaire un instant quand la carte change.
 */
public class EtiquetteCarte {
    /** Les noms affichés, dans l'ordre des modes de FirstScreen (NOMS_MODES). */
    public static final String[] NOMS = { "Plaques", "Relief", "Température", "Pluie", "Biomes", "Régions", "Nourriture", "Population",
        "Villes", "Commerce", "États", "Routes" };
    private final Stage stage = new Stage(new ScreenViewport());
    private final Theme t = Theme.get();
    private final Table plaque;
    private final Label nom;
    private int mode = -1;
    private boolean visible;

    public EtiquetteCarte() {
        plaque = t.panneau();
        plaque.pad(8f, 16f, 8f, 16f);
        Table ligne = new Table();
        ligne.add(t.label("Carte :", t.texte, Theme.ETEINT)).padRight(8f);
        // Le texte est blanc et c'est la couleur de l'acteur qui le teinte : on peut ainsi le faire briller puis revenir à l'or.
        nom = t.label("", t.bouton, com.badlogic.gdx.graphics.Color.WHITE);
        nom.setColor(Theme.OR_VIF);
        ligne.add(nom);
        plaque.add(ligne).right().row();
        plaque.add(t.label("Tab : changer de carte", t.petit, Theme.ETEINT)).right();
        stage.addActor(plaque);
        placer();
    }

    /** À chaque image : le mode affiché et s'il faut montrer la plaque. */
    public void montrer(int modeAffichage, boolean montrer) {
        if (montrer != visible) {
            visible = montrer;
            plaque.clearActions();
            plaque.getColor().a = montrer ? 0f : plaque.getColor().a;
            plaque.addAction(montrer ? Actions.fadeIn(0.4f) : Actions.fadeOut(0.3f));
        }
        if (modeAffichage != mode) {
            mode = modeAffichage;
            nom.setText(modeAffichage >= 0 && modeAffichage < NOMS.length ? NOMS[modeAffichage] : "?");
            placer();
            // Un éclat bref pour qu'on voie que la carte a changé.
            nom.clearActions();
            nom.setColor(1f, 1f, 1f, 1f);
            nom.addAction(Actions.color(Theme.OR_VIF, 0.8f));
        }
    }

    private void placer() {
        plaque.pack();
        plaque.setPosition(stage.getWidth() - plaque.getWidth() - 20f, stage.getHeight() - plaque.getHeight() - 20f);
    }

    public void dessiner(float delta) {
        if (!visible && plaque.getActions().size == 0) return;
        stage.act(delta);
        Gdx.gl.glColorMask(true, true, true, false);
        stage.draw();
        Gdx.gl.glColorMask(true, true, true, true);
    }

    public void redimensionner(int largeur, int hauteur) {
        stage.getViewport().update(largeur, hauteur, true);
        placer();
    }

    public void dispose() {
        stage.dispose();
    }
}
