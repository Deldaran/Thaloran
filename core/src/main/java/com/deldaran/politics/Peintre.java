package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.deldaran.politics.histoire.Peuple;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Peintre {
    public static final int L = 64, H = 96, PAS = 9, DIRECTIONS = 8, ANCRE = 4;
    public static final int AL = 96, AH = 112, DORT = 6, CRI = 7;
    private static final int ANIMATIONS_MAX = 160;
    public static final double METRES_PIXEL = 0.0234;
    private static final int CACHE_MAX = 512;
    private static final int ENVOIS_PAR_IMAGE = 2;
    private static final int ALPHA_CAPE = 254;
    private static final double INCLINAISON = RenduTuiles.ANGLE_CAMERA;
    private static final double[] LUMIERE = normer(-0.62, 0.68, 0.38);
    private static final double[] CAP = { 0, 180, 90, -90, 45, -45, 135, -135 };
    private static final int[] BAYER = { 0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5 };

    private static final int ELLIPSOIDE = 0, CAPSULE = 1;
    private static final int LIBRE = 0, CHEVEUX = 1, CAPUCHE = 2, CAPE = 3, JUPE = 4, VISAGE = 5, BARBE = 6, CASQUE = 7;
    private static final int PAS_PRIM = 16;
    private static final double COIN = 0.8;

    private final int[][] palette = new int[Batiments.RANGEES][8];
    private final Map<Long, Texture> cache = new LinkedHashMap<>(64, 0.75f, true);
    private final Map<Long, Texture> animations = new LinkedHashMap<>(16, 0.75f, true);
    private final Map<Long, Future<int[]>> enCours = new HashMap<>();
    private ExecutorService ouvrier;
    private final Map<Texture, int[]> sommets = new java.util.HashMap<>();
    private ThreadLocal<Peintre> ateliers;
    private long imageCourante = -1;
    private int envois;

    public Peintre() {
        Pixmap p = new Pixmap(Gdx.files.internal("palettes/sol.png"));
        for (int r = 0; r < Batiments.RANGEES; r++) for (int c = 0; c < 8; c++) palette[r][c] = p.getPixel(c, r) >>> 8;
        p.dispose();
        int fils = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 3));
        ateliers = ThreadLocal.withInitial(() -> new Peintre(palette));
        ouvrier = Executors.newFixedThreadPool(fils, r -> {
            Thread t = new Thread(r, "peintre");
            t.setDaemon(true);
            return t;
        });
    }

    public Peintre(int[][] palette) {
        for (int r = 0; r < Batiments.RANGEES; r++) System.arraycopy(palette[r], 0, this.palette[r], 0, 8);
    }

    public Texture texture(Apparence a, Tenue tenue, Traits traits) {
        return texture(a, tenue, traits, null, 0);
    }

    public Texture texture(Apparence a, Tenue tenue, Traits traits, Animations.Animation animation, int direction) {
        // Le sexe et l'âge comptent : une même graine peut être peinte enfant puis adulte (la vignette de la création).
        Long cle = (a.cle() * 1000003L ^ tenue.cle() ^ traits.cle()) * 4L + (a.femme ? 1 : 0) + (a.enfant ? 2 : 0);
        if (animation != null) cle = (cle * 31L + (animation.id + 1) * 0x9E3779B97F4A7C15L) * 11L + direction;
        Map<Long, Texture> reserve = animation == null ? cache : animations;
        Texture t = reserve.get(cle);
        if (t != null) return t;
        Future<int[]> travail = enCours.get(cle);
        if (travail == null) {
            long graine = a.graine;
            int espece = a.espece;
            boolean femme = a.femme, enfant = a.enfant;
            Tenue copie = new Tenue(tenue);
            Traits traitsCopie = new Traits(traits);
            if (animation == null) enCours.put(cle, ouvrier.submit(() -> ateliers.get().peindre(graine, espece, femme, enfant, copie, traitsCopie)));
            else enCours.put(cle, ouvrier.submit(() -> ateliers.get().peindreAnimation(graine, espece, femme, enfant, copie, traitsCopie, animation, direction)));
            return null;
        }
        if (!travail.isDone()) return null;
        long image = Gdx.graphics.getFrameId();
        if (image != imageCourante) {
            imageCourante = image;
            envois = 0;
        }
        if (envois >= ENVOIS_PAR_IMAGE) return null;
        envois++;
        enCours.remove(cle);
        int[] pixels;
        try {
            pixels = travail.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        int largeur = animation == null ? L * PAS : AL * animation.images, hauteur = animation == null ? H * DIRECTIONS * 2 : AH;
        Pixmap p = new Pixmap(largeur, hauteur, Pixmap.Format.RGBA8888);
        ByteBuffer octets = p.getPixels();
        for (int k = 0; k < pixels.length; k++) {
            int v = pixels[k];
            octets.put(k * 4, (byte) (v >>> 24));
            octets.put(k * 4 + 1, (byte) (v >>> 16));
            octets.put(k * 4 + 2, (byte) (v >>> 8));
            octets.put(k * 4 + 3, (byte) v);
        }
        t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        p.dispose();
        reserve.put(cle, t);
        if (animation != null) {
            int[] haut = new int[animation.images];
            for (int k = 0; k < pixels.length; k++) {
                if ((pixels[k] & 255) < 128) continue;
                int c = (k % largeur) / AL;
                haut[c] = Math.max(haut[c], AH - k / largeur);
            }
            sommets.put(t, haut);
        }
        if (reserve.size() > (animation == null ? CACHE_MAX : ANIMATIONS_MAX)) {
            Iterator<Map.Entry<Long, Texture>> it = reserve.entrySet().iterator();
            Texture vieille = it.next().getValue();
            sommets.remove(vieille);
            vieille.dispose();
            it.remove();
        }
        return t;
    }

    public int sommet(Texture t, int image) {
        int[] haut = sommets.get(t);
        return haut == null ? AH * 3 / 4 : haut[image];
    }

    public int enCours() {
        return enCours.size();
    }

    public void dispose() {
        if (ouvrier != null) ouvrier.shutdownNow();
        for (Texture t : cache.values()) t.dispose();
        cache.clear();
        for (Texture t : animations.values()) t.dispose();
        animations.clear();
        sommets.clear();
    }

    private static class Matiere {
        final int rangee;
        final double base, etendue, eclat;
        final int motif;

        Matiere(int rangee, double base, double etendue, double eclat) {
            this(rangee, base, etendue, eclat, 0);
        }

        Matiere(int rangee, double base, double etendue, double eclat, int motif) {
            this.rangee = rangee;
            this.base = base;
            this.etendue = etendue;
            this.eclat = eclat;
            this.motif = motif;
        }
    }

    private static final int MAILLES = 1, LUEUR = 2;

    private static class Corps {
        double taille = 1.0, largeur = 1.0, epaules = 1.0, hanches = 1.0, tete = 1.0, membres = 1.0;
        boolean femme;
        int espece;
        Matiere peau, cheveux, ivoire;
    }

    private static class Habits {
        Matiere haut, bas, linge, cuir, metal, cape, bois, maille, bouclier, lueur, fleche;
        boolean chemise, tunique, robe, pantalon, bottes, ceinture, capuche, manteau, plastron, sac, braies, bande, casque, cotte, carquois;
        boolean epee, outil, arc, bouclier2, lanterne, hache, lance, dague, baton;
        int droite = -1, gauche = -1;
    }

    private final double[] prim = new double[160 * PAS_PRIM];
    private final int[] type = new int[160], decoupe = new int[160];
    private final Matiere[] matieres = new Matiere[160];
    private int nombre;
    private boolean ombreVisage, chaume;
    private int expression;
    private double phaseCape;

    public int[] peindre(long graine, int espece, boolean femme, boolean enfant) {
        return peindre(graine, espece, femme, enfant, Tenue.tirer(graine, femme, enfant));
    }

    public int[] peindre(long graine, int espece, boolean femme, boolean enfant, Tenue tenue) {
        return peindre(graine, espece, femme, enfant, tenue, Traits.tirer(graine, espece, femme, enfant));
    }

    public int[] peindre(long graine, int espece, boolean femme, boolean enfant, Tenue tenue, Traits traits) {
        Random r = new Random(graine ^ 0x5DEECE66DL);
        Corps c = tirer(r, espece, femme, enfant);
        Habits h = habiller(tenue);
        int largeur = L * PAS;
        int[] sortie = new int[largeur * H * DIRECTIONS * 2];
        for (int d = 0; d < DIRECTIONS; d++) {
            for (int f = 0; f < PAS; f++) {
                double phase = f == 0 ? -1.0 : (f - 1) / 8.0 * Math.PI * 2.0;
                construire(c, h, traits, Pose.marche(phase), Math.toRadians(CAP[d]));
                int[] image = rendre(graine, true, L, H);
                int[] dessous = h.manteau ? rendre(graine, false, L, H) : image;
                for (int y = 0; y < H; y++) {
                    System.arraycopy(image, y * L, sortie, (d * H + y) * largeur + f * L, L);
                    System.arraycopy(dessous, y * L, sortie, ((DIRECTIONS + d) * H + y) * largeur + f * L, L);
                }
            }
        }
        return sortie;
    }

    public int[] peindreAnimation(long graine, int espece, boolean femme, boolean enfant, Tenue tenue, Traits traits, Animations.Animation animation, int direction) {
        Random r = new Random(graine ^ 0x5DEECE66DL);
        Corps c = tirer(r, espece, femme, enfant);
        Habits h = habiller(tenue);
        int n = animation.images, largeur = AL * n;
        int[] sortie = new int[largeur * AH];
        Pose pose = new Pose();
        for (int f = 0; f < n; f++) {
            animation.pose(f, pose);
            construire(c, h, traits, pose, Math.toRadians(CAP[direction]));
            int[] image = rendre(graine, true, AL, AH);
            for (int k = 0; k < image.length; k++) if ((image[k] & 255) == ALPHA_CAPE) image[k] |= 255;
            for (int y = 0; y < AH; y++) System.arraycopy(image, y * AL, sortie, y * largeur + f * AL, AL);
        }
        return sortie;
    }

    public int[] peindreImage(long graine, int espece, boolean femme, boolean enfant, Tenue tenue, Traits traits, Pose pose, int direction) {
        Random r = new Random(graine ^ 0x5DEECE66DL);
        Corps c = tirer(r, espece, femme, enfant);
        construire(c, habiller(tenue), traits, pose, Math.toRadians(CAP[direction]));
        return rendre(graine, true, AL, AH);
    }

    private Corps tirer(Random r, int espece, boolean femme, boolean enfant) {
        Corps c = new Corps();
        c.femme = femme && !enfant;
        c.espece = espece;
        int[] peaux = { 12, 12, 2, 3 };
        int rangeePeau = peaux[r.nextInt(peaux.length)];
        double basePeau = rangeePeau == 12 ? 2.6 : 3.0;
        if (espece == Peuple.NAIN) {
            c.taille = 0.74;
            c.largeur = 1.25;
            c.epaules = 1.2;
            c.tete = 1.15;
            c.membres = 0.82;
        } else if (espece == Peuple.ELFE) {
            c.taille = 1.06;
            c.largeur = 0.88;
            c.epaules = 0.92;
            rangeePeau = r.nextBoolean() ? 3 : 14;
            basePeau = rangeePeau == 14 ? 2.5 : 3.6;
        } else if (espece == Peuple.ORK) {
            c.taille = 1.1;
            c.largeur = 1.25;
            c.epaules = 1.3;
            c.tete = 1.05;
            rangeePeau = r.nextBoolean() ? 8 : 10;
            basePeau = 3.6;
        }
        if (femme) {
            c.epaules *= 0.88;
            c.hanches = 1.1;
            c.taille *= 0.95;
        }
        if (enfant) {
            c.taille *= 0.62;
            c.tete = 1.25;
        }
        c.peau = new Matiere(rangeePeau, basePeau, 3.0, 0.0);
        int[] cheveux = { 9, 2, 4, 5, 1, 8 };
        c.cheveux = new Matiere(cheveux[r.nextInt(cheveux.length)], 1.6, 3.0, 0.6);
        c.ivoire = new Matiere(3, 4.2, 2.4, 0.8);
        return c;
    }

    private static Habits habiller(Tenue t) {
        Habits h = new Habits();
        int[] k = t.couleurs;
        int haut = k[Tenue.ROBE] >= 0 ? k[Tenue.ROBE] : k[Tenue.TUNIQUE] >= 0 ? k[Tenue.TUNIQUE] : k[Tenue.CHEMISE];
        if (haut >= 0) h.haut = new Matiere(haut, haut == 15 ? 1.1 : 1.6, 3.2, 0.0);
        if (k[Tenue.PANTALON] >= 0) h.bas = new Matiere(k[Tenue.PANTALON], 1.2, 2.8, 0.0);
        int linge = k[Tenue.BRAIES] >= 0 ? k[Tenue.BRAIES] : k[Tenue.BANDE];
        if (linge >= 0) h.linge = new Matiere(linge, 2.8, 2.6, 0.0);
        int cuir = k[Tenue.BOTTES] >= 0 ? k[Tenue.BOTTES] : k[Tenue.CEINTURE] >= 0 ? k[Tenue.CEINTURE] : k[Tenue.SAC] >= 0 ? k[Tenue.SAC] : k[Tenue.CARQUOIS] >= 0 ? k[Tenue.CARQUOIS] : 9;
        h.cuir = new Matiere(cuir, 1.5, 3.2, 0.4);
        h.metal = new Matiere(k[Tenue.PLASTRON] >= 0 ? k[Tenue.PLASTRON] : k[Tenue.CASQUE] >= 0 ? k[Tenue.CASQUE] : 5, 1.4, 4.2, 1.6);
        h.maille = new Matiere(k[Tenue.MAILLE] >= 0 ? k[Tenue.MAILLE] : 5, 1.0, 3.4, 0.8, MAILLES);
        int cape = k[Tenue.CAPE] >= 0 ? k[Tenue.CAPE] : k[Tenue.CAPUCHE];
        if (cape >= 0) h.cape = new Matiere(cape, cape == 15 ? 0.6 : 1.0, 2.6, 0.0);
        h.bois = new Matiere(9, 2.2, 3.0, 0.2);
        h.bouclier = new Matiere(k[Tenue.BOUCLIER] >= 0 ? k[Tenue.BOUCLIER] : 9, 1.4, 3.0, 0.2);
        h.lueur = new Matiere(3, 6.6, 0.0, 0.0, LUEUR);
        h.fleche = new Matiere(3, 3.0, 2.6, 0.0);
        h.chemise = t.porte(Tenue.CHEMISE);
        h.tunique = t.porte(Tenue.TUNIQUE);
        h.robe = t.porte(Tenue.ROBE);
        h.pantalon = t.porte(Tenue.PANTALON);
        h.bottes = t.porte(Tenue.BOTTES);
        h.ceinture = t.porte(Tenue.CEINTURE);
        h.capuche = t.porte(Tenue.CAPUCHE);
        h.manteau = t.porte(Tenue.CAPE);
        h.plastron = t.porte(Tenue.PLASTRON);
        h.sac = t.porte(Tenue.SAC);
        h.braies = t.porte(Tenue.BRAIES);
        h.bande = t.porte(Tenue.BANDE);
        h.casque = t.porte(Tenue.CASQUE);
        h.cotte = t.porte(Tenue.MAILLE);
        h.carquois = t.porte(Tenue.CARQUOIS);
        h.epee = t.porte(Tenue.EPEE);
        h.outil = t.porte(Tenue.OUTIL);
        h.arc = t.porte(Tenue.ARC);
        h.bouclier2 = t.porte(Tenue.BOUCLIER);
        h.lanterne = t.porte(Tenue.LANTERNE);
        h.hache = t.porte(Tenue.HACHE);
        h.lance = t.porte(Tenue.LANCE);
        h.dague = t.porte(Tenue.DAGUE);
        h.baton = t.porte(Tenue.BATON);
        h.droite = t.tenue(Tenue.MAIN_DROITE);
        h.gauche = t.tenue(Tenue.MAIN_GAUCHE);
        return h;
    }

    private final double[] droite = new double[3], avant = new double[3];

    private static double[] milieu(double[] a, double[] b, double t) {
        return new double[] { a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t };
    }

    private static double[] plus(double[] a, double x, double y, double z) {
        return new double[] { a[0] + x, a[1] + y, a[2] + z };
    }

    private static double[] le(double[] a, double[] d, double t) {
        return new double[] { a[0] + d[0] * t, a[1] + d[1] * t, a[2] + d[2] * t };
    }

    private static final int RACINE = 0, BUSTE = 1, TETE = 2, MAIN_D = 3, MAIN_G = 4;
    private static final int[] PARENT = { -1, RACINE, BUSTE, BUSTE, BUSTE };
    private final double[][] rot = new double[5][9], pivot = new double[5][3];
    private final double[] translation = new double[3];
    private int niveau;

    private void repere(int n, double y, double lacet, double tangage, double roulis) {
        double ct = Math.cos(tangage), st = Math.sin(tangage), cr = Math.cos(roulis), sr = Math.sin(roulis), cy = Math.cos(lacet), sy = Math.sin(lacet);
        double[] x = { 1, 0, 0, 0, ct, -st, 0, st, ct };
        double[] z = { cr, sr, 0, -sr, cr, 0, 0, 0, 1 };
        double[] l = { cy, 0, sy, 0, 1, 0, -sy, 0, cy };
        double[] xz = new double[9];
        produit(x, z, xz);
        produit(l, xz, rot[n]);
        pivot[n][0] = 0.0;
        pivot[n][1] = y;
        pivot[n][2] = 0.0;
    }

    private static void produit(double[] a, double[] b, double[] sortie) {
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) sortie[i * 3 + j] = a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j];
    }

    private double[] transformer(double x, double y, double z, boolean point) {
        for (int n = niveau; n >= 0; n = PARENT[n]) {
            double[] m = rot[n], c = pivot[n];
            double dx = point ? x - c[0] : x, dy = point ? y - c[1] : y, dz = point ? z - c[2] : z;
            x = m[0] * dx + m[1] * dy + m[2] * dz;
            y = m[3] * dx + m[4] * dy + m[5] * dz;
            z = m[6] * dx + m[7] * dy + m[8] * dz;
            if (point) {
                x += c[0];
                y += c[1];
                z += c[2];
            }
        }
        if (point) {
            x += translation[0];
            z += translation[2];
        }
        return new double[] { x, y, z };
    }

    private double[] sens(double x, double y, double z) {
        double[] q = transformer(x, y, z, false);
        return new double[] { q[0] * droite[0] + q[2] * avant[0], q[1], q[0] * droite[2] + q[2] * avant[2] };
    }

    private static double[] repereMain(double[] f) {
        double[] x = { 1.0, 0.0, 0.0 };
        if (Math.abs(f[0]) > 0.92) x = new double[] { 0.0, 0.0, f[0] > 0 ? -1.0 : 1.0 };
        double p = x[0] * f[0] + x[1] * f[1] + x[2] * f[2];
        double[] u = normer(x[0] - p * f[0], x[1] - p * f[1], x[2] - p * f[2]);
        double[] h = { f[1] * u[2] - f[2] * u[1], f[2] * u[0] - f[0] * u[2], f[0] * u[1] - f[1] * u[0] };
        return new double[] { u[0], f[0], h[0], u[1], f[1], h[1], u[2], f[2], h[2] };
    }

    private void reglerMain(int n, double[] poing, double[] coude, double[] poignet, int objet) {
        double[] f = normer(poignet[0] - coude[0], poignet[1] - coude[1], poignet[2] - coude[2]);
        double angle = objet == Tenue.BOUCLIER ? 1.4 : objet == Tenue.ARC ? 0.85 : objet == Tenue.LANTERNE ? 0.55 : objet == Tenue.LANCE || objet == Tenue.BATON ? 1.0 : 0.75;
        double[] b = repereMain(f), b0 = repereMain(new double[] { 0.0, -Math.cos(angle), Math.sin(angle) });
        double[] t = { b0[0], b0[3], b0[6], b0[1], b0[4], b0[7], b0[2], b0[5], b0[8] };
        produit(b, t, rot[n]);
        System.arraycopy(poing, 0, pivot[n], 0, 3);
    }

    private static double[] pied(double[] t, double f, double y, double z) {
        double c = Math.cos(f), s = Math.sin(f);
        return new double[] { t[0], t[1] + y * c - z * s, t[2] + y * s + z * c };
    }

    private static void tournerY(double[] p, double[] centre, double a) {
        double dx = p[0] - centre[0], dz = p[2] - centre[2], c = Math.cos(a), s = Math.sin(a);
        p[0] = centre[0] + dx * c + dz * s;
        p[2] = centre[2] - dx * s + dz * c;
    }

    private void poser(double hauteur) {
        double bas = 1e9;
        for (int i = 0; i < nombre; i++) {
            if (decoupe[i] == CAPE || decoupe[i] == JUPE || decoupe[i] == CAPUCHE) continue;
            int o = i * PAS_PRIM;
            if (type[i] == CAPSULE) bas = Math.min(bas, Math.min(prim[o + 1], prim[o + 4]) - prim[o + 6]);
            else bas = Math.min(bas, prim[o + 1] - (prim[o + 3] * Math.abs(prim[o + 7]) + prim[o + 4] * Math.abs(prim[o + 10]) + prim[o + 5] * Math.abs(prim[o + 13])));
        }
        double dy = hauteur - bas;
        for (int i = 0; i < nombre; i++) {
            int o = i * PAS_PRIM;
            prim[o + 1] += dy;
            if (type[i] == CAPSULE) prim[o + 4] += dy;
        }
    }

    private void construire(Corps c, Habits v, Traits traits, Pose pose, double cap) {
        nombre = 0;
        ombreVisage = v.capuche && !v.casque;
        expression = pose.expression >= 0 ? pose.expression : traits.expression;
        phaseCape = pose.phase < 0 ? 0.0 : pose.phase;
        cap += pose.v[Pose.CORPS_LACET];
        double sn = Math.sin(cap), cs = Math.cos(cap);
        avant[0] = sn;
        avant[1] = 0;
        avant[2] = cs;
        droite[0] = -cs;
        droite[1] = 0;
        droite[2] = sn;
        double s = c.taille, l = c.largeur;
        double tissu = 0.007 * s;
        boolean enMarche = pose.marche;
        double balance = enMarche ? Math.sin(pose.phase * 2.0 + 1.0) : 0.0;
        boolean hautCouvre = v.haut != null;
        boolean manches = v.tunique || v.robe;
        int objetD = pose.objetD == Pose.GARDER ? v.droite : pose.objetD;
        int objetG = pose.objetG == Pose.GARDER ? v.gauche : pose.objetG;

        double cuisse = 0.45 * s * c.membres, tibia = 0.44 * s * c.membres;
        double hanche = 0.075 * s + cuisse + tibia;
        double ecart = 0.095 * s * c.hanches * l;
        double bassin = hanche + 0.03 * s;
        double epaule = bassin + 0.5 * s;
        for (int k = 0; k < 5; k++) {
            java.util.Arrays.fill(rot[k], 0.0);
            rot[k][0] = rot[k][4] = rot[k][8] = 1.0;
        }
        repere(RACINE, bassin, 0.0, pose.v[Pose.CORPS_TANGAGE], pose.v[Pose.CORPS_ROULIS]);
        translation[0] = pose.v[Pose.CORPS_X];
        translation[2] = pose.v[Pose.CORPS_Z];
        repere(BUSTE, bassin, pose.v[Pose.BUSTE_LACET], pose.v[Pose.BUSTE_TANGAGE], pose.v[Pose.BUSTE_ROULIS]);
        repere(TETE, epaule + 0.05 * s, pose.v[Pose.TETE_LACET], pose.v[Pose.TETE_TANGAGE], pose.v[Pose.TETE_ROULIS]);
        niveau = RACINE;
        for (int k = 0; k < 2; k++) {
            double cote = k == 0 ? -1.0 : 1.0;
            int j = k == 0 ? Pose.JG_HANCHE : Pose.JD_HANCHE;
            double angle = pose.v[j], pli = pose.v[j + 1], ouvre = pose.v[j + 2];
            double co = Math.cos(ouvre), so = cote * Math.sin(ouvre);
            double tib = angle - pli;
            double[] h = { cote * ecart, hanche, 0.0 };
            double[] g = { h[0] + so * cuisse, h[1] - co * cuisse * Math.cos(angle), h[2] + co * cuisse * Math.sin(angle) };
            double[] t = { g[0] + so * tibia, g[1] - co * tibia * Math.cos(tib), g[2] + co * tibia * Math.sin(tib) };
            double f = -(tib - Math.max(-1.3, Math.min(0.45, tib)));
            double[] mi = milieu(h, g, 0.45);
            Matiere haut = v.pantalon ? v.bas : v.robe ? v.haut : v.braies ? v.linge : c.peau;
            Matiere bas = v.pantalon ? v.bas : v.robe ? v.haut : c.peau;
            capsule(h, mi, 0.075 * s * l + (haut != c.peau ? tissu : 0.0), haut, LIBRE);
            capsule(mi, g, 0.068 * s * l + (bas != c.peau ? tissu : 0.0), bas, LIBRE);
            capsule(g, t, 0.056 * s * l + (bas != c.peau ? tissu : 0.0), bas, LIBRE);
            if (v.bottes) {
                capsule(pied(t, f, 0.16 * s, -0.01), t, 0.064 * s * l, v.cuir, LIBRE);
                capsule(pied(t, f, -0.02 * s, -0.03 * s), pied(t, f, -0.035 * s, 0.13 * s), 0.05 * s * l, v.cuir, LIBRE);
            } else {
                capsule(pied(t, f, -0.03 * s, -0.025 * s), pied(t, f, -0.045 * s, 0.11 * s), 0.038 * s * l, c.peau, LIBRE);
            }
        }
        Matiere matBassin = v.pantalon ? v.bas : v.braies ? v.linge : c.peau;
        ellipsoide(0, bassin, 0, 0.17 * s * l * c.hanches + (matBassin != c.peau ? tissu : 0.0), 0.12 * s, 0.11 * s * l + (matBassin != c.peau ? tissu : 0.0), 0.0, matBassin, LIBRE);
        if (v.robe) ellipsoide(0, bassin - 0.22 * s, 0, 0.23 * s * l * c.hanches, 0.62 * s, 0.17 * s * l, 0.0, v.haut, JUPE);
        else if (v.cotte) ellipsoide(0, bassin - 0.08 * s, 0, 0.205 * s * l * c.hanches, 0.3 * s, 0.155 * s * l, 0.0, v.maille, JUPE);
        else if (v.tunique) ellipsoide(0, bassin - 0.1 * s, 0, 0.2 * s * l * c.hanches, 0.32 * s, 0.15 * s * l, 0.0, v.haut, JUPE);
        Matiere matTorse = v.cotte ? v.maille : hautCouvre ? v.haut : c.peau;
        double gonfle = hautCouvre || v.cotte ? tissu * (v.cotte ? 1.6 : 1.0) : 0.0;
        double torseZ = 0.12 * s * l + gonfle;
        double ceint = v.robe ? bassin + 0.12 * s : bassin + 0.06 * s;
        double ceintZ = 0.13 * s * l + 2.0 * tissu + gonfle;
        if (v.ceinture) {
            ellipsoide(0, ceint, 0, 0.18 * s * l * c.hanches + 2.0 * tissu + gonfle, 0.035 * s, 0.125 * s * l + 2.0 * tissu + gonfle, 0.0, v.cuir, LIBRE);
            ellipsoide(0.0, ceint, ceintZ, 0.03 * s, 0.03 * s, 0.015 * s, 0.0, v.metal, LIBRE);
            double[] p0 = { 0.06 * s * l, ceint - 0.01 * s, ceintZ };
            double[] p1 = { 0.075 * s * l, ceint - 0.14 * s, ceintZ + 0.012 * s + 0.02 * s * balance };
            double[] p2 = { 0.07 * s * l + 0.015 * s * balance, ceint - 0.27 * s, ceintZ + 0.025 * s + 0.035 * s * balance };
            capsule(p0, p1, 0.016 * s, v.cuir, LIBRE);
            capsule(p1, p2, 0.015 * s, v.cuir, LIBRE);
            double[] q0 = { -0.13 * s * l * c.hanches, ceint - 0.02 * s, 0.09 * s * l };
            ellipsoide(q0[0], q0[1] - 0.06 * s, q0[2] + 0.01 * s, 0.045 * s, 0.055 * s, 0.035 * s, 0.0, v.cuir, LIBRE);
        }
        niveau = BUSTE;
        ellipsoide(0, bassin + 0.35 * s, 0.0, 0.19 * s * l * c.epaules + gonfle, 0.165 * s, torseZ, 0.0, matTorse, LIBRE);
        ellipsoide(0, bassin + 0.16 * s, 0.0, 0.155 * s * l * c.hanches + gonfle, 0.15 * s, 0.105 * s * l + gonfle, 0.0, matTorse, LIBRE);
        if (c.femme) {
            for (int k = -1; k <= 1; k += 2) ellipsoide(k * 0.068 * s * l, bassin + 0.37 * s, 0.075 * s * l + gonfle, 0.062 * s * l + gonfle, 0.058 * s + gonfle, 0.055 * s * l + gonfle, 0.0, matTorse, LIBRE);
            if (!hautCouvre && !v.cotte && v.bande) ellipsoide(0, bassin + 0.37 * s, 0.035 * s * l, 0.2 * s * l * c.epaules, 0.075 * s, 0.16 * s * l, 0.0, v.linge, LIBRE);
        }
        if (v.plastron) ellipsoide(0, bassin + 0.33 * s, 0.015 * s, 0.195 * s * l * c.epaules + gonfle, 0.17 * s, 0.135 * s * l + gonfle, 0.0, v.metal, LIBRE);

        boolean tientD = objetD >= 0, tientG = objetG >= 0;
        double[][] poignes = new double[2][];
        for (int k = 0; k < 2; k++) {
            double cote = k == 0 ? -1.0 : 1.0;
            boolean tient = k == 1 ? tientD : tientG;
            int objet = k == 1 ? objetD : objetG;
            int b = k == 0 ? Pose.BG_LEVE : Pose.BD_LEVE;
            double m = pose.v[b], ouvre = pose.v[b + 1], pliCoude = pose.v[b + 2], croise = pose.v[b + 3], tourne = pose.v[k == 0 ? Pose.BG_TOURNE : Pose.BD_TOURNE];
            if (pose.naturel) {
                if (tient) {
                    m *= 0.3;
                    pliCoude += 0.5;
                }
                if (objet == Tenue.BOUCLIER) {
                    m = 0.15;
                    pliCoude = 1.25;
                    ouvre = 0.22;
                } else if (objet == Tenue.ARC) {
                    m = 0.1;
                    pliCoude = 0.75;
                } else if (objet == Tenue.LANTERNE) {
                    pliCoude = 0.55;
                } else if (objet == Tenue.LANCE || objet == Tenue.BATON) {
                    m = 0.05;
                    pliCoude = 0.95;
                }
            }
            double[] e = { cote * 0.2 * s * l * c.epaules, epaule - 0.04 * s, 0.0 };
            double bras = 0.29 * s * c.membres, avantBras = 0.27 * s * c.membres;
            double co = Math.cos(ouvre), so = cote * Math.sin(ouvre);
            double[] u = { so, -Math.cos(m) * co, Math.sin(m) * co };
            double[] fr = { 0.0, Math.sin(m), Math.cos(m) };
            double th = -cote * tourne, ct = Math.cos(th), st = Math.sin(th);
            double[] ux = { u[1] * fr[2] - u[2] * fr[1], u[2] * fr[0] - u[0] * fr[2], u[0] * fr[1] - u[1] * fr[0] };
            double[] fp = { fr[0] * ct + ux[0] * st, fr[1] * ct + ux[1] * st, fr[2] * ct + ux[2] * st };
            double cp = Math.cos(pliCoude), sp = Math.sin(pliCoude);
            double[] coude = le(e, u, bras);
            double[] poignet = le(coude, new double[] { u[0] * cp + fp[0] * sp + so * 0.15, u[1] * cp + fp[1] * sp, u[2] * cp + fp[2] * sp }, avantBras);
            if (croise != 0.0) {
                tournerY(coude, e, -cote * croise);
                tournerY(poignet, e, -cote * croise);
            }
            Matiere matBras = v.cotte ? v.maille : hautCouvre ? v.haut : c.peau;
            Matiere matAvant = manches ? v.haut : c.peau;
            ellipsoide(e[0], e[1] + 0.01 * s, e[2], 0.06 * s * l + gonfle, 0.055 * s + gonfle, 0.06 * s * l + gonfle, 0.0, v.plastron ? v.metal : matBras, LIBRE);
            capsule(e, coude, 0.052 * s * l + (matBras != c.peau ? tissu : 0.0), matBras, LIBRE);
            capsule(coude, poignet, 0.045 * s * l + (matAvant != c.peau ? tissu : 0.0), matAvant, LIBRE);
            poignes[k] = main(poignet, coude, tient, cote, s, l, c.peau);
            reglerMain(k == 1 ? MAIN_D : MAIN_G, poignes[k], coude, poignet, objet);
        }
        double dos = -0.13 * s * l - gonfle - 0.035 * s;
        if (v.sac) {
            ellipsoide(0, epaule - 0.19 * s, dos - 0.035 * s, 0.145 * s * l, 0.17 * s, 0.08 * s, 0.0, v.cuir, LIBRE);
            ellipsoide(0, epaule - 0.08 * s, dos - 0.065 * s, 0.13 * s * l, 0.05 * s, 0.07 * s, 0.0, v.cuir, LIBRE);
            for (int k = -1; k <= 1; k += 2) {
                double[] dessus = { k * 0.1 * s * l, epaule + 0.03 * s, 0.0 };
                capsule(plus(dessus, 0, 0, -0.13 * s), dessus, 0.016 * s, v.cuir, LIBRE);
                capsule(dessus, new double[] { k * 0.13 * s * l, epaule - 0.24 * s, torseZ + 0.005 * s }, 0.016 * s, v.cuir, LIBRE);
            }
            dos -= 0.13 * s;
        }
        equiper(c, v, s, l, bassin, epaule, ceint, ceintZ, dos, balance, poignes, objetD, objetG);
        niveau = BUSTE;
        tete(c, v, traits, s, l, epaule, torseZ, dos);
        if (v.manteau) {
            niveau = BUSTE;
            double flotte = pose.v[Pose.CAPE];
            double recul = (enMarche ? 0.08 * s : 0.03 * s) + (v.sac ? 0.1 * s : 0.0);
            ellipsoide(0, bassin - 0.45 * s, -0.12 * s - recul, 0.36 * s * l * c.epaules, 1.0 * s, 0.25 * s * l, flotte, v.cape, CAPE);
        }
        niveau = RACINE;
        poser(pose.v[Pose.CORPS_Y]);
    }

    private double[] main(double[] poignet, double[] coude, boolean tient, double cote, double s, double l, Matiere peau) {
        double dx = poignet[0] - coude[0], dy = poignet[1] - coude[1], dz = poignet[2] - coude[2];
        double ld = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double[] d = { dx / ld, dy / ld, dz / ld };
        double h = s * Math.sqrt(l);
        if (tient) {
            double[] poing = le(poignet, d, 0.045 * h);
            boite(poing[0], poing[1], poing[2], 0.028 * h, 0.032 * h, 0.03 * h, 1.3, peau, LIBRE);
            capsule(plus(poing, cote * 0.022 * h, 0.012 * h, 0.022 * h), plus(poing, -cote * 0.008 * h, -0.004 * h, 0.03 * h), 0.009 * h, peau, LIBRE);
            capsule(poignet, poing, 0.03 * h, peau, LIBRE);
            return poing;
        }
        double[] paume = le(poignet, d, 0.04 * h);
        boite(paume[0], paume[1], paume[2], 0.024 * h, 0.034 * h, 0.012 * h, 1.4, peau, LIBRE);
        double[] doigts = le(paume, d, 0.05 * h);
        doigts[2] += 0.008 * h;
        boite(doigts[0], doigts[1], doigts[2], 0.022 * h, 0.022 * h, 0.011 * h, 1.2, peau, LIBRE);
        capsule(plus(paume, -cote * 0.02 * h, 0.01 * h, 0.01 * h), plus(paume, -cote * 0.028 * h, -0.03 * h, 0.03 * h), 0.009 * h, peau, LIBRE);
        capsule(poignet, paume, 0.026 * h, peau, LIBRE);
        return doigts;
    }

    private void lame(double[] poing, double[] d, double longueur, double s, Habits v) {
        capsule(le(poing, d, -0.06 * s), le(poing, d, 0.05 * s), 0.014 * s, v.cuir, LIBRE);
        double[] garde = le(poing, d, 0.055 * s);
        capsule(plus(garde, -0.06 * s, 0, 0), plus(garde, 0.06 * s, 0, 0), 0.013 * s, v.metal, LIBRE);
        boite(le(poing, d, -0.07 * s), 0.018 * s, v.metal);
        capsule(le(poing, d, 0.07 * s), le(poing, d, longueur * s), 0.017 * s, v.metal, LIBRE);
    }

    private void arc(double[] centre, double[] axe, double[] ventre, double s, Habits v) {
        double[] precedent = null;
        for (int k = 0; k <= 6; k++) {
            double t = -1.0 + k / 3.0;
            double courbe = 0.1 * s * (1.0 - t * t);
            double[] p = { centre[0] + axe[0] * t * 0.55 * s + ventre[0] * courbe, centre[1] + axe[1] * t * 0.55 * s + ventre[1] * courbe, centre[2] + axe[2] * t * 0.55 * s + ventre[2] * courbe };
            if (precedent != null) capsule(precedent, p, 0.012 * s, v.bois, LIBRE);
            precedent = p;
        }
        capsule(le(centre, axe, -0.55 * s), le(centre, axe, 0.55 * s), 0.004 * s, v.fleche, LIBRE);
    }

    private void lanterne(double[] crochet, double s, Habits v) {
        capsule(crochet, plus(crochet, 0, -0.05 * s, 0), 0.006 * s, v.metal, LIBRE);
        boite(crochet[0], crochet[1] - 0.06 * s, crochet[2], 0.042 * s, 0.012 * s, 0.042 * s, 1.3, v.metal, LIBRE);
        boite(crochet[0], crochet[1] - 0.105 * s, crochet[2], 0.034 * s, 0.035 * s, 0.034 * s, 1.5, v.lueur, LIBRE);
        boite(crochet[0], crochet[1] - 0.15 * s, crochet[2], 0.045 * s, 0.01 * s, 0.045 * s, 1.3, v.metal, LIBRE);
    }

    private void hampe(double[] bas, double[] haut, boolean lance, double s, Habits v) {
        capsule(bas, haut, (lance ? 0.014 : 0.019) * s, v.bois, LIBRE);
        double dx = haut[0] - bas[0], dy = haut[1] - bas[1], dz = haut[2] - bas[2];
        double ld = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double[] d = { dx / ld, dy / ld, dz / ld };
        if (lance) capsule(haut, le(haut, d, 0.17 * s), 0.022 * s, v.metal, LIBRE);
        else boite(haut, 0.03 * s, v.bois);
    }

    private void hache(double[] bas, double[] haut, double[] tranchant, double s, Habits v) {
        capsule(bas, haut, 0.016 * s, v.bois, LIBRE);
        double[] fer = le(haut, tranchant, 0.035 * s);
        fer[1] -= 0.03 * s;
        capsule(plus(haut, 0, -0.03 * s, 0), le(fer, tranchant, 0.04 * s), 0.04 * s, v.metal, LIBRE);
    }

    public static final int OBJET_BASE = 100;
    public static final String[] OBJETS = { "pain", "chope", "gobelet", "bol", "louche", "seau", "buche", "fagot", "sac", "panier",
            "livre", "plume", "parchemin", "bebe", "poisson", "pelle_four", "couteau", "couperet", "scie", "rabot",
            "marteau", "tissu", "fuseau", "quenouille", "pot", "torche", "bougie", "fleur", "bourse", "piece",
            "balai", "linge", "flute", "luth", "tambour", "houlette", "faucille", "fleau", "fourche", "faux",
            "pioche", "pelle", "houe", "arrosoir", "filet", "rame", "canne", "truelle", "pierre", "plateau",
            "cloche", "gourde", "cle", "peau", "corde", "caisse", "gerbe", "lettre", "pince", "calice",
            "baguette", "ardoise", "carotte", "viande", "coupe_vin", "bouquet", "ciseaux", "aiguille", "gant", "masse" };
    private static final Matiere PAIN = new Matiere(12, 4.0, 2.6, 0.2), ARGILE = new Matiere(12, 3.0, 2.8, 0.3), TOILE = new Matiere(3, 3.6, 2.6, 0.0);
    private static final Matiere PAPIER = new Matiere(15, 5.4, 1.8, 0.0), EAU = new Matiere(7, 3.0, 3.0, 1.2), FEUILLE = new Matiere(0, 3.0, 3.0, 0.0);
    private static final Matiere FLEUR = new Matiere(15, 3.0, 3.0, 0.4), ECAILLE = new Matiere(6, 2.6, 3.4, 1.4), VIANDE = new Matiere(12, 2.2, 2.6, 0.6);
    private static final Matiere PAILLE = new Matiere(3, 4.4, 2.4, 0.2), ROCHE = new Matiere(4, 2.4, 3.0, 0.2), BRAISE = new Matiere(12, 6.2, 0.0, 0.0, LUEUR);
    private static final Matiere LAINE = new Matiere(6, 4.4, 2.0, 0.0), OR = new Matiere(3, 4.6, 2.6, 2.0), CAROTTE = new Matiere(12, 5.0, 2.2, 0.3);
    private static final double[] H0 = { 0.0, 0.6816, 0.7317 }, F0 = { 0.0, -0.7317, 0.6816 };

    public static int objet(String nom) {
        switch (nom) {
            case "aucun": return Pose.AUCUN;
            case "epee": return Tenue.EPEE;
            case "dague": return Tenue.DAGUE;
            case "hache": return Tenue.HACHE;
            case "lance": return Tenue.LANCE;
            case "baton": return Tenue.BATON;
            case "outil": return Tenue.OUTIL;
            case "bouclier": return Tenue.BOUCLIER;
            case "arc": return Tenue.ARC;
            case "lanterne": return Tenue.LANTERNE;
            default:
                for (int i = 0; i < OBJETS.length; i++) if (OBJETS[i].equals(nom)) return OBJET_BASE + i;
                throw new IllegalArgumentException("objet inconnu : " + nom);
        }
    }

    private static double[] a(double[] p, double x, double u, double f) {
        return new double[] { p[0] + x, p[1] + u * H0[1] + f * F0[1], p[2] + u * H0[2] + f * F0[2] };
    }

    private void manche(double[] p, double bas, double haut, double r, Matiere m) {
        capsule(a(p, 0, bas, 0), a(p, 0, haut, 0), r, m, LIBRE);
    }

    private void accessoire(int id, double[] p, double s, Habits v) {
        if (id < OBJET_BASE) return;
        switch (OBJETS[id - OBJET_BASE]) {
            case "pain":
                ellipsoide(p[0], p[1] + 0.02 * s, p[2] + 0.04 * s, 0.07 * s, 0.045 * s, 0.055 * s, 0.0, PAIN, LIBRE);
                break;
            case "chope":
                capsule(plus(p, 0, -0.03 * s, 0.035 * s), plus(p, 0, 0.08 * s, 0.035 * s), 0.04 * s, v.bois, LIBRE);
                ellipsoide(p[0], p[1] + 0.1 * s, p[2] + 0.035 * s, 0.035 * s, 0.012 * s, 0.035 * s, 0.0, TOILE, LIBRE);
                capsule(plus(p, 0, 0.04 * s, -0.01 * s), plus(p, 0, -0.01 * s, -0.01 * s), 0.01 * s, v.metal, LIBRE);
                break;
            case "gobelet":
                capsule(plus(p, 0, -0.02 * s, 0.03 * s), plus(p, 0, 0.06 * s, 0.03 * s), 0.028 * s, v.metal, LIBRE);
                break;
            case "bol":
                ellipsoide(p[0], p[1] + 0.03 * s, p[2] + 0.05 * s, 0.07 * s, 0.03 * s, 0.07 * s, 0.0, ARGILE, LIBRE);
                ellipsoide(p[0], p[1] + 0.055 * s, p[2] + 0.05 * s, 0.055 * s, 0.008 * s, 0.055 * s, 0.0, PAIN, LIBRE);
                break;
            case "louche":
                manche(p, -0.06 * s, 0.3 * s, 0.01 * s, v.bois);
                boite(a(p, 0, 0.32 * s, 0.03 * s), 0.035 * s, v.bois);
                break;
            case "seau":
                capsule(p, plus(p, 0, -0.08 * s, 0), 0.008 * s, v.metal, LIBRE);
                capsule(plus(p, 0, -0.12 * s, 0), plus(p, 0, -0.26 * s, 0), 0.075 * s, v.bois, LIBRE);
                ellipsoide(p[0], p[1] - 0.11 * s, p[2], 0.065 * s, 0.006 * s, 0.065 * s, 0.0, EAU, LIBRE);
                break;
            case "buche":
                capsule(new double[] { -0.22 * s, p[1] + 0.05 * s, p[2] + 0.06 * s }, new double[] { 0.22 * s, p[1] + 0.05 * s, p[2] + 0.06 * s }, 0.05 * s, v.bois, LIBRE);
                break;
            case "fagot":
                for (int k = 0; k < 4; k++) {
                    double y = p[1] + 0.04 * s + (k / 2) * 0.07 * s, z = p[2] + 0.04 * s + (k % 2) * 0.07 * s;
                    capsule(new double[] { -0.24 * s, y, z }, new double[] { 0.24 * s, y + 0.01 * s, z }, 0.038 * s, v.bois, LIBRE);
                }
                break;
            case "sac":
                ellipsoide(p[0], p[1] - 0.14 * s, p[2], 0.12 * s, 0.16 * s, 0.1 * s, 0.0, TOILE, LIBRE);
                capsule(p, plus(p, 0, -0.04 * s, 0), 0.03 * s, TOILE, LIBRE);
                break;
            case "panier":
                capsule(p, plus(p, 0, -0.06 * s, 0), 0.01 * s, v.bois, LIBRE);
                ellipsoide(p[0], p[1] - 0.13 * s, p[2], 0.12 * s, 0.07 * s, 0.09 * s, 0.0, PAILLE, LIBRE);
                ellipsoide(p[0], p[1] - 0.075 * s, p[2], 0.09 * s, 0.02 * s, 0.07 * s, 0.0, PAIN, LIBRE);
                break;
            case "livre":
                boite(p[0], p[1] + 0.03 * s, p[2] + 0.05 * s, 0.075 * s, 0.1 * s, 0.025 * s, 1.2, VIANDE, LIBRE);
                boite(p[0], p[1] + 0.03 * s, p[2] + 0.06 * s, 0.07 * s, 0.095 * s, 0.022 * s, 1.2, PAPIER, LIBRE);
                break;
            case "plume":
                manche(p, -0.02 * s, 0.16 * s, 0.006 * s, PAPIER);
                ellipsoide(a(p, 0, 0.13 * s, 0)[0], a(p, 0, 0.13 * s, 0)[1], a(p, 0, 0.13 * s, 0)[2], 0.01 * s, 0.05 * s, 0.02 * s, 0.0, PAPIER, LIBRE);
                break;
            case "parchemin":
            case "lettre":
                capsule(plus(p, -0.09 * s, 0.01 * s, 0.03 * s), plus(p, 0.09 * s, 0.01 * s, 0.03 * s), 0.022 * s, PAPIER, LIBRE);
                break;
            case "bebe":
                ellipsoide(0.0, p[1] + 0.05 * s, p[2] + 0.08 * s, 0.16 * s, 0.07 * s, 0.075 * s, 0.0, TOILE, LIBRE);
                ellipsoide(0.13 * s, p[1] + 0.08 * s, p[2] + 0.08 * s, 0.05 * s, 0.05 * s, 0.05 * s, 0.0, new Matiere(12, 3.4, 3.0, 0.0), LIBRE);
                break;
            case "poisson":
                ellipsoide(p[0], p[1] - 0.12 * s, p[2], 0.03 * s, 0.12 * s, 0.045 * s, 0.0, ECAILLE, LIBRE);
                capsule(p, plus(p, 0, -0.02 * s, 0), 0.02 * s, ECAILLE, LIBRE);
                break;
            case "pelle_four":
                manche(p, -0.5 * s, 0.9 * s, 0.016 * s, v.bois);
                boite(a(p, 0, 1.0 * s, 0)[0], a(p, 0, 1.0 * s, 0)[1], a(p, 0, 1.0 * s, 0)[2], 0.12 * s, 0.015 * s, 0.14 * s, 1.2, v.bois, LIBRE);
                break;
            case "couteau":
                manche(p, -0.04 * s, 0.05 * s, 0.014 * s, v.bois);
                capsule(a(p, 0, 0.05 * s, 0), a(p, 0, 0.18 * s, 0), 0.012 * s, v.metal, LIBRE);
                break;
            case "couperet":
                manche(p, -0.04 * s, 0.06 * s, 0.016 * s, v.bois);
                boite(a(p, 0, 0.14 * s, 0.03 * s)[0], a(p, 0, 0.14 * s, 0.03 * s)[1], a(p, 0, 0.14 * s, 0.03 * s)[2], 0.008 * s, 0.08 * s, 0.06 * s, 1.2, v.metal, LIBRE);
                break;
            case "scie":
                manche(p, -0.04 * s, 0.05 * s, 0.022 * s, v.bois);
                capsule(a(p, 0, 0.05 * s, 0), a(p, 0, 0.45 * s, 0), 0.03 * s, v.metal, LIBRE);
                break;
            case "rabot":
                boite(p[0], p[1] - 0.02 * s, p[2] + 0.04 * s, 0.035 * s, 0.035 * s, 0.11 * s, 1.2, v.bois, LIBRE);
                break;
            case "marteau":
                manche(p, -0.05 * s, 0.28 * s, 0.014 * s, v.bois);
                boite(a(p, 0, 0.28 * s, 0)[0], a(p, 0, 0.28 * s, 0)[1], a(p, 0, 0.28 * s, 0)[2], 0.03 * s, 0.03 * s, 0.07 * s, 1.3, v.metal, LIBRE);
                break;
            case "masse":
                manche(p, -0.1 * s, 0.6 * s, 0.016 * s, v.bois);
                boite(a(p, 0, 0.62 * s, 0)[0], a(p, 0, 0.62 * s, 0)[1], a(p, 0, 0.62 * s, 0)[2], 0.05 * s, 0.05 * s, 0.1 * s, 1.3, v.metal, LIBRE);
                break;
            case "tissu":
            case "linge":
                boite(p[0], p[1] - 0.12 * s, p[2] + 0.03 * s, 0.14 * s, 0.13 * s, 0.01 * s, 1.1, TOILE, LIBRE);
                break;
            case "fuseau":
                manche(p, -0.08 * s, 0.1 * s, 0.008 * s, v.bois);
                ellipsoide(p[0], p[1], p[2], 0.03 * s, 0.03 * s, 0.03 * s, 0.0, LAINE, LIBRE);
                break;
            case "quenouille":
                manche(p, -0.5 * s, 0.4 * s, 0.012 * s, v.bois);
                ellipsoide(a(p, 0, 0.42 * s, 0)[0], a(p, 0, 0.42 * s, 0)[1], a(p, 0, 0.42 * s, 0)[2], 0.06 * s, 0.09 * s, 0.06 * s, 0.0, LAINE, LIBRE);
                break;
            case "pot":
                ellipsoide(p[0], p[1] + 0.05 * s, p[2] + 0.07 * s, 0.08 * s, 0.1 * s, 0.08 * s, 0.0, ARGILE, LIBRE);
                capsule(plus(p, 0, 0.13 * s, 0.07 * s), plus(p, 0, 0.16 * s, 0.07 * s), 0.04 * s, ARGILE, LIBRE);
                break;
            case "torche":
                manche(p, -0.08 * s, 0.32 * s, 0.018 * s, v.bois);
                ellipsoide(a(p, 0, 0.38 * s, 0)[0], a(p, 0, 0.38 * s, 0)[1] + 0.03 * s, a(p, 0, 0.38 * s, 0)[2], 0.045 * s, 0.08 * s, 0.045 * s, 0.0, v.lueur, LIBRE);
                break;
            case "bougie":
                capsule(plus(p, 0, -0.01 * s, 0.03 * s), plus(p, 0, 0.1 * s, 0.03 * s), 0.016 * s, PAPIER, LIBRE);
                ellipsoide(p[0], p[1] + 0.13 * s, p[2] + 0.03 * s, 0.012 * s, 0.025 * s, 0.012 * s, 0.0, v.lueur, LIBRE);
                break;
            case "fleur":
                manche(p, -0.02 * s, 0.16 * s, 0.006 * s, FEUILLE);
                boite(a(p, 0, 0.18 * s, 0), 0.03 * s, FLEUR);
                break;
            case "bouquet":
                for (int k = -1; k <= 1; k++) {
                    capsule(a(p, 0, -0.02 * s, 0), a(p, k * 0.04 * s, 0.17 * s, 0), 0.007 * s, FEUILLE, LIBRE);
                    boite(a(p, k * 0.045 * s, 0.19 * s, (k & 1) * 0.02 * s), 0.028 * s, k == 0 ? FLEUR : CAROTTE);
                }
                break;
            case "bourse":
                capsule(p, plus(p, 0, -0.04 * s, 0), 0.008 * s, v.cuir, LIBRE);
                ellipsoide(p[0], p[1] - 0.08 * s, p[2], 0.045 * s, 0.05 * s, 0.04 * s, 0.0, v.cuir, LIBRE);
                break;
            case "piece":
                ellipsoide(p[0], p[1] + 0.03 * s, p[2] + 0.02 * s, 0.022 * s, 0.022 * s, 0.006 * s, 0.0, OR, LIBRE);
                break;
            case "balai":
                manche(p, -0.9 * s, 0.4 * s, 0.014 * s, v.bois);
                ellipsoide(a(p, 0, -0.98 * s, 0)[0], a(p, 0, -0.98 * s, 0)[1], a(p, 0, -0.98 * s, 0)[2], 0.09 * s, 0.13 * s, 0.05 * s, 0.0, PAILLE, LIBRE);
                break;
            case "flute":
                capsule(a(p, 0, -0.06 * s, 0), a(p, 0, 0.3 * s, 0), 0.012 * s, v.bois, LIBRE);
                break;
            case "luth":
                ellipsoide(a(p, 0, -0.08 * s, 0.1 * s)[0], a(p, 0, -0.08 * s, 0.1 * s)[1], a(p, 0, -0.08 * s, 0.1 * s)[2], 0.13 * s, 0.17 * s, 0.06 * s, 0.0, v.bois, LIBRE);
                capsule(a(p, 0, 0.05 * s, 0.1 * s), a(p, 0, 0.4 * s, 0.1 * s), 0.02 * s, v.bois, LIBRE);
                break;
            case "tambour":
                capsule(plus(p, 0, -0.12 * s, 0.04 * s), plus(p, 0, 0.04 * s, 0.04 * s), 0.12 * s, TOILE, LIBRE);
                break;
            case "houlette":
                manche(p, -0.85 * s, 0.75 * s, 0.017 * s, v.bois);
                capsule(a(p, 0, 0.75 * s, 0), a(p, 0, 0.83 * s, 0.07 * s), 0.016 * s, v.bois, LIBRE);
                capsule(a(p, 0, 0.83 * s, 0.07 * s), a(p, 0, 0.77 * s, 0.13 * s), 0.016 * s, v.bois, LIBRE);
                break;
            case "faucille":
                manche(p, -0.04 * s, 0.1 * s, 0.015 * s, v.bois);
                capsule(a(p, 0, 0.1 * s, 0), a(p, 0, 0.22 * s, 0.08 * s), 0.012 * s, v.metal, LIBRE);
                capsule(a(p, 0, 0.22 * s, 0.08 * s), a(p, 0, 0.2 * s, 0.2 * s), 0.012 * s, v.metal, LIBRE);
                break;
            case "fleau":
                manche(p, -0.3 * s, 0.6 * s, 0.016 * s, v.bois);
                capsule(a(p, 0, 0.62 * s, 0), a(p, 0, 0.62 * s, 0.4 * s), 0.02 * s, v.bois, LIBRE);
                break;
            case "fourche":
                manche(p, -0.6 * s, 0.8 * s, 0.016 * s, v.bois);
                for (int k = -1; k <= 1; k++) capsule(a(p, k * 0.04 * s, 0.8 * s, 0), a(p, k * 0.05 * s, 1.0 * s, 0), 0.008 * s, v.metal, LIBRE);
                break;
            case "faux":
                manche(p, -0.6 * s, 0.9 * s, 0.017 * s, v.bois);
                capsule(a(p, 0, 0.9 * s, 0), a(p, -0.45 * s, 0.82 * s, 0.05 * s), 0.016 * s, v.metal, LIBRE);
                break;
            case "pioche":
                manche(p, -0.1 * s, 0.6 * s, 0.017 * s, v.bois);
                capsule(a(p, 0, 0.6 * s, -0.22 * s), a(p, 0, 0.62 * s, 0.22 * s), 0.02 * s, v.metal, LIBRE);
                break;
            case "pelle":
                manche(p, -0.3 * s, 0.75 * s, 0.016 * s, v.bois);
                boite(a(p, 0, 0.88 * s, 0)[0], a(p, 0, 0.88 * s, 0)[1], a(p, 0, 0.88 * s, 0)[2], 0.09 * s, 0.13 * s, 0.012 * s, 1.2, v.metal, LIBRE);
                break;
            case "houe":
                manche(p, -0.4 * s, 0.85 * s, 0.016 * s, v.bois);
                capsule(a(p, 0, 0.85 * s, 0), a(p, 0, 0.8 * s, 0.14 * s), 0.035 * s, v.metal, LIBRE);
                break;
            case "arrosoir":
                capsule(plus(p, 0, -0.18 * s, 0.02 * s), plus(p, 0, -0.04 * s, 0.02 * s), 0.08 * s, v.metal, LIBRE);
                capsule(plus(p, 0, -0.16 * s, 0.09 * s), plus(p, 0, -0.06 * s, 0.25 * s), 0.014 * s, v.metal, LIBRE);
                break;
            case "filet":
                ellipsoide(p[0], p[1] - 0.2 * s, p[2] + 0.06 * s, 0.25 * s, 0.25 * s, 0.03 * s, 0.0, new Matiere(3, 3.0, 2.4, 0.0, MAILLES), LIBRE);
                break;
            case "rame":
                manche(p, -0.5 * s, 0.9 * s, 0.017 * s, v.bois);
                boite(a(p, 0, -0.6 * s, 0)[0], a(p, 0, -0.6 * s, 0)[1], a(p, 0, -0.6 * s, 0)[2], 0.07 * s, 0.2 * s, 0.012 * s, 1.2, v.bois, LIBRE);
                break;
            case "canne":
                capsule(a(p, 0, -0.15 * s, 0), a(p, 0, 1.6 * s, 0), 0.01 * s, v.bois, LIBRE);
                capsule(a(p, 0, 1.6 * s, 0), plus(a(p, 0, 1.6 * s, 0), 0, -1.2 * s, 0), 0.003 * s, PAPIER, LIBRE);
                break;
            case "truelle":
                manche(p, -0.03 * s, 0.06 * s, 0.014 * s, v.bois);
                boite(a(p, 0, 0.12 * s, 0.02 * s)[0], a(p, 0, 0.12 * s, 0.02 * s)[1], a(p, 0, 0.12 * s, 0.02 * s)[2], 0.05 * s, 0.07 * s, 0.006 * s, 1.1, v.metal, LIBRE);
                break;
            case "pierre":
                boite(0.0, p[1] + 0.05 * s, p[2] + 0.08 * s, 0.14 * s, 0.09 * s, 0.09 * s, 1.3, ROCHE, LIBRE);
                break;
            case "caisse":
                boite(0.0, p[1] + 0.06 * s, p[2] + 0.1 * s, 0.18 * s, 0.13 * s, 0.13 * s, 1.25, v.bois, LIBRE);
                break;
            case "plateau":
                boite(p[0] - 0.05 * s, p[1] + 0.03 * s, p[2] + 0.1 * s, 0.17 * s, 0.012 * s, 0.13 * s, 1.15, v.bois, LIBRE);
                capsule(plus(p, -0.08 * s, 0.04 * s, 0.1 * s), plus(p, -0.08 * s, 0.12 * s, 0.1 * s), 0.035 * s, v.bois, LIBRE);
                ellipsoide(p[0] + 0.02 * s, p[1] + 0.07 * s, p[2] + 0.12 * s, 0.05 * s, 0.035 * s, 0.04 * s, 0.0, PAIN, LIBRE);
                break;
            case "cloche":
                manche(p, -0.02 * s, 0.08 * s, 0.01 * s, v.bois);
                ellipsoide(p[0], p[1] - 0.07 * s, p[2] + 0.02 * s, 0.05 * s, 0.06 * s, 0.05 * s, 0.0, OR, LIBRE);
                break;
            case "gourde":
                ellipsoide(p[0], p[1] - 0.06 * s, p[2] + 0.02 * s, 0.06 * s, 0.08 * s, 0.04 * s, 0.0, v.cuir, LIBRE);
                break;
            case "cle":
                capsule(a(p, 0, 0.0, 0), a(p, 0, 0.08 * s, 0), 0.007 * s, OR, LIBRE);
                break;
            case "peau":
                boite(p[0], p[1] - 0.16 * s, p[2] + 0.05 * s, 0.2 * s, 0.17 * s, 0.01 * s, 1.1, VIANDE, LIBRE);
                break;
            case "corde":
                capsule(p, plus(p, 0.05 * s, -0.25 * s, 0.05 * s), 0.012 * s, PAILLE, LIBRE);
                capsule(plus(p, 0.05 * s, -0.25 * s, 0.05 * s), plus(p, -0.03 * s, -0.5 * s, 0.03 * s), 0.012 * s, PAILLE, LIBRE);
                break;
            case "gerbe":
                capsule(plus(p, 0, -0.25 * s, 0.05 * s), plus(p, 0, 0.3 * s, 0.05 * s), 0.07 * s, PAILLE, LIBRE);
                ellipsoide(p[0], p[1] + 0.32 * s, p[2] + 0.05 * s, 0.1 * s, 0.07 * s, 0.1 * s, 0.0, OR, LIBRE);
                break;
            case "pince":
                capsule(a(p, 0, -0.04 * s, 0), a(p, 0, 0.35 * s, 0), 0.012 * s, v.metal, LIBRE);
                capsule(a(p, 0, 0.35 * s, 0), a(p, 0, 0.5 * s, 0), 0.025 * s, BRAISE, LIBRE);
                break;
            case "calice":
            case "coupe_vin":
                capsule(plus(p, 0, -0.01 * s, 0.03 * s), plus(p, 0, 0.06 * s, 0.03 * s), 0.008 * s, OR, LIBRE);
                ellipsoide(p[0], p[1] + 0.09 * s, p[2] + 0.03 * s, 0.035 * s, 0.03 * s, 0.035 * s, 0.0, OR, LIBRE);
                break;
            case "baguette":
                capsule(a(p, 0, -0.03 * s, 0), a(p, 0, 0.45 * s, 0), 0.007 * s, v.bois, LIBRE);
                break;
            case "ardoise":
                boite(p[0], p[1] + 0.05 * s, p[2] + 0.06 * s, 0.11 * s, 0.08 * s, 0.01 * s, 1.1, ROCHE, LIBRE);
                break;
            case "carotte":
                capsule(a(p, 0, -0.02 * s, 0), a(p, 0, 0.12 * s, 0), 0.018 * s, CAROTTE, LIBRE);
                boite(a(p, 0, -0.04 * s, 0), 0.02 * s, FEUILLE);
                break;
            case "viande":
                ellipsoide(p[0], p[1] - 0.1 * s, p[2] + 0.02 * s, 0.07 * s, 0.11 * s, 0.05 * s, 0.0, VIANDE, LIBRE);
                break;
            case "ciseaux":
                capsule(a(p, 0, 0.0, 0), a(p, -0.015 * s, 0.12 * s, 0), 0.006 * s, v.metal, LIBRE);
                capsule(a(p, 0, 0.0, 0), a(p, 0.015 * s, 0.12 * s, 0), 0.006 * s, v.metal, LIBRE);
                break;
            case "aiguille":
                capsule(a(p, 0, 0.0, 0), a(p, 0, 0.06 * s, 0), 0.003 * s, v.metal, LIBRE);
                break;
            case "gant":
                ellipsoide(p[0], p[1], p[2], 0.045 * s, 0.05 * s, 0.045 * s, 0.0, v.cuir, LIBRE);
                break;
            default:
                break;
        }
    }

    private void equiper(Corps c, Habits v, double s, double l, double bassin, double epaule, double ceint, double ceintZ, double dos, double balance, double[][] poignes, int droite, int gauche) {
        double[] pd = poignes[1], pg = poignes[0];
        double hancheX = 0.2 * s * l * c.hanches;
        niveau = RACINE;
        if (v.epee) {
            double[] a = { -hancheX, bassin + 0.05 * s, -0.02 * s };
            double[] b = plus(a, -0.05 * s, -0.6 * s, -0.3 * s - 0.04 * s * balance);
            capsule(a, b, 0.024 * s, v.cuir, LIBRE);
            if (droite != Tenue.EPEE) {
                double[] d = { a[0] - b[0], a[1] - b[1], a[2] - b[2] };
                double ld = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
                double[] poignee = le(a, d, 0.17 * s / ld);
                capsule(a, poignee, 0.015 * s, v.cuir, LIBRE);
                capsule(plus(a, 0, 0.0, 0.07 * s), plus(a, 0, 0.0, -0.07 * s), 0.014 * s, v.metal, LIBRE);
                boite(poignee, 0.022 * s, v.metal);
            }
        }
        if (v.dague) {
            double[] a = { 0.12 * s * l, ceint - 0.005 * s, ceintZ + 0.005 * s };
            if (droite != Tenue.DAGUE) {
                capsule(a, plus(a, 0.03 * s, -0.17 * s, 0.01 * s), 0.018 * s, v.cuir, LIBRE);
                capsule(a, plus(a, -0.01 * s, 0.07 * s, 0.0), 0.012 * s, v.cuir, LIBRE);
                capsule(plus(a, -0.035 * s, 0.0, 0), plus(a, 0.035 * s, 0.0, 0), 0.01 * s, v.metal, LIBRE);
            } else {
                capsule(a, plus(a, 0.03 * s, -0.17 * s, 0.01 * s), 0.018 * s, v.cuir, LIBRE);
            }
        }
        if (v.hache && droite != Tenue.HACHE) {
            double[] haut = { hancheX + 0.03 * s, ceint + 0.06 * s, -0.04 * s };
            hache(plus(haut, 0.01 * s, -0.4 * s, 0.05 * s + 0.03 * s * balance), haut, new double[] { 0, 0, -1 }, s, v);
        }
        if (v.lanterne && gauche != Tenue.LANTERNE) lanterne(new double[] { -0.12 * s * l, ceint - 0.02 * s, ceintZ + 0.01 * s }, s, v);
        niveau = BUSTE;
        double dosY = epaule - 0.2 * s;
        if (v.bouclier2 && gauche != Tenue.BOUCLIER) {
            boite(0, dosY, dos - 0.025 * s, 0.2 * s, 0.24 * s, 0.022 * s, 1.25, v.bouclier, LIBRE);
            boite(0, dosY, dos - 0.05 * s, 0.045 * s, 0.045 * s, 0.015 * s, 1.2, v.metal, LIBRE);
            dos -= 0.05 * s;
        }
        if (v.carquois) {
            double[] bas = { 0.08 * s, bassin + 0.12 * s, dos - 0.02 * s };
            double[] haut = { 0.17 * s, epaule + 0.1 * s, dos - 0.03 * s };
            capsule(bas, haut, 0.045 * s, v.cuir, LIBRE);
            for (int k = 0; k < 3; k++) {
                double[] f = plus(haut, (k - 1) * 0.02 * s, 0.0, (k % 2) * 0.015 * s);
                capsule(f, plus(f, 0.015 * s, 0.09 * s, 0.0), 0.012 * s, v.fleche, LIBRE);
            }
        }
        if (v.arc && gauche != Tenue.ARC) {
            double r = Math.sqrt(0.5);
            arc(new double[] { 0.0, epaule - 0.12 * s, dos - 0.045 * s }, new double[] { -r, r, 0 }, new double[] { 0, 0, -1 }, s, v);
        }
        if ((v.lance && droite != Tenue.LANCE) || (v.baton && droite != Tenue.BATON)) {
            boolean lance = v.lance && droite != Tenue.LANCE;
            hampe(new double[] { -0.28 * s, bassin - 0.3 * s, dos - 0.06 * s }, new double[] { 0.24 * s, epaule + 0.5 * s, dos - 0.06 * s }, lance, s, v);
        }
        if (v.outil && droite != Tenue.OUTIL) {
            double[] haut = { -0.2 * s, epaule + 0.25 * s, dos - 0.07 * s };
            double[] bas = { 0.18 * s, bassin - 0.05 * s, dos - 0.07 * s };
            capsule(haut, bas, 0.018 * s, v.bois, LIBRE);
            boite(haut[0], haut[1], haut[2], 0.07 * s, 0.025 * s, 0.025 * s, 1.3, v.metal, LIBRE);
        }
        niveau = MAIN_D;
        switch (droite) {
            case Tenue.EPEE: lame(pd, normer(0.45, -0.5, 0.75), 0.78, s, v); break;
            case Tenue.DAGUE: lame(pd, normer(0.4, -0.4, 0.8), 0.3, s, v); break;
            case Tenue.HACHE: {
                double[] d = normer(0.0, 0.85, 0.5);
                hache(le(pd, d, -0.1 * s), le(pd, d, 0.45 * s), new double[] { 0, 0, 1 }, s, v);
                break;
            }
            case Tenue.LANCE: hampe(plus(pd, 0, -0.85 * s, -0.08 * s), plus(pd, 0, 0.95 * s, 0.1 * s), true, s, v); break;
            case Tenue.BATON: hampe(plus(pd, 0, -0.82 * s, 0.0), plus(pd, 0, 0.72 * s, 0.06 * s), false, s, v); break;
            case Tenue.OUTIL: {
                double[] haut = plus(pd, 0.0, 0.1 * s, -0.04 * s);
                double[] bas = plus(pd, 0.02 * s, -0.42 * s, 0.22 * s);
                capsule(haut, bas, 0.018 * s, v.bois, LIBRE);
                ellipsoide(bas[0], bas[1] + 0.02 * s, bas[2] + 0.01 * s, 0.025 * s, 0.07 * s, 0.07 * s, 0.0, v.metal, LIBRE);
                break;
            }
            default: accessoire(droite, pd, s, v); break;
        }
        niveau = MAIN_G;
        switch (gauche) {
            case Tenue.BOUCLIER:
                boite(pg[0] - 0.035 * s, pg[1] + 0.05 * s, pg[2] + 0.05 * s, 0.2 * s, 0.24 * s, 0.022 * s, 1.25, v.bouclier, LIBRE);
                boite(pg[0] - 0.035 * s, pg[1] + 0.05 * s, pg[2] + 0.075 * s, 0.045 * s, 0.045 * s, 0.015 * s, 1.2, v.metal, LIBRE);
                break;
            case Tenue.ARC: arc(pg, new double[] { 0, 1, 0 }, new double[] { 0, 0, 1 }, s, v); break;
            case Tenue.LANTERNE: lanterne(plus(pg, 0, -0.01 * s, 0), s, v); break;
            default: accessoire(gauche, pg, s, v); break;
        }
    }

    private void tete(Corps c, Habits v, Traits traits, double s, double l, double epaule, double torseZ, double dos) {
        double cou = epaule + 0.05 * s;
        double rt = s * c.tete * 1.08;
        double teteY = cou + 0.13 * s * c.tete;
        double ta = 0.086 * rt, tb = 0.108 * rt, tc = 0.094 * rt, tz = 0.01 * s;
        double front = tz + tc;
        double crane = teteY + tb;
        capsule(new double[] { 0, epaule - 0.05 * s, 0 }, new double[] { 0, cou + 0.04 * s, 0.01 * s }, 0.05 * s * l, c.peau, LIBRE);
        niveau = TETE;
        boite(0, teteY, tz, ta, tb, tc, 1.5, c.peau, VISAGE);
        boite(0, teteY - 0.025 * rt, front + 0.008 * rt, 0.013 * rt, 0.026 * rt, 0.012 * rt, 1.4, c.peau, LIBRE);
        for (int k = -1; k <= 1; k += 2) {
            double[] oreille = { k * ta, teteY + 0.005 * rt, tz - 0.01 * rt };
            if (c.espece == Peuple.ELFE) capsule(oreille, plus(oreille, k * 0.06 * rt, 0.07 * rt, -0.03 * rt), 0.014 * rt, c.peau, LIBRE);
            else if (c.espece == Peuple.ORK) capsule(oreille, plus(oreille, k * 0.045 * rt, 0.03 * rt, -0.02 * rt), 0.016 * rt, c.peau, LIBRE);
            else boite(oreille[0] + k * 0.008 * rt, oreille[1], oreille[2], 0.01 * rt, 0.025 * rt, 0.016 * rt, 1.4, c.peau, LIBRE);
            if (c.espece == Peuple.ORK) {
                double[] croc = { k * 0.035 * rt, teteY - 0.085 * rt, front - 0.004 * rt };
                capsule(croc, plus(croc, k * 0.005 * rt, 0.035 * rt, 0.012 * rt), 0.009 * rt, c.ivoire, LIBRE);
            }
        }
        double cm = traits.palierCheveux() * 2.0 / 100.0 * s;
        boolean couvert = v.capuche || v.casque;
        int coupe = traits.coupe;
        double ep = Math.min(0.022 * rt, 0.004 * rt + cm * 0.5);
        if (cm > 0.003 * s && coupe != CRETE_COUPE) {
            if (!couvert) boite(0, teteY + 0.015 * rt, tz - 0.008 * rt, ta + ep, tb + ep * 0.6, tc + ep, 1.5, c.cheveux, CHEVEUX);
        }
        if (coupe == CRETE_COUPE && !couvert && cm > 0.0) {
            double hc = Math.min(cm, 0.2 * s) * 0.7 + 0.01 * s;
            boite(0, crane + hc * 0.5 - 0.01 * s, tz - 0.01 * rt, 0.02 * rt, hc * 0.5 + 0.01 * s, tc * 0.95, 1.3, c.cheveux, LIBRE);
        }
        double bas = crane - cm;
        double nuque = teteY - tb * 0.6;
        if (coupe == Traits.MI_LONG || coupe == Traits.LONG || (coupe == Traits.COURT && cm > 0.1 * s)) {
            double large = coupe == Traits.LONG ? 1.15 : 1.0;
            double finTete = Math.max(bas, nuque);
            if (!couvert || finTete < teteY - tb) {
                double haut = couvert ? teteY - tb * 0.3 : crane;
                if (haut > finTete) boite(0, (haut + finTete) * 0.5, tz - tc * 0.7, (ta + ep) * large, (haut - finTete) * 0.5, tc * 0.4 + ep, 1.4, c.cheveux, LIBRE);
                if (!couvert && coupe != Traits.COURT) {
                    double cote = Math.max(bas, teteY - tb * 1.1);
                    for (int k = -1; k <= 1; k += 2) boite(k * (ta + ep * 0.7) * large, (crane - 0.02 * rt + cote) * 0.5, tz - tc * 0.25, ep * 0.9, (crane - 0.02 * rt - cote) * 0.5, tc * 0.6, 1.4, c.cheveux, LIBRE);
                }
            }
            if (bas < nuque) {
                double haut = nuque;
                boite(0, (haut + bas) * 0.5, Math.min(tz - tc * 0.7, dos + 0.035 * s - 0.012 * s), (ta + ep) * large * (coupe == Traits.LONG ? 1.3 : 1.05), (haut - bas) * 0.5, 0.025 * s, 1.4, c.cheveux, LIBRE);
            }
        }
        if (coupe == Traits.QUEUE || coupe == Traits.TRESSE) {
            double[] noeud = { 0, teteY + 0.01 * rt, tz - tc - 0.012 * rt };
            boite(noeud, 0.018 * rt, c.cheveux);
            double reste = Math.max(0.0, cm - 0.12 * s);
            if (reste > 0.0) {
                double[] cou2 = { 0, teteY - tb * 1.2, Math.min(noeud[2] - 0.01 * s, dos + 0.02 * s) };
                double[] fin = { 0.01 * s, cou2[1] - Math.max(0.0, reste - (noeud[1] - cou2[1])), dos + 0.01 * s };
                if (fin[1] > cou2[1]) fin = cou2;
                if (coupe == Traits.QUEUE) {
                    capsule(noeud, cou2, 0.026 * rt, c.cheveux, LIBRE);
                    if (fin != cou2) capsule(cou2, fin, 0.03 * rt, c.cheveux, LIBRE);
                } else {
                    tresse(noeud, cou2, 0.022 * rt, c.cheveux);
                    if (fin != cou2) tresse(cou2, fin, 0.022 * rt, c.cheveux);
                }
            }
        }
        if (coupe == Traits.CHIGNON && !couvert) {
            double r = 0.035 * rt + Math.min(cm, 0.6 * s) / (0.6 * s) * 0.025 * rt;
            boite(0, teteY + 0.06 * rt, tz - tc - r * 0.6, r, r, r, 1.3, c.cheveux, LIBRE);
        }
        barbe(c, traits, s, rt, teteY, tb, ta, tc, tz, front, torseZ);
        if (v.casque) {
            boite(0, teteY + 0.03 * rt, tz - 0.004 * rt, ta + 0.02 * rt, tb + 0.016 * rt, tc + 0.02 * rt, 1.45, v.metal, CASQUE);
            boite(0, teteY + 0.005 * rt, front + 0.022 * rt, 0.012 * rt, 0.055 * rt, 0.008 * rt, 1.4, v.metal, LIBRE);
        } else if (v.capuche) {
            boite(0, teteY + 0.01 * rt, -0.01 * rt, 0.125 * rt, 0.15 * rt, 0.135 * rt, 1.2, v.cape, CAPUCHE);
            niveau = BUSTE;
            ellipsoide(0, epaule - 0.02 * s, -0.01, 0.2 * s * l * c.epaules, 0.09 * s, 0.14 * s * l, 0.0, v.cape, LIBRE);
        }
    }

    private static final int CRETE_COUPE = Traits.CRETE;

    private void tresse(double[] a, double[] b, double r, Matiere m) {
        double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        double ld = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int n = Math.max(1, (int) Math.round(ld / (r * 2.2)));
        for (int k = 0; k < n; k++) {
            double t = (k + 0.5) / n;
            double cote = (k % 2 == 0 ? 1 : -1) * r * 0.3;
            boite(a[0] + dx * t + cote, a[1] + dy * t, a[2] + dz * t, r, ld / n * 0.55, r * 0.9, 1.25, m, LIBRE);
        }
    }

    private void barbe(Corps c, Traits traits, double s, double rt, double teteY, double tb, double ta, double tc, double tz, double front, double torseZ) {
        chaume = false;
        int palier = traits.palierBarbe();
        if (palier <= 0) return;
        if (palier == 1) {
            chaume = true;
            return;
        }
        double cm = (palier - 1) * 2.0 / 100.0 * s;
        int style = traits.barbe == Traits.SANS_BARBE ? Traits.PLEINE : traits.barbe;
        double menton = teteY - tb;
        if (style == Traits.MOUSTACHE || style == Traits.BOUC) {
            double tombe = Math.min(cm, 0.12 * s) * 0.5;
            boite(0, teteY - 0.05 * rt, front + 0.006 * rt, 0.045 * rt, 0.011 * rt, 0.01 * rt, 1.3, c.cheveux, LIBRE);
            if (style == Traits.MOUSTACHE && tombe > 0.005 * s) {
                for (int k = -1; k <= 1; k += 2) capsule(new double[] { k * 0.042 * rt, teteY - 0.05 * rt, front + 0.004 * rt }, new double[] { k * 0.05 * rt, teteY - 0.05 * rt - tombe, front + 0.006 * rt }, 0.01 * rt, c.cheveux, LIBRE);
            }
            if (style == Traits.BOUC) {
                double h = 0.025 * rt + Math.min(cm, 0.15 * s) * 0.8;
                boite(0, menton + 0.02 * rt - h * 0.5, front - 0.01 * rt, 0.028 * rt, h * 0.5, 0.016 * rt, 1.3, c.cheveux, LIBRE);
            }
            return;
        }
        double ep = Math.min(0.025 * rt, 0.008 * rt + cm * 0.4);
        double basHaut = Math.max(menton - Math.min(cm, 0.06 * s), menton - 0.06 * s);
        boite(0, (teteY - 0.01 * rt + basHaut) * 0.5, tz + tc * 0.45, ta + ep * 0.6, (teteY - 0.01 * rt - basHaut) * 0.5, tc * 0.55 + ep, 1.4, c.cheveux, BARBE);
        double reste = cm - 0.06 * s;
        if (reste > 0.0) {
            double z = Math.max(front - 0.02 * rt, torseZ + 0.018 * s);
            double[] haut = { 0, basHaut + 0.01 * s, z };
            double[] bas = { 0, basHaut - reste, z + 0.01 * s };
            if (style == Traits.TRESSEE) {
                boite(0, basHaut - Math.min(reste, 0.05 * s) * 0.5, z, ta * 0.8, Math.min(reste, 0.05 * s) * 0.5 + 0.01 * s, 0.025 * s, 1.4, c.cheveux, LIBRE);
                if (reste > 0.05 * s) {
                    double[] debut = { 0, basHaut - 0.05 * s, z + 0.005 * s };
                    tresse(debut, bas, 0.024 * rt, c.cheveux);
                    boite(plus(bas, 0, -0.01 * s, 0), 0.014 * s, new Matiere(5, 1.6, 4.2, 1.6));
                }
            } else {
                double large = ta * (0.9 - Math.min(0.5, reste / (0.4 * s)));
                boite(0, (haut[1] + bas[1]) * 0.5, z, Math.max(large, 0.03 * s), (haut[1] - bas[1]) * 0.5, 0.025 * s, 1.35, c.cheveux, LIBRE);
            }
        }
    }

    private void boite(double[] p, double r, Matiere m) {
        boite(p[0], p[1], p[2], r, r, r, 1.4, m, LIBRE);
    }

    private void boite(double x, double y, double z, double a, double b, double cz, double coin, Matiere m, int clip) {
        ellipsoide(x, y, z, a, b, cz, 0.0, m, clip);
        prim[(nombre - 1) * PAS_PRIM + 15] = coin;
    }

    private void ellipsoide(double x, double y, double z, double a, double b, double cz, double bascule, Matiere m, int clip) {
        int i = nombre++;
        type[i] = ELLIPSOIDE;
        int o = i * PAS_PRIM;
        double[] w = monde(x, y, z);
        prim[o] = w[0];
        prim[o + 1] = w[1];
        prim[o + 2] = w[2];
        prim[o + 3] = a;
        prim[o + 4] = b;
        prim[o + 5] = cz;
        double cb = Math.cos(bascule), sb = Math.sin(bascule);
        double[] ex = sens(1.0, 0.0, 0.0), ey = sens(0.0, cb, sb), ez = sens(0.0, -sb, cb);
        System.arraycopy(ex, 0, prim, o + 6, 3);
        System.arraycopy(ey, 0, prim, o + 9, 3);
        System.arraycopy(ez, 0, prim, o + 12, 3);
        prim[o + 15] = COIN;
        matieres[i] = m;
        decoupe[i] = clip;
    }

    private void capsule(double[] p, double[] q, double rayon, Matiere m, int clip) {
        int i = nombre++;
        type[i] = CAPSULE;
        double[] a = monde(p[0], p[1], p[2]), b = monde(q[0], q[1], q[2]);
        int o = i * PAS_PRIM;
        prim[o] = a[0];
        prim[o + 1] = a[1];
        prim[o + 2] = a[2];
        prim[o + 3] = b[0];
        prim[o + 4] = b[1];
        prim[o + 5] = b[2];
        prim[o + 6] = rayon;
        matieres[i] = m;
        decoupe[i] = clip;
    }

    private double[] monde(double r, double u, double f) {
        double[] q = transformer(r, u, f, true);
        return new double[] { q[0] * droite[0] + q[2] * avant[0], q[1], q[0] * droite[2] + q[2] * avant[2] };
    }

    private final double[] n = new double[3], loc = new double[3];
    private boolean interieur;

    private int[] rendre(long graine, boolean avecCape, int L, int H) {
        int[] image = new int[L * H];
        double[] profondeur = new double[L * H];
        double[] indice = new double[L * H];
        int[] qui = new int[L * H];
        double[] rd = { 0.0, -Math.sin(INCLINAISON), -Math.cos(INCLINAISON) };
        double[] haut = { 0.0, Math.cos(INCLINAISON), -Math.sin(INCLINAISON) };
        double[] ro = new double[3], nm = new double[3], lm = new double[3];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < L; x++) {
                double px = (x + 0.5 - L / 2.0) * METRES_PIXEL;
                double py = (H - ANCRE - y - 0.5) * METRES_PIXEL;
                ro[0] = px - rd[0] * 5.0;
                ro[1] = py * haut[1] - rd[1] * 5.0;
                ro[2] = py * haut[2] - rd[2] * 5.0;
                double meilleur = 1e9;
                int gagnant = -1;
                boolean dedans = false;
                for (int i = 0; i < nombre; i++) {
                    if (!avecCape && decoupe[i] == CAPE) continue;
                    double t = toucher(i, ro, rd);
                    if (t > 0 && t < meilleur) {
                        meilleur = t;
                        gagnant = i;
                        dedans = interieur;
                        System.arraycopy(n, 0, nm, 0, 3);
                        System.arraycopy(loc, 0, lm, 0, 3);
                    }
                }
                int k = y * L + x;
                qui[k] = gagnant;
                if (gagnant < 0) continue;
                profondeur[k] = meilleur;
                Matiere m = matieres[gagnant];
                double lambert = nm[0] * LUMIERE[0] + nm[1] * LUMIERE[1] + nm[2] * LUMIERE[2];
                double v = m.base + m.etendue * (0.7 * lambert + 0.2) + m.eclat * Math.pow(Math.max(0.0, lambert), 6.0);
                v += (hasard(graine, x * 7 + gagnant * 131, y * 3) - 0.5) * 0.25;
                if (dedans) v -= 2.6;
                if (decoupe[gagnant] == VISAGE) {
                    v -= visage(lm[0], lm[1], lm[2]);
                    if (ombreVisage) v -= 1.8;
                }
                if (m.motif == MAILLES) v += ((x + y) & 1) == 0 ? 0.7 : -0.5;
                else if (m.motif == LUEUR) v = m.base + (hasard(graine, x, y) - 0.5) * 0.6;
                v -= Math.max(0.0, 0.45 - py * haut[1]) * 1.2;
                indice[k] = v;
            }
        }
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < L; x++) {
                int k = y * L + x;
                if (qui[k] < 0) continue;
                double t = profondeur[k] - 0.07;
                boolean derriere = (x > 0 && qui[k - 1] >= 0 && profondeur[k - 1] < t)
                        || (x < L - 1 && qui[k + 1] >= 0 && profondeur[k + 1] < t)
                        || (y > 0 && qui[k - L] >= 0 && profondeur[k - L] < t)
                        || (y < H - 1 && qui[k + L] >= 0 && profondeur[k + L] < t);
                double v = indice[k] - (derriere ? 1.8 : 0.0);
                double b = (BAYER[(y & 3) * 4 + (x & 3)] + 0.5) / 16.0 - 0.5;
                int col = Math.max(0, Math.min(7, (int) Math.floor(v + b * 0.45 + 0.5)));
                int alpha = decoupe[qui[k]] == CAPE ? ALPHA_CAPE : 255;
                image[k] = (palette[matieres[qui[k]].rangee][col] << 8) | alpha;
            }
        }
        int[] fini = image.clone();
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < L; x++) {
                int k = y * L + x;
                if (qui[k] >= 0) continue;
                int voisin = -1;
                if (x > 0 && qui[k - 1] >= 0) voisin = qui[k - 1];
                else if (x < L - 1 && qui[k + 1] >= 0) voisin = qui[k + 1];
                else if (y > 0 && qui[k - L] >= 0) voisin = qui[k - L];
                else if (y < H - 1 && qui[k + L] >= 0) voisin = qui[k + L];
                if (voisin < 0) continue;
                int c = palette[matieres[voisin].rangee][0];
                int rr = (int) (((c >> 16) & 255) * 0.55), gg = (int) (((c >> 8) & 255) * 0.55), bb = (int) ((c & 255) * 0.55);
                fini[k] = (rr << 24) | (gg << 16) | (bb << 8) | (decoupe[voisin] == CAPE ? ALPHA_CAPE : 255);
            }
        }
        for (int x = 0; x < L; x++) {
            for (int y = H - ANCRE - 2; y < H - ANCRE + 1; y++) {
                double dx = (x + 0.5 - L / 2.0) / 9.0, dy = (y - (H - ANCRE - 0.5)) / 1.6;
                int k = y * L + x;
                if (fini[k] == 0 && dx * dx + dy * dy < 1.0 && ((x + y) & 1) == 0) fini[k] = 0x00000060;
            }
        }
        return fini;
    }

    private double visage(double x, double y, double z) {
        double ax = Math.abs(x);
        boolean face = z > 0.9;
        boolean profil = ax > 0.9 && z > 0.35;
        double sombre = 0.0;
        if (chaume && y < -0.3 && (face || profil)) sombre += 0.9;
        double u = face ? ax : profil ? 1.0 - (z - 0.35) / 0.65 * 0.8 : 9.0;
        if (u > 1.0) return sombre;
        double oeilBas = -0.1, oeilHaut = 0.12;
        double sourcil = 0.0;
        double bouche = 0.4, boucheBas = -0.6, boucheHaut = -0.4, coinBas = 9, coinHaut = 9, noir = 1.6;
        switch (expression) {
            case Traits.COLERE: sourcil = -0.22; noir = 2.6; boucheBas = -0.62; break;
            case Traits.TRISTE: sourcil = 0.2; bouche = 0.26; coinBas = -0.72; coinHaut = -0.54; break;
            case Traits.JOIE: oeilBas = -0.04; bouche = 0.32; coinBas = -0.46; coinHaut = -0.28; break;
            case Traits.PEUR: oeilBas = -0.18; oeilHaut = 0.22; sourcil = 0.14; bouche = 0.2; boucheBas = -0.76; boucheHaut = -0.36; noir = 3.0; break;
            case Traits.DOULEUR: oeilBas = -0.05; oeilHaut = 0.04; sourcil = 0.22; bouche = 0.42; noir = 2.6; break;
            case DORT: oeilBas = -0.02; oeilHaut = 0.03; noir = 1.0; break;
            case CRI: oeilBas = -0.14; oeilHaut = 0.18; sourcil = -0.18; bouche = 0.3; boucheBas = -0.82; boucheHaut = -0.34; noir = 3.0; break;
            default: break;
        }
        if (u > 0.18 && u < 0.64 && y > oeilBas && y < oeilHaut) return sombre + 2.6;
        if (u > 0.14 && u < 0.7) {
            double pente = sourcil * (0.7 - u) / 0.5;
            if (y > 0.24 + pente && y < 0.42 + pente) return sombre + 1.3;
        }
        if (u < bouche && y > boucheBas && y < boucheHaut) return sombre + noir;
        if (u >= bouche - 0.06 && u < bouche + 0.14 && y > coinBas && y < coinHaut) return sombre + noir;
        return sombre;
    }

    private static final double[][] FACES = faces();
    private static final double[][] PRISME = prisme();

    private static double[][] faces() {
        double[][] f = new double[14][];
        int k = 0;
        for (int axe = 0; axe < 3; axe++) {
            for (int signe = -1; signe <= 1; signe += 2) {
                double[] n = new double[4];
                n[axe] = signe;
                n[3] = 1.0;
                f[k++] = n;
            }
        }
        double r = 1.0 / Math.sqrt(3.0);
        for (int x = -1; x <= 1; x += 2) for (int y = -1; y <= 1; y += 2) for (int z = -1; z <= 1; z += 2) f[k++] = new double[] { x * r, y * r, z * r, COIN };
        return f;
    }

    private static double[][] prisme() {
        double[][] f = new double[8][];
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(30.0 + 60.0 * k);
            f[k] = new double[] { 0.0, Math.cos(a), Math.sin(a) };
        }
        f[6] = new double[] { -1.0, 0.0, 0.0 };
        f[7] = new double[] { 1.0, 0.0, 0.0 };
        return f;
    }

    private int face;
    private boolean sortie;

    private double polyedre(double[][] plans, double[] d, double ox, double oy, double oz, double dx, double dy, double dz, int i) {
        double entree = -1e30, fin = 1e30;
        int faceEntree = -1, faceSortie = -1;
        for (int k = 0; k < plans.length; k++) {
            double[] p = plans[k];
            double den = p[0] * dx + p[1] * dy + p[2] * dz;
            double dist = d[k] - (p[0] * ox + p[1] * oy + p[2] * oz);
            if (Math.abs(den) < 1e-12) {
                if (dist < 0.0) return -1.0;
                continue;
            }
            double t = dist / den;
            if (den < 0.0) {
                if (t > entree) {
                    entree = t;
                    faceEntree = k;
                }
            } else if (t < fin) {
                fin = t;
                faceSortie = k;
            }
            if (entree > fin) return -1.0;
        }
        for (int passe = 0; passe < 2; passe++) {
            double t = passe == 0 ? entree : fin;
            if (t <= 0.0) continue;
            loc[0] = ox + dx * t;
            loc[1] = oy + dy * t;
            loc[2] = oz + dz * t;
            if (i >= 0 && !garder(decoupe[i], loc[0], loc[1], loc[2], i)) continue;
            face = passe == 0 ? faceEntree : faceSortie;
            sortie = passe == 1;
            return t;
        }
        return -1.0;
    }

    private final double[] distances = new double[14], distancesPrisme = new double[8];

    private double toucher(int i, double[] ro, double[] rd) {
        int o = i * PAS_PRIM;
        interieur = false;
        if (type[i] == CAPSULE) {
            double ax = prim[o], ay = prim[o + 1], az = prim[o + 2];
            double ux = prim[o + 3] - ax, uy = prim[o + 4] - ay, uz = prim[o + 5] - az;
            double longueur = Math.sqrt(ux * ux + uy * uy + uz * uz);
            double r = prim[o + 6];
            double mx = ax + ux * 0.5 - ro[0], my = ay + uy * 0.5 - ro[1], mz = az + uz * 0.5 - ro[2];
            double proj = mx * rd[0] + my * rd[1] + mz * rd[2];
            double borne = longueur * 0.5 + r * 1.6;
            if (mx * mx + my * my + mz * mz - proj * proj > borne * borne) return -1.0;
            ux /= longueur;
            uy /= longueur;
            uz /= longueur;
            double vx = uy * 0.0 - uz * 1.0, vy = uz * 0.0 - ux * 0.0, vz = ux * 1.0 - uy * 0.0;
            double lv = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (lv < 0.3) {
                vx = uy * avant[2] - uz * avant[1];
                vy = uz * avant[0] - ux * avant[2];
                vz = ux * avant[1] - uy * avant[0];
                lv = Math.sqrt(vx * vx + vy * vy + vz * vz);
            }
            vx /= lv;
            vy /= lv;
            vz /= lv;
            double wx = uy * vz - uz * vy, wy = uz * vx - ux * vz, wz = ux * vy - uy * vx;
            double px = ro[0] - ax, py = ro[1] - ay, pz = ro[2] - az;
            for (int k = 0; k < 6; k++) distancesPrisme[k] = r;
            distancesPrisme[6] = r * 0.45;
            distancesPrisme[7] = longueur + r * 0.45;
            double t = polyedre(PRISME, distancesPrisme, px * ux + py * uy + pz * uz, px * vx + py * vy + pz * vz, px * wx + py * wy + pz * wz,
                    rd[0] * ux + rd[1] * uy + rd[2] * uz, rd[0] * vx + rd[1] * vy + rd[2] * vz, rd[0] * wx + rd[1] * wy + rd[2] * wz, -1);
            if (t <= 0.0) return -1.0;
            double[] f = PRISME[face];
            double sens = sortie ? -1.0 : 1.0;
            n[0] = (f[0] * ux + f[1] * vx + f[2] * wx) * sens;
            n[1] = (f[0] * uy + f[1] * vy + f[2] * wy) * sens;
            n[2] = (f[0] * uz + f[1] * vz + f[2] * wz) * sens;
            interieur = sortie;
            return t;
        }
        double a = prim[o + 3], b = prim[o + 4], c = prim[o + 5];
        double ox = ro[0] - prim[o], oy = ro[1] - prim[o + 1], oz = ro[2] - prim[o + 2];
        double proj = -(ox * rd[0] + oy * rd[1] + oz * rd[2]);
        double borne = Math.max(a, Math.max(b, c)) * 1.75;
        if (ox * ox + oy * oy + oz * oz - proj * proj > borne * borne) return -1.0;
        double lox = (ox * prim[o + 6] + oy * prim[o + 7] + oz * prim[o + 8]) / a;
        double loy = (ox * prim[o + 9] + oy * prim[o + 10] + oz * prim[o + 11]) / b;
        double loz = (ox * prim[o + 12] + oy * prim[o + 13] + oz * prim[o + 14]) / c;
        double ldx = (rd[0] * prim[o + 6] + rd[1] * prim[o + 7] + rd[2] * prim[o + 8]) / a;
        double ldy = (rd[0] * prim[o + 9] + rd[1] * prim[o + 10] + rd[2] * prim[o + 11]) / b;
        double ldz = (rd[0] * prim[o + 12] + rd[1] * prim[o + 13] + rd[2] * prim[o + 14]) / c;
        for (int k = 0; k < FACES.length; k++) distances[k] = k < 6 ? 1.0 : prim[o + 15];
        double t = polyedre(FACES, distances, lox, loy, loz, ldx, ldy, ldz, i);
        if (t <= 0.0) return -1.0;
        double[] f = FACES[face];
        double gx = f[0] / a, gy = f[1] / b, gz = f[2] / c;
        double nx = gx * prim[o + 6] + gy * prim[o + 9] + gz * prim[o + 12];
        double ny = gx * prim[o + 7] + gy * prim[o + 10] + gz * prim[o + 13];
        double nz = gx * prim[o + 8] + gy * prim[o + 11] + gz * prim[o + 14];
        double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
        double sens = sortie ? -1.0 : 1.0;
        n[0] = nx / l * sens;
        n[1] = ny / l * sens;
        n[2] = nz / l * sens;
        interieur = sortie;
        return t;
    }

    private boolean garder(int clip, double x, double y, double z, int i) {
        switch (clip) {
            case CHEVEUX:
                return y > 0.12 + 0.3 * z;
            case BARBE:
                return z > 0.1 && y < 0.3;
            case CAPUCHE:
                if (z > 0.25 && y < 0.55 && Math.abs(x) < 0.62) return false;
                return y > -0.55;
            case CAPE: {
                double angle = Math.atan2(x, -z);
                double dechire = 0.16 * hasard(i, (int) Math.floor(angle * 7.0 + 20.0), 3) + 0.05 * Math.sin(angle * 9.0 + phaseCape);
                return z < 0.2 && y < 0.92 && y > -0.32 + dechire;
            }
            case CASQUE:
                return y > 0.1 || (z < 0.35 && y > -0.62);
            case JUPE: {
                double dechire = 0.12 * hasard(i, (int) Math.floor((x + 2.0) * 10.0 + z * 4.0), 7);
                return y < 0.55 && y > -0.95 + dechire;
            }
            default:
                return true;
        }
    }

    private static double hasard(long graine, int a, int b) {
        long x = graine * 0x9E3779B97F4A7C15L + a * 0xBF58476D1CE4E5B9L + b * 0x94D049BB133111EBL;
        x ^= x >>> 31;
        x *= 0xD6E8FEB86659FD93L;
        x ^= x >>> 32;
        return (x >>> 11) * 0x1.0p-53;
    }

    private static double[] normer(double x, double y, double z) {
        double l = Math.sqrt(x * x + y * y + z * z);
        return new double[] { x / l, y / l, z / l };
    }
}
