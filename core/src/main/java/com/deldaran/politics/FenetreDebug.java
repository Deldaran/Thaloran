package com.deldaran.politics;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class FenetreDebug {
    private static final float LARGEUR = 240f;
    private static final float MARGE = 10f;

    private final Stage stage;
    private final BitmapFont police;
    private final Texture blanc;
    private final Window fenetre;
    private final Label.LabelStyle styleLabel;
    private final Slider.SliderStyle styleSlider;
    private final Array<Info> infos = new Array<>();

    private static class Info {
        final Label label;
        final String nom;
        final Supplier<String> valeur;

        Info(Label label, String nom, Supplier<String> valeur) {
            this.label = label;
            this.nom = nom;
            this.valeur = valeur;
        }

        void mettreAJour() {
            label.setText(nom + " : " + valeur.get());
        }
    }

    public FenetreDebug() {
        stage = new Stage(new ScreenViewport());
        police = new BitmapFont();
        blanc = creerTextureBlanche();

        styleLabel = new Label.LabelStyle(police, Color.WHITE);
        styleSlider = new Slider.SliderStyle(
            creerDrawable(1f, 6f, new Color(0.35f, 0.35f, 0.40f, 1f)),
            creerDrawable(10f, 18f, new Color(0.85f, 0.85f, 0.90f, 1f)));

        Window.WindowStyle styleFenetre = new Window.WindowStyle(
            police, Color.WHITE, creerDrawable(1f, 1f, new Color(0.08f, 0.08f, 0.10f, 0.85f)));

        fenetre = new Window("Debug", styleFenetre);
        fenetre.getTitleLabel().setAlignment(Align.left);
        fenetre.padTop(26f);
        fenetre.padLeft(MARGE);
        fenetre.padRight(MARGE);
        fenetre.padBottom(MARGE);
        fenetre.defaults().left().width(LARGEUR).padBottom(4f);
        stage.addActor(fenetre);
        placerEnHautAGauche();
    }

    private Texture creerTextureBlanche() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private Drawable creerDrawable(float largeur, float hauteur, Color couleur) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(blanc));
        drawable.setMinWidth(largeur);
        drawable.setMinHeight(hauteur);
        return drawable.tint(couleur);
    }

    public void ajouterInfo(String nom, Supplier<String> valeur) {
        Info info = new Info(new Label("", styleLabel), nom, valeur);
        info.mettreAJour();
        infos.add(info);
        fenetre.add(info.label).row();
        placerEnHautAGauche();
    }

    public void ajouterSlider(String nom, float min, float max, float pas, float depart, Consumer<Float> action) {
        Label label = new Label(nom + " : " + depart, styleLabel);
        Slider slider = new Slider(min, max, pas, false, styleSlider);
        slider.setValue(depart);
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                label.setText(nom + " : " + slider.getValue());
                action.accept(slider.getValue());
            }
        });
        fenetre.add(label).row();
        fenetre.add(slider).row();
        placerEnHautAGauche();
    }

    private void placerEnHautAGauche() {
        fenetre.pack();
        fenetre.setPosition(MARGE, stage.getHeight() - fenetre.getHeight() - MARGE);
    }

    public Stage getStage() {
        return stage;
    }

    public void dessiner(float delta) {
        for (Info info : infos) {
            info.mettreAJour();
        }
        stage.act(delta);
        stage.draw();
    }

    public void redimensionner(int largeur, int hauteur) {
        stage.getViewport().update(largeur, hauteur, true);
    }

    public void dispose() {
        stage.dispose();
        police.dispose();
        blanc.dispose();
    }
}
