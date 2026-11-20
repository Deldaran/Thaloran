package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class Interface {
    private static final float LARGEUR = 640f, HAUTEUR_TEXTE = 280f, MARGE_HAUT = 14f;
    private static final String ACCENTS = "àâäéèêëîïôöùûüÿçœæÀÂÄÉÈÊËÎÏÔÖÙÛÜÇŒÆ«»’‘“”…—–•";
    private static final float CHOIX_MAX = 0.5f;
    private static final Color OR = new Color(0.86f, 0.70f, 0.40f, 1f), OR_VIF = new Color(1f, 0.86f, 0.55f, 1f);
    private static final Color ENCRE = new Color(0.88f, 0.84f, 0.76f, 1f), ETEINT = new Color(0.55f, 0.52f, 0.47f, 1f);

    private final Stage stage = new Stage(new ScreenViewport());
    private final BitmapFont police, policeTitre, policeBouton;
    private final Texture cadre, cadreClair, blanc;
    private final Table bouton = new Table();
    private final Label texteBouton;
    private final Table fenetre = new Table();
    private final Label titre, texte;
    private final ScrollPane defilement;
    private final Table choix = new Table();
    private final ScrollPane defilementChoix;
    private final TextField.TextFieldStyle styleSaisie;
    private TextField champ;
    private final Label.LabelStyle styleChoix, styleSurvol;
    private Runnable actionBouton;
    private IntConsumer actionChoix;
    private boolean boutonActif;
    /** Lot K1 : Échap a servi à fermer la fenêtre (le menu du jeu ne s'ouvre pas en même temps). */
    private boolean echap;

    public boolean prendreEchap() {
        boolean e = echap;
        echap = false;
        return e;
    }

    public Interface() {
        police = police("georgia.ttf", 18);
        policeTitre = police("georgiab.ttf", 26);
        policeBouton = police("georgiab.ttf", 20);
        cadre = texture(new Color(0.07f, 0.06f, 0.05f, 0.94f), new Color(0.55f, 0.42f, 0.22f, 1f));
        cadreClair = texture(new Color(0.16f, 0.12f, 0.08f, 0.96f), new Color(0.86f, 0.68f, 0.36f, 1f));
        NinePatchDrawable fond = new NinePatchDrawable(new NinePatch(cadre, 4, 4, 4, 4));
        NinePatchDrawable fondSurvol = new NinePatchDrawable(new NinePatch(cadreClair, 4, 4, 4, 4));
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        blanc = new Texture(pixel);
        pixel.dispose();
        Drawable curseur = new TextureRegionDrawable(new TextureRegion(blanc)).tint(OR_VIF);
        curseur.setMinWidth(2f);
        Drawable selection = new TextureRegionDrawable(new TextureRegion(blanc)).tint(new Color(0.55f, 0.42f, 0.22f, 0.6f));
        NinePatchDrawable fondChamp = new NinePatchDrawable(new NinePatch(cadreClair, 4, 4, 4, 4));
        fondChamp.setLeftWidth(10f);
        fondChamp.setRightWidth(10f);
        fondChamp.setTopHeight(6f);
        fondChamp.setBottomHeight(6f);
        styleSaisie = new TextField.TextFieldStyle(police, Color.WHITE, curseur, selection, fondChamp);
        styleChoix = new Label.LabelStyle(police, OR);
        styleSurvol = new Label.LabelStyle(police, OR_VIF);

        texteBouton = new Label("", new Label.LabelStyle(policeBouton, OR));
        bouton.setBackground(fond);
        bouton.pad(8f, 26f, 8f, 26f);
        bouton.add(texteBouton);
        bouton.setTouchable(Touchable.enabled);
        bouton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (boutonActif && actionBouton != null) actionBouton.run();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor depuis) {
                if (boutonActif) bouton.setBackground(fondSurvol);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor vers) {
                bouton.setBackground(fond);
            }
        });
        stage.addActor(bouton);

        titre = new Label("", new Label.LabelStyle(policeTitre, OR_VIF));
        titre.setAlignment(Align.center);
        texte = new Label("", new Label.LabelStyle(police, ENCRE));
        texte.setWrap(true);
        texte.setAlignment(Align.topLeft);
        defilement = new ScrollPane(texte);
        defilement.setScrollingDisabled(true, false);
        defilement.setFlickScroll(false);
        defilementChoix = new ScrollPane(choix);
        defilementChoix.setScrollingDisabled(true, false);
        defilementChoix.setFlickScroll(false);
        defilementChoix.setFadeScrollBars(false);
        fenetre.setBackground(fond);
        fenetre.pad(18f, 26f, 18f, 26f);
        fenetre.setTouchable(Touchable.enabled);
        fenetre.setVisible(false);
        stage.addActor(fenetre);

        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int code) {
                if (code == Input.Keys.ESCAPE && fenetre.isVisible()) {
                    echap = true;
                    fermer();
                    return true;
                }
                return false;
            }
        });
        placer();
    }

    private static BitmapFont police(String fichier, int taille) {
        String windows = System.getenv("WINDIR") != null ? System.getenv("WINDIR") : "C:/Windows";
        FileHandle f = Gdx.files.absolute(windows + "/Fonts/" + fichier);
        if (!f.exists()) return new BitmapFont();
        FreeTypeFontGenerator generateur = new FreeTypeFontGenerator(f);
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = taille;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + ACCENTS;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        BitmapFont b = generateur.generateFont(p);
        generateur.dispose();
        return b;
    }

    private static Texture texture(Color fond, Color bord) {
        Pixmap p = new Pixmap(12, 12, Pixmap.Format.RGBA8888);
        p.setColor(bord);
        p.fill();
        p.setColor(fond.r * 0.6f, fond.g * 0.6f, fond.b * 0.6f, 1f);
        p.drawRectangle(1, 1, 10, 10);
        p.setColor(fond);
        p.fillRectangle(2, 2, 8, 8);
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    public Stage getStage() {
        return stage;
    }

    public boolean ouverte() {
        return fenetre.isVisible();
    }

    /** Lot K2 : pendant la création du monde, le bouton du haut se cache (la barre du bas dit où l'on en est). */
    public void cacherBouton() {
        bouton.setVisible(false);
        boutonActif = false;
    }

    public void bouton(String libelle, boolean actif, Runnable action) {
        bouton.setVisible(true);
        boutonActif = actif;
        actionBouton = action;
        if (!libelle.contentEquals(texteBouton.getText())) {
            texteBouton.setText(libelle);
            bouton.pack();
            placer();
        }
        texteBouton.setColor(actif ? Color.WHITE : ETEINT);
    }

    public void appuyer() {
        if (boutonActif && actionBouton != null) actionBouton.run();
    }

    public void choisir(int i) {
        if (fenetre.isVisible() && actionChoix != null) actionChoix.accept(i);
    }

    public boolean saisieActive() {
        return fenetre.isVisible() && champ != null && stage.getKeyboardFocus() == champ;
    }

    public void saisir(String libelleTitre, String contenu, String initial, Consumer<String> valider, Runnable annuler) {
        actionChoix = null;
        fenetre.clear();
        titre.setText(libelleTitre);
        texte.setText(contenu);
        fenetre.add(titre).width(LARGEUR).padBottom(12f).row();
        fenetre.add(defilement).width(LARGEUR).height(Math.min(HAUTEUR_TEXTE, Math.max(24f, hauteurTexte(contenu)))).padBottom(14f).row();
        TextField nouveau = new TextField(initial, styleSaisie);
        nouveau.setMaxLength(24);
        nouveau.setTextFieldListener((c, touche) -> {
            if (touche == '\r' || touche == '\n') valider.accept(c.getText());
        });
        champ = nouveau;
        fenetre.add(nouveau).width(LARGEUR).height(36f).padBottom(12f).row();
        fenetre.add(lien("Valider (Entrée)", () -> valider.accept(nouveau.getText()))).width(LARGEUR).left().padBottom(6f).row();
        fenetre.add(lien("« Revenir", annuler)).width(LARGEUR).left().row();
        fenetre.pack();
        defilement.layout();
        defilement.setScrollY(0f);
        defilement.updateVisualScroll();
        fenetre.setVisible(true);
        fenetre.toFront();
        placer();
        stage.setKeyboardFocus(nouveau);
    }

    private Label lien(String libelle, Runnable action) {
        Label l = new Label(libelle, styleChoix);
        l.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor depuis) {
                l.setStyle(styleSurvol);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor vers) {
                l.setStyle(styleChoix);
            }
        });
        return l;
    }

    public void afficher(String libelleTitre, String contenu, List<String> options, IntConsumer action) {
        actionChoix = action;
        champ = null;
        fenetre.clear();
        titre.setText(libelleTitre);
        texte.setText(contenu);
        fenetre.add(titre).width(LARGEUR).padBottom(12f).row();
        choix.clear();
        for (int i = 0; i < options.size(); i++) {
            int indice = i;
            Label l = new Label(options.get(i), styleChoix);
            l.setWrap(true);
            l.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    action.accept(indice);
                }

                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor depuis) {
                    l.setStyle(styleSurvol);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor vers) {
                    l.setStyle(styleChoix);
                }
            });
            choix.add(l).width(LARGEUR).left().padBottom(6f).row();
        }
        choix.pack();
        float hauteurChoix = Math.min(choix.getPrefHeight(), stage.getHeight() * CHOIX_MAX);
        float hauteur = Math.min(HAUTEUR_TEXTE, Math.max(24f, hauteurTexte(contenu)));
        hauteur = Math.max(Math.min(hauteur, 60f), Math.min(hauteur, stage.getHeight() - hauteurChoix - 170f));
        fenetre.add(defilement).width(LARGEUR).height(hauteur).padBottom(14f).row();
        fenetre.add(defilementChoix).width(LARGEUR + 14f).height(hauteurChoix).row();
        Label fermer = new Label("Fermer (Echap)", new Label.LabelStyle(police, ETEINT));
        fermer.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                fermer();
            }
        });
        fenetre.add(fermer).right().padTop(10f);
        fenetre.pack();
        defilement.layout();
        defilement.setScrollY(0f);
        defilement.updateVisualScroll();
        fenetre.setVisible(true);
        fenetre.toFront();
        placer();
        defilementChoix.layout();
        defilementChoix.setScrollY(0f);
        defilementChoix.updateVisualScroll();
        stage.setScrollFocus(defilement);
        stage.setKeyboardFocus(fenetre);
    }

    private float hauteurTexte(String contenu) {
        Label essai = new Label(contenu, texte.getStyle());
        essai.setWrap(true);
        essai.setWidth(LARGEUR);
        essai.layout();
        return essai.getPrefHeight() + 4f;
    }

    public void fermer() {
        fenetre.setVisible(false);
        stage.setScrollFocus(null);
    }

    private void placer() {
        bouton.pack();
        bouton.setPosition(Math.round((stage.getWidth() - bouton.getWidth()) / 2f), Math.round(stage.getHeight() - MARGE_HAUT - bouton.getHeight()));
        fenetre.setPosition(Math.round((stage.getWidth() - fenetre.getWidth()) / 2f), Math.round((stage.getHeight() - fenetre.getHeight()) / 2f));
    }

    public void dessiner(float delta) {
        stage.act(delta);
        stage.draw();
    }

    public void redimensionner(int largeur, int hauteur) {
        stage.getViewport().update(largeur, hauteur, true);
        placer();
    }

    public void dispose() {
        stage.dispose();
        police.dispose();
        policeTitre.dispose();
        policeBouton.dispose();
        cadre.dispose();
        cadreClair.dispose();
        blanc.dispose();
    }
}
