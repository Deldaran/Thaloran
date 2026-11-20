package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Lot K1 : le menu du jeu, ouvert et fermé par Échap : Reprendre, Paramètres, Menu principal, Quitter le jeu. Tant qu'il est
 * ouvert, l'écran s'assombrit et les touches du jeu ne répondent plus.
 */
public class MenuPause {
    private final Stage stage = new Stage(new ScreenViewport());
    private final Theme t = Theme.get();
    private final Container<Actor> centre = new Container<>();
    private final Image voile;
    private final String monde;
    private final Runnable quitterMonde;
    private boolean ouvert;

    /** monde : le nom du monde, en titre ; quitterMonde : enregistre avant de partir. */
    public MenuPause(String monde, Runnable quitterMonde) {
        this.monde = monde;
        this.quitterMonde = quitterMonde;
        voile = new Image(t.fondNoir);
        voile.setFillParent(true);
        voile.setTouchable(Touchable.enabled);
        stage.addActor(voile);
        centre.setFillParent(true);
        centre.center();
        stage.addActor(centre);
        stage.getRoot().setVisible(false);
    }

    public Stage getStage() {
        return stage;
    }

    public boolean ouvert() {
        return ouvert;
    }

    public void basculer() {
        if (ouvert) fermer();
        else ouvrir();
    }

    public void ouvrir() {
        ouvert = true;
        stage.getRoot().setVisible(true);
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().clearActions();
        stage.getRoot().addAction(Actions.fadeIn(0.2f));
        principal();
        Sons.effet("page");
    }

    public void fermer() {
        ouvert = false;
        stage.getRoot().setVisible(false);
    }

    private void principal() {
        Table p = t.panneau();
        p.defaults().left();
        p.add(t.label(monde, t.grand, Theme.OR_VIF)).padBottom(4f).row();
        p.add(t.label("Le monde attend ton retour.", t.sousTitre, Theme.ETEINT)).padBottom(24f).row();
        p.add(t.bouton("Reprendre", t.bouton, this::fermer)).padBottom(4f).row();
        p.add(t.bouton("Paramètres", t.bouton, () -> centre.setActor(Panneaux.parametres(this::principal)))).padBottom(4f).row();
        Theme.Bouton menu = t.bouton("Menu principal", t.bouton, null);
        menu.action = () -> {
            stage.getRoot().setTouchable(Touchable.disabled);
            quitterMonde.run();
            Gdx.app.postRunnable(() -> Thaloran.jeu().menu());
        };
        p.add(menu).padBottom(4f).row();
        p.add(t.bouton("Quitter le jeu", t.bouton, () -> {
            quitterMonde.run();
            Gdx.app.exit();
        })).row();
        centre.setActor(p);
    }

    public void dessiner(float delta) {
        if (!ouvert) return;
        stage.act(delta);
        Gdx.gl.glColorMask(true, true, true, false);
        stage.draw();
        Gdx.gl.glColorMask(true, true, true, true);
    }

    public void redimensionner(int largeur, int hauteur) {
        stage.getViewport().update(largeur, hauteur, true);
    }

    public void dispose() {
        stage.dispose();
    }
}
