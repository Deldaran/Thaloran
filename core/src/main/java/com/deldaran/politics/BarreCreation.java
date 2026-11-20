package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import java.util.function.IntConsumer;

/**
 * Lot K2 : la barre de la création du monde, en bas de l'écran. Elle dit l'étape (I La terre, II Les peuples, III L'histoire),
 * ce qui se passe, quand (il y a tant de millions d'années, ou l'an de l'histoire), où l'on en est (une barre d'or), le dernier
 * grand fait de la chronique, et laisse choisir la vitesse de la formation de la terre.
 */
public class BarreCreation {
    public static final String[] ETAPES = { "I · La terre", "II · Les peuples", "III · L'histoire" };
    public static final String[] VITESSES = { "Calme", "Rapide", "Très rapide" };
    public static final float[] PAS_PAR_SECONDE = { 10f, 60f, 250f };
    private final Stage stage = new Stage(new ScreenViewport());
    private final Theme t = Theme.get();
    private final Table panneau;
    private final Label[] etapes = new Label[ETAPES.length];
    private final Label titre, quand, pourcent, evenement;
    private final Barre barre = new Barre();
    private final Table vitesse;
    private final com.badlogic.gdx.scenes.scene2d.ui.Cell<Table> celluleVitesse;
    private boolean visible;
    private String evenementAffiche = "";

    public BarreCreation(int vitesseDepart, IntConsumer choixVitesse) {
        panneau = t.panneau();
        panneau.pad(18f, 30f, 18f, 30f);
        Table ligneEtapes = new Table();
        for (int k = 0; k < ETAPES.length; k++) {
            etapes[k] = t.label(ETAPES[k], t.texte, Theme.ETEINT);
            ligneEtapes.add(etapes[k]).padRight(k < ETAPES.length - 1 ? 40f : 0f);
        }
        panneau.add(ligneEtapes).colspan(2).left().padBottom(10f).row();
        titre = t.label("", t.grand, Theme.OR_VIF);
        pourcent = t.label("", t.bouton, Theme.OR);
        panneau.add(titre).left().expandX();
        panneau.add(pourcent).right().row();
        quand = t.label("", t.sousTitre, Theme.ENCRE);
        panneau.add(quand).colspan(2).left().padBottom(10f).row();
        panneau.add(barre).colspan(2).growX().height(14f).padBottom(10f).row();
        evenement = t.label("", t.petit, Theme.ETEINT);
        evenement.setWrap(true);
        evenement.setAlignment(Align.left);
        panneau.add(evenement).colspan(2).growX().left().row();
        vitesse = new Table();
        vitesse.add(t.label("Vitesse :", t.texte, Theme.ETEINT)).padRight(6f);
        vitesse.add(t.options(VITESSES, vitesseDepart, choixVitesse::accept));
        celluleVitesse = panneau.add(vitesse).colspan(2).left().padTop(6f);
        panneau.row();
        stage.addActor(panneau);
        placer();
    }

    public Stage getStage() {
        return stage;
    }

    /**
     * etape : 0, 1 ou 2 ; progres : de 0 à 1, ou négatif quand on ne sait pas (la barre ondoie) ; evenement : peut être vide.
     */
    public void montrer(int etape, String titreTexte, String quandTexte, float progres, String evenementTexte, boolean vitesseVisible) {
        if (!visible) {
            visible = true;
            stage.getRoot().setVisible(true);
            panneau.getColor().a = 0f;
            panneau.addAction(Actions.fadeIn(0.6f));
        }
        for (int k = 0; k < etapes.length; k++) {
            Color c = k == etape ? Theme.OR_VIF : k < etape ? Theme.OR_SOMBRE : Theme.ETEINT;
            if (!etapes[k].getStyle().fontColor.equals(c)) {
                Label.LabelStyle s = new Label.LabelStyle(etapes[k].getStyle());
                s.fontColor = c;
                etapes[k].setStyle(s);
            }
        }
        if (!titre.textEquals(titreTexte)) {
            titre.setText(titreTexte);
            titre.getColor().a = 0f;
            titre.addAction(Actions.fadeIn(0.5f));
        }
        quand.setText(quandTexte);
        barre.progres = progres;
        pourcent.setText(progres < 0f ? "" : Math.round(progres * 100f) + " %");
        if (!evenementTexte.equals(evenementAffiche)) {
            evenementAffiche = evenementTexte;
            evenement.clearActions();
            evenement.addAction(Actions.sequence(Actions.fadeOut(0.25f), Actions.run(() -> evenement.setText(evenementTexte)), Actions.fadeIn(0.5f)));
        }
        // Sans le choix de la vitesse, sa ligne disparaît : le panneau se resserre.
        if (vitesse.isVisible() != vitesseVisible) {
            vitesse.setVisible(vitesseVisible);
            celluleVitesse.setActor(vitesseVisible ? vitesse : null).padTop(vitesseVisible ? 6f : 0f);
            placer();
        }
    }

    public void cacher() {
        if (!visible) return;
        visible = false;
        stage.getRoot().setVisible(false);
    }

    public boolean visible() {
        return visible;
    }

    private void placer() {
        float l = Math.min(820f, stage.getWidth() - 60f);
        panneau.getCell(barre).width(l - 60f);
        panneau.getCell(evenement).width(l - 60f);
        panneau.setWidth(l);
        panneau.pack();
        panneau.setWidth(l);
        panneau.setPosition((stage.getWidth() - l) / 2f, 28f);
    }

    public void dessiner(float delta) {
        if (!visible) return;
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

    /** La barre : un fond sombre, un filet d'or, un remplissage d'or dont la pointe brille ; sans progrès connu, une lueur qui va et vient. */
    private final class Barre extends Actor {
        float progres;
        private float temps;

        @Override
        public void act(float delta) {
            super.act(delta);
            temps += delta;
        }

        @Override
        public void draw(Batch b, float alpha) {
            float x = getX(), y = getY(), l = getWidth(), h = getHeight();
            float a = alpha * getParent().getColor().a;
            b.setColor(Theme.OR_SOMBRE.r, Theme.OR_SOMBRE.g, Theme.OR_SOMBRE.b, a);
            b.draw(t.blanc, x, y, l, h);
            b.setColor(0.05f, 0.04f, 0.035f, a);
            b.draw(t.blanc, x + 1, y + 1, l - 2, h - 2);
            float dedans = l - 6, haut = h - 6;
            if (progres >= 0f) {
                float p = Math.min(1f, progres) * dedans;
                b.setColor(Theme.OR.r * 0.75f, Theme.OR.g * 0.62f, Theme.OR.b * 0.45f, a);
                b.draw(t.blanc, x + 3, y + 3, p, haut);
                b.setColor(Theme.OR.r, Theme.OR.g, Theme.OR.b, a);
                b.draw(t.blanc, x + 3, y + 3 + haut * 0.5f, p, haut * 0.5f);
                float eclat = 0.6f + 0.4f * (float) Math.sin(temps * 4f);
                b.setColor(Theme.OR_VIF.r, Theme.OR_VIF.g, Theme.OR_VIF.b, a * eclat);
                b.draw(t.blanc, x + 3 + Math.max(0f, p - 4f), y + 3, Math.min(4f, p), haut);
            } else {
                float u = (float) (0.5 + 0.5 * Math.sin(temps * 1.6)), w = dedans * 0.22f;
                b.setColor(Theme.OR.r, Theme.OR.g, Theme.OR.b, a * 0.85f);
                b.draw(t.blanc, x + 3 + u * (dedans - w), y + 3, w, haut);
            }
            b.setColor(Color.WHITE);
        }
    }
}
