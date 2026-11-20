package com.deldaran.politics;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class FenetreDebug {
    private static final float LARGEUR = 260f;
    private static final float MARGE = 10f;
    private static final float RETRAIT = 12f;
    private static final float HAUTEUR_TITRE = 26f;
    private static final float LARGEUR_LECTURE = 540f;
    private static final float HAUTEUR_LECTURE = 460f;
    private static final float HAUTEUR_CHOIX = 22f;

    private final Stage stage;
    private final BitmapFont police;
    private final Texture blanc;
    private final Window fenetre;
    private final Table racine = new Table();
    private final ScrollPane defilement;
    private final Cell<ScrollPane> placeDefilement;
    private final Label.LabelStyle styleLabel;
    private final Label.LabelStyle styleSection;
    private final Label.LabelStyle styleBouton;

    private final Slider.SliderStyle styleSlider;
    private final Array<Section> sections = new Array<>();
    private final Window lecture;
    private final Label texteLecture;
    private final ScrollPane defilementLecture;
    private final Cell<ScrollPane> placeLecture;
    private final Table choixLecture = new Table();
    private Label bandeau;

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

    public final class Section {
        private final String nom;
        private final float largeur;
        private final Label titre;
        private final Table contenu = new Table();
        private final Cell<Table> place;
        private final Array<Info> infos = new Array<>();
        private final Array<Section> enfants = new Array<>();
        private boolean ouverte = false;

        private Section(String nom, Table parent, float largeur) {
            this.nom = nom;
            this.largeur = largeur;
            titre = new Label("[+] " + nom, styleSection);
            contenu.defaults().left().width(largeur - RETRAIT).padBottom(4f);
            parent.add(titre).row();
            place = parent.add((Table) null).padLeft(RETRAIT);
            parent.row();
            titre.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    basculer();
                }
            });
        }

        private void basculer() {
            ouverte = !ouverte;
            titre.setText((ouverte ? "[-] " : "[+] ") + nom);
            place.setActor(ouverte ? contenu : null);
            ajuster();
        }

        public Section ouvrir() {
            if (!ouverte) basculer();
            return this;
        }

        public Section ajouterSection(String nom) {
            Section section = new Section(nom, contenu, largeur - RETRAIT);
            enfants.add(section);
            ajuster();
            return section;
        }

        public void ajouterInfo(String nom, Supplier<String> valeur) {
            Info info = new Info(new Label("", styleLabel), nom, valeur);
            info.mettreAJour();
            infos.add(info);
            contenu.add(info.label).row();
            ajuster();
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
            contenu.add(label).row();
            contenu.add(slider).row();
            ajuster();
        }
        
        public void ajouterBouton(String nom, Runnable action) {
            Label bouton = new Label("> " + nom, styleBouton);
            bouton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    action.run();
                }
            });
            contenu.add(bouton).row();
            ajuster();
        }

        private void mettreAJour() {
            if (!ouverte) return;
            for (Info info : infos) {
                info.mettreAJour();
            }
            for (Section enfant : enfants) {
                enfant.mettreAJour();
            }
        }
    }

    public FenetreDebug() {
        stage = new Stage(new ScreenViewport());
        police = new BitmapFont();
        blanc = creerTextureBlanche();

        styleLabel = new Label.LabelStyle(police, Color.WHITE);
        styleSection = new Label.LabelStyle(police, new Color(0.65f, 0.85f, 1f, 1f));
        styleBouton = new Label.LabelStyle(police, new Color(1f, 0.85f, 0.45f, 1f));

        styleSlider = new Slider.SliderStyle(
            creerDrawable(1f, 6f, new Color(0.35f, 0.35f, 0.40f, 1f)),
            creerDrawable(10f, 18f, new Color(0.85f, 0.85f, 0.90f, 1f)));

        Window.WindowStyle styleFenetre = new Window.WindowStyle(
            police, Color.WHITE, creerDrawable(1f, 1f, new Color(0.08f, 0.08f, 0.10f, 0.85f)));

        fenetre = new Window("Debug", styleFenetre);
        fenetre.getTitleLabel().setAlignment(Align.left);
        fenetre.padTop(HAUTEUR_TITRE);
        fenetre.padLeft(MARGE);
        fenetre.padRight(MARGE);
        fenetre.padBottom(MARGE);

        racine.top().left();
        racine.defaults().left().width(LARGEUR).padBottom(4f);
        defilement = new ScrollPane(racine);
        defilement.setScrollingDisabled(true, false);
        defilement.setFlickScroll(false);
        defilement.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor depuis) {
                stage.setScrollFocus(defilement);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor vers) {
                if (vers == null || !vers.isDescendantOf(defilement)) {
                    stage.setScrollFocus(null);
                }
            }
        });
        placeDefilement = fenetre.add(defilement).left().width(LARGEUR + RETRAIT);
        stage.addActor(fenetre);
        fenetre.setPosition(MARGE, stage.getHeight() - MARGE);
        ajuster();
        
        lecture = new Window("", styleFenetre);
        lecture.getTitleLabel().setAlignment(Align.left);
        lecture.padTop(HAUTEUR_TITRE);
        lecture.padLeft(MARGE);
        lecture.padRight(MARGE);
        lecture.padBottom(MARGE);
        texteLecture = new Label("", styleLabel);
        texteLecture.setWrap(true);
        texteLecture.setAlignment(Align.topLeft);
        defilementLecture = new ScrollPane(texteLecture);
        defilementLecture.setScrollingDisabled(true, false);
        defilementLecture.setFlickScroll(false);
        defilementLecture.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor depuis) {
                stage.setScrollFocus(defilementLecture);
            }
        });
        placeLecture = lecture.add(defilementLecture).width(LARGEUR_LECTURE).height(HAUTEUR_LECTURE);
        lecture.row();
        lecture.add(choixLecture).left().padTop(6f).row();
        Label fermer = new Label("> Fermer", styleBouton);
        fermer.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                lecture.setVisible(false);
            }
        });
        lecture.add(fermer).left().padTop(6f);
        lecture.pack();
        lecture.setVisible(false);
        stage.addActor(lecture);
        bandeau = new Label("", new Label.LabelStyle(police, new Color(1f, 0.9f, 0.6f, 1f)));
        bandeau.setAlignment(Align.center);
        bandeau.setVisible(false);
        stage.addActor(bandeau);

    }
    
    public void afficherTexte(String titre, String texte) {
        afficherChoix(titre, texte, List.of(), i -> { });
    }

    public void afficherChoix(String titre, String texte, List<String> choix, IntConsumer action) {
        choixLecture.clear();
        for (int i = 0; i < choix.size(); i++) {
            int indice = i;
            Label bouton = new Label("> " + choix.get(i), styleBouton);
            bouton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    action.accept(indice);
                }
            });
            choixLecture.add(bouton).left().height(HAUTEUR_CHOIX).row();
        }
        placeLecture.height(HAUTEUR_LECTURE - choix.size() * HAUTEUR_CHOIX / 2f);
        lecture.getTitleLabel().setText(titre);
        texteLecture.setText(texte);
        lecture.pack();
        defilementLecture.layout();
        defilementLecture.setScrollY(0f);
        defilementLecture.updateVisualScroll();
        lecture.setPosition((stage.getWidth() - lecture.getWidth()) / 2f, (stage.getHeight() - lecture.getHeight()) / 2f);
        lecture.setVisible(true);
        lecture.toFront();
        stage.setScrollFocus(defilementLecture);
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

    public Section ajouterSection(String nom) {
        Section section = new Section(nom, racine, LARGEUR);
        sections.add(section);
        ajuster();
        return section;
    }

    private void ajuster() {
        float haut = fenetre.getTop();
        racine.invalidateHierarchy();
        float hauteurMax = stage.getHeight() - 2f * MARGE - HAUTEUR_TITRE - MARGE;
        placeDefilement.height(Math.max(0f, Math.min(racine.getPrefHeight(), hauteurMax)));
        fenetre.invalidateHierarchy();
        fenetre.pack();
        fenetre.setPosition(fenetre.getX(), Math.max(MARGE, haut - fenetre.getHeight()));
    }

    public Stage getStage() {
        return stage;
    }

    public void bandeau(String texte) {
        if (texte == null || texte.isEmpty()) {
            bandeau.setVisible(false);
            return;
        }
        if (!texte.contentEquals(bandeau.getText())) bandeau.setText(texte);
        bandeau.pack();
        bandeau.setPosition((stage.getWidth() - bandeau.getWidth()) / 2f, 40f);
        bandeau.setVisible(true);
    }

    /** Lot K1 : la fenêtre de debug se montre ou se cache (touche ²) ; cachée, elle ne prend plus la souris. */
    public void afficher(boolean oui) {
        stage.getRoot().setVisible(oui);
    }

    public boolean affichee() {
        return stage.getRoot().isVisible();
    }

    public void dessiner(float delta) {
        if (!affichee()) return;
        for (Section section : sections) {
            section.mettreAJour();
        }
        stage.act(delta);
        stage.draw();
    }

    public void redimensionner(int largeur, int hauteur) {
        stage.getViewport().update(largeur, hauteur, true);
        fenetre.setPosition(fenetre.getX(), stage.getHeight() - MARGE);
        ajuster();
    }

    public void dispose() {
        stage.dispose();
        police.dispose();
        blanc.dispose();
    }
}
