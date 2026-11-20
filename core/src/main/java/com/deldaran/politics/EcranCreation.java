package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.deldaran.politics.histoire.Aspect;
import com.deldaran.politics.histoire.Joueur;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Lot K3 : l'écran de création du personnage. Il prend tout l'écran, sur le fond du menu voilé :
 * - à gauche, ton personnage en grand, qui tourne sur lui-même, et sous lui les réglages de son apparence ;
 * - au milieu, ta fiche, qui se remplit à chaque choix (peuple, lieu, nom, famille…) ;
 * - à droite, l'étape de ton histoire : son titre, son texte et ses choix.
 */
public class EcranCreation {
    /** Lot K3 : peint une zone du monde en détail (l x h pixels, de lon0 à lon1 et de lat0 à lat1, en radians) ; RGBA, ligne 0 en haut. */
    public interface PeintreZone {
        int[] peindre(double lon0, double lon1, double lat0, double lat1, int l, int h, int etatCadre, int niveau);
    }

    /** La taille de la carte détaillée d'un royaume : chaque pixel s'affiche en 2 x 2 à l'écran. */
    private static final int DETAIL_L = 260, DETAIL_H = 130, NIVEAU_DETAIL = 6;
    public PeintreZone zone;
    private final java.util.concurrent.ExecutorService atelierCarte = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread f = new Thread(r, "carte-creation");
        f.setDaemon(true);
        return f;
    });
    /** L'ordre des directions pour faire un tour complet (face, trois-quarts, profil, dos…). */
    private static final int[] TOUR = { Personnage.BAS, Personnage.BAS_DROITE, Personnage.DROITE, Personnage.HAUT_DROITE, Personnage.HAUT,
        Personnage.HAUT_GAUCHE, Personnage.GAUCHE, Personnage.BAS_GAUCHE };
    private static final String[] COUPES = { "Rasée", "Courte", "Mi-longue", "Longue", "Queue", "Tresse", "Chignon", "Crête" };
    private static final String[] BARBES = { "Aucune", "Pleine", "Moustache", "Bouc", "Tressée" };
    private static final float TOUR_AUTO = 2.2f, PAUSE_TOUR = 8f, VOILE = 0.5f;

    private final Stage stage = new Stage(new ScreenViewport());
    private final Theme t = Theme.get();
    private final SpriteBatch pinceau = new SpriteBatch();
    private final FondMenu fond = new FondMenu();
    private final Peintre peintre = new Peintre();
    private final int[] teintes = new int[Batiments.RANGEES];
    private final Table racine = new Table();
    private final Table colonnePerso, colonneFiche, colonneScene;
    private final Table reglages = new Table(), lignesFiche = new Table(), choix = new Table();
    private final Label titre, attente;
    private final Label texte;
    private final Portrait portrait = new Portrait();
    /** Lot K3 : la carte à plat pour choisir son royaume et sa ville, et la ligne qui dit ce qu'on survole. */
    private final CarteMonde carte = new CarteMonde();
    private final Label infoCarte;
    private final Table zoneCarte = new Table();
    /** Lot K4 : la vignette de l'étape (la famille devant la ville natale), à la place de la carte quand il n'y en a pas. */
    private final Vignette vignette;
    private com.badlogic.gdx.scenes.scene2d.ui.Cell<Vignette> celluleVignette;
    private com.badlogic.gdx.scenes.scene2d.ui.Cell<Table> celluleCarte;
    private final java.util.List<String> libelles = new java.util.ArrayList<>();
    private String aideCarte = "";
    private boolean ouvert;

    // L'aperçu : ce que la création nous dit du personnage.
    private boolean possible;
    private int espece;
    private boolean femme;
    private long graine = Long.MIN_VALUE;
    private Aspect aspect;
    private long cleReglages = Long.MIN_VALUE;
    private Tenue tenue;
    private Traits traits;

    public EcranCreation() {
        Pixmap p = new Pixmap(Gdx.files.internal("palettes/sol.png"));
        for (int r = 0; r < teintes.length; r++) teintes[r] = p.getPixel(4, r);
        p.dispose();

        colonnePerso = t.panneau();
        colonnePerso.add(t.label("Ton personnage", t.sousTitre, Theme.OR)).padBottom(6f).row();
        attente = t.label("Ton visage se dessinera\nquand tu sauras d'où tu viens\net qui tu es.", t.texte, Theme.ETEINT);
        attente.setAlignment(Align.center);
        Table scenePerso = new Table();
        scenePerso.stack(portrait, attente).grow().minHeight(0f);
        colonnePerso.add(scenePerso).grow().minHeight(0f).row();
        Table tourner = new Table();
        tourner.add(fleche("«", () -> portrait.tourner(-1)));
        tourner.add(t.label("tourner", t.petit, Theme.ETEINT)).pad(0f, 10f, 0f, 10f);
        tourner.add(fleche("»", () -> portrait.tourner(1)));
        colonnePerso.add(tourner).padBottom(8f).row();
        colonnePerso.add(reglages).growX();

        colonneFiche = t.panneau();
        colonneFiche.top();
        colonneFiche.add(t.label("Ta fiche", t.sousTitre, Theme.OR)).left().padBottom(10f).row();
        lignesFiche.top().left();
        ScrollPane defileFiche = new ScrollPane(lignesFiche, t.defilement);
        defileFiche.setFadeScrollBars(true);
        defileFiche.setScrollingDisabled(true, false);
        colonneFiche.add(defileFiche).grow().minHeight(0f).top().left();

        colonneScene = t.panneau();
        colonneScene.top();
        titre = t.label("", t.grand, Theme.OR_VIF);
        titre.setWrap(true);
        colonneScene.add(titre).growX().left().padBottom(10f).row();
        infoCarte = t.label("", t.texte, Theme.OR_VIF);
        infoCarte.setWrap(true);
        zoneCarte.add(carte).grow().row();
        zoneCarte.add(infoCarte).growX().left().padTop(8f).minHeight(46f);
        celluleCarte = colonneScene.add(zoneCarte).growX().height(0f).padBottom(0f);
        colonneScene.row();
        zoneCarte.setVisible(false);
        vignette = new Vignette(peintre, atelierCarte, () -> zone);
        celluleVignette = colonneScene.add(vignette).growX().height(0f).padBottom(0f);
        colonneScene.row();
        vignette.setVisible(false);
        texte = t.paragraphe("");
        Table corps = new Table();
        corps.top().left();
        corps.add(texte).growX().left().padBottom(16f).row();
        choix.top().left();
        corps.add(choix).growX().left();
        ScrollPane defile = new ScrollPane(corps, t.defilement);
        defile.setFadeScrollBars(false);
        defile.setScrollingDisabled(true, false);
        colonneScene.add(defile).grow().minHeight(0f).row();
        colonneScene.add(t.label("Échap : revenir au monde", t.petit, Theme.ETEINT)).right().padTop(8f);

        racine.setFillParent(true);
        racine.pad(28f);
        stage.addActor(racine);
        placer();
        stage.getRoot().setVisible(false);
        // Les chiffres 1 à 9 choisissent directement.
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int code) {
                if (!ouvert || stage.getKeyboardFocus() instanceof TextField) return false;
                int n = code >= Input.Keys.NUM_1 && code <= Input.Keys.NUM_9 ? code - Input.Keys.NUM_1
                    : code >= Input.Keys.NUMPAD_1 && code <= Input.Keys.NUMPAD_9 ? code - Input.Keys.NUMPAD_1 : -1;
                if (n < 0 || n >= choix.getChildren().size) return false;
                Actor a = choix.getChildren().get(n);
                if (a instanceof Choix) ((Choix) a).cliquer();
                return true;
            }
        });
    }

    private void placer() {
        racine.clearChildren();
        float l = stage.getWidth();
        // Chaque colonne tient dans la hauteur de l'écran : ce qui dépasse défile (les choix, la fiche).
        float h = stage.getHeight() - 56f;
        racine.add(colonnePerso).width(Math.max(300f, l * 0.27f)).height(h).padRight(18f);
        racine.add(colonneFiche).width(Math.max(240f, l * 0.2f)).height(h).padRight(18f);
        racine.add(colonneScene).growX().height(h);
    }

    public Stage getStage() {
        return stage;
    }

    public boolean ouvert() {
        return ouvert;
    }

    /** Une étape de la création : son titre, son texte, ses choix (ou une saisie), et la fiche telle qu'elle est maintenant. */
    public void afficher(Joueur.Scene scene, List<String[]> fiche, IntConsumer action, Consumer<String> valider, Runnable annuler) {
        if (!ouvert) {
            ouvert = true;
            stage.getRoot().setVisible(true);
            racine.getColor().a = 0f;
            racine.addAction(Actions.fadeIn(0.5f));
        }
        titre.setText(scene.titre);
        texte.setText(scene.texte);
        libelles.clear();
        libelles.addAll(scene.choix);
        // La carte : pour le royaume puis la ville. Elle prend la moitié de la hauteur de la colonne.
        boolean avecCarte = scene.carte != Joueur.Scene.SANS_CARTE && carte.prete();
        zoneCarte.setVisible(avecCarte);
        celluleCarte.height(avecCarte ? Math.min(stage.getHeight() * 0.5f, 420f) : 0f).padBottom(avecCarte ? 12f : 0f);
        if (avecCarte) {
            aideCarte = scene.carte == Joueur.Scene.CARTE_ROYAUMES ? "Survole un royaume, clique pour le choisir." : "Survole une ville, clique pour y naître. La capitale est en losange.";
            infoCarte.setText(aideCarte);
            carte.montrer(scene.carte, scene.lieux, scene.etatCarte, action);
        }
        boolean avecVignette = !avecCarte && !vignette.vide();
        vignette.setVisible(avecVignette);
        celluleVignette.height(avecVignette ? Math.min(stage.getHeight() * 0.36f, 300f) : 0f).padBottom(avecVignette ? 12f : 0f);
        colonneScene.invalidateHierarchy();
        choix.clearChildren();
        if (scene.saisie != null) {
            TextField champ = new TextField(scene.saisie, t.saisie);
            champ.setTextFieldListener((c, touche) -> {
                if (touche == '\r' || touche == '\n') valider.accept(champ.getText());
            });
            choix.add(champ).width(360f).left().padBottom(10f).row();
            choix.add(new Choix("Valider", () -> valider.accept(champ.getText()))).growX().left().row();
            choix.add(new Choix("« Revenir", annuler)).growX().left().row();
            stage.setKeyboardFocus(champ);
        } else {
            for (int i = 0; i < scene.choix.size(); i++) {
                int k = i;
                choix.add(new Choix(scene.choix.get(i), () -> action.accept(k))).growX().left().row();
            }
            stage.setKeyboardFocus(null);
        }
        lignesFiche.clearChildren();
        for (String[] ligne : fiche) {
            lignesFiche.add(t.label(ligne[0], t.petit, Theme.ETEINT)).left().row();
            Label v = t.label(ligne[1], t.texte, Theme.ENCRE);
            v.setWrap(true);
            lignesFiche.add(v).growX().left().padBottom(8f).row();
        }
        if (fiche.isEmpty()) lignesFiche.add(t.label("Elle se remplira à chaque choix.", t.petit, Theme.ETEINT)).left();
    }

    /** Lot K4 : la vignette de la prochaine étape (à donner avant afficher) ; lon et lat de la ville natale, en radians. */
    public void montrerVignette(com.deldaran.politics.histoire.Creation.Tableau tableau, double lon, double lat, int etat) {
        vignette.montrer(tableau, lon, lat, etat);
    }

    /** À chaque image : ce que la création sait du personnage. Les réglages ne se refont que si quelque chose a changé. */
    public void apercu(boolean possible, int espece, boolean femme, long graine, Aspect aspect) {
        long cle = !possible ? -1L : graine * 31L + espece * 7L + (femme ? 1 : 0) + aspect.coupe * 101L + aspect.barbe * 1009L + aspect.vetement * 10007L
            + aspect.haut * 100003L + aspect.bas * 1000033L + aspect.cape * 10000019L + aspect.couleurCape * 100000007L;
        vignette.aspect = aspect;
        if (cle == cleReglages && possible == this.possible) return;
        cleReglages = cle;
        this.possible = possible;
        this.espece = espece;
        this.femme = femme;
        this.graine = graine;
        this.aspect = aspect;
        attente.setVisible(!possible);
        if (possible) {
            traits = aspect.traits(graine, espece, femme);
            tenue = aspect.tenue(graine, femme);
            // Dans l'aperçu, rien en main ni d'armure : les armes viendront de ton sac.
            Vignette.desarmer(tenue);
        }
        refaireReglages();
    }

    private void refaireReglages() {
        reglages.clearChildren();
        if (!possible) return;
        reglage("Visage", "n° " + (aspect.visage + 1), -1, s -> aspect.visage = Math.max(0, aspect.visage + s));
        reglage("Coiffure", COUPES[traits.coupe], -1, s -> aspect.coupe = Math.floorMod(traits.coupe + s, COUPES.length));
        if (!femme) reglage("Barbe", BARBES[traits.barbe], -1, s -> aspect.barbe = Math.floorMod(traits.barbe + s, BARBES.length));
        int v = Aspect.vetementDe(tenue);
        reglage("Habit", Aspect.VETEMENTS[v], -1, s -> aspect.vetement = Math.floorMod(v + s, Aspect.VETEMENTS.length));
        int haut = tenue.couleurs[Aspect.pieceHaut(v)];
        reglage("Couleur", "", haut, s -> aspect.haut = Aspect.tissuSuivant(haut, s));
        if (v != Aspect.ROBE && tenue.porte(Tenue.PANTALON)) {
            int bas = tenue.couleurs[Tenue.PANTALON];
            reglage("Braies", "", bas, s -> aspect.bas = Aspect.tissuSuivant(bas, s));
        }
        int c = Aspect.capeDe(tenue);
        reglage("Cape", Aspect.CAPES[c], -1, s -> aspect.cape = Math.floorMod(c + s, Aspect.CAPES.length));
        if (c != Aspect.SANS_CAPE) {
            int teinte = tenue.couleurs[Tenue.CAPE];
            reglage("Couleur de la cape", "", teinte, s -> aspect.couleurCape = Aspect.tissuSuivant(teinte, s));
        }
    }

    /** Une ligne de réglage : son nom, « valeur » ; la valeur est un texte ou une pastille de couleur (rangée de la palette). */
    private void reglage(String nom, String valeur, int rangee, IntConsumer changer) {
        reglages.add(t.label(nom, t.petit, Theme.ETEINT)).left().padRight(10f);
        reglages.add(fleche("«", () -> changer.accept(-1))).padRight(4f);
        if (rangee >= 0) {
            Image pastille = new Image(t.blanc);
            pastille.setColor(new Color(teintes[rangee]));
            reglages.add(pastille).size(56f, 16f).pad(0f, 6f, 0f, 6f);
        } else reglages.add(t.label(valeur, t.texte, Theme.OR)).minWidth(110f).pad(0f, 6f, 0f, 6f);
        reglages.add(fleche("»", () -> changer.accept(1))).padLeft(4f).row();
    }

    private Label fleche(String signe, Runnable action) {
        Label l = t.label(signe, t.bouton, Theme.OR);
        l.setTouchable(Touchable.enabled);
        l.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Sons.effet("clic");
                action.run();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor depuis) {
                if (pointer == -1) l.setColor(1.3f, 1.3f, 1.2f, 1f);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor vers) {
                if (pointer == -1) l.setColor(Color.WHITE);
            }
        });
        return l;
    }

    /** Pour les essais : le titre de l'étape et un clic sur le choix n° i. */
    String titreEtape() {
        return titre.getText().toString();
    }

    boolean choisirPourEssai(int i) {
        if (i < 0 || i >= choix.getChildren().size || !(choix.getChildren().get(i) instanceof Choix)) return false;
        ((Choix) choix.getChildren().get(i)).cliquer();
        return true;
    }

    public boolean carteManque() {
        return !carte.prete();
    }

    /** La carte à plat (un pixel par case du globe), peinte par FirstScreen avec les tons du monde. */
    public void donnerCarte(com.deldaran.politics.planet.Monde monde, com.deldaran.politics.histoire.Population population, Pixmap image) {
        carte.donner(monde, population, image);
    }

    /** Ce qu'on survole sur la carte : son libellé sous la carte, et le choix allumé dans la liste. */
    private void survolCarte(int k) {
        infoCarte.setText(k >= 0 && k < libelles.size() ? libelles.get(k) : aideCarte);
        for (int i = 0; i < choix.getChildren().size; i++) {
            Actor a = choix.getChildren().get(i);
            if (a instanceof Choix) ((Choix) a).allumer(i == k);
        }
    }

    /** Pour les essais : survoler le choix n° k sur la carte. */
    void survolerCartePourEssai(int k) {
        if (zoneCarte.isVisible()) carte.survolerPourEssai(k);
    }

    public void fermer() {
        if (!ouvert) return;
        ouvert = false;
        stage.setKeyboardFocus(null);
        stage.getRoot().setVisible(false);
    }

    /** Échap ferme l'écran : on revient au monde, et la création reprendra où on l'a laissée. */
    public boolean prendreEchap() {
        if (!ouvert) return false;
        fermer();
        return true;
    }

    public void dessiner(float delta) {
        if (!ouvert) return;
        float l = stage.getViewport().getScreenWidth(), h = stage.getViewport().getScreenHeight();
        Gdx.gl.glColorMask(true, true, true, false);
        pinceau.getProjectionMatrix().setToOrtho2D(0, 0, l, h);
        pinceau.begin();
        pinceau.setColor(Color.WHITE);
        fond.dessiner(pinceau, delta, l, h, Gdx.input.getX());
        pinceau.setColor(0f, 0f, 0f, VOILE);
        pinceau.draw(t.blanc, 0, 0, l, h);
        pinceau.setColor(Color.WHITE);
        pinceau.end();
        stage.act(delta);
        stage.draw();
        Gdx.gl.glColorMask(true, true, true, true);
    }

    public void redimensionner(int largeur, int hauteur) {
        stage.getViewport().update(largeur, hauteur, true);
        placer();
    }

    public void dispose() {
        stage.dispose();
        pinceau.dispose();
        fond.dispose();
        peintre.dispose();
        carte.dispose();
        vignette.dispose();
        atelierCarte.shutdownNow();
    }

    /** Un choix de l'histoire : un losange et un texte qui va à la ligne ; il s'allume au survol. */
    private final class Choix extends Table {
        private final Label libelle;
        private final Image marque;
        private final Runnable action;

        Choix(String texteChoix, Runnable action) {
            this.action = action;
            marque = t.losangeImage();
            marque.setVisible(false);
            libelle = t.label(texteChoix, t.texte, Theme.OR);
            libelle.setWrap(true);
            add(marque).size(10f, 10f).padRight(10f).top().padTop(6f);
            add(libelle).growX().left();
            pad(5f, 8f, 5f, 8f);
            setTouchable(Touchable.enabled);
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    cliquer();
                }

                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor depuis) {
                    if (pointer != -1) return;
                    Sons.effet("survol");
                    allumer(true);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor vers) {
                    if (pointer != -1 || (vers != null && vers.isDescendantOf(Choix.this))) return;
                    allumer(false);
                }
            });
        }

        void cliquer() {
            Sons.effet("clic");
            action.run();
        }

        private void allumer(boolean oui) {
            marque.setVisible(oui);
            libelle.getStyle().fontColor = oui ? Theme.OR_VIF : Theme.OR;
            libelle.setStyle(libelle.getStyle());
            setBackground(oui ? t.fondLueur : null);
        }
    }

    /**
     * Lot K3 : la carte à plat du monde (projection équirectangulaire, une case du globe par pixel) pour choisir son lieu de naissance.
     * Royaumes : chaque royaume possible prend une teinte et une bordure d'or ; celui sous la souris s'éclaire ; un clic le choisit.
     * Villes : la carte se resserre sur le royaume choisi ; ses villes sont des points (la capitale en losange) ; un clic choisit.
     */
    private final class CarteMonde extends Actor {
        private static final float RAYON_SURVOL = 14f;
        private Texture base, calqueTexture;
        private Pixmap calque;
        private int[] etatPixel;
        private int largeur, hauteur;
        private com.deldaran.politics.histoire.Population population;
        private com.deldaran.politics.planet.Monde monde;
        private int mode;
        private java.util.List<Integer> lieux = new java.util.ArrayList<>();
        private int etatCadre;
        private float u0, v0, u1, v1;
        private int survol = -1, survolDessine = -2;
        private Texture detail;
        private java.util.concurrent.Future<int[]> detailEnCours;
        /** Pour les essais : un survol imposé, que la souris ne remplace pas. */
        private boolean impose;
        private IntConsumer action;
        private float mx, my, mw, mh;

        CarteMonde() {
            setTouchable(Touchable.enabled);
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (survol < 0 || action == null) return;
                    Sons.effet("clic");
                    action.accept(survol);
                }
            });
        }

        boolean prete() {
            return base != null;
        }

        void donner(com.deldaran.politics.planet.Monde m, com.deldaran.politics.histoire.Population p, Pixmap image) {
            monde = m;
            population = p;
            largeur = image.getWidth();
            hauteur = image.getHeight();
            base = new Texture(image);
            base.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            base.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
            image.dispose();
            calque = new Pixmap(largeur, hauteur, Pixmap.Format.RGBA8888);
            calque.setBlending(Pixmap.Blending.None);
            calqueTexture = new Texture(calque);
            calqueTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            calqueTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
            // L'État de chaque pixel : -3 la mer (ou hors des régions), -1 une terre sans maître.
            etatPixel = new int[largeur * hauteur];
            for (int y = 0; y < hauteur; y++) {
                for (int x = 0; x < largeur; x++) {
                    int c = monde.index(x, y);
                    int r = c >= 0 && c < p.regions.regionDe.length ? p.regions.regionDe[c] : -1;
                    int e = r < 0 ? -3 : p.etats.regionEtat[r];
                    etatPixel[(hauteur - 1 - y) * largeur + x] = r < 0 ? -3 : e >= 0 && p.etats.estVivant(e) ? e : -1;
                }
            }
        }

        /** Une nouvelle étape : royaumes ou villes, la liste des lieux de chaque choix, et ce que fait un clic. */
        void montrer(int mode, java.util.List<Integer> lieux, int etatCadre, IntConsumer action) {
            this.mode = mode;
            this.lieux = new java.util.ArrayList<>(lieux);
            this.etatCadre = etatCadre;
            this.action = action;
            survol = -1;
            survolDessine = -2;
            impose = false;
            cadrer();
            if (detail != null) {
                detail.dispose();
                detail = null;
            }
            if (detailEnCours != null) detailEnCours.cancel(true);
            detailEnCours = null;
            if (mode == Joueur.Scene.CARTE_VILLES && zone != null) {
                double lon0 = (u0 / largeur - 0.5) * 2.0 * Math.PI, lon1 = (u1 / largeur - 0.5) * 2.0 * Math.PI;
                double lat1 = (0.5 - v0 / hauteur) * Math.PI, lat0 = (0.5 - v1 / hauteur) * Math.PI;
                int cadre = etatCadre;
                PeintreZone z = zone;
                detailEnCours = atelierCarte.submit(() -> z.peindre(lon0, lon1, lat0, lat1, DETAIL_L, DETAIL_H, cadre, NIVEAU_DETAIL));
            }
        }

        private boolean candidat(int e) {
            return e >= -1 && choixDeEtat(e) >= 0;
        }

        private int choixDeEtat(int e) {
            for (int k = 0; k < lieux.size(); k++) if (lieux.get(k) == e) return k;
            return -1;
        }

        /** Royaumes : tout le monde. Villes : le cadre du royaume, avec une marge, au rapport 2:1 de la carte. */
        private void cadrer() {
            u0 = 0f;
            v0 = 0f;
            u1 = largeur;
            v1 = hauteur;
            if (mode != Joueur.Scene.CARTE_VILLES) return;
            // Le cadre le plus étroit, en coupant le monde à 0 ou à 180 degrés : un royaume peut chevaucher le bord de la carte.
            float meilleur = Float.MAX_VALUE;
            for (int decalage : new int[] { 0, largeur / 2 }) {
                int xmin = Integer.MAX_VALUE, xmax = Integer.MIN_VALUE, ymin = Integer.MAX_VALUE, ymax = Integer.MIN_VALUE;
                for (int y = 0; y < hauteur; y++) {
                    for (int x = 0; x < largeur; x++) {
                        if (etatPixel[y * largeur + x] != etatCadre) continue;
                        int xx = (x + decalage) % largeur;
                        xmin = Math.min(xmin, xx);
                        xmax = Math.max(xmax, xx);
                        ymin = Math.min(ymin, y);
                        ymax = Math.max(ymax, y);
                    }
                }
                for (int k = 0; k < lieux.size(); k++) {
                    int v = lieux.get(k);
                    if (v < 0) continue;
                    float[] pos = pixelVille(v);
                    int xx = Math.floorMod((int) pos[0] + decalage, largeur);
                    xmin = Math.min(xmin, xx);
                    xmax = Math.max(xmax, xx);
                    ymin = Math.min(ymin, (int) pos[1]);
                    ymax = Math.max(ymax, (int) pos[1]);
                }
                if (xmin > xmax) continue;
                if (xmax - xmin < meilleur) {
                    meilleur = xmax - xmin;
                    float cx = (xmin + xmax) / 2f - decalage, cy = (ymin + ymax) / 2f;
                    float l = Math.max(10f, Math.max(xmax - xmin + 1, (ymax - ymin + 1) * 2f) * 1.6f), h = l / 2f;
                    u0 = cx - l / 2f;
                    u1 = cx + l / 2f;
                    v0 = Math.max(0f, Math.min(hauteur - h, cy - h / 2f));
                    v1 = v0 + h;
                }
            }
        }

        /** La position d'une ville sur la carte à plat (en pixels, y vers le bas). */
        private float[] pixelVille(int ville) {
            int c = population.villes.caseDe[ville];
            com.deldaran.politics.planet.Sphere s = monde.sphere;
            double lon = Math.atan2(s.y[c], s.x[c]), lat = Math.asin(Math.max(-1.0, Math.min(1.0, s.z[c])));
            float x = (float) ((lon / (2.0 * Math.PI) + 0.5) * largeur);
            float y = (float) ((0.5 - lat / Math.PI) * hauteur);
            return new float[] { x, y };
        }

        /** Le calque : les teintes et les bordures des royaumes, ou l'ombre autour du royaume choisi. */
        private void peindreCalque() {
            survolDessine = survol;
            int eSurvol = mode == Joueur.Scene.CARTE_ROYAUMES && survol >= 0 ? lieux.get(survol) : -9;
            int[] dx = { 1, -1, 0, 0 }, dy = { 0, 0, 1, -1 };
            int or = Color.rgba8888(0.86f, 0.68f, 0.32f, 0.95f), orVif = Color.rgba8888(1f, 0.9f, 0.6f, 1f), ombre = Color.rgba8888(0f, 0f, 0f, 0.3f);
            for (int y = 0; y < hauteur; y++) {
                for (int x = 0; x < largeur; x++) {
                    int e = etatPixel[y * largeur + x];
                    int couleur = 0;
                    if (e > -3) {
                        boolean bord = false;
                        for (int k = 0; k < 4 && !bord; k++) {
                            int xx = Math.floorMod(x + dx[k], largeur), yy = y + dy[k];
                            if (yy >= 0 && yy < hauteur && etatPixel[yy * largeur + xx] != e) bord = true;
                        }
                        if (mode == Joueur.Scene.CARTE_ROYAUMES) {
                            if (candidat(e)) {
                                Color c = teinteEtat(e);
                                boolean allume = e == eSurvol;
                                couleur = bord ? (allume ? orVif : or) : Color.rgba8888(c.r, c.g, c.b, allume ? 0.7f : 0.48f);
                            } else couleur = ombre;
                        } else if (e == etatCadre) couleur = bord ? or : 0;
                        else couleur = ombre;
                    }
                    calque.drawPixel(x, y, couleur);
                }
            }
            calqueTexture.draw(calque, 0, 0);
        }

        /** Une teinte par royaume, prise dans la palette du jeu (sang, pourpre, indigo, vert sombre…). */
        private Color teinteEtat(int e) {
            if (e < 0) return new Color(0.55f, 0.55f, 0.5f, 1f);
            int[] rangees = { 17, 19, 20, 21, 16, 10, 13, 22, 18, 9 };
            return new Color(teintes[rangees[e % rangees.length]]);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            if (detailEnCours != null && detailEnCours.isDone()) {
                try {
                    int[] pixels = detailEnCours.get();
                    Pixmap image = new Pixmap(DETAIL_L, DETAIL_H, Pixmap.Format.RGBA8888);
                    java.nio.ByteBuffer octets = image.getPixels();
                    for (int k = 0; k < pixels.length; k++) octets.putInt(k * 4, pixels[k]);
                    detail = new Texture(image);
                    detail.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    image.dispose();
                } catch (Exception e) {
                    // La zone n'a pas pu être peinte : on garde la carte du monde.
                }
                detailEnCours = null;
            }
            if (base == null || !isVisible() || impose) return;
            placerDessin();
            com.badlogic.gdx.math.Vector2 souris = screenToLocalCoordinates(new com.badlogic.gdx.math.Vector2(Gdx.input.getX(), Gdx.input.getY()));
            int avant = survol;
            survol = -1;
            if (souris.x >= mx && souris.x <= mx + mw && souris.y >= my && souris.y <= my + mh) {
                float px = u0 + (souris.x - mx) / mw * (u1 - u0), py = v0 + (1f - (souris.y - my) / mh) * (v1 - v0);
                if (mode == Joueur.Scene.CARTE_ROYAUMES) {
                    int x = Math.floorMod((int) px, largeur), y = Math.max(0, Math.min(hauteur - 1, (int) py));
                    int e = etatPixel[y * largeur + x];
                    if (candidat(e)) survol = choixDeEtat(e);
                    float meilleur = RAYON_SURVOL;
                    for (int k = 0; k < lieux.size(); k++) {
                        int etat = lieux.get(k);
                        if (etat < 0) continue;
                        float[] p = ecran(pixelVille(population.etats.capitale[etat]));
                        float d = (float) Math.hypot(p[0] - souris.x, p[1] - souris.y);
                        if (d < meilleur) {
                            meilleur = d;
                            survol = k;
                        }
                    }
                } else {
                    float meilleur = RAYON_SURVOL;
                    for (int k = 0; k < lieux.size(); k++) {
                        int v = lieux.get(k);
                        if (v < 0) continue;
                        float[] p = ecran(pixelVille(v));
                        float d = (float) Math.hypot(p[0] - souris.x, p[1] - souris.y);
                        if (d < meilleur) {
                            meilleur = d;
                            survol = k;
                        }
                    }
                }
            }
            if (survol != avant && survol >= 0) Sons.effet("survol");
            if (survol != avant) survolCarte(survol);
            if (survol != survolDessine) peindreCalque();
        }

        /** Pour les essais : survoler un choix comme si la souris était dessus. */
        void survolerPourEssai(int k) {
            impose = true;
            survol = k;
            survolCarte(k);
            peindreCalque();
        }

        /** Le rectangle où dessiner la carte dans l'acteur, au rapport de la vue. */
        private void placerDessin() {
            float rapport = (u1 - u0) / (v1 - v0);
            mw = getWidth();
            mh = mw / rapport;
            if (mh > getHeight()) {
                mh = getHeight();
                mw = mh * rapport;
            }
            mx = (getWidth() - mw) / 2f;
            my = (getHeight() - mh) / 2f;
        }

        /** Des pixels de la carte (x, y vers le bas) aux coordonnées de l'acteur, en tenant compte du tour du monde. */
        private float[] ecran(float[] p) {
            float x = p[0];
            while (x < u0) x += largeur;
            while (x > u1) x -= largeur;
            return new float[] { mx + (x - u0) / (u1 - u0) * mw, my + (1f - (p[1] - v0) / (v1 - v0)) * mh };
        }

        @Override
        public void draw(Batch b, float alpha) {
            if (base == null) return;
            placerDessin();
            float x = getX() + mx, y = getY() + my;
            float su = 1f / largeur, sv = 1f / hauteur;
            b.setColor(1f, 1f, 1f, alpha);
            if (mode == Joueur.Scene.CARTE_VILLES && detail != null) b.draw(detail, x, y, mw, mh);
            else {
                b.draw(base, x, y, mw, mh, u0 * su, v1 * sv, u1 * su, v0 * sv);
                b.draw(calqueTexture, x, y, mw, mh, u0 * su, v1 * sv, u1 * su, v0 * sv);
            }
            // Le cadre d'or.
            b.setColor(Theme.OR_SOMBRE.r, Theme.OR_SOMBRE.g, Theme.OR_SOMBRE.b, alpha);
            b.draw(t.blanc, x - 2, y - 2, mw + 4, 2);
            b.draw(t.blanc, x - 2, y + mh, mw + 4, 2);
            b.draw(t.blanc, x - 2, y, 2, mh);
            b.draw(t.blanc, x + mw, y, 2, mh);
            if (mode == Joueur.Scene.CARTE_ROYAUMES) {
                for (int k = 0; k < lieux.size(); k++) {
                    int etat = lieux.get(k);
                    if (etat < 0) continue;
                    float[] p = ecran(pixelVille(population.etats.capitale[etat]));
                    float taille = k == survol ? 11f : 7f, px = getX() + p[0], py = getY() + p[1];
                    b.setColor(0f, 0f, 0f, alpha * 0.85f);
                    b.draw(t.blanc, px - taille / 2f - 2f, py - taille / 2f - 2f, taille / 2f + 2f, taille / 2f + 2f, taille + 4f, taille + 4f, 1f, 1f, 45f, 0, 0, 1, 1, false, false);
                    Color c = k == survol ? Color.WHITE : Theme.OR_VIF;
                    b.setColor(c.r, c.g, c.b, alpha);
                    b.draw(t.blanc, px - taille / 2f, py - taille / 2f, taille / 2f, taille / 2f, taille, taille, 1f, 1f, 45f, 0, 0, 1, 1, false, false);
                }
            }
            if (mode == Joueur.Scene.CARTE_VILLES) {
                for (int k = 0; k < lieux.size(); k++) {
                    int v = lieux.get(k);
                    if (v < 0) continue;
                    float[] p = ecran(pixelVille(v));
                    boolean capitale = etatCadre >= 0 && population.etats.capitale[etatCadre] == v;
                    float taille = 6f + Math.min(5f, population.villes.habitants[v] / 900f) + (k == survol ? 4f : 0f);
                    float px = getX() + p[0], py = getY() + p[1];
                    b.setColor(0f, 0f, 0f, alpha * 0.85f);
                    if (capitale) b.draw(t.blanc, px - taille / 2f - 2f, py - taille / 2f - 2f, taille / 2f + 2f, taille / 2f + 2f, taille + 4f, taille + 4f, 1f, 1f, 45f, 0, 0, 1, 1, false, false);
                    else b.draw(t.blanc, px - taille / 2f - 1.5f, py - taille / 2f - 1.5f, taille + 3f, taille + 3f);
                    Color c = k == survol ? Color.WHITE : capitale ? Theme.OR_VIF : Theme.OR;
                    b.setColor(c.r, c.g, c.b, alpha);
                    if (capitale) b.draw(t.blanc, px - taille / 2f, py - taille / 2f, taille / 2f, taille / 2f, taille, taille, 1f, 1f, 45f, 0, 0, 1, 1, false, false);
                    else b.draw(t.blanc, px - taille / 2f, py - taille / 2f, taille, taille);
                }
            }
            b.setColor(Color.WHITE);
        }

        void dispose() {
            if (detail != null) detail.dispose();
            if (base != null) base.dispose();
            if (calqueTexture != null) calqueTexture.dispose();
            if (calque != null) calque.dispose();
        }
    }

    /** Le personnage en grand, à l'échelle entière la plus grande qui tienne, qui fait un tour de lui-même de temps en temps. */
    private final class Portrait extends Actor {
        private int indice;
        private float temps, pause;
        private Texture derniere;

        void tourner(int sens) {
            indice = Math.floorMod(indice + sens, TOUR.length);
            pause = PAUSE_TOUR;
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            if (pause > 0f) pause -= delta;
            else {
                temps += delta;
                if (temps > TOUR_AUTO) {
                    temps = 0f;
                    indice = (indice + 1) % TOUR.length;
                }
            }
        }

        @Override
        public void draw(Batch b, float alpha) {
            if (!possible) return;
            Apparence a = new Apparence(graine, espece);
            a.femme = femme;
            a.enfant = false;
            Texture tex = peintre.texture(a, tenue, traits);
            if (tex != null) derniere = tex;
            if (derniere == null) return;
            float echelle = Math.max(1f, (float) Math.floor(Math.min(getWidth() / Peintre.L, getHeight() / Peintre.H)));
            float l = Peintre.L * echelle, h = Peintre.H * echelle;
            float x = getX() + (getWidth() - l) / 2f, y = getY() + (getHeight() - h) / 2f;
            // Une ombre au sol et une lueur derrière.
            b.setColor(0f, 0f, 0f, 0.35f * alpha);
            b.draw(t.blanc, x + l * 0.25f, y + h * 0.04f, l * 0.5f, h * 0.025f);
            b.setColor(1f, 1f, 1f, alpha * (tex == null ? 0.6f : 1f));
            int d = TOUR[indice];
            b.draw(derniere, x, y, l, h, 0, d * Peintre.H, Peintre.L, Peintre.H, false, false);
            b.setColor(Color.WHITE);
        }
    }
}
