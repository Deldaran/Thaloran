package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.deldaran.politics.planet.Biome;
import com.deldaran.politics.planet.Bruit;
import com.deldaran.politics.planet.Cube;
import com.deldaran.politics.planet.Terrain;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Vegetation {
    public static final double METRES_SPRITE = 0.045;
    public static final double METRES_ECRAN_MAX = 0.4;

    private static final int NIVEAU_PARCELLE = 15;
    private static final int CASES = 40;
    private static final int CASE_L = 104;
    private static final int CASE_H = 148;
    private static final int ANCRE = 9;
    private static final int ANCRE_PERSONNAGE = 2;
    private static final int ANCRE_MAISON = 2;
    private static final int MAISON = 1 << 20;
    private static final int FEU = 1 << 26;
    private static final float QUAD_L = 10f, QUAD_H = 15f, ANCRE_ARBRE = 1f;
    public static final int VARIANTES = 4;
    private static final int PAS_ECRAN = 48;
    private static final int PARCELLES_MAX = 3000;
    private static final double RAYON_METRES = Terrain.RAYON_KM * 1000.0;

    public static final int FEUILLU = 0, SOMBRE = 1, CONIFERE = 2, MORT = 3, BUISSON = 4, ROCHER = 5;

    private final Terrain terrain;
    private final Texture planche;
    private final ShaderProgram agrandir;
    private final ShaderProgram arbres;
    private final Texture palette;
    private final Peintre peintre;
    private final Bulles bulles;
    private final SpriteBatch pinceau = new SpriteBatch();

    private final Map<Long, float[]> parcelles = new ConcurrentHashMap<>();
    private final Map<Long, Long> usages = new HashMap<>();
    private final Set<Long> demandees = ConcurrentHashMap.newKeySet();
    private final LinkedBlockingQueue<Long> file = new LinkedBlockingQueue<>();
    private final Thread ouvrier;
    private long image;

    private final double[] est = new double[3], nord = new double[3], centre = new double[3];
    private final Set<Long> visibles = new HashSet<>();
    private float[] xs = new float[4096], ys = new float[4096], phases = new float[4096];
    private static final float[] SOUPLESSE = { 1f, 1f, 0.6f, 0.3f, 0.5f, 0f, 0.9f };
    public float vent, neige, ventEst, ventNord;
    private double rotationVue;
    public int parcellesManquantes, personnagesManquants, maisonsManquantes;
    private float ecrasement = 1f;
    private final Texture blanc;
    private float temps;
    private int[] sortes = new int[4096];
    private long[] ordre = new long[4096];
    private float[] rx = new float[4096], ry = new float[4096], rw = new float[4096], rh = new float[4096];
    public Personnage joueur;
    private float masque, echelleSprite, mpp;
    private boolean ombreJoueur, modeArbres;
    private double metresEcran;
    private Texture courante;
    private List<Personnage> personnages;
    private Assembleur assembleur;
    private Batiments batiments;
    private Villages villages;
    public Incendies incendies;
    private float feuB, feuE, feuL;

    public Vegetation(Terrain terrain) {
        this.terrain = terrain;
        planche = new Texture(Gdx.files.internal("vegetation/vegetation.png"));
        planche.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        com.badlogic.gdx.graphics.Pixmap point = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        point.setColor(1f, 1f, 1f, 1f);
        point.fill();
        blanc = new Texture(point);
        point.dispose();
        agrandir = new ShaderProgram(Gdx.files.internal("shaders/agrandir.vert"), Gdx.files.internal("shaders/vegetation.frag"));
        if (!agrandir.isCompiled()) {
            throw new IllegalStateException(agrandir.getLog());
        }
        pinceau.setShader(agrandir);
        arbres = new ShaderProgram(Gdx.files.internal("shaders/agrandir.vert"), Gdx.files.internal("shaders/arbre.frag"));
        if (!arbres.isCompiled()) {
            throw new IllegalStateException(arbres.getLog());
        }
        palette = new Texture(Gdx.files.internal("palettes/sol.png"));
        peintre = new Peintre();
        bulles = new Bulles();
        pinceau.setBlendFunctionSeparate(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ZERO, com.badlogic.gdx.graphics.GL20.GL_ONE);
        ouvrier = new Thread(this::travailler, "vegetation");
        ouvrier.setDaemon(true);
        ouvrier.start();
    }

    private void travailler() {
        Terrain.Point point = new Terrain.Point();
        double[] d = new double[3];
        try {
            while (true) {
                long cle = file.take();
                parcelles.put(cle, planter(cle, point, d));
                demandees.remove(cle);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private float[] planter(long cle, Terrain.Point point, double[] d) {
        int face = (int) (cle >>> 40);
        int px = (int) ((cle >>> 20) & 0xFFFFF);
        int py = (int) (cle & 0xFFFFF);
        double cote = 2.0 / (1 << NIVEAU_PARCELLE);
        float[] arbres = new float[CASES * CASES * 5];
        Terrain.Point sol = new Terrain.Point();
        double[] coin = new double[3];
        int n = 0;
        for (int j = 0; j < CASES; j++) {
            for (int i = 0; i < CASES; i++) {
                long h = melanger(cle * 1000003L + j * CASES + i);
                double a = -1.0 + (px + (i + hasard(h, 1)) / CASES) * cote;
                double b = -1.0 + (py + (j + hasard(h, 2)) / CASES) * cote;
                Cube.direction(face, a, b, d);
                terrain.echantillonner(d[0], d[1], d[2], 11, point);
                if (point.altitude * 1000f < 6f || point.riviere || point.lac) continue;
                byte biome = terrain.biome(d[0], d[1], d[2], point);
                double f = RAYON_METRES / 180.0;
                double clairiere = 0.55 + 0.9 * Bruit.perlin(d[0] * f, d[1] * f, d[2] * f, terrain.graine + 700);
                double densite = densite(biome) * Math.max(0.0, Math.min(1.0, clairiere));
                if (hasard(h, 3) >= densite) continue;
                int sorte = choisir(biome, hasard(h, 4));
                if (sorte < 0) continue;
                arbres[n++] = (float) d[0];
                arbres[n++] = (float) d[1];
                arbres[n++] = (float) d[2];
                arbres[n++] = sorte * VARIANTES + (int) (hasard(h, 5) * VARIANTES);
                arbres[n++] = RenduTuiles.altitudeSol(terrain, d[0], d[1], d[2], sol, coin);

            }
        }
        return Arrays.copyOf(arbres, n);
    }

    private static double densite(byte biome) {
        switch (biome) {
            case Biome.FORET: return 0.35;
            case Biome.JUNGLE: return 0.45;
            case Biome.TAIGA: return 0.3;
            case Biome.PRAIRIE: return 0.03;
            case Biome.SAVANE: return 0.02;
            case Biome.TOUNDRA: return 0.015;
            case Biome.DESERT: return 0.008;
            case Biome.MONTAGNE: return 0.03;
            default: return 0.0;
        }
    }

    private static int choisir(byte biome, double t) {
        switch (biome) {
            case Biome.FORET: return t < 0.45 ? FEUILLU : t < 0.8 ? SOMBRE : t < 0.95 ? BUISSON : MORT;
            case Biome.JUNGLE: return t < 0.7 ? SOMBRE : t < 0.9 ? FEUILLU : BUISSON;
            case Biome.TAIGA: return t < 0.85 ? CONIFERE : t < 0.9 ? MORT : t < 0.95 ? ROCHER : BUISSON;
            case Biome.PRAIRIE: return t < 0.5 ? FEUILLU : t < 0.9 ? BUISSON : ROCHER;
            case Biome.SAVANE: return t < 0.4 ? FEUILLU : t < 0.8 ? BUISSON : MORT;
            case Biome.TOUNDRA: return t < 0.5 ? ROCHER : t < 0.8 ? MORT : BUISSON;
            case Biome.DESERT: return t < 0.7 ? ROCHER : MORT;
            case Biome.MONTAGNE: return t < 0.7 ? ROCHER : CONIFERE;
            default: return -1;
        }
    }

    private static long melanger(long x) {
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return x;
    }

    private static double hasard(long h, int k) {
        return (melanger(h + k * 0x9E3779B97F4A7C15L) >>> 11) * 0x1.0p-53;
    }

    public void dessiner(GameCamera camera, RenduTuiles tuiles, List<Personnage> personnages, Assembleur assembleur, Villages villages, Batiments batiments) {
        double rayon = camera.rayonPixels;
        double metresEcran = RAYON_METRES / rayon;
        parcellesManquantes = 0;
        personnagesManquants = 0;
        maisonsManquantes = 0;
        if (incendies != null) incendies.masquer();
        if (metresEcran > METRES_ECRAN_MAX) return;
        image++;
        preparerVue(camera);
        tuiles.tourner(est, nord);
        rotationVue = tuiles.rotation();
        ecrasement = tuiles.ecrasement();
        int largeur = Gdx.graphics.getBackBufferWidth(), hauteur = Gdx.graphics.getBackBufferHeight();
        if (incendies != null) {
            incendies.cadrer(est, nord, rayon, ecrasement, largeur, hauteur, metresEcran, tuiles);
            incendies.front.dessinerSol();
        }
        chercherParcelles(rayon, largeur, hauteur);

        float margeL = (float) (QUAD_L / metresEcran), margeH = (float) (QUAD_H / metresEcran);
        int n = 0;
        for (long cle : visibles) {
            float[] arbres = parcelles.get(cle);
            if (arbres == null) continue;
            for (int k = 0; k < arbres.length; k += 5) {
                double x = arbres[k], y = arbres[k + 1], z = arbres[k + 2];
                if (x * centre[0] + y * centre[1] + z * centre[2] <= 0.0) continue;
                float sx = (float) (largeur / 2.0 + (x * est[0] + y * est[1] + z * est[2]) * rayon);
                float sy = (float) (hauteur / 2.0 + (x * nord[0] + y * nord[1] + z * nord[2]) * rayon * ecrasement);
                if (sx < -margeL || sx > largeur + margeL || sy < -margeH || sy > hauteur + margeL) continue;
                if (villages.dansVillage(x, y, z) || villages.enClairiere(x, y, z) || villages.dansChamp(x, y, z)) continue;
                int sorteArbre = brule((int) arbres[k + 3], x, y, z);
                if (sorteArbre < 0) continue;
                if (n == xs.length) agrandirTableaux();
                xs[n] = sx;
                ys[n] = sy + tuiles.decalage(x, y, z, arbres[k + 4]);
                sortes[n] = sorteArbre;
                phases[n] = (float) hasard(Double.doubleToLongBits(x * 7919.0 + y * 104729.0 + z), 6);
                ordre[n] = ((long) (100000 - (int) sy) << 32) | n;
                n++;
            }
        }

        float[] plantes = villages.arbres;
        for (int k = 0; k + 4 < villages.nombreArbres; k += 5) {
            double x = plantes[k], y = plantes[k + 1], z = plantes[k + 2];
            if (x * centre[0] + y * centre[1] + z * centre[2] <= 0.0) continue;
            float sx = (float) (largeur / 2.0 + (x * est[0] + y * est[1] + z * est[2]) * rayon);
            float sy = (float) (hauteur / 2.0 + (x * nord[0] + y * nord[1] + z * nord[2]) * rayon * ecrasement);
            if (sx < -margeL || sx > largeur + margeL || sy < -margeH || sy > hauteur + margeL) continue;
            int sortePlante = brule((int) plantes[k + 3], x, y, z);
            if (sortePlante < 0) continue;
            if (n == xs.length) agrandirTableaux();
            xs[n] = sx;
            ys[n] = sy + tuiles.decalage(x, y, z, plantes[k + 4]);
            sortes[n] = sortePlante;
            phases[n] = (float) hasard(Double.doubleToLongBits(x * 7919.0 + y * 104729.0 + z), 6);
            ordre[n] = ((long) (100000 - (int) sy) << 32) | n;
            n++;
        }

        for (int k = 0; k < personnages.size(); k++) {
            Personnage p = personnages.get(k);
            double x = p.position[0], y = p.position[1], z = p.position[2];
            if (x * centre[0] + y * centre[1] + z * centre[2] <= 0.0) continue;
            float sx = (float) (largeur / 2.0 + (x * est[0] + y * est[1] + z * est[2]) * rayon);
            float sy = (float) (hauteur / 2.0 + (x * nord[0] + y * nord[1] + z * nord[2]) * rayon * ecrasement);
            if (sx < -400 || sx > largeur + 400 || sy < -800 || sy > hauteur + 200) continue;
            if (n == xs.length) agrandirTableaux();
            xs[n] = sx;
            ys[n] = sy + (p.profondeur > 0f ? tuiles.decalage(p.altitude) : tuiles.decalage(x, y, z, tuiles.altitudeSol(x, y, z)));
            sortes[n] = -1 - k;
            ordre[n] = ((long) (100000 - (int) sy + (p.hauteurPortee > 0.0 ? 1 : 0)) << 32) | n;
            n++;
        }
        for (int k = 0; k < villages.nombre; k++) {
            double x = villages.x[k], y = villages.y[k], z = villages.z[k];
            if (x * centre[0] + y * centre[1] + z * centre[2] <= 0.0) continue;
            float sx = (float) (largeur / 2.0 + (x * est[0] + y * est[1] + z * est[2]) * rayon);
            float sy = (float) (hauteur / 2.0 + (x * nord[0] + y * nord[1] + z * nord[2]) * rayon * ecrasement);
            if (sx < -1500 || sx > largeur + 1500 || sy < -3000 || sy > hauteur + 200) continue;
            if (n == xs.length) agrandirTableaux();
            xs[n] = sx;
            Batiments.Plan plan = villages.plans[k];
            boolean surEau = plan.style == Batiments.ARCHE || plan.style == Batiments.PONTON;
            ys[n] = sy + (surEau ? tuiles.decalage(villages.altitude[k]) : tuiles.decalage(x, y, z, villages.altitude[k]));

            sortes[n] = MAISON + k;
            float recul = surEau ? (float) (0.5 * Math.hypot(plan.largeur, plan.profondeur) / RAYON_METRES * rayon * ecrasement) : 0f;
            ordre[n] = ((long) (100000 - (int) (sy + recul)) << 32) | n;

            n++;
        }
        if (incendies != null && incendies.ancre()) {
            Flammes flammes = incendies.front.flammes;
            for (int j = 0; j < flammes.bandes; j++) {
                if (n == xs.length) agrandirTableaux();
                xs[n] = 0f;
                ys[n] = flammes.profondeur(j);
                sortes[n] = FEU + j;
                ordre[n] = ((long) (100000 - flammes.profondeur(j)) << 32) | n;
                n++;
            }
        }
        Arrays.sort(ordre, 0, n);

        float e = (float) (METRES_SPRITE / metresEcran);
        echelleSprite = e;
        this.metresEcran = metresEcran;
        this.personnages = personnages;
        this.assembleur = assembleur;
        this.batiments = batiments;
        this.villages = villages;
        pinceau.getProjectionMatrix().setToOrtho2D(0f, 0f, largeur, hauteur);
        temps = (temps + Gdx.graphics.getDeltaTime()) % 3600f;
        mpp = (float) tuiles.metresPixel();
        pinceau.begin();
        regler(e);
        courante = null;
        modeArbres = false;
        int rangJoueur = -1;
        for (int k = 0; k < n; k++) {
            int i = (int) ordre[k];
            rw[i] = 0f;
            if (sortes[i] < 0 && joueur != null && personnages.get(-1 - sortes[i]) == joueur) rangJoueur = k;
            tracer(i, false);
        }
        if (rangJoueur >= 0) silhouette(rangJoueur, n);
        pinceau.end();
        oublier();
    }

    private int brule(int sorte, double x, double y, double z) {
        if (incendies == null || !incendies.front.present()) return sorte;
        if (incendies.front.etat(x, y, z) != Front.CENDRE) return sorte;
        int type = sorte / VARIANTES;
        if (type == BUISSON) return -1;
        if (type == ROCHER || type == MORT) return sorte;
        return MORT * VARIANTES + sorte % VARIANTES;
    }

    public float[] arbresAutour(double x, double y, double z, double rayonMetres) {
        double r = rayonMetres / RAYON_METRES, large = (rayonMetres + 250.0) / RAYON_METRES;
        float[] sortie = new float[64];
        int n = 0;
        for (float[] a : parcelles.values()) {
            if (a.length < 5) continue;
            double dx = a[0] - x, dy = a[1] - y, dz = a[2] - z;
            if (dx * dx + dy * dy + dz * dz > large * large) continue;
            for (int k = 0; k + 4 < a.length; k += 5) {
                dx = a[k] - x;
                dy = a[k + 1] - y;
                dz = a[k + 2] - z;
                if (dx * dx + dy * dy + dz * dz > r * r) continue;
                if (n + 4 > sortie.length) sortie = Arrays.copyOf(sortie, sortie.length * 2);
                sortie[n++] = a[k];
                sortie[n++] = a[k + 1];
                sortie[n++] = a[k + 2];
                sortie[n++] = a[k + 3];
            }
        }
        return Arrays.copyOf(sortie, n);
    }

    private void tracerFeu(int i, int j) {
        if (modeArbres) {
            pinceau.setShader(agrandir);
            regler(echelleSprite);
            courante = null;
            modeArbres = false;
        }
        Flammes flammes = incendies.front.flammes;
        Texture palette = flammes.palette();
        if (palette == null) return;
        if (palette != courante || feuB != 0f || feuE != 0f || feuL != 0f) {
            pinceau.flush();
            agrandir.setUniformf("u_echelle", flammes.cote());
            agrandir.setUniformf("u_taille", palette.getWidth(), palette.getHeight());
            agrandir.setUniformf("u_case", palette.getWidth(), palette.getHeight());
            agrandir.setUniformf("u_feu", 0f, 0f, 0f);
            feuB = feuE = feuL = 0f;
            courante = palette;
        }
        pinceau.setColor(0f, 0f, 0f, 1f);
        flammes.tracer(pinceau, j);
    }

    private void tracer(int i, boolean seconde) {
        int sorte = sortes[i];
        float e = echelleSprite;
        if (sorte >= FEU) {
            if (!seconde) tracerFeu(i, sorte - FEU);
            return;
        }
        if (sorte >= 0 && sorte < MAISON) {
            if (!modeArbres) {
                pinceau.setShader(arbres);
                palette.bind(1);
                Gdx.gl.glActiveTexture(com.badlogic.gdx.graphics.GL20.GL_TEXTURE0);
                arbres.setUniformi("u_palette", 1);
                arbres.setUniformf("u_taille", QUAD_L / mpp, QUAD_H / mpp);
                arbres.setUniformf("u_quad", QUAD_L, QUAD_H);
                arbres.setUniformf("u_ancre", ANCRE_ARBRE);
                arbres.setUniformf("u_metresPixel", mpp);
                arbres.setUniformf("u_temps", temps);
                arbres.setUniformf("u_vent", vent);
                arbres.setUniformf("u_neige", neige);
                arbres.setUniformf("u_masque", masque);
                modeArbres = true;
            }
            pinceau.setColor(((int) (phases[i] * 65536f) & 255) / 255f, sorte / 255f, phases[i], 1f);
            retenir(i, Math.round(xs[i] - QUAD_L / 2f / metresEcran), Math.round(ys[i] - ANCRE_ARBRE / metresEcran), (float) (QUAD_L / metresEcran), (float) (QUAD_H / metresEcran));
            pinceau.draw(blanc, rx[i], ry[i], rw[i], rh[i]);
            return;
        }
        if (modeArbres) {
            pinceau.setShader(agrandir);
            regler(e);
            courante = null;
            modeArbres = false;
        }
        Incendies.Foyer feu = sorte >= MAISON && incendies != null ? incendies.foyer(villages, sorte - MAISON) : null;
        float fb = feu == null ? 0f : feu.brule, fe = feu == null ? 0f : feu.effondre, fl = feu == null ? 0f : feu.lueur;
        if (fb != feuB || fe != feuE || fl != feuL) {
            pinceau.flush();
            agrandir.setUniformf("u_feu", fb, fe, fl);
            feuB = fb;
            feuE = fe;
            feuL = fl;
        }
        Personnage perso = sorte < 0 ? personnages.get(-1 - sorte) : null;
        Batiments.Image maison = null;
        if (feu != null && feu.abattu()) {
            maison = batiments.image(feu.planRuine());
            if (maison != null && (feuB != 0f || feuE != 0f || feuL != fl)) {
                pinceau.flush();
                agrandir.setUniformf("u_feu", 0f, 0f, fl);
                feuB = 0f;
                feuE = 0f;
                feuL = fl;
            }
        }
        if (sorte >= MAISON && maison == null) maison = batiments.image(villages.plans[sorte - MAISON]);
        if (sorte >= MAISON && maison == null) {
            maisonsManquantes++;
            return;
        }
        Animations.Animation anim = perso != null && perso.peint ? perso.animation : null;
        Texture texture = anim != null ? peintre.texture(perso.apparence, perso.tenue(), perso.traits(), anim, perso.direction) : null;
        if (texture == null) anim = null;
        if (texture == null) texture = sorte >= MAISON ? maison.texture : perso != null ? (perso.peint ? peintre.texture(perso.apparence, perso.tenue(), perso.traits()) : assembleur.texture(perso.apparence)) : planche;
        if (texture == null) {
            if (perso != null) personnagesManquants++;
            return;
        }
        boolean peint = (perso != null && perso.peint) || sorte >= MAISON;
        float ep = peint ? (float) (Peintre.METRES_PIXEL / metresEcran) : e;
        if (texture != courante) {
            pinceau.flush();
            agrandir.setUniformf("u_echelle", ep);
            agrandir.setUniformf("u_taille", texture.getWidth(), texture.getHeight());
            agrandir.setUniformf("u_case", sorte >= MAISON ? texture.getWidth() : anim != null ? Peintre.AL : peint ? Peintre.L : CASE_L, sorte >= MAISON ? texture.getHeight() : anim != null ? Peintre.AH : peint ? Peintre.H : CASE_H);
            courante = texture;
        }
        if (sorte >= MAISON) {
            int l = texture.getWidth(), h = texture.getHeight();
            pinceau.setColor(0f, 1f / 255f, 0f, 1f);
            retenir(i, Math.round(xs[i] - maison.ancreX * ep), Math.round(ys[i] - maison.ancreY * ep), l * ep, h * ep);
            pinceau.draw(texture, rx[i], ry[i], l * ep, h * ep, 0, 0, l, h, false, false);
            return;
        }
        if (sorte < 0) {
            Personnage p = perso;
            int l = anim != null ? Peintre.AL : p.peint ? Peintre.L : Assembleur.L, h = anim != null ? Peintre.AH : p.peint ? Peintre.H : Assembleur.H;
            int ancre = p.peint ? Peintre.ANCRE : ANCRE_PERSONNAGE;
            int sx = anim != null ? p.imageAnimation() * l : p.peint ? (p.image() % Assembleur.PAS) * l : p.image() * l;
            int sy = anim != null ? 0 : p.peint ? p.direction * h : 0;
            int herbe = p.peint ? 0 : h;
            if (p.peint) pinceau.setColor((-sorte * 0.618f) % 1f, 0f, p.dansHerbe && p.profondeur <= 0f ? 1f : 0.6f, 1f);
            else pinceau.setColor(0f, 0f, 0f, 1f);
            float x = Math.round(xs[i] - l / 2f * ep);
            float y = Math.round(ys[i] - ancre * ep + (float) (p.hauteurPortee / Peintre.METRES_PIXEL) * ep);
            retenir(i, x, y, l * ep, h * ep);
            if (p.profondeur <= 0f) {
                pinceau.draw(texture, x, y, l * ep, h * ep, sx, sy + (p.dansHerbe ? herbe : 0), l, h, false, false);
                String nomBulle = p.bulleReste > 0f ? p.bulle : anim != null && (p.partenaire == null || p.meneur) ? anim.bulle : null;
                if (nomBulle != null && !seconde) bulle(nomBulle, xs[i], y + ((anim != null ? peintre.sommet(texture, p.imageAnimation()) : p.apparence.enfant ? 62 : 84) + 3) * ep, ep);
                return;
            }
            int cache = ancre + Math.min(h / 2, Math.round((float) (p.profondeur / (p.peint ? Peintre.METRES_PIXEL : METRES_SPRITE))));
            pinceau.draw(texture, x, y + cache * ep, l * ep, (h - cache) * ep, sx, sy, l, h - cache, false, false);
            if (seconde) return;
            pinceau.flush();
            agrandir.setUniformf("u_taille", 1f, 1f);
            int remous = 6 + (int) ((temps * 3f + i) % 2f);
            pinceau.setColor(0f, 0f, 0f, 0.55f);
            pinceau.draw(blanc, Math.round(xs[i] - remous * e), y + cache * e, remous * 2 * e, e);
            pinceau.setColor(0f, 0f, 0f, 0.3f);
            pinceau.draw(blanc, Math.round(xs[i] - (remous + 3) * e), y + (cache - 1) * e, (remous + 3) * 2 * e, e);
            pinceau.flush();
            courante = null;
            return;
        }
        int cx = (sorte % VARIANTES) * CASE_L, cy = (sorte / VARIANTES) * CASE_H;
        float x = Math.round(xs[i] - CASE_L / 2f * e);
        float y = Math.round(ys[i] - ANCRE * e);
        retenir(i, x, y, CASE_L * e, CASE_H * e);
        pinceau.setColor(phases[i], SOUPLESSE[sorte / VARIANTES], 0f, 1f);
        pinceau.draw(planche, x, y, CASE_L * e, CASE_H * e, cx, cy, CASE_L, CASE_H, false, false);
    }

    private void bulle(String nom, float cx, float y, float ep) {
        int k = bulles.indice(nom);
        if (k < 0) return;
        float e = Math.max(ep * 1.5f, echelleSprite);
        int c = Bulles.CASE;
        pinceau.flush();
        agrandir.setUniformf("u_echelle", e);
        agrandir.setUniformf("u_taille", bulles.texture.getWidth(), bulles.texture.getHeight());
        agrandir.setUniformf("u_case", bulles.texture.getWidth(), bulles.texture.getHeight());
        pinceau.setColor(0f, 0f, 0f, 1f);
        float saut = Math.round((float) Math.sin(temps * 4f) * 1.5f) * e;
        pinceau.draw(bulles.texture, Math.round(cx - c / 2f * e), Math.round(y + saut), c * e, c * e, k * c, 0, c, c, false, false);
        pinceau.flush();
        courante = null;
    }

    private void retenir(int i, float x, float y, float l, float h) {
        rx[i] = x;
        ry[i] = y;
        rw[i] = l;
        rh[i] = h;
    }

    private void silhouette(int rang, int n) {
        int j = (int) ordre[rang];
        if (rw[j] <= 0f) return;
        int devant = 0;
        for (int k = rang + 1; k < n; k++) {
            int i = (int) ordre[k];
            if (rw[i] <= 0f || rx[i] >= rx[j] + rw[j] || rx[i] + rw[i] <= rx[j] || ry[i] >= ry[j] + rh[j] || ry[i] + rh[i] <= ry[j]) continue;
            if (devant++ == 0) {
                pinceau.flush();
                Gdx.gl.glClearStencil(0);
                Gdx.gl.glClear(GL20.GL_STENCIL_BUFFER_BIT);
                Gdx.gl.glEnable(GL20.GL_STENCIL_TEST);
                Gdx.gl.glStencilFunc(GL20.GL_ALWAYS, 1, 0xFF);
                Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_REPLACE);
                Gdx.gl.glColorMask(false, false, false, false);
                masque = 1f;
                reprendre();
            }
            tracer(i, true);
        }
        if (devant == 0) return;
        pinceau.flush();
        Gdx.gl.glColorMask(true, true, true, true);
        Gdx.gl.glStencilFunc(GL20.GL_EQUAL, 1, 0xFF);
        Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_KEEP);
        masque = 0f;
        ombreJoueur = true;
        reprendre();
        tracer(j, true);
        pinceau.flush();
        Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);
        ombreJoueur = false;
        reprendre();
    }

    private void reprendre() {
        pinceau.setShader(agrandir);
        regler(echelleSprite);
        courante = null;
        modeArbres = false;
    }

    private void regler(float e) {
        agrandir.setUniformf("u_echelle", e);
        agrandir.setUniformf("u_case", CASE_L, CASE_H);
        agrandir.setUniformf("u_temps", temps);
        agrandir.setUniformf("u_vent", vent);
        float force = (float) Math.hypot(ventEst, ventNord);
        float cr = (float) Math.cos(rotationVue), sr = (float) Math.sin(rotationVue);
        float ve = force > 0.01f ? ventEst / force : 0f, vn = force > 0.01f ? ventNord / force : 0f;
        agrandir.setUniformf("u_ventDir", cr * ve - sr * vn, sr * ve + cr * vn);
        agrandir.setUniformf("u_neige", neige);
        agrandir.setUniformf("u_masque", masque);
        agrandir.setUniformf("u_feu", 0f, 0f, 0f);
        feuB = feuE = feuL = 0f;
        if (ombreJoueur) agrandir.setUniformf("u_silhouette", 0.62f, 0.78f, 1f, 0.55f);
        else agrandir.setUniformf("u_silhouette", 0f, 0f, 0f, 0f);
    }

    private void agrandirTableaux() {
        int t = xs.length * 2;
        xs = Arrays.copyOf(xs, t);
        ys = Arrays.copyOf(ys, t);
        phases = Arrays.copyOf(phases, t);
        sortes = Arrays.copyOf(sortes, t);
        ordre = Arrays.copyOf(ordre, t);
        rx = Arrays.copyOf(rx, t);
        ry = Arrays.copyOf(ry, t);
        rw = Arrays.copyOf(rw, t);
        rh = Arrays.copyOf(rh, t);
    }

    private static final double[] TRONCS = { 0.32, 0.4, 0.28, 0.25, 0.0, 0.55 };
    private final double[] voisin = new double[3], origine = new double[3];
    private final long[] clesVues = new long[5];

    public boolean bloque(double px, double py, double pz, double rayon) {
        int vues = 0;
        origine[0] = px;
        origine[1] = py;
        origine[2] = pz;
        for (int k = 0; k < 5; k++) {
            double e = k == 1 ? 1.6 : k == 2 ? -1.6 : 0.0, n = k == 3 ? 1.6 : k == 4 ? -1.6 : 0.0;
            Personnage.decaler(origine, e, n, voisin);
            long cle = cleParcelle(voisin[0], voisin[1], voisin[2]);
            boolean deja = false;
            for (int q = 0; q < vues; q++) deja |= clesVues[q] == cle;
            if (deja) continue;
            clesVues[vues++] = cle;
            float[] arbres = parcelles.get(cle);
            if (arbres == null) continue;
            for (int i = 0; i < arbres.length; i += 5) {
                int code = (int) arbres[i + 3];
                int espece = code / VARIANTES;
                double tronc = espece == ROCHER ? rayonRocher(arbres[i], arbres[i + 1], arbres[i + 2], code) * 0.95 : espece == BUISSON ? rayonBuisson(arbres[i], arbres[i + 1], arbres[i + 2], code) * 0.7 : TRONCS[espece];
                if (tronc <= 0.0 || (villages != null && (villages.enClairiere(arbres[i], arbres[i + 1], arbres[i + 2]) || villages.dansChamp(arbres[i], arbres[i + 1], arbres[i + 2])))) continue;
                double dx = (arbres[i] - px) * RAYON_METRES, dy = (arbres[i + 1] - py) * RAYON_METRES, dz = (arbres[i + 2] - pz) * RAYON_METRES;
                double r = tronc + rayon;
                if (dx * dx + dy * dy + dz * dz < r * r) return true;
            }
        }
        if (villages != null) {
            float[] plantes = villages.arbres;
            for (int i = 0; i + 4 < villages.nombreArbres; i += 5) {
                int espece = (int) plantes[i + 3] / VARIANTES;
                double tronc = espece == BUISSON ? 0.3 : TRONCS[espece];
                double dx = (plantes[i] - px) * RAYON_METRES, dy = (plantes[i + 1] - py) * RAYON_METRES, dz = (plantes[i + 2] - pz) * RAYON_METRES;
                double r = tronc + rayon;
                if (dx * dx + dy * dy + dz * dz < r * r) return true;
            }
        }

        return false;
    }

    private static float rayonRocher(double x, double y, double z, int code) {
        float graine = graineShader(x, y, z, code);
        return (0.4f + hasardShader(graine, 23f) * 0.6f) * (0.8f + 0.45f * hasardShader(graine, 91f));
    }

    private static float rayonBuisson(double x, double y, double z, int code) {
        float graine = graineShader(x, y, z, code);
        return (0.7f + hasardShader(graine, 23f) * 0.5f) * (0.8f + 0.45f * hasardShader(graine, 91f));
    }

    private static float graineShader(double x, double y, double z, int code) {
        float phase = (float) hasard(Double.doubleToLongBits(x * 7919.0 + y * 104729.0 + z), 6);
        float rouge = ((int) (phase * 65536f) & 255) / 255f;
        return (float) Math.floor(rouge * 255f + 0.5f) + 1f + (code % VARIANTES) * 256f;
    }

    private static float hasardShader(float x, float y) {
        return permute(permute(mod289(x)) + mod289(y)) / 288f;
    }

    private static float mod289(float x) {
        return x - (float) Math.floor(x / 289f) * 289f;
    }

    private static float permute(float x) {
        return mod289((x * 34f + 1f) * x);
    }

    private static long cleParcelle(double x, double y, double z) {
        int cote = 1 << NIVEAU_PARCELLE;
        int face = Cube.face(x, y, z);
        int px = Math.min(cote - 1, (int) ((Cube.coordonneeA(face, x, y, z) + 1.0) * 0.5 * cote));
        int py = Math.min(cote - 1, (int) ((Cube.coordonneeB(face, x, y, z) + 1.0) * 0.5 * cote));
        return ((long) face << 40) | ((long) px << 20) | py;
    }

    private void preparerVue(GameCamera camera) {
        double cosLon = Math.cos(camera.longitude), sinLon = Math.sin(camera.longitude);
        double cosLat = Math.cos(camera.latitude), sinLat = Math.sin(camera.latitude);
        est[0] = -sinLon; est[1] = cosLon; est[2] = 0;
        nord[0] = -sinLat * cosLon; nord[1] = -sinLat * sinLon; nord[2] = cosLat;
        centre[0] = cosLat * cosLon; centre[1] = cosLat * sinLon; centre[2] = sinLat;
    }

    private void chercherParcelles(double rayon, int largeur, int hauteur) {
        visibles.clear();
        int cote = 1 << NIVEAU_PARCELLE;
        for (int sy = -PAS_ECRAN * 16; sy <= hauteur + PAS_ECRAN; sy += PAS_ECRAN) {
            for (int sx = -PAS_ECRAN; sx <= largeur + PAS_ECRAN; sx += PAS_ECRAN) {
                double qx = (sx - largeur / 2.0) / rayon, qy = (sy - hauteur / 2.0) / rayon / ecrasement;
                double q2 = qx * qx + qy * qy;
                if (q2 >= 1.0) continue;
                double qz = Math.sqrt(1.0 - q2);
                double x = est[0] * qx + nord[0] * qy + centre[0] * qz;
                double y = est[1] * qx + nord[1] * qy + centre[1] * qz;
                double z = est[2] * qx + nord[2] * qy + centre[2] * qz;
                int face = Cube.face(x, y, z);
                int px = Math.min(cote - 1, (int) ((Cube.coordonneeA(face, x, y, z) + 1.0) * 0.5 * cote));
                int py = Math.min(cote - 1, (int) ((Cube.coordonneeB(face, x, y, z) + 1.0) * 0.5 * cote));
                long cle = ((long) face << 40) | ((long) px << 20) | py;
                if (visibles.add(cle)) {
                    usages.put(cle, image);
                    if (!parcelles.containsKey(cle)) {
                        parcellesManquantes++;
                        if (demandees.add(cle)) file.add(cle);
                    }
                }
            }
        }
    }

    private void oublier() {
        if (usages.size() <= PARCELLES_MAX) return;
        Iterator<Map.Entry<Long, Long>> it = usages.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Long> e = it.next();
            if (image - e.getValue() > 120) {
                parcelles.remove(e.getKey());
                it.remove();
            }
        }
    }

    public void dispose() {
        ouvrier.interrupt();
        planche.dispose();
        blanc.dispose();
        agrandir.dispose();
        arbres.dispose();
        peintre.dispose();
        bulles.dispose();
        palette.dispose();
        pinceau.dispose();
    }
}
