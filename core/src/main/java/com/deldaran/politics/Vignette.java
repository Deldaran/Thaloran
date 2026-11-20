package com.deldaran.politics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.deldaran.politics.histoire.Aspect;
import com.deldaran.politics.histoire.Creation;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Lot K4 : la vignette d'une étape de la création. La famille se tient devant les environs de la ville natale.
 * - Le décor est peint depuis le terrain, autour de la ville (FOND_KM de large), comme la carte de la ville mais de tout près.
 * - Chacun est peint par le Peintre avec la graine qu'il a dans le monde : on reconnaîtra ses parents en les croisant.
 * - Survoler quelqu'un donne son nom, son lien et son âge.
 */
final class Vignette extends Table {
    /** La taille du décor en points, et sa largeur en kilomètres. */
    static final int FOND_L = 240, FOND_H = 110;
    static final double FOND_KM = 8.0;
    /** Le niveau de détail du terrain pour le décor (la carte de la ville en prend 6). */
    static final int NIVEAU_FOND = 9;
    /** Ce que personne ne porte sur la vignette : armes, armures, sac. */
    private static final int[] SANS = { Tenue.EPEE, Tenue.OUTIL, Tenue.ARC, Tenue.CARQUOIS, Tenue.BOUCLIER, Tenue.LANTERNE, Tenue.HACHE, Tenue.LANCE,
        Tenue.DAGUE, Tenue.CASQUE, Tenue.MAILLE, Tenue.BATON, Tenue.PLASTRON, Tenue.SAC };

    private final Theme t = Theme.get();
    private final Peintre peintre;
    private final ExecutorService atelier;
    private final Supplier<EcranCreation.PeintreZone> zone;
    private final Decor decor = new Decor();
    private final Table rangee = new Table();
    private final Label legende;
    private Creation.Tableau tableau;
    /** L'apparence choisie par le joueur (pour se peindre soi-même avec ses réglages). */
    Aspect aspect;
    private Texture fond;
    private Future<int[]> fondEnCours;
    private int villeFond = -1;
    /**
     * Les maisons de la ville, derrière la famille : les premières que le monde y bâtit (mêmes graines que Villages.placerMaison).
     * Elles sont plus loin, donc réduites (ECHELLE_MAISON) ; leurs textures sont lissées, d'où un Batiments à part.
     */
    static final float ECHELLE_MAISON = 0.33f;
    private static final float[] MAISON_X = { 0.07f, 0.29f, 0.51f, 0.73f, 0.94f }, MAISON_Y = { 0.44f, 0.37f, 0.46f, 0.38f, 0.43f };
    private Batiments batiments;
    private final java.util.Set<Texture> lissees = new java.util.HashSet<>();
    private final Batiments.Plan[] maisons = new Batiments.Plan[MAISON_X.length];

    Vignette(Peintre peintre, ExecutorService atelier, Supplier<EcranCreation.PeintreZone> zone) {
        this.peintre = peintre;
        this.atelier = atelier;
        this.zone = zone;
        legende = t.label("", t.petit, Theme.ENCRE);
        legende.setAlignment(Align.left);
        Table couche = new Table();
        couche.add(legende).expandX().left().top().pad(8f, 12f, 0f, 12f).row();
        couche.add(rangee).expand().bottom().padBottom(6f);
        Stack pile = new Stack();
        pile.add(decor);
        pile.add(couche);
        add(pile).grow();
    }

    static Tenue desarmer(Tenue tenue) {
        for (int piece : SANS) tenue.enlever(piece);
        return tenue;
    }

    boolean vide() {
        return tableau == null || tableau.figures.isEmpty();
    }

    /** Une nouvelle étape : qui est là, et où se trouve la ville (en radians) pour le décor. */
    void montrer(Creation.Tableau tableau, double lon, double lat, int etat) {
        this.tableau = tableau;
        legende.setText(tableau.lieu);
        rangee.clearChildren();
        int n = tableau.figures.size();
        float echelle = n <= 4 ? 2f : n <= 6 ? 1.5f : 1.25f;
        int toi = 0;
        for (int i = 0; i < n; i++) if (tableau.figures.get(i).toi) toi = i;
        for (int i = 0; i < n; i++) {
            Creation.Figure f = tableau.figures.get(i);
            int direction = i < toi ? Personnage.BAS_DROITE : i > toi ? Personnage.BAS_GAUCHE : Personnage.BAS;
            Personne p = new Personne(f, direction, echelle);
            Table colonne = new Table();
            colonne.add(p).size(Peintre.L * echelle * 0.62f, Peintre.H * echelle).row();
            colonne.add(t.label(f.role, t.petit, f.toi ? Theme.OR_VIF : Theme.OR)).padTop(-4f);
            rangee.add(colonne).bottom().pad(0f, 2f, 0f, 2f);
        }
        if (tableau.ville != villeFond && zone.get() != null) {
            villeFond = tableau.ville;
            for (int k = 0; k < maisons.length; k++) {
                maisons[k] = Batiments.plan(melanger(tableau.ville * 1000003L + k));
                // Un peu de biais, pas toutes de face : un hameau, pas une façade de théâtre.
                maisons[k].orienter((k % 2 == 0 ? 1 : -1) * 0.35 + k * 0.2);
            }
            if (fondEnCours != null) fondEnCours.cancel(true);
            if (fond != null) {
                fond.dispose();
                fond = null;
            }
            double demiLon = FOND_KM / 2.0 / com.deldaran.politics.planet.Terrain.RAYON_KM / Math.max(0.2, Math.cos(lat));
            double demiLat = FOND_KM / 2.0 / com.deldaran.politics.planet.Terrain.RAYON_KM * FOND_H / FOND_L;
            EcranCreation.PeintreZone z = zone.get();
            fondEnCours = atelier.submit(() -> z.peindre(lon - demiLon, lon + demiLon, lat - demiLat, lat + demiLat, FOND_L, FOND_H, etat, NIVEAU_FOND));
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (fondEnCours != null && fondEnCours.isDone()) {
            try {
                int[] pixels = fondEnCours.get();
                Pixmap image = new Pixmap(FOND_L, FOND_H, Pixmap.Format.RGBA8888);
                java.nio.ByteBuffer octets = image.getPixels();
                for (int k = 0; k < pixels.length; k++) octets.putInt(k * 4, pixels[k]);
                fond = new Texture(image);
                fond.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                image.dispose();
            } catch (Exception e) {
                // Pas de décor : la vignette reste sur son fond sombre.
            }
            fondEnCours = null;
        }
    }

    void dispose() {
        if (fondEnCours != null) fondEnCours.cancel(true);
        if (fond != null) fond.dispose();
        if (batiments != null) batiments.dispose();
    }

    /** Le même mélange que Villages (pour retrouver les graines des maisons de la ville). */
    private static long melanger(long x) {
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return x;
    }

    /** Les maisons, de la plus lointaine (la plus haute) à la plus proche. */
    private void dessinerMaisons(Batch b, float x, float y, float l, float h, float alpha) {
        if (maisons[0] == null) return;
        if (batiments == null) batiments = new Batiments();
        Integer[] ordre = new Integer[maisons.length];
        for (int k = 0; k < ordre.length; k++) ordre[k] = k;
        java.util.Arrays.sort(ordre, (u, v) -> Float.compare(MAISON_Y[v], MAISON_Y[u]));
        for (int k : ordre) {
            Batiments.Image im = batiments.image(maisons[k]);
            if (im == null) continue;
            if (lissees.add(im.texture)) im.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            float s = ECHELLE_MAISON, sol = y + h * MAISON_Y[k];
            // Plus loin, un peu plus sombre et bleuté (l'air entre elles et nous).
            float voile = 0.82f + (0.46f - MAISON_Y[k]) * 1.2f;
            b.setColor(voile, voile, voile * 1.04f, alpha);
            int tl = im.texture.getWidth(), th = im.texture.getHeight();
            b.draw(im.texture, x + l * MAISON_X[k] - im.ancreX * s, sol - im.ancreY * s, tl * s, th * s, 0, 0, tl, th, false, false);
        }
    }

    /** Le décor : les environs de la ville, un peu assombris, avec un sol plus sombre en bas pour les noms, et un filet d'or. */
    private final class Decor extends Actor {
        @Override
        public void draw(Batch b, float alpha) {
            float x = getX(), y = getY(), l = getWidth(), h = getHeight();
            if (fond != null) {
                b.setColor(0.9f, 0.9f, 0.92f, alpha);
                b.draw(fond, x, y, l, h);
            } else {
                b.setColor(0.07f, 0.06f, 0.05f, alpha);
                b.draw(t.blanc, x, y, l, h);
            }
            // Les maisons ne sortent pas du cadre (on coupe ce qui dépasse).
            b.flush();
            if (clipBegin()) {
                dessinerMaisons(b, x, y, l, h, alpha);
                b.flush();
                clipEnd();
            }
            // Le bas s'assombrit par bandes : la famille se détache, les noms se lisent.
            for (int k = 0; k < 6; k++) {
                b.setColor(0f, 0f, 0f, alpha * 0.07f);
                b.draw(t.blanc, x, y, l, h * (0.36f - k * 0.05f));
            }
            b.setColor(0f, 0f, 0f, alpha * 0.45f);
            b.draw(t.blanc, x, y + h - 30f, l, 30f);
            b.setColor(Theme.OR_SOMBRE.r, Theme.OR_SOMBRE.g, Theme.OR_SOMBRE.b, alpha);
            b.draw(t.blanc, x, y, l, 1f);
            b.draw(t.blanc, x, y + h - 1f, l, 1f);
            b.draw(t.blanc, x, y, 1f, h);
            b.draw(t.blanc, x + l - 1f, y, 1f, h);
            b.setColor(Color.WHITE);
        }
    }

    /** Une personne debout, tournée vers le joueur. Le survol donne sa légende. */
    private final class Personne extends Actor {
        private final Creation.Figure f;
        private final int direction;
        private final float echelle;
        private final Apparence apparence;
        private Tenue tenue;
        private Traits traits;

        Personne(Creation.Figure f, int direction, float echelle) {
            this.f = f;
            this.direction = direction;
            this.echelle = echelle;
            apparence = new Apparence(f.graine, f.espece);
            apparence.femme = f.femme;
            apparence.enfant = f.enfant;
            if (!f.toi) {
                // Comme dans le monde (Personnage) : la tenue et les traits viennent de la graine.
                tenue = desarmer(Tenue.tirer(f.graine, f.femme, f.enfant));
                traits = Traits.tirer(f.graine, f.espece, f.femme && !f.enfant, f.enfant);
            }
            addListener(new InputListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor depuis) {
                    if (pointer == -1) legende.setText(f.legende);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor vers) {
                    if (pointer == -1 && tableau != null) legende.setText(tableau.lieu);
                }
            });
        }

        @Override
        public void draw(Batch b, float alpha) {
            if (f.toi) {
                if (aspect == null) return;
                // Soi-même : avec les réglages du portrait ; enfant, on garde ses couleurs et sa coiffure, sans barbe.
                if (f.enfant) {
                    tenue = Tenue.tirer(f.graine, f.femme, true);
                    aspect.appliquer(tenue);
                    traits = Traits.tirer(f.graine, f.espece, false, true);
                    aspect.appliquer(traits);
                    traits.barbe = Traits.SANS_BARBE;
                    traits.longueurBarbe = 0;
                } else {
                    tenue = aspect.tenue(f.graine, f.femme);
                    traits = aspect.traits(f.graine, f.espece, f.femme);
                }
                desarmer(tenue);
            }
            Texture tex = peintre.texture(apparence, tenue, traits);
            if (tex == null) return;
            float l = Peintre.L * echelle, h = Peintre.H * echelle;
            float x = getX() + (getWidth() - l) / 2f, y = getY();
            b.setColor(0f, 0f, 0f, 0.4f * alpha);
            b.draw(t.blanc, x + l * 0.3f, y + h * 0.035f, l * 0.4f, h * 0.025f);
            b.setColor(1f, 1f, 1f, alpha);
            b.draw(tex, x, y, l, h, 0, direction * Peintre.H, Peintre.L, Peintre.H, false, false);
            b.setColor(Color.WHITE);
        }
    }
}
