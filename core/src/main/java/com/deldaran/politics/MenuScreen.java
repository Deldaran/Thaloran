package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Lot K1 : le menu d'accueil. À gauche le titre et le menu (Continuer, Créer un monde, Charger un monde, Paramètres,
 * Quitter) ; à droite la page ouverte, qui apparaît en fondu. Derrière, le fond animé (FondMenu) et la musique du menu.
 */
public class MenuScreen extends ScreenAdapter {
    private static final String[] PHRASES = {
        "Les cendres se souviennent.", "Cinq mille ans d'histoire, et pas une page pour toi. Pas encore.", "Chaque royaume tombe.",
        "Les morts attendent leur tour.", "Rien ne dure, sauf ce qu'on raconte.", "La terre boit le sang des anciens.", "Il faut bien que quelqu'un se souvienne."
    };
    private static final float DUREE_PHRASE = 9f, FONDU_SORTIE = 0.9f;
    private final Stage stage = new Stage(new ScreenViewport());
    private final SpriteBatch pinceau = new SpriteBatch();
    private final FondMenu fond = new FondMenu();
    private final Theme t = Theme.get();
    private final Container<Actor> page = new Container<>();
    private final Label phrase;
    private final Random hasard = new Random();
    private Theme.Bouton ouvert;
    private float tempsPhrase, sortie = -1f;
    private int indicePhrase;
    private Mondes.Fiche aLancer;
    private final Table racine = new Table(), gauche = new Table();
    private final Label titre;
    /** Pour les essais : la page à ouvrir tout de suite. */
    static String pageEssai;

    public MenuScreen() {
        racine.setFillParent(true);
        stage.addActor(racine);

        gauche.left();
        titre = t.label("T H A L O R A N", t.titre, Theme.OR_VIF);
        gauche.add(titre).left().padBottom(4f).row();
        phrase = t.label(PHRASES[0], t.sousTitre, Theme.ETEINT);
        gauche.add(phrase).left().padLeft(6f).padBottom(70f).row();

        List<Mondes.Fiche> mondes = Mondes.tous();
        if (!mondes.isEmpty()) {
            Mondes.Fiche dernier = mondes.get(0);
            Theme.Bouton b = t.bouton("Continuer", t.bouton, () -> lancer(dernier));
            gauche.add(b).left().row();
            Label quel = t.label(dernier.nom + " — " + dernier.etat().toLowerCase(), t.petit, Theme.ETEINT);
            gauche.add(quel).left().padLeft(36f).padBottom(14f).row();
        }
        Theme.Bouton creer = t.bouton("Créer un monde", t.bouton, null);
        creer.action = () -> ouvrir(creer, pageCreer());
        Theme.Bouton charger = t.bouton("Charger un monde", t.bouton, null);
        charger.action = () -> ouvrir(charger, pageCharger());
        charger.actif(!mondes.isEmpty());
        Theme.Bouton reglages = t.bouton("Paramètres", t.bouton, null);
        reglages.action = () -> ouvrir(reglages, Panneaux.parametres(null));
        Theme.Bouton quitter = t.bouton("Quitter le jeu", t.bouton, () -> Gdx.app.exit());
        for (Theme.Bouton b : new Theme.Bouton[] { creer, charger, reglages, quitter }) gauche.add(b).left().padBottom(6f).row();

        racine.add(gauche).expandY().left().padLeft(70f).width(560f);
        racine.add(page).expand().fill().padTop(60f).padBottom(60f).padRight(60f);

        Label pied = t.label("Échap en jeu : le menu   ·   ² : la fenêtre de debug", t.petit, Theme.ETEINT);
        pied.setPosition(20f, 14f);
        stage.addActor(pied);
        if ("creer".equals(pageEssai)) ouvrir(creer, pageCreer());
        else if ("charger".equals(pageEssai)) ouvrir(charger, pageCharger());
        else if ("parametres".equals(pageEssai)) ouvrir(reglages, Panneaux.parametres(null));
    }

    private void ouvrir(Theme.Bouton b, Actor contenu) {
        if (ouvert != null) ouvert.choisi(false);
        if (ouvert == b) {
            ouvert = null;
            page.setActor(null);
            return;
        }
        ouvert = b;
        b.choisi(true);
        Container<Actor> c = new Container<>(contenu);
        c.center();
        c.getColor().a = 0f;
        c.addAction(Actions.fadeIn(0.35f));
        page.setActor(c);
        Sons.effet("page");
    }

    // ---------------------------------------------------------------- Créer un monde

    private Actor pageCreer() {
        Table p = t.panneau();
        p.defaults().left();
        p.add(t.label("Forger un monde", t.grand, Theme.OR_VIF)).padBottom(8f).row();
        Label intro = t.paragraphe("D'abord la terre se forme : les continents dérivent, les montagnes se lèvent, les fleuves creusent leur lit. "
            + "Puis les peuples s'éveillent et vivent leur histoire, siècle après siècle. Quand elle s'achève, tu y nais.");
        intro.setColor(Theme.ETEINT);
        p.add(intro).width(560f).padBottom(22f).row();

        p.add(t.label("Nom du monde", t.bouton, Theme.OR)).padBottom(6f).row();
        TextField nom = new TextField(Mondes.nomAuHasard(hasard), t.saisie);
        Table ligneNom = new Table();
        ligneNom.add(nom).width(420f).padRight(10f);
        ligneNom.add(t.bouton("Au hasard", t.texte, () -> nom.setText(Mondes.nomAuHasard(hasard))));
        p.add(ligneNom).padBottom(18f).row();

        p.add(t.label("Graine", t.bouton, Theme.OR)).padBottom(6f).row();
        TextField graine = new TextField(Long.toString(Math.abs(hasard.nextLong() % 1_000_000L)), t.saisie);
        graine.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
        graine.setMaxLength(12);
        Table ligneGraine = new Table();
        ligneGraine.add(graine).width(220f).padRight(10f);
        ligneGraine.add(t.bouton("Au hasard", t.texte, () -> graine.setText(Long.toString(Math.abs(hasard.nextLong() % 1_000_000L)))));
        p.add(ligneGraine).padBottom(4f).row();
        Label aideGraine = t.label("La même graine donne toujours le même monde.", t.petit, Theme.ETEINT);
        p.add(aideGraine).padBottom(18f).row();

        p.add(t.label("Durée de l'histoire", t.bouton, Theme.OR)).padBottom(6f).row();
        String[] durees = new String[Mondes.DUREES.length];
        for (int k = 0; k < durees.length; k++) durees[k] = Mondes.DUREES[k] + " ans";
        int[] duree = { Mondes.DUREES.length - 1 };
        p.add(t.options(durees, duree[0], i -> duree[0] = i)).padBottom(4f).row();
        Label aideDuree = t.paragraphe("Plus elle est longue, plus le monde a de passé : de vieilles villes, des ruines, des lignées, des légendes. Et plus l'attente est longue.");
        aideDuree.setColor(Theme.ETEINT);
        p.add(aideDuree).width(560f).padBottom(26f).row();

        p.add(t.bouton("Forger ce monde", t.grand, () -> {
            long g;
            try {
                g = Long.parseLong(graine.getText().isBlank() ? "0" : graine.getText());
            } catch (NumberFormatException e) {
                g = 0L;
            }
            lancer(Mondes.creer(nom.getText(), g, Mondes.DUREES[duree[0]]));
        })).row();
        return p;
    }

    // ---------------------------------------------------------------- Charger un monde

    private Actor pageCharger() {
        Table p = t.panneau();
        p.defaults().left();
        p.add(t.label("Tes mondes", t.grand, Theme.OR_VIF)).padBottom(18f).row();
        Table liste = new Table();
        liste.top().left();
        SimpleDateFormat date = new SimpleDateFormat("d/MM/yyyy à HH:mm");
        for (Mondes.Fiche f : Mondes.tous()) {
            Table carte = new Table();
            carte.setBackground(t.fondCadre);
            carte.pad(14f, 20f, 14f, 20f);
            carte.defaults().left();
            Table texte = new Table();
            texte.defaults().left();
            texte.left();
            texte.add(t.label(f.nom, t.bouton, Theme.OR_VIF)).row();
            texte.add(t.label("Graine " + f.graine + "   ·   " + f.duree + " ans d'histoire", t.petit, Theme.ETEINT)).row();
            texte.add(t.label(f.etat(), t.texte, Theme.ENCRE)).padTop(4f).row();
            if (f.joue > 0) texte.add(t.label("Dernière visite le " + date.format(new Date(f.joue)), t.petit, Theme.ETEINT)).row();
            carte.add(texte).expandX().fillX();
            Table actions = new Table();
            actions.add(t.bouton("Jouer", t.bouton, () -> lancer(f))).right().row();
            Theme.Bouton effacer = t.bouton("Effacer", t.texte, null);
            effacer.action = () -> {
                if (!"Vraiment ?".equals(effacer.getUserObject())) {
                    effacer.setUserObject("Vraiment ?");
                    effacer.libelle("Effacer pour toujours ?");
                    return;
                }
                Mondes.supprimer(f);
                carte.addAction(Actions.sequence(Actions.fadeOut(0.3f), Actions.removeActor()));
            };
            actions.add(effacer).right();
            carte.add(actions).right();
            liste.add(carte).width(540f).padBottom(10f).row();
        }
        ScrollPane defile = new ScrollPane(liste, t.defilement);
        defile.setFadeScrollBars(false);
        defile.setScrollingDisabled(true, false);
        p.add(defile).width(560f).height(Math.min(520f, Gdx.graphics.getHeight() * 0.6f)).row();
        return p;
    }

    // ---------------------------------------------------------------- Entrer dans un monde

    private void lancer(Mondes.Fiche f) {
        if (sortie >= 0f) return;
        Mondes.marquerJoue(f);
        aLancer = f;
        sortie = 0f;
        stage.getRoot().setTouchable(Touchable.disabled);
        Sons.musique(null);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        Sons.musique("main_menu");
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.BLACK);
        // L'image reste opaque : on ne touche pas à sa couche alpha.
        Gdx.gl.glColorMask(true, true, true, false);
        tempsPhrase += delta;
        if (tempsPhrase > DUREE_PHRASE) {
            tempsPhrase = 0f;
            indicePhrase = (indicePhrase + 1) % PHRASES.length;
            String s = PHRASES[indicePhrase];
            phrase.clearActions();
            phrase.addAction(Actions.sequence(Actions.fadeOut(0.8f), Actions.run(() -> phrase.setText(s)), Actions.fadeIn(1.2f)));
        }
        float l = stage.getViewport().getScreenWidth(), h = stage.getViewport().getScreenHeight();
        pinceau.getProjectionMatrix().setToOrtho2D(0, 0, l, h);
        pinceau.begin();
        fond.dessiner(pinceau, delta, l, h, Gdx.input.getX());
        pinceau.end();
        stage.act(delta);
        stage.draw();
        Gdx.gl.glColorMask(true, true, true, true);
        if (sortie >= 0f) {
            Gdx.gl.glColorMask(true, true, true, false);
            sortie += delta;
            pinceau.begin();
            pinceau.setColor(0f, 0f, 0f, Math.min(1f, sortie / FONDU_SORTIE));
            pinceau.draw(t.blanc, 0, 0, l, h);
            pinceau.setColor(Color.WHITE);
            pinceau.end();
            Gdx.gl.glColorMask(true, true, true, true);
            if (sortie >= FONDU_SORTIE && aLancer != null) {
                Mondes.Fiche f = aLancer;
                aLancer = null;
                Gdx.app.postRunnable(() -> Thaloran.jeu().lancer(f));
            }
        }
    }

    @Override
    public void resize(int largeur, int hauteur) {
        stage.getViewport().update(largeur, hauteur, true);
        // La colonne de gauche prend 40 % de l'écran ; le titre se réduit pour y tenir.
        float colonne = Math.max(470f, Math.min(760f, largeur * 0.40f));
        racine.getCell(gauche).width(colonne);
        titre.setFontScale(1f);
        float naturel = titre.getPrefWidth();
        titre.setFontScale(Math.min(1f, (colonne - 30f) / naturel));
        racine.invalidateHierarchy();
    }

    @Override
    public void dispose() {
        stage.dispose();
        pinceau.dispose();
        fond.dispose();
    }
}
