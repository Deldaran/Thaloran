package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import java.util.function.Consumer;

/**
 * Lot K1 : le style commun des menus (accueil, pause, paramètres) : polices, cadres ornés, boutons, curseurs.
 * Un seul exemplaire pour tout le jeu ({@link #get()}), créé à la première utilisation.
 */
public final class Theme {
    public static final Color OR = new Color(0.86f, 0.70f, 0.40f, 1f), OR_VIF = new Color(1f, 0.88f, 0.58f, 1f), OR_SOMBRE = new Color(0.50f, 0.38f, 0.19f, 1f);
    public static final Color ENCRE = new Color(0.90f, 0.86f, 0.78f, 1f), ETEINT = new Color(0.55f, 0.51f, 0.46f, 1f), SANG = new Color(0.78f, 0.26f, 0.18f, 1f);
    private static final String CARACTERES = FreeTypeFontGenerator.DEFAULT_CHARS + "àâäéèêëîïôöùûüÿçœæÀÂÄÉÈÊËÎÏÔÖÙÛÜÇŒÆ«»’‘“”…—–•²·◆";
    private static Theme theme;

    public final BitmapFont petit, texte, bouton, grand, sousTitre, titre;
    public final Texture blanc;
    private final Texture cadre, cadreClair, lueur, losange;
    public final Drawable fondCadre, fondCadreClair, fondLueur, fondNoir;
    public final TextField.TextFieldStyle saisie;
    public final Slider.SliderStyle curseur;
    public final ScrollPane.ScrollPaneStyle defilement;

    public static Theme get() {
        if (theme == null) theme = new Theme();
        return theme;
    }

    public static void liberer() {
        if (theme != null) theme.dispose();
        theme = null;
    }

    private Theme() {
        petit = police("georgia.ttf", 15);
        texte = police("georgia.ttf", 18);
        bouton = police("georgiab.ttf", 24);
        grand = police("georgiab.ttf", 30);
        sousTitre = police("georgiai.ttf", 20);
        titre = police("georgiab.ttf", 92);
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        blanc = new Texture(p);
        p.dispose();
        cadre = cadreOrne(new Color(0.055f, 0.045f, 0.04f, 0.94f), OR_SOMBRE, OR);
        cadreClair = cadreOrne(new Color(0.15f, 0.11f, 0.07f, 0.96f), OR, OR_VIF);
        fondCadre = new NinePatchDrawable(new NinePatch(cadre, 12, 12, 12, 12));
        fondCadreClair = new NinePatchDrawable(new NinePatch(cadreClair, 12, 12, 12, 12));
        lueur = lueur();
        fondLueur = new TextureRegionDrawable(new TextureRegion(lueur));
        fondNoir = new TextureRegionDrawable(new TextureRegion(blanc)).tint(new Color(0f, 0f, 0f, 0.6f));
        losange = losange();

        Drawable trait = new TextureRegionDrawable(new TextureRegion(blanc)).tint(OR_VIF);
        trait.setMinWidth(2f);
        Drawable selection = new TextureRegionDrawable(new TextureRegion(blanc)).tint(new Color(0.55f, 0.42f, 0.22f, 0.6f));
        NinePatchDrawable fondChamp = new NinePatchDrawable(new NinePatch(cadreClair, 12, 12, 12, 12));
        fondChamp.setLeftWidth(14f);
        fondChamp.setRightWidth(14f);
        fondChamp.setTopHeight(8f);
        fondChamp.setBottomHeight(8f);
        saisie = new TextField.TextFieldStyle(texte, ENCRE, trait, selection, fondChamp);

        Drawable rail = new TextureRegionDrawable(new TextureRegion(blanc)).tint(OR_SOMBRE);
        rail.setMinHeight(4f);
        Drawable plein = new TextureRegionDrawable(new TextureRegion(blanc)).tint(OR);
        plein.setMinHeight(4f);
        Drawable bouton = new TextureRegionDrawable(new TextureRegion(losange));
        bouton.setMinWidth(18f);
        bouton.setMinHeight(18f);
        curseur = new Slider.SliderStyle(rail, bouton);
        curseur.knobBefore = plein;

        defilement = new ScrollPane.ScrollPaneStyle();
        Drawable barre = new TextureRegionDrawable(new TextureRegion(blanc)).tint(OR_SOMBRE);
        barre.setMinWidth(4f);
        defilement.vScrollKnob = barre;
    }

    private static BitmapFont police(String fichier, int taille) {
        String windows = System.getenv("WINDIR") != null ? System.getenv("WINDIR") : "C:/Windows";
        FileHandle f = Gdx.files.absolute(windows + "/Fonts/" + fichier);
        if (!f.exists()) f = Gdx.files.absolute(windows + "/Fonts/georgia.ttf");
        if (!f.exists()) return new BitmapFont();
        FreeTypeFontGenerator generateur = new FreeTypeFontGenerator(f);
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = taille;
        p.characters = CARACTERES;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        BitmapFont police = generateur.generateFont(p);
        generateur.dispose();
        return police;
    }

    /** Un cadre de 32 px : fond sombre, double filet d'or, petit losange à chaque coin. */
    private static Texture cadreOrne(Color fond, Color filet, Color vif) {
        int t = 32;
        Pixmap p = new Pixmap(t, t, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        p.setColor(fond);
        p.fillRectangle(1, 1, t - 2, t - 2);
        p.setColor(filet);
        p.drawRectangle(1, 1, t - 2, t - 2);
        p.setColor(vif);
        p.drawRectangle(4, 4, t - 8, t - 8);
        for (int c = 0; c < 4; c++) {
            int x = c % 2 == 0 ? 4 : t - 5, y = c < 2 ? 4 : t - 5;
            p.setColor(vif);
            p.drawPixel(x, y);
            p.drawPixel(x - 1, y);
            p.drawPixel(x + 1, y);
            p.drawPixel(x, y - 1);
            p.drawPixel(x, y + 1);
            p.setColor(filet);
            p.drawPixel(x - 2, y);
            p.drawPixel(x + 2, y);
            p.drawPixel(x, y - 2);
            p.drawPixel(x, y + 2);
        }
        Texture tx = new Texture(p);
        p.dispose();
        return tx;
    }

    /** La lueur derrière un bouton survolé : de l'or qui s'éteint vers la droite. */
    private static Texture lueur() {
        Pixmap p = new Pixmap(128, 8, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int x = 0; x < 128; x++) {
            float a = 0.28f * (float) Math.pow(1f - x / 128f, 1.6f);
            p.setColor(0.86f, 0.62f, 0.28f, a);
            p.drawLine(x, 0, x, 7);
        }
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return t;
    }

    private static Texture losange() {
        int t = 11, c = 5;
        Pixmap p = new Pixmap(t, t, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < t; y++) {
            for (int x = 0; x < t; x++) {
                int d = Math.abs(x - c) + Math.abs(y - c);
                if (d > c) continue;
                p.setColor(d == c ? OR_SOMBRE : d < 2 ? OR_VIF : OR);
                p.drawPixel(x, y);
            }
        }
        Texture tx = new Texture(p);
        p.dispose();
        return tx;
    }

    public Label label(String s, BitmapFont police, Color couleur) {
        Label l = new Label(s, new Label.LabelStyle(police, couleur));
        return l;
    }

    public Label paragraphe(String s) {
        Label l = new Label(s, new Label.LabelStyle(texte, ENCRE));
        l.setWrap(true);
        l.setAlignment(Align.topLeft);
        return l;
    }

    public Image losangeImage() {
        return new Image(new TextureRegionDrawable(new TextureRegion(losange)));
    }

    /** Un cadre orné vide, à remplir. */
    public Table panneau() {
        Table t = new Table();
        t.setBackground(fondCadre);
        t.pad(26f, 34f, 26f, 34f);
        t.setTouchable(Touchable.enabled);
        return t;
    }

    /** Un bouton du menu : du texte, un losange et une lueur au survol, un son au clic. */
    public Bouton bouton(String libelle, BitmapFont police, Runnable action) {
        return new Bouton(this, libelle, police, action);
    }

    public static final class Bouton extends Table {
        private final Label texte;
        private final Image marque;
        private final Theme t;
        private boolean actif = true, choisi;
        public Runnable action;

        Bouton(Theme t, String libelle, BitmapFont police, Runnable action) {
            this.t = t;
            this.action = action;
            texte = new Label(libelle, new Label.LabelStyle(police, OR));
            marque = t.losangeImage();
            marque.setVisible(false);
            add(marque).size(11f, 11f).padRight(12f);
            add(texte).left();
            pad(6f, 12f, 6f, 40f);
            left();
            setTouchable(Touchable.enabled);
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!actif || Bouton.this.action == null) return;
                    Sons.effet("clic");
                    Bouton.this.action.run();
                }

                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor depuis) {
                    if (!actif || pointer != -1 || isOver()) return;
                    Sons.effet("survol");
                    allumer(true);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor vers) {
                    if (pointer != -1 || (vers != null && vers.isDescendantOf(Bouton.this))) return;
                    allumer(false);
                }
            });
        }

        private void allumer(boolean oui) {
            boolean vif = oui || choisi;
            marque.setVisible(vif && actif);
            texte.getStyle().fontColor = !actif ? ETEINT : vif ? OR_VIF : OR;
            texte.setStyle(texte.getStyle());
            setBackground(oui && actif ? t.fondLueur : null);
        }

        public Bouton actif(boolean oui) {
            actif = oui;
            allumer(false);
            return this;
        }

        /** Un bouton choisi reste allumé (une option parmi d'autres, une page ouverte). */
        public Bouton choisi(boolean oui) {
            choisi = oui;
            allumer(false);
            return this;
        }

        public void libelle(String s) {
            texte.setText(s);
        }
    }

    /** Un curseur de 0 à 1 avec son libellé et sa valeur en pour cent. */
    public Table curseur(String libelle, float depart, Consumer<Float> action) {
        Table ligne = new Table();
        Label nom = label(libelle, texte, ENCRE);
        Label valeur = label(Math.round(depart * 100) + " %", petit, OR);
        Slider s = new Slider(0f, 1f, 0.05f, false, curseur);
        s.setValue(depart);
        s.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                valeur.setText(Math.round(s.getValue() * 100) + " %");
                action.accept(s.getValue());
            }
        });
        ligne.add(nom).width(190f).left();
        ligne.add(s).width(260f).padRight(16f);
        ligne.add(valeur).width(60f).left();
        return ligne;
    }

    /** Une rangée d'options exclusives ; rend l'index choisi par action. */
    public Table options(String[] libelles, int depart, Consumer<Integer> action) {
        Table ligne = new Table();
        Array<Bouton> tous = new Array<>();
        for (int k = 0; k < libelles.length; k++) {
            int i = k;
            Bouton b = bouton(libelles[k], texte, null);
            b.pad(4f, 8f, 4f, 14f);
            b.action = () -> {
                for (int q = 0; q < tous.size; q++) tous.get(q).choisi(q == i);
                action.accept(i);
            };
            b.choisi(k == depart);
            tous.add(b);
            ligne.add(b).left();
        }
        return ligne;
    }

    public void dispose() {
        petit.dispose();
        texte.dispose();
        bouton.dispose();
        grand.dispose();
        sousTitre.dispose();
        titre.dispose();
        blanc.dispose();
        cadre.dispose();
        cadreClair.dispose();
        lueur.dispose();
        losange.dispose();
    }
}
