package com.deldaran.politics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Batiments {
    public static final double METRES_PIXEL = Peintre.METRES_PIXEL;
    private static final double INCLINAISON = RenduTuiles.ANGLE_CAMERA;
    private static final double[] LUMIERE = normer(-0.62, 0.68, 0.38);
    private static final int[] BAYER = { 0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5 };
    private static final int CACHE_MAX = 4096;
    private static final int ENVOIS_PAR_IMAGE = 6;
    private static final double OMBRE_PORTEE = 5.0;
    private static final int TROU = 1000;

    public static final int CHAUMIERE = 0, COLOMBAGES = 1, PIERRE = 2, RONDINS = 3, LONGERE = 4, HALLE = 5;
    public static final int ARCHE = 6, PONTON = 7, TONNEAUX = 8, CAISSES = 9, BARRIERE = 10, PORTAIL = 11, PUITS = 12, ETAL = 13, TABLEAU = 14;
    /** Le mobilier des campements de brigands. */
    public static final int TENTE = 15, FEU = 16;
    /** Lot G3 : la roue à aubes et les ailes d'un moulin, objets à part qui tournent ; PHASES_MOULIN positions peintes chacun. */
    public static final int ROUE_EAU = 17, AILES = 18, PHASES_MOULIN = 4;
    /** Lot G3 : un loup (variante = pas + 4 × pelage : 4 pas, 2 pelages). */
    public static final int LOUP = 19, PAS_LOUP = 4;
    /** Rangées de la palette (palettes/sol.png) : 0-15 le monde d'origine ; 16 feu, 17 sang, 18 or terni, 19 pourpre, 20 indigo,
     *  21 lueur spectrale, 22 bile, 23 os et parchemin. Chaque rangée : 8 teintes, de la plus sombre à la plus claire. */
    public static final int RANGEES = 24;
    public static final int R_FEU = 16, R_SANG = 17, R_OR = 18, R_POURPRE = 19, R_INDIGO = 20, R_SPECTRE = 21, R_BILE = 22, R_OS = 23;
    public static final int ANGLES = 16;
    private static final double TOUR_AVANT = 2.9, TOUR_ARRIERE = 0.3, TOUR_DEMI = 1.6;
    private static final int UNI = 0, BLOCS = 1, PLANCHES = 2, TRONCS = 3, CHAUME = 4, TUILES = 5, CREPI = 6, VITRE = 7, INTERIEUR = 8;

    public static class Plan {
        public int style;
        public double largeur, profondeur, murs, pente, debord;
        public double angle;
        public boolean etage, cheminee, appentis, bois, tonneau, tour;
        public double usure, porteX, porteL = 0.55, porteH = 1.95;
        public long graine;
        public int metier;
        public boolean roue;
        public boolean ruine, delabre;
        public double age;
        /** Lot G3 : la position de rotation d'une roue ou des ailes (0 à PHASES_MOULIN − 1). */
        public int phase;
        /** Lot G5 : un chantier du joueur (peint comme des murs à demi montés) ; ce n'est pas une ruine où l'on peut bâtir. */
        public boolean chantier;
        /** Lot H2 : un bâtiment du joueur (terrain libre ou ruine rebâtie) : ses ouvriers y travaillent. */
        public boolean joueur;

        public Plan ruiner() {
            return ruiner(false, 0.0);
        }

        public Plan ruiner(boolean delabre, double age) {
            Plan q = new Plan();
            q.style = style;
            q.largeur = largeur;
            q.profondeur = profondeur;
            q.murs = murs;
            q.pente = pente;
            q.debord = debord;
            q.angle = angle;
            q.etage = etage;
            q.cheminee = cheminee;
            q.appentis = appentis;
            q.bois = bois;
            q.tonneau = tonneau;
            q.tour = tour;
            q.usure = usure;
            q.porteX = porteX;
            q.porteL = porteL;
            q.porteH = porteH;
            q.graine = graine;
            q.metier = metier;
            q.roue = roue;
            q.ruine = true;
            q.delabre = delabre;
            q.age = age;
            return q;
        }

        public void orienter(double yaw) {
            double pas = 2.0 * Math.PI / ANGLES;
            angle = Math.floorMod(Math.round(yaw / pas), ANGLES) * pas;
        }

        public long cle() {
            return graine * 0x9E3779B97F4A7C15L + Math.round(angle * 4096.0) * 7919L + style + metier * 104729L + (roue ? 15485863L : 0L) + (ruine ? 0x5DEECE66DL : 0L) + (delabre ? 0x2545F491L : 0L) + Math.round(Math.min(age, 400.0) / 40.0) * 0x9E3779L;
        }

        public double annexe() {
            if (metier == Metiers.BOULANGERIE) return 1.8;
            if (roue) return 0.7;
            return appentis ? 1.6 : 0.0;
        }

        public double avant() {
            return tour ? TOUR_AVANT : 0.0;
        }

        public double rayon() {
            return Math.hypot(largeur * 0.5 + (tour ? TOUR_AVANT : 0.0) + 0.6, profondeur * 0.5 + 0.6);
        }

        public boolean bloque(double est, double nord, double rayon) {
            if (style == ARCHE || style == PONTON || style == PORTAIL || style == AILES || style == LOUP) return false;
            double c = Math.cos(angle), s = Math.sin(angle);
            double sud = -nord;
            double x = est * c - sud * s, z = est * s + sud * c;
            double dx = largeur / 2.0, dz = profondeur / 2.0;
            if (Math.abs(x) < dx + rayon && Math.abs(z) < dz + rayon) return true;
            if (tour && x > -dx - TOUR_AVANT - rayon && x < -dx + TOUR_ARRIERE && Math.abs(z) < TOUR_DEMI + rayon) return true;
            if (appentis && x > dx - rayon && x < dx + 1.6 + rayon && z > -dz * 0.7 - rayon && z < dz * 0.6 + rayon) return true;
            if (bois && x > -dx - 0.9 - rayon && x < -dx && Math.abs(z + dz * 0.5) < 0.45 + rayon) return true;
            if (metier == Metiers.BOULANGERIE && x > -dx - 1.75 - rayon && x < -dx && Math.abs(z) < 0.9 + rayon) return true;
            if (roue && x > dx - rayon && x < dx + 0.6 + rayon && Math.abs(z) < 1.7 + rayon) return true;
            if (tonneau) {
                double tx = (porteX >= 0 ? -1 : 1) * Math.min(dx - 0.5, Math.abs(porteX) + 1.0), tz = dz + 0.45;
                double ex = x - tx, ez = z - tz;
                if (ex * ex + ez * ez < (0.32 + rayon) * (0.32 + rayon)) return true;
            }
            return false;
        }

        public double demiX() {
            return (Math.abs(Math.cos(angle)) * largeur + Math.abs(Math.sin(angle)) * profondeur) * 0.5;
        }

        public double demiZ() {
            return (Math.abs(Math.sin(angle)) * largeur + Math.abs(Math.cos(angle)) * profondeur) * 0.5;
        }
    }

    public static class Image {
        public final Texture texture;
        public final int ancreX, ancreY;

        Image(Texture texture, int ancreX, int ancreY) {
            this.texture = texture;
            this.ancreX = ancreX;
            this.ancreY = ancreY;
        }
    }

    public static class Rendu {
        public int[] pixels;
        public int largeur, hauteur, ancreX, ancreY;
    }

    private static class Matiere {
        final int rangee;
        final double base, etendue, eclat;
        final int motif;

        Matiere(int rangee, double base, double etendue, double eclat, int motif) {
            this.rangee = rangee;
            this.base = base;
            this.etendue = etendue;
            this.eclat = eclat;
            this.motif = motif;
        }
    }

    private static class Solide {
        double[][] n;
        double[] d;
        double[] min = new double[3], max = new double[3];
        Matiere m;
        Matiere dessus;
        double[][] trous;
    }

    private final int[][] palette = new int[RANGEES][8];
    private final Map<Long, Image> cache = new LinkedHashMap<>(64, 0.75f, true);
    private final Map<Long, Future<Rendu>> enCours = new HashMap<>();
    private ExecutorService ouvrier;
    private ThreadLocal<Batiments> ateliers;
    private long imageCourante = -1;
    private int envois;
    public int manquants;

    public Batiments() {
        Pixmap p = new Pixmap(Gdx.files.internal("palettes/sol.png"));
        for (int r = 0; r < RANGEES; r++) for (int c = 0; c < 8; c++) palette[r][c] = p.getPixel(c, r) >>> 8;
        p.dispose();
        ateliers = ThreadLocal.withInitial(() -> new Batiments(palette));
        ouvrier = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2), r -> {
            Thread t = new Thread(r, "architecte");
            t.setDaemon(true);
            return t;
        });
    }

    public Batiments(int[][] palette) {
        for (int r = 0; r < RANGEES; r++) System.arraycopy(palette[r], 0, this.palette[r], 0, 8);
    }

    public Image image(Plan plan) {
        long graine = plan.cle();
        Image im = cache.get(graine);
        if (im != null) return im;
        Future<Rendu> travail = enCours.get(graine);
        if (travail == null) {
            enCours.put(graine, ouvrier.submit(() -> ateliers.get().peindre(plan)));
            return null;
        }
        if (!travail.isDone()) return null;
        long frame = Gdx.graphics.getFrameId();
        if (frame != imageCourante) {
            imageCourante = frame;
            envois = 0;
        }
        if (envois >= ENVOIS_PAR_IMAGE) return null;
        envois++;
        enCours.remove(graine);
        Rendu r;
        try {
            r = travail.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Pixmap p = new Pixmap(r.largeur, r.hauteur, Pixmap.Format.RGBA8888);
        ByteBuffer octets = p.getPixels();
        for (int k = 0; k < r.pixels.length; k++) {
            int v = r.pixels[k];
            octets.put(k * 4, (byte) (v >>> 24));
            octets.put(k * 4 + 1, (byte) (v >>> 16));
            octets.put(k * 4 + 2, (byte) (v >>> 8));
            octets.put(k * 4 + 3, (byte) v);
        }
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        p.dispose();
        im = new Image(t, r.ancreX, r.ancreY);
        cache.put(graine, im);
        if (cache.size() > CACHE_MAX) {
            Iterator<Map.Entry<Long, Image>> it = cache.entrySet().iterator();
            it.next().getValue().texture.dispose();
            it.remove();
        }
        return im;
    }

    public static Plan plan(long graine) {
        Random r = new Random(graine * 0x2545F4914F6CDD1DL + 3L);
        Plan p = new Plan();
        p.graine = graine;
        double t = r.nextDouble();
        p.style = t < 0.3 ? CHAUMIERE : t < 0.55 ? COLOMBAGES : t < 0.75 ? PIERRE : t < 0.9 ? RONDINS : LONGERE;
        switch (p.style) {
            case CHAUMIERE:
                p.largeur = 5.0 + r.nextDouble() * 2.0;
                p.profondeur = 4.0 + r.nextDouble() * 1.2;
                p.murs = 2.4 + r.nextDouble() * 0.3;
                p.pente = Math.toRadians(38 + r.nextInt(6));
                p.debord = 0.55;
                break;
            case COLOMBAGES:
                p.largeur = 5.5 + r.nextDouble() * 2.5;
                p.profondeur = 4.5 + r.nextDouble() * 1.5;
                p.etage = r.nextFloat() < 0.6f;
                p.murs = p.etage ? 5.0 : 2.9;
                p.pente = Math.toRadians(36 + r.nextInt(6));
                p.debord = 0.35;
                break;
            case PIERRE:
                p.largeur = 5.5 + r.nextDouble() * 3.0;
                p.profondeur = 4.5 + r.nextDouble() * 1.5;
                p.etage = r.nextFloat() < 0.35f;
                p.murs = p.etage ? 4.8 : 3.0;
                p.pente = Math.toRadians(30 + r.nextInt(6));
                p.debord = 0.25;
                break;
            case RONDINS:
                p.largeur = 4.5 + r.nextDouble() * 2.0;
                p.profondeur = 4.0 + r.nextDouble() * 1.0;
                p.murs = 2.6;
                p.pente = Math.toRadians(30 + r.nextInt(6));
                p.debord = 0.45;
                break;
            default:
                p.largeur = 8.0 + r.nextDouble() * 1.5;
                p.profondeur = 4.5 + r.nextDouble() * 0.8;
                p.murs = 2.8;
                p.pente = Math.toRadians(34 + r.nextInt(6));
                p.debord = 0.35;
                break;
        }
        p.cheminee = p.style != CHAUMIERE ? r.nextFloat() < 0.75f : r.nextFloat() < 0.35f;
        p.appentis = r.nextFloat() < 0.3f && p.largeur < 8.0;
        p.bois = r.nextFloat() < 0.4f;
        p.tonneau = r.nextFloat() < 0.35f;
        p.porteX = p.largeur > 6.0 ? (r.nextBoolean() ? -1 : 1) * (p.largeur * 0.5 * 0.35) : 0.0;
        double u = r.nextDouble();
        p.usure = u < 0.55 ? u * 0.4 : 0.25 + (u - 0.55) * 1.6;
        return p;
    }

    private static void regler(Plan p, int style, double largeur, double profondeur, boolean etage) {
        p.style = style;
        p.largeur = largeur;
        p.profondeur = profondeur;
        p.etage = etage;
        switch (style) {
            case CHAUMIERE:
                p.murs = 2.5;
                p.pente = Math.toRadians(40);
                p.debord = 0.55;
                break;
            case COLOMBAGES:
                p.murs = etage ? 5.0 : 2.9;
                p.pente = Math.toRadians(38);
                p.debord = 0.35;
                break;
            case RONDINS:
                p.murs = 2.7;
                p.pente = Math.toRadians(33);
                p.debord = 0.45;
                break;
            case LONGERE:
                p.murs = 3.0;
                p.pente = Math.toRadians(36);
                p.debord = 0.35;
                break;
            default:
                p.murs = etage ? 4.9 : 3.1;
                p.pente = Math.toRadians(33);
                p.debord = 0.25;
                break;
        }
    }

    public static Plan planMetier(long graine, int metier, boolean roue) {
        Plan p = plan(graine);
        Random r = new Random(graine * 0x9E3779B97F4A7C15L + metier * 7919L);
        p.metier = metier;
        p.roue = roue;
        p.usure = Math.min(p.usure, 0.3);
        p.appentis = false;
        p.bois = false;
        p.tonneau = false;
        p.tour = false;
        p.cheminee = true;
        switch (metier) {
            case Metiers.MAIRIE:
                regler(p, PIERRE, 10.0 + r.nextDouble() * 2.0, 7.0, true);
                p.porteL = 0.8;
                p.porteH = 2.4;
                break;
            case Metiers.BOULANGERIE:
                regler(p, r.nextBoolean() ? COLOMBAGES : PIERRE, 6.0 + r.nextDouble(), 5.0, r.nextBoolean());
                break;
            case Metiers.BOUCHERIE:
                regler(p, COLOMBAGES, 6.0 + r.nextDouble(), 5.0, true);
                break;
            case Metiers.MOULIN:
                if (roue) regler(p, PIERRE, 6.5, 5.0, true);
                else {
                    regler(p, PIERRE, 4.6, 4.6, true);
                    p.murs = 7.0;
                    p.pente = Math.toRadians(50);
                }
                p.cheminee = false;
                break;
            case Metiers.GRENIER:
                regler(p, LONGERE, 10.0 + r.nextDouble() * 2.0, 6.0, false);
                p.murs = 3.8;
                p.porteL = 1.2;
                p.porteH = 2.9;
                p.cheminee = false;
                break;
            case Metiers.FORGE:
                regler(p, PIERRE, 6.0 + r.nextDouble(), 5.0, false);
                p.appentis = true;
                break;
            case Metiers.MENUISERIE:
                regler(p, RONDINS, 7.0 + r.nextDouble(), 5.0, false);
                p.appentis = true;
                p.bois = true;
                break;
            case Metiers.TANNERIE:
                regler(p, r.nextBoolean() ? CHAUMIERE : RONDINS, 7.0, 5.0, false);
                break;
            case Metiers.TISSERAND:
                regler(p, COLOMBAGES, 6.5 + r.nextDouble(), 5.0, true);
                break;
            case Metiers.POTIER:
                regler(p, CHAUMIERE, 6.0 + r.nextDouble(), 5.0, false);
                break;
            case Metiers.AUBERGE:
                regler(p, COLOMBAGES, 10.5 + r.nextDouble() * 1.5, 7.0, true);
                p.murs = 5.4;
                p.tonneau = true;
                p.porteL = 0.7;
                break;
            case Metiers.TAVERNE:
                regler(p, r.nextBoolean() ? COLOMBAGES : PIERRE, 8.0 + r.nextDouble(), 6.0, r.nextBoolean());
                p.tonneau = true;
                break;
            case Metiers.ENTREPOT:
                regler(p, r.nextBoolean() ? PIERRE : RONDINS, 10.0, 7.0, false);
                p.murs = 4.0;
                p.porteL = 1.3;
                p.porteH = 3.0;
                p.cheminee = false;
                break;
            case Metiers.PECHERIE:
                regler(p, RONDINS, 6.0, 4.5, false);
                p.cheminee = false;
                p.tonneau = true;
                break;
            case Metiers.CASERNE:
                regler(p, PIERRE, 13.0, 7.0, true);
                p.murs = 5.4;
                p.porteL = 0.9;
                p.porteH = 2.6;
                break;
            case Metiers.ECOLE:
                regler(p, COLOMBAGES, 9.0, 6.0, true);
                break;
            case Metiers.BAINS:
                regler(p, PIERRE, 10.0, 7.0, false);
                p.murs = 3.6;
                break;
            case Metiers.CHATEAU:
                regler(p, PIERRE, 15.0, 10.0, true);
                p.murs = 7.5;
                p.tour = true;
                p.porteL = 1.1;
                p.porteH = 3.0;
                break;
            default:
                break;
        }
        p.porteX = p.largeur > 6.0 && p.porteL < 0.8 ? (r.nextBoolean() ? -1 : 1) * (p.largeur * 0.5 * 0.35) : 0.0;
        return p;
    }

    public static Plan objet(int style, double largeur, double profondeur, double murs, double angle, int variante) {
        Plan p = new Plan();
        p.style = style;
        p.largeur = Math.round(largeur * 10.0) / 10.0;
        p.profondeur = Math.round(profondeur * 10.0) / 10.0;
        p.murs = Math.round(murs * 10.0) / 10.0;
        p.angle = angle;
        p.phase = variante;
        long g = style * 1000003L + Math.round(p.largeur * 10.0) * 7919L + Math.round(p.profondeur * 10.0) * 104729L + Math.round(p.murs * 10.0) * 1299709L + variante * 15485863L;
        g ^= g >>> 33;
        g *= 0xff51afd7ed558ccdL;
        g ^= g >>> 33;
        p.graine = g;
        return p;
    }

    public static Plan planCentral(long graine, int maisons) {
        Random r = new Random(graine * 0x2545F4914F6CDD1DL + 11L);
        Plan p = new Plan();
        p.graine = graine;
        p.style = HALLE;
        double t = Math.min(1.0, maisons / 120.0);
        p.largeur = 9.0 + 5.0 * t + r.nextDouble();
        p.profondeur = 6.5 + 2.0 * t + r.nextDouble() * 0.5;
        p.murs = 4.5 + 1.5 * t;
        p.pente = Math.toRadians(42 + r.nextInt(5));
        p.debord = 0.35;
        p.tour = maisons >= 12;
        p.porteL = 0.9;
        p.porteH = 2.6;
        p.usure = r.nextDouble() * 0.2;
        return p;
    }

    private final List<Solide> solides = new ArrayList<>();
    private Matiere sombre;

    public Rendu peindre(Plan p) {
        long graine = p.graine;
        Random r = new Random(graine ^ 0x6C8E9CF570932BD5L);
        solides.clear();
        construire(p, r);
        double yaw = p.angle + RenduTuiles.ROTATION;
        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        double[] mn = { 1e9, 1e9, 1e9 }, mx = { -1e9, -1e9, -1e9 };
        for (Solide s : solides) {
            for (int k = 0; k < 3; k++) {
                mn[k] = Math.min(mn[k], s.min[k]);
                mx[k] = Math.max(mx[k], s.max[k]);
            }
        }
        double ci = Math.cos(INCLINAISON), si = Math.sin(INCLINAISON);
        double minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
        double portee = Math.min(OMBRE_PORTEE * 2.0, mx[1] / LUMIERE[1]);
        for (int k = 0; k < 16; k++) {
            double lx = (k & 1) == 0 ? mn[0] : mx[0];
            double ly = (k & 2) == 0 ? 0.0 : mx[1];
            double lz = (k & 4) == 0 ? mn[2] : mx[2];
            double[] w = versMonde(lx, ly, lz, cy, sy);
            if ((k & 8) != 0) {
                double t = Math.min(portee, ly / LUMIERE[1]);
                w[0] -= LUMIERE[0] * t;
                w[1] = 0.0;
                w[2] -= LUMIERE[2] * t;
            }
            double px = w[0], py = w[1] * ci - w[2] * si;
            minX = Math.min(minX, px);
            maxX = Math.max(maxX, px);
            minY = Math.min(minY, py);
            maxY = Math.max(maxY, py);
        }
        int largeur = (int) Math.ceil((maxX - minX) / METRES_PIXEL) + 4;
        int hauteur = (int) Math.ceil((maxY - minY) / METRES_PIXEL) + 4;
        minX -= 2 * METRES_PIXEL;
        maxY += 2 * METRES_PIXEL;
        double[] rdW = { 0.0, -si, -ci };
        double[] haut = { 0.0, ci, -si };
        double[] rd = versLocal(rdW[0], rdW[1], rdW[2], cy, sy);
        double[] lumiere = versLocal(LUMIERE[0], LUMIERE[1], LUMIERE[2], cy, sy);
        int n = largeur * hauteur;
        double[] profondeur = new double[n];
        double[] indice = new double[n];
        int[] qui = new int[n];
        int[] rangees = new int[n];
        boolean[] ombre = new boolean[n];
        double[] hit = new double[3];
        double[] ro = new double[3];
        int[] face = new int[1];
        for (int j = 0; j < hauteur; j++) {
            for (int i = 0; i < largeur; i++) {
                int k = j * largeur + i;
                double px = minX + (i + 0.5) * METRES_PIXEL;
                double py = maxY - (j + 0.5) * METRES_PIXEL;
                double[] o = versLocal(px - rdW[0] * 40.0, py * haut[1] - rdW[1] * 40.0, py * haut[2] - rdW[2] * 40.0, cy, sy);
                double meilleur = 1e9;
                int gagnant = -1, faceGagnante = -1;
                for (int s = 0; s < solides.size(); s++) {
                    double t = toucher(solides.get(s), o, rd, face);
                    if (t > 0 && t < meilleur) {
                        meilleur = t;
                        gagnant = s;
                        faceGagnante = face[0];
                    }
                }
                qui[k] = gagnant;
                if (gagnant < 0) {
                    if (rd[1] < 0) {
                        double t = -o[1] / rd[1];
                        ro[0] = o[0] + rd[0] * t;
                        ro[1] = 0.0;
                        ro[2] = o[2] + rd[2] * t;
                        if (dansOmbre(ro, lumiere)) ombre[k] = true;
                    }
                    continue;
                }
                profondeur[k] = meilleur;
                Solide s = solides.get(gagnant);
                boolean troue = faceGagnante >= TROU;
                double[] nl = s.n[troue ? faceGagnante - TROU : faceGagnante];
                hit[0] = o[0] + rd[0] * meilleur;
                hit[1] = o[1] + rd[1] * meilleur;
                hit[2] = o[2] + rd[2] * meilleur;
                Matiere m = troue ? sombre : nl[1] > 0.95 && s.dessus != null ? s.dessus : s.m;
                double lambert = nl[0] * lumiere[0] + nl[1] * lumiere[1] + nl[2] * lumiere[2];
                double v = m.base + m.etendue * (0.7 * lambert + 0.2) + m.eclat * Math.pow(Math.max(0.0, lambert), 6.0);
                int rangee = m.rangee;
                double[] motif = motif(m, nl, hit, graine, p);
                v += motif[0];
                if (motif[1] >= 0) rangee = (int) motif[1];
                if (troue) {
                    rangee = 9;
                    double chevron = (Math.abs(nl[0]) > Math.abs(nl[2]) ? hit[2] : hit[0]) / 0.55;
                    v = chevron - Math.floor(chevron) < 0.22 ? 1.3 + lambert : -0.6;
                }
                v += (hasard(graine, i * 7 + gagnant * 131, j * 3) - 0.5) * 0.3;
                v -= Math.max(0.0, 0.4 - hit[1]) * 1.6;
                if (Math.abs(nl[1]) < 0.3 && hit[1] < p.murs && hit[1] > p.murs - 0.35 && m.motif != VITRE) v -= 0.9;
                if (m != sombre && dansOmbre(hit, lumiere, gagnant)) v -= 1.6;
                indice[k] = v;
                rangees[k] = rangee;
            }
        }
        int[] image = new int[n];
        for (int j = 0; j < hauteur; j++) {
            for (int i = 0; i < largeur; i++) {
                int k = j * largeur + i;
                if (qui[k] < 0) {
                    if (ombre[k] && ((i + j) & 1) == 0) image[k] = 0x00000070;
                    else if (ombre[k]) image[k] = 0x00000048;
                    continue;
                }
                double t = profondeur[k] - 0.25;
                boolean derriere = (i > 0 && qui[k - 1] >= 0 && profondeur[k - 1] < t)
                        || (i < largeur - 1 && qui[k + 1] >= 0 && profondeur[k + 1] < t)
                        || (j > 0 && qui[k - largeur] >= 0 && profondeur[k - largeur] < t)
                        || (j < hauteur - 1 && qui[k + largeur] >= 0 && profondeur[k + largeur] < t);
                double v = indice[k] - (derriere ? 1.4 : 0.0);
                double b = (BAYER[(j & 3) * 4 + (i & 3)] + 0.5) / 16.0 - 0.5;
                int col = Math.max(0, Math.min(7, (int) Math.floor(v + b * 0.45 + 0.5)));
                image[k] = (palette[rangees[k]][col] << 8) | 255;
            }
        }
        int[] fini = image.clone();
        for (int j = 0; j < hauteur; j++) {
            for (int i = 0; i < largeur; i++) {
                int k = j * largeur + i;
                if (qui[k] >= 0) continue;
                int voisin = -1;
                if (i > 0 && qui[k - 1] >= 0) voisin = k - 1;
                else if (i < largeur - 1 && qui[k + 1] >= 0) voisin = k + 1;
                else if (j > 0 && qui[k - largeur] >= 0) voisin = k - largeur;
                else if (j < hauteur - 1 && qui[k + largeur] >= 0) voisin = k + largeur;
                if (voisin < 0) continue;
                int c = palette[rangees[voisin]][0];
                int rr = (int) (((c >> 16) & 255) * 0.5), gg = (int) (((c >> 8) & 255) * 0.5), bb = (int) ((c & 255) * 0.5);
                fini[k] = (rr << 24) | (gg << 16) | (bb << 8) | 255;
            }
        }
        Rendu rendu = new Rendu();
        rendu.pixels = fini;
        rendu.largeur = largeur;
        rendu.hauteur = hauteur;
        rendu.ancreX = (int) Math.round(-minX / METRES_PIXEL);
        rendu.ancreY = (int) Math.round(hauteur - maxY / METRES_PIXEL);
        return rendu;
    }

    private boolean dansOmbre(double[] p, double[] lumiere) {
        return dansOmbre(p, lumiere, -1);
    }

    private final int[] faceOmbre = new int[1];

    private boolean dansOmbre(double[] p, double[] lumiere, int ignore) {
        double[] o = { p[0] + lumiere[0] * 0.02, p[1] + lumiere[1] * 0.02, p[2] + lumiere[2] * 0.02 };
        for (int s = 0; s < solides.size(); s++) {
            if (s == ignore) continue;
            if (toucher(solides.get(s), o, lumiere, faceOmbre) > 0) return true;
        }
        return false;
    }

    private double[] motif(Matiere m, double[] n, double[] p, long graine, Plan plan) {
        double u, v;
        boolean toit = n[1] > 0.3 && n[1] < 0.95;
        if (Math.abs(n[1]) > 0.95) {
            u = p[0];
            v = p[2];
        } else if (toit) {
            u = Math.abs(n[0]) > Math.abs(n[2]) ? p[2] : p[0];
            v = p[1] / Math.max(0.2, Math.sqrt(1.0 - n[1] * n[1]));
        } else {
            u = Math.abs(n[0]) > Math.abs(n[2]) ? p[2] : p[0];
            v = p[1];
        }
        double dv = 0.0;
        int rangee = -1;
        switch (m.motif) {
            case BLOCS: {
                double h = 0.3;
                int ligne = (int) Math.floor(v / h);
                double decal = (ligne & 1) * 0.27 + hasard(graine, ligne, 5) * 0.2;
                double l = 0.42 + hasard(graine, ligne, 9) * 0.18;
                int col = (int) Math.floor((u + decal) / l);
                double fu = (u + decal) / l - col, fv = v / h - ligne;
                if (fv < 0.14 || fu < 0.07) dv -= 1.7;
                else dv += (hasard(graine, ligne * 31 + col, 11) - 0.5) * 1.1 + (fv > 0.8 ? 0.4 : 0.0);
                break;
            }
            case PLANCHES: {
                double l = 0.2;
                int col = (int) Math.floor(u / l);
                double fu = u / l - col;
                if (fu < 0.12) dv -= 1.5;
                else dv += (hasard(graine, col, 13) - 0.5) * 0.9 + (hasard(graine, col, (int) Math.floor(v / 0.07)) - 0.5) * 0.35;
                break;
            }
            case TRONCS: {
                double h = 0.26;
                int ligne = (int) Math.floor(v / h);
                double f = v / h - ligne;
                dv += 0.7 - Math.abs(f - 0.55) * 3.2 + (hasard(graine, ligne, (int) Math.floor(u / 0.15)) - 0.5) * 0.5;
                break;
            }
            case CHAUME: {
                double h = 0.32;
                int ligne = (int) Math.floor(v / h);
                double f = v / h - ligne;
                dv += (hasard(graine, (int) Math.floor(u / 0.047), ligne * 7 + (int) Math.floor(f * 3.0)) - 0.5) * 1.3;
                if (f < 0.16) dv -= 1.1;
                if (bruit(u * 2.2, v * 2.6, graine) * 0.7 + hasard(graine, (int) Math.floor(u / 0.1), (int) Math.floor(v / 0.1)) * 0.3 > 0.7 - plan.usure * 0.1) rangee = 8;
                break;
            }
            case TUILES: {
                double h = 0.2;
                int ligne = (int) Math.floor(v / h);
                double decal = (ligne & 1) * 0.15;
                int col = (int) Math.floor((u + decal) / 0.3);
                double fv = v / h - ligne, fu = (u + decal) / 0.3 - col;
                if (fv < 0.2) dv -= 1.4;
                else if (fu < 0.08) dv -= 0.9;
                else dv += (hasard(graine, ligne * 57 + col, 17) - 0.5) * 0.9;
                if (bruit(u * 2.4, v * 3.0, graine + 9) * 0.65 + hasard(graine, (int) Math.floor(u / 0.08), (int) Math.floor(v / 0.08)) * 0.35 > 0.74 - plan.usure * 0.12 && fv > 0.2) rangee = 8;
                break;
            }
            case CREPI: {
                dv += (hasard(graine, (int) Math.floor(u / 0.05), (int) Math.floor(v / 0.05)) - 0.5) * 0.6;
                dv -= Math.max(0.0, 0.7 - p[1]) * 1.4;
                if (bruit(u * 0.6, v * 0.8, graine + 3) > 0.72) dv -= 0.8;
                break;
            }
            case VITRE:
                dv += (hasard(graine, (int) Math.floor(u / 0.05), (int) Math.floor(v / 0.05)) - 0.5) * 0.3;
                break;
            default:
                break;
        }
        if (plan.usure > 0.35 && m.motif != VITRE && m.motif != INTERIEUR) {
            double suie = bruit(u * 1.6 + 7.0, v * 1.3, graine + 21) * 0.75 + hasard(graine, (int) Math.floor(u / 0.06), (int) Math.floor(v / 0.06)) * 0.25;
            double seuil = 0.98 - plan.usure * 0.28;
            if (suie > seuil) dv -= Math.min(2.6, (suie - seuil) * 14.0);
        }
        return new double[] { dv, rangee };
    }

    private static double bruit(double x, double y, long graine) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        double fx = x - ix, fy = y - iy;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        double a = hasard(graine, ix, iy), b = hasard(graine, ix + 1, iy), c = hasard(graine, ix, iy + 1), d = hasard(graine, ix + 1, iy + 1);
        return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
    }

    private void construire(Plan p, Random r) {
        double dx = p.largeur / 2.0, dz = p.profondeur / 2.0, h = p.murs;
        Matiere mur, toit, poutre, bois, pierre, porte, vitre, fer;
        sombre = new Matiere(9, -0.4, 0.6, 0.0, INTERIEUR);
        poutre = new Matiere(9, 0.6, 2.4, 0.2, PLANCHES);
        bois = new Matiere(9, 1.4, 3.0, 0.2, PLANCHES);
        pierre = new Matiere(r.nextBoolean() ? 5 : 4, 1.4, 3.2, 0.3, BLOCS);
        porte = new Matiere(r.nextBoolean() ? 9 : 2, 0.9, 2.6, 0.2, PLANCHES);
        vitre = new Matiere(r.nextFloat() < 0.5f ? 13 : 10, 0.0, 1.0, 1.2, VITRE);
        fer = new Matiere(5, 0.8, 3.2, 1.6, UNI);
        if (p.ruine) {
            ruine(p, r, pierre);
            return;
        }
        if (p.style >= ARCHE) {
            objet(p, r, bois, poutre, pierre, fer);
            return;
        }
        switch (p.style) {
            case CHAUMIERE:
                mur = new Matiere(3, 1.6, 3.0, 0.0, CREPI);
                toit = new Matiere(r.nextBoolean() ? 2 : 11, 1.1, 3.2, 0.0, CHAUME);
                break;
            case COLOMBAGES:
                mur = new Matiere(3, 2.2, 3.0, 0.0, CREPI);
                toit = new Matiere(r.nextBoolean() ? 13 : 4, 0.9, 3.4, 0.4, TUILES);
                break;
            case PIERRE:
                mur = pierre;
                toit = new Matiere(r.nextBoolean() ? 13 : 5, 0.8, 3.4, 0.5, TUILES);
                break;
            case HALLE:
                mur = pierre;
                toit = new Matiere(r.nextBoolean() ? 13 : 5, 0.8, 3.4, 0.5, TUILES);
                break;
            case RONDINS:
                mur = new Matiere(9, 1.3, 3.0, 0.2, TRONCS);
                toit = new Matiere(r.nextBoolean() ? 9 : 4, 0.9, 3.2, 0.2, PLANCHES);
                break;
            default:
                mur = r.nextBoolean() ? pierre : new Matiere(3, 1.8, 3.0, 0.0, CREPI);
                toit = new Matiere(r.nextBoolean() ? 2 : 11, 1.1, 3.2, 0.0, CHAUME);
                break;
        }
        Solide corps = boite(-dx, dx, 0, h, -dz, dz, mur);
        corps.dessus = sombre;
        if (p.style == RONDINS) {
            for (int k = -1; k <= 1; k += 2) for (int q = -1; q <= 1; q += 2) boite(k * dx - 0.13, k * dx + 0.13, 0, h + 0.1, q * dz - 0.13, q * dz + 0.13, new Matiere(9, 1.6, 3.0, 0.2, TRONCS));
        }
        if (p.style != PIERRE && p.style != RONDINS && p.style != HALLE) boite(-dx - 0.05, dx + 0.05, 0, 0.45, -dz - 0.05, dz + 0.05, pierre);
        double faite = h + dz * Math.tan(p.pente);
        double ep = p.style == CHAUMIERE || (p.style == LONGERE && toit.motif == CHAUME) ? 0.35 : 0.12;
        boolean croupe = toit.motif == CHAUME;
        double deb = p.debord;
        double basToit = h - deb * Math.tan(p.pente);
        double[][] trous = trous(p, r, dx, dz);
        if (croupe) {
            double ax = dx + deb, az = dz + deb;
            double sa = Math.sin(p.pente), ca = Math.cos(p.pente);
            Solide s = new Solide();
            s.n = new double[][] { { 0, -1, 0 }, { 0, ca, sa }, { 0, ca, -sa }, { sa, ca, 0 }, { -sa, ca, 0 } };
            double cote = ca * faite + sa * (ax - az);
            s.d = new double[] { -basToit, ca * faite, ca * faite, cote, cote };
            borner(s, -ax, ax, basToit, faite, -az, az);
            s.m = toit;
            s.trous = trous;
            solides.add(s);
        } else {
            double sa = Math.sin(p.pente), ca = Math.cos(p.pente);
            double ax = dx + deb;
            for (int k = -1; k <= 1; k += 2) {
                Solide s = new Solide();
                double[] no = { 0, ca, k * sa };
                double dOut = ca * (faite + ep / ca) + 0.0;
                s.n = new double[][] { no, { 0, -ca, -k * sa }, { 0, 0, -k }, { 0, -1, 0 }, { 1, 0, 0 }, { -1, 0, 0 } };
                s.d = new double[] { dOut, -(dOut - ep), 0.0, -basToit, ax, ax };
                borner(s, -ax, ax, basToit - ep, faite + ep * 2, k < 0 ? -dz - deb : 0, k < 0 ? 0 : dz + deb);
                s.m = toit;
                s.trous = trous;
                solides.add(s);
            }
            Solide pignon = new Solide();
            pignon.n = new double[][] { { 0, ca, sa }, { 0, ca, -sa }, { 0, -1, 0 }, { 1, 0, 0 }, { -1, 0, 0 } };
            pignon.d = new double[] { ca * faite, ca * faite, -h + 0.001, dx, dx };
            borner(pignon, -dx, dx, h, faite, -dz, dz);
            pignon.m = p.style == COLOMBAGES ? mur : p.style == RONDINS ? bois : mur;
            pignon.dessus = sombre;
            solides.add(pignon);
            if (p.style == COLOMBAGES) {
                for (int k = -1; k <= 1; k += 2) {
                    double x = k * (dx + 0.015);
                    for (double y : new double[] { h, (h + faite) * 0.5 }) {
                        double demi = (faite - y) / Math.tan(p.pente);
                        if (demi > 0.2) boite(x - 0.03, x + 0.03, y - 0.1, y, -demi, demi, poutre);
                    }
                    boite(x - 0.03, x + 0.03, h, faite - 0.15, -0.08, 0.08, poutre);
                }
            }
        }
        double av = dz + 0.015;
        if (p.style == COLOMBAGES) {
            for (double y : p.etage ? new double[] { 0.45, h * 0.5, h - 0.12 } : new double[] { 0.45, h - 0.12 }) boite(-dx, dx, y - 0.06, y + 0.06, -dz - 0.02, dz + 0.02, poutre);
            int n = (int) Math.round(p.largeur / 1.3);
            for (int k = 0; k <= n; k++) {
                double x = -dx + k * p.largeur / n;
                boite(x - 0.07, x + 0.07, 0.45, h, -dz - 0.02, dz + 0.02, poutre);
                if (k < n && (k + (int) (p.graine & 1)) % 2 == 0) {
                    double x2 = -dx + (k + 1) * p.largeur / n;
                    double y0 = 0.45, y1 = p.etage ? h * 0.5 : h - 0.12;
                    diagonale(x, y0, x2, y1, av, poutre);
                    if (p.etage) diagonale(x2, h * 0.5, x, h - 0.12, av, poutre);
                }
            }
            for (int k = -1; k <= 1; k += 2) {
                double x = k * (dx + 0.015);
                for (double y : p.etage ? new double[] { 0.45, h * 0.5, h - 0.12 } : new double[] { 0.45, h - 0.12 }) boite(x - 0.03, x + 0.03, y - 0.06, y + 0.06, -dz, dz, poutre);
            }
        }
        double porteX = p.porteX;
        double porteL = p.porteL, porteH = p.porteH;
        boite(porteX - porteL, porteX + porteL, 0.0, porteH, dz - 0.05, dz + 0.04, porte);
        boite(porteX - porteL - 0.1, porteX + porteL + 0.1, porteH, porteH + 0.14, dz, dz + 0.07, poutre);
        boite(porteX - porteL - 0.1, porteX - porteL, 0, porteH, dz, dz + 0.07, poutre);
        boite(porteX + porteL, porteX + porteL + 0.1, 0, porteH, dz, dz + 0.07, poutre);
        boite(porteX + porteL - 0.25, porteX + porteL - 0.15, 0.95, 1.05, dz + 0.04, dz + 0.08, fer);
        boite(porteX - porteL - 0.25, porteX + porteL + 0.25, 0.0, 0.12, dz, dz + 0.45, pierre);
        boolean halle = p.style == HALLE;
        double fenY = halle ? 1.3 : 1.0, fenH = halle ? 2.0 : 0.75, fenL = halle ? 0.3 : 0.36;
        double ecart = porteL + 1.05;
        List<Double> fenetres = new ArrayList<>();
        for (double x = porteX - ecart; x > -dx + 0.6; x -= 1.8) fenetres.add(x);
        for (double x = porteX + ecart; x < dx - 0.6; x += 1.8) fenetres.add(x);
        for (double x : fenetres) fenetre(x, fenY, fenL, fenH, dz, false, 1, p, r, vitre, bois, poutre);
        if (p.etage) {
            for (double x = -dx + 1.0; x < dx - 0.6; x += 1.7) fenetre(x, h * 0.5 + 0.55, fenL, fenH * 0.9, dz, false, 1, p, r, vitre, bois, poutre);
        }
        for (double x = -dx + 1.1; x < dx - 0.6; x += 1.9) {
            if (halle || r.nextFloat() < 0.7f) fenetre(x, fenY, fenL, fenH, dz, false, -1, p, r, vitre, bois, poutre);
        }
        if (p.etage) {
            for (double x = -dx + 1.0; x < dx - 0.6; x += 1.7) fenetre(x, h * 0.5 + 0.55, fenL, fenH * 0.9, dz, false, -1, p, r, vitre, bois, poutre);
        }
        if (!p.appentis) fenetre(0.0, fenY, fenL * 0.8, fenH * 0.8, dx, true, 1, p, r, vitre, bois, poutre);
        if (!p.tour) fenetre(dz * 0.25, fenY, fenL * 0.8, fenH * 0.8, dx, true, -1, p, r, vitre, bois, poutre);
        if (p.etage || halle) {
            if (!p.appentis) fenetre(0.0, h - 1.2, fenL * 0.7, 0.6, dx, true, 1, p, r, vitre, bois, poutre);
            if (!p.tour) fenetre(0.0, h - 1.2, fenL * 0.7, 0.6, dx, true, -1, p, r, vitre, bois, poutre);
        }
        if (p.tour) {
            double t0 = -dx - TOUR_AVANT, t1 = -dx + TOUR_ARRIERE, tc = (t0 + t1) * 0.5, ht = h + 4.5;
            boite(t0, t1, 0, ht, -TOUR_DEMI, TOUR_DEMI, pierre).dessus = sombre;
            boite(t0 - 0.08, t1 + 0.08, ht - 0.25, ht, -TOUR_DEMI - 0.08, TOUR_DEMI + 0.08, pierre);
            pyramide(tc, 0.0, TOUR_DEMI + 0.3, TOUR_DEMI + 0.3, ht - 0.1, Math.toRadians(58), toit);
            for (int sens = -1; sens <= 1; sens += 2) {
                double zf = sens * TOUR_DEMI;
                for (double y : new double[] { 1.6, h * 0.5 + 1.2 }) boite(tc - 0.09, tc + 0.09, y, y + 0.8, Math.min(zf, zf + sens * 0.03), Math.max(zf, zf + sens * 0.03), sombre);
                boite(tc - 0.55, tc + 0.55, ht - 1.9, ht - 0.6, Math.min(zf, zf + sens * 0.03), Math.max(zf, zf + sens * 0.03), sombre);
            }
            for (double y : new double[] { 1.6, h * 0.5 + 1.2 }) boite(t0 - 0.03, t0, y, y + 0.8, -0.09, 0.09, sombre);
            boite(t0 - 0.03, t0, ht - 1.9, ht - 0.6, -0.55, 0.55, sombre);
        }
        if (p.cheminee) {
            double cx = (r.nextBoolean() ? -1 : 1) * dx * 0.6, cz = -dz * 0.35;
            double haut = faite + 0.7;
            boite(cx - 0.32, cx + 0.32, h - 0.5, haut, cz - 0.32, cz + 0.32, pierre).dessus = sombre;
            boite(cx - 0.38, cx + 0.38, haut, haut + 0.12, cz - 0.38, cz + 0.38, pierre);
        }
        if (p.appentis) {
            double ax = dx, bx = dx + 1.6, hh = 1.7;
            boite(ax, bx, 0, hh, -dz * 0.7, dz * 0.6, bois);
            Solide s = new Solide();
            double a = Math.atan2(0.45, 1.6), sa = Math.sin(a), ca = Math.cos(a);
            s.n = new double[][] { { sa, ca, 0 }, { -sa, -ca, 0 }, { -1, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
            double top = hh + 0.45;
            s.d = new double[] { sa * ax + ca * (top + 0.08), -(sa * ax + ca * (top - 0.02)), -ax, bx + 0.25, dz * 0.6 + 0.2, dz * 0.7 + 0.2 };
            borner(s, ax, bx + 0.25, hh - 0.2, top + 0.2, -dz * 0.7 - 0.2, dz * 0.6 + 0.2);
            s.m = toit.motif == CHAUME ? new Matiere(9, 1.0, 3.0, 0.2, PLANCHES) : toit;
            solides.add(s);
        }
        if (p.bois) {
            double x0 = -dx - 0.15, z0 = -dz * 0.5;
            Matiere buche = new Matiere(9, 1.6, 3.0, 0.1, TRONCS);
            for (int rang = 0; rang < 4; rang++) {
                for (int k = 0; k < 4 - rang; k++) {
                    double y = 0.12 + rang * 0.2, z = z0 + (k - (3 - rang) * 0.5) * 0.22;
                    boite(x0 - 0.75, x0, y - 0.1, y + 0.1, z - 0.1, z + 0.1, buche);
                }
            }
        }
        if (p.tonneau) {
            double tx = (porteX >= 0 ? -1 : 1) * (Math.min(dx - 0.5, Math.abs(porteX) + 1.0)), tz = dz + 0.45;
            octogone(tx, tz, 0.3, 0.0, 0.85, bois);
            octogone(tx, tz, 0.32, 0.2, 0.26, fer);
            octogone(tx, tz, 0.32, 0.6, 0.66, fer);
        }
        if (p.metier != Metiers.MAISON) atelier(p, r, dx, dz, h, faite, pierre, bois, poutre, fer);
    }

    private void ruine(Plan p, Random r, Matiere pierre) {
        double dx = p.largeur / 2.0, dz = p.profondeur / 2.0, h = p.murs;
        boolean delabre = p.delabre;
        double tenue = Math.max(0.35, 1.0 - p.age / 400.0);
        Matiere charbon = delabre ? new Matiere(9, 1.7, 2.4, 0.0, PLANCHES) : new Matiere(9, -0.35, 0.9, 0.0, TRONCS);
        Matiere braise = !delabre && p.age < 12.0 ? new Matiere(9, -0.15, 1.1, 0.0, PLANCHES) : charbon;
        Matiere cendre = delabre ? new Matiere(pierre.rangee, 0.8, 2.2, 0.0, BLOCS) : new Matiere(5, 0.7, 1.6, 0.0, UNI);
        Matiere suie = delabre ? pierre : new Matiere(pierre.rangee, 0.3, 1.6, 0.1, BLOCS);
        Matiere torchis = delabre ? new Matiere(3, 1.2, 2.6, 0.0, CREPI) : new Matiere(3, 0.0, 1.1, 0.0, CREPI);
        Matiere crete = delabre ? null : charbon;
        if (p.style >= ARCHE) {
            boite(-dx * 0.8, dx * 0.8, 0.0, 0.08, -dz * 0.8, dz * 0.8, cendre);
            for (int k = 0; k < 3; k++) {
                double x = (r.nextDouble() - 0.5) * dx, z = (r.nextDouble() - 0.5) * dz;
                boite(x - 0.25, x + 0.25, 0.0, 0.12 + r.nextDouble() * 0.1, z - 0.06, z + 0.06, charbon);
            }
            return;
        }
        boolean maconne = p.style == PIERRE || p.style == HALLE;
        double ep = maconne ? 0.42 : 0.24;
        double hautMax = (maconne ? h * 0.9 : p.style == COLOMBAGES ? 1.5 : p.style == RONDINS ? 1.1 : 0.9) * tenue;
        Matiere mur = maconne ? suie : p.style == RONDINS ? charbon : torchis;
        if (!delabre && p.age < 30.0) boite(-dx, dx, 0.0, 0.12, -dz, dz, cendre);
        for (int cote = 0; cote < 4; cote++) {
            boolean long_ = cote < 2;
            double longueur = long_ ? p.largeur : p.profondeur;
            double fixe = long_ ? (cote == 0 ? dz - ep / 2 : -dz + ep / 2) : (cote == 2 ? dx - ep / 2 : -dx + ep / 2);
            double t = -longueur / 2.0;
            while (t < longueur / 2.0 - 0.05) {
                double l = Math.min(longueur / 2.0 - t, 0.5 + r.nextDouble() * 0.8);
                boolean porte = cote == 0 && t + l > p.porteX - p.porteL && t < p.porteX + p.porteL;
                double haut = maconne ? hautMax * (0.35 + 0.65 * r.nextDouble()) : r.nextDouble() < 0.35 ? 0.0 : hautMax * (0.15 + 0.85 * r.nextDouble());
                if (porte) haut = 0.0;
                if (haut > 0.15) {
                    Solide s = long_ ? boite(t, t + l, 0.0, haut, fixe - ep / 2, fixe + ep / 2, mur) : boite(fixe - ep / 2, fixe + ep / 2, 0.0, haut, t, t + l, mur);
                    s.dessus = crete;
                }
                t += l;
            }
        }
        if (!maconne) {
            for (int k = -1; k <= 1; k += 2) {
                for (int q = -1; q <= 1; q += 2) {
                    if (r.nextDouble() < 0.25 || (delabre && r.nextDouble() * 150.0 < p.age)) continue;
                    double haut = (1.0 + r.nextDouble() * (h - 0.6)) * tenue;
                    boite(k * dx - 0.11, k * dx + 0.11, 0.0, haut, q * dz - 0.11, q * dz + 0.11, charbon);
                }
            }
        }
        int poutres = delabre ? (int) Math.max(0, 3 - p.age / 60.0) + r.nextInt(2) : 3 + r.nextInt(4);
        for (int k = 0; k < poutres; k++) {
            double y = 0.12 + r.nextDouble() * 0.3;
            double c = (r.nextDouble() - 0.5) * 2.0;
            if (r.nextBoolean()) {
                double z = c * dz * 0.8, l = dx * (0.5 + r.nextDouble() * 0.6), x = (r.nextDouble() - 0.5) * dx;
                boite(x - l, x + l, y, y + 0.16, z - 0.09, z + 0.09, r.nextDouble() < 0.3 ? braise : charbon);
            } else {
                double x = c * dx * 0.8, l = dz * (0.5 + r.nextDouble() * 0.6), z = (r.nextDouble() - 0.5) * dz;
                boite(x - 0.09, x + 0.09, y, y + 0.16, z - l, z + l, r.nextDouble() < 0.3 ? braise : charbon);
            }
        }
        int tas = 2 + r.nextInt(3);
        for (int k = 0; k < tas; k++) {
            double x = (r.nextDouble() - 0.5) * dx * 1.2, z = (r.nextDouble() - 0.5) * dz * 1.2;
            double a = 0.3 + r.nextDouble() * 0.5;
            boite(x - a, x + a, 0.0, 0.2 + r.nextDouble() * 0.25, z - a * 0.8, z + a * 0.8, r.nextBoolean() ? cendre : maconne ? suie : charbon);
        }
        if (p.cheminee) {
            double cx = (r.nextBoolean() ? -1 : 1) * dx * 0.6, cz = -dz * 0.35;
            double haut = h + dz * Math.tan(p.pente) * (0.5 + r.nextDouble() * 0.5);
            boite(cx - 0.32, cx + 0.32, 0.0, haut * Math.max(0.5, tenue), cz - 0.32, cz + 0.32, suie).dessus = crete;
        }
    }

    private void atelier(Plan p, Random r, double dx, double dz, double h, double faite, Matiere pierre, Matiere bois, Matiere poutre, Matiere fer) {
        int m = p.metier;
        if (Metiers.ENSEIGNE[m] >= 0) {
            double x = p.porteX + p.porteL + 0.5;
            if (x > dx - 0.35) x = p.porteX - p.porteL - 0.5;
            double y = Math.min(h - 0.3, 2.8);
            boite(x - 0.04, x + 0.04, y - 0.05, y + 0.03, dz, dz + 0.8, fer);
            boite(x - 0.05, x + 0.05, y - 0.7, y - 0.1, dz + 0.22, dz + 0.74, new Matiere(Metiers.ENSEIGNE[m], 1.9, 3.2, 0.3, PLANCHES));
        }
        if (Metiers.DRAPEAU[m] >= 0) {
            double x = -dx * 0.6, haut = faite + 2.6;
            boite(x - 0.05, x + 0.05, faite - 0.4, haut, -0.05, 0.05, poutre);
            boite(x + 0.05, x + 1.2, haut - 0.75, haut - 0.05, -0.03, 0.03, new Matiere(Metiers.DRAPEAU[m], 2.0, 3.2, 0.2, UNI));
        }
        switch (m) {
            case Metiers.BOULANGERIE: {
                double cx = -dx - 0.9;
                octogone(cx, 0.0, 0.85, 0.0, 0.95, pierre).dessus = sombre;
                pyramide(cx, 0.0, 0.8, 0.8, 0.95, Math.toRadians(32), pierre);
                boite(cx - 0.18, cx + 0.18, 0.95, 1.9, -0.55, -0.2, pierre).dessus = sombre;
                boite(cx - 0.3, cx + 0.3, 0.2, 0.7, 0.8, 0.86, sombre);
                break;
            }
            case Metiers.FORGE: {
                double x = dx + 0.8;
                boite(dx + 0.15, dx + 1.4, 0.0, 0.8, -dz * 0.6, -dz * 0.6 + 0.9, pierre).dessus = new Matiere(2, 2.4, 3.0, 0.5, UNI);
                octogone(x, dz * 0.25, 0.22, 0.0, 0.5, bois);
                boite(x - 0.28, x + 0.28, 0.5, 0.72, dz * 0.25 - 0.13, dz * 0.25 + 0.13, fer);
                break;
            }
            case Metiers.MOULIN:
                // Lot G3 : la roue et les ailes tournent ; ce sont des objets à part (ROUE_EAU, AILES), posés par l'écran à chaque image.
                // Il ne reste ici que l'axe de la roue, qui sort du mur.
                if (p.roue) octogone(dx + 0.1, 0.0, 0.16, 1.6, 1.9, sombre);
                break;
            case Metiers.TANNERIE:
                for (int k = -1; k <= 1; k++) octogone(k * 1.2, dz + 1.0, 0.45, 0.0, 0.55, bois).dessus = new Matiere(9, 0.6, 2.0, 0.2, UNI);
                break;
            case Metiers.POTIER:
                for (int k = 0; k < 4; k++) octogone(dx - 0.4 - k * 0.45, dz + 0.4, 0.18 + (k % 2) * 0.05, 0.0, 0.45 + (k % 2) * 0.15, new Matiere(2, 2.0, 3.0, 0.3, UNI));
                break;
            case Metiers.ENTREPOT:
            case Metiers.PECHERIE: {
                Matiere caisse = new Matiere(9, 1.6, 3.0, 0.2, PLANCHES);
                boite(dx - 1.4, dx - 0.6, 0.0, 0.7, dz + 0.3, dz + 1.0, caisse);
                boite(dx - 1.3, dx - 0.7, 0.7, 1.2, dz + 0.4, dz + 0.9, caisse);
                break;
            }
            case Metiers.MENUISERIE: {
                Matiere planche = new Matiere(9, 1.8, 3.0, 0.2, PLANCHES);
                for (int k = 0; k < 5; k++) boite(dx + 0.2, dx + 1.5, 0.08 + k * 0.08, 0.14 + k * 0.08, dz * 0.1, dz * 0.1 + 0.9, planche);
                break;
            }
            default:
                break;
        }
    }

    private void bras(double cx, double cy, double angle, double longueur, double largeur, double z0, double z1, Matiere m) {
        double ux = Math.cos(angle), uy = Math.sin(angle), vx = -uy, vy = ux;
        double pu = ux * cx + uy * cy, pv = vx * cx + vy * cy;
        Solide s = new Solide();
        s.n = new double[][] { { ux, uy, 0 }, { -ux, -uy, 0 }, { vx, vy, 0 }, { -vx, -vy, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        s.d = new double[] { pu + longueur * 0.5, -(pu - longueur * 0.5), pv + largeur * 0.5, -(pv - largeur * 0.5), z1, -z0 };
        double e = longueur * 0.5 + largeur;
        borner(s, cx - e, cx + e, cy - e, cy + e, z0, z1);
        s.m = m;
        solides.add(s);
    }

    /** Lot G3 : une barre dans le plan (y, z), d'axe x entre x0 et x1, centrée en (cy, cz), tournée de « angle ». */
    private void rais(double x0, double x1, double cy, double cz, double angle, double longueur, double largeur, Matiere m) {
        double uy = Math.cos(angle), uz = Math.sin(angle), vy = -uz, vz = uy;
        double pu = uy * cy + uz * cz, pv = vy * cy + vz * cz;
        Solide s = new Solide();
        s.n = new double[][] { { 0, uy, uz }, { 0, -uy, -uz }, { 0, vy, vz }, { 0, -vy, -vz }, { 1, 0, 0 }, { -1, 0, 0 } };
        s.d = new double[] { pu + longueur * 0.5, -(pu - longueur * 0.5), pv + largeur * 0.5, -(pv - largeur * 0.5), x1, -x0 };
        double e = longueur * 0.5 + largeur;
        borner(s, x0, x1, cy - e, cy + e, cz - e, cz + e);
        s.m = m;
        solides.add(s);
    }

    private void roue(double x, double yc, double zc, double rayon, Matiere m) {
        Solide s = new Solide();
        s.n = new double[10][];
        s.d = new double[10];
        s.n[0] = new double[] { 1, 0, 0 };
        s.d[0] = x + 0.18;
        s.n[1] = new double[] { -1, 0, 0 };
        s.d[1] = -(x - 0.18);
        double apotheme = rayon * Math.cos(Math.PI / 8.0);
        for (int k = 0; k < 8; k++) {
            double t = Math.PI / 8.0 + k * Math.PI / 4.0, cy = Math.cos(t), sz = Math.sin(t);
            s.n[2 + k] = new double[] { 0, cy, sz };
            s.d[2 + k] = apotheme + cy * yc + sz * zc;
        }
        borner(s, x - 0.18, x + 0.18, yc - rayon, yc + rayon, zc - rayon, zc + rayon);
        s.m = m;
        solides.add(s);
        octogone(x + 0.2, zc, 0.18, yc - 0.18, yc + 0.18, sombre);
    }

    private double[][] trous(Plan p, Random r, double dx, double dz) {
        if (p.usure < 0.45) return null;
        int n = 1 + (int) ((p.usure - 0.45) * 6.0);
        double[][] t = new double[n][];
        for (int k = 0; k < n; k++) t[k] = new double[] { (r.nextDouble() * 2 - 1) * dx * 0.8, (r.nextDouble() * 2 - 1) * dz * 0.8, 0.35 + r.nextDouble() * 0.7 * p.usure };
        return t;
    }

    private void fenetre(double x, double y, double l, double h, double d, boolean cote, int sens, Plan p, Random r, Matiere vitre, Matiere bois, Matiere poutre) {
        boolean condamnee = p.usure > 0.3 && r.nextFloat() < p.usure;
        if (!cote) {
            face(x - l, x + l, y, y + h, d - 0.05, d + 0.02, sens, false, vitre);
            face(x - l - 0.08, x + l + 0.08, y - 0.1, y, d, d + 0.09, sens, false, poutre);
            face(x - l - 0.08, x + l + 0.08, y + h, y + h + 0.08, d, d + 0.06, sens, false, poutre);
            face(x - 0.03, x + 0.03, y, y + h, d, d + 0.04, sens, false, poutre);
            if (condamnee) {
                for (int k = 0; k < 3; k++) {
                    double yy = y + 0.12 + k * h * 0.33;
                    face(x - l - 0.12, x + l + 0.12, yy, yy + 0.13, d + 0.04, d + 0.08, sens, false, bois);
                }
            } else if (r.nextFloat() < 0.6f) {
                face(x - l * 2.0 - 0.1, x - l - 0.1, y, y + h, d + 0.02, d + 0.06, sens, false, bois);
                if (r.nextFloat() < 0.8f) face(x + l + 0.1, x + l * 2.0 + 0.1, y, y + h, d + 0.02, d + 0.06, sens, false, bois);
            }
        } else {
            face(x - l, x + l, y, y + h, d - 0.02, d + 0.05, sens, true, vitre);
            face(x - l - 0.08, x + l + 0.08, y - 0.1, y, d, d + 0.09, sens, true, poutre);
            face(x - 0.03, x + 0.03, y, y + h, d, d + 0.04, sens, true, poutre);
            if (condamnee) face(x - l - 0.1, x + l + 0.1, y + h * 0.3, y + h * 0.5, d + 0.04, d + 0.08, sens, true, bois);
        }
    }

    private void face(double u0, double u1, double y0, double y1, double d0, double d1, int sens, boolean cote, Matiere m) {
        double a = sens > 0 ? d0 : -d1, b = sens > 0 ? d1 : -d0;
        if (cote) boite(a, b, y0, y1, u0, u1, m);
        else boite(u0, u1, y0, y1, a, b, m);
    }

    private void pyramide(double cx, double cz, double ax, double az, double base, double pente, Matiere m) {
        double sa = Math.sin(pente), ca = Math.cos(pente);
        double faite = base + Math.min(ax, az) * Math.tan(pente);
        double cote = ca * faite + sa * Math.abs(ax - az);
        Solide s = new Solide();
        s.n = new double[][] { { 0, -1, 0 }, { 0, ca, sa }, { 0, ca, -sa }, { sa, ca, 0 }, { -sa, ca, 0 } };
        s.d = new double[] { -base, ca * faite + sa * cz, ca * faite - sa * cz, cote + sa * cx, cote - sa * cx };
        borner(s, cx - ax, cx + ax, base, faite, cz - az, cz + az);
        s.m = m;
        solides.add(s);
    }

    private void diagonale(double x0, double y0, double x1, double y1, double z, Matiere m) {
        for (int face = -1; face <= 1; face += 2) {
            double ux = x1 - x0, uy = y1 - y0, lu = Math.hypot(ux, uy);
            ux /= lu;
            uy /= lu;
            double vx = -uy, vy = ux;
            double cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
            double hu = lu / 2, hv = 0.065;
            double pu = ux * cx + uy * cy, pv = vx * cx + vy * cy;
            double zc = face * z;
            Solide s = new Solide();
            s.n = new double[][] { { ux, uy, 0 }, { -ux, -uy, 0 }, { vx, vy, 0 }, { -vx, -vy, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
            s.d = new double[] { pu + hu, -(pu - hu), pv + hv, -(pv - hv), zc + 0.03, -(zc - 0.03) };
            borner(s, Math.min(x0, x1) - 0.2, Math.max(x0, x1) + 0.2, Math.min(y0, y1) - 0.2, Math.max(y0, y1) + 0.2, zc - 0.05, zc + 0.05);
            s.m = m;
            solides.add(s);
        }
    }

    private void objet(Plan p, Random r, Matiere bois, Matiere poutre, Matiere pierre, Matiere fer) {
        double dx = p.largeur / 2.0, dz = p.profondeur / 2.0, h = p.murs;
        Matiere planche = new Matiere(9, 1.5, 3.0, 0.2, PLANCHES);
        switch (p.style) {
            case ARCHE:
                arche(dx, dz, h, pierre);
                break;
            case PONTON: {
                Matiere pieu = new Matiere(9, 0.8, 2.6, 0.1, TRONCS);
                for (int i = -1; i <= 1; i += 2) for (int k = -1; k <= 1; k += 2) octogone(i * (dx - 0.25), k * (dz + 0.04), 0.13, 0.0, h - 0.05, pieu);
                boite(-dx, dx, h - 0.35, h - 0.15, -dz + 0.05, dz - 0.05, pieu);
                boite(-dx, dx, h - 0.15, h, -dz, dz, planche);
                break;
            }
            case TONNEAUX: {
                double[][] places = { { -0.3, 0.0 }, { 0.32, 0.12 }, { 0.0, -0.5 } };
                int n = 1 + r.nextInt(3);
                for (int k = 0; k < n; k++) {
                    double tx = places[k][0], tz = places[k][1];
                    octogone(tx, tz, 0.3, 0.0, 0.85, bois);
                    octogone(tx, tz, 0.32, 0.2, 0.26, fer);
                    octogone(tx, tz, 0.32, 0.6, 0.66, fer);
                }
                break;
            }
            case CAISSES: {
                Matiere caisse = new Matiere(r.nextBoolean() ? 9 : 2, 1.6, 3.0, 0.2, PLANCHES);
                boite(-0.55, 0.1, 0.0, 0.62, -0.35, 0.3, caisse);
                if (r.nextBoolean()) boite(0.15, 0.6, 0.0, 0.48, -0.2, 0.25, caisse);
                if (r.nextBoolean()) boite(-0.45, 0.0, 0.62, 1.1, -0.25, 0.2, caisse);
                break;
            }
            case BARRIERE: {
                int n = Math.max(1, (int) Math.ceil(p.largeur / 1.3));
                for (int k = 0; k <= n; k++) {
                    double x = -dx + k * p.largeur / n;
                    boite(x - 0.06, x + 0.06, 0.0, 1.05, -0.06, 0.06, poutre);
                }
                if ((p.graine & 1) == 0) {
                    boite(-dx, dx, 0.35, 0.45, -0.035, 0.035, bois);
                    boite(-dx, dx, 0.75, 0.85, -0.035, 0.035, bois);
                } else {
                    boite(-dx, dx, 0.05, 0.9, -0.045, 0.045, new Matiere(9, 1.2, 2.6, 0.0, TRONCS));
                }
                break;
            }
            case PORTAIL: {
                boite(-dx, -dx + 0.14, 0.0, 1.6, -0.07, 0.07, poutre);
                boite(dx - 0.14, dx, 0.0, 1.6, -0.07, 0.07, poutre);
                boite(-dx - 0.1, dx + 0.1, 1.6, 1.72, -0.09, 0.09, poutre);
                boite(-dx + 0.16, dx - 0.16, 0.1, 1.15, -0.03, 0.03, planche);
                diagonale(-dx + 0.2, 0.2, dx - 0.2, 1.05, 0.05, poutre);
                break;
            }
            case ETAL: {
                int[] couleurs = { 13, 2, 11, 4 };
                Matiere toile = new Matiere(couleurs[(int) ((p.graine >>> 7) & 3)], 2.2, 3.0, 0.2, UNI);
                for (int i = -1; i <= 1; i += 2) for (int k = -1; k <= 1; k += 2) boite(i * (dx - 0.1) - 0.05, i * (dx - 0.1) + 0.05, 0.0, k < 0 ? 2.0 : 0.8, k * (dz - 0.1) - 0.05, k * (dz - 0.1) + 0.05, poutre);
                boite(-dx, dx, 0.75, 0.85, -dz + 0.1, dz, bois);
                boite(-dx * 0.7, -0.1, 0.85, 1.1, -0.1, dz * 0.7, new Matiere(r.nextBoolean() ? 11 : 2, 1.8, 3.0, 0.4, UNI));
                boite(0.1, dx * 0.7, 0.85, 1.05, -0.1, dz * 0.7, new Matiere(r.nextBoolean() ? 4 : 10, 1.8, 3.0, 0.4, UNI));
                Solide s = new Solide();
                double a = Math.atan2(0.5, p.profondeur), sa = Math.sin(a), ca = Math.cos(a);
                s.n = new double[][] { { 0, ca, sa }, { 0, -ca, -sa }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
                s.d = new double[] { ca * 1.75 + 0.06, -(ca * 1.75 - 0.02), dx + 0.1, dx + 0.1, dz + 0.2, dz + 0.1 };
                borner(s, -dx - 0.1, dx + 0.1, 1.0, 2.4, -dz - 0.1, dz + 0.2);
                s.m = toile;
                solides.add(s);
                break;
            }
            case TABLEAU: {
                // Le tableau des annonces : deux poteaux, un panneau de planches, un petit toit, des affiches sur les deux faces.
                Matiere papier = new Matiere(3, 2.6, 3.0, 0.0, UNI);
                Matiere auvent = new Matiere(r.nextBoolean() ? 9 : 4, 0.9, 3.2, 0.2, PLANCHES);
                boite(-dx, -dx + 0.14, 0.0, 2.2, -0.07, 0.07, poutre);
                boite(dx - 0.14, dx, 0.0, 2.2, -0.07, 0.07, poutre);
                boite(-dx + 0.1, dx - 0.1, 0.85, 1.95, -0.04, 0.04, bois);
                pyramide(0.0, 0.0, dx + 0.15, 0.35, 2.15, Math.toRadians(35), auvent);
                int n = 5 + r.nextInt(4);
                for (int k = 0; k < n; k++) {
                    double l = 0.18 + r.nextDouble() * 0.14, ht = 0.22 + r.nextDouble() * 0.14;
                    double x = -dx + 0.2 + r.nextDouble() * (2.0 * dx - 0.4 - l), y = 0.95 + r.nextDouble() * (0.9 - ht);
                    double cote = k % 2 == 0 ? 1.0 : -1.0;
                    boite(x, x + l, y, y + ht, cote > 0 ? 0.04 : -0.06, cote > 0 ? 0.06 : -0.04, papier);
                }
                break;
            }
            case TENTE: {
                // Une tente de toile : un prisme couché sur deux mâts ; l'entrée, sombre, est sur la face +x (tournée vers le feu).
                int[] toiles = { 3, R_SANG, 12, R_INDIGO };
                Matiere toile = new Matiere(toiles[(int) ((p.graine >>> 9) & 3)], 1.6, 3.0, 0.2, UNI);
                double l = Math.hypot(dz, h), pan = h * dz / l;
                Solide s = new Solide();
                s.n = new double[][] { { 1, 0, 0 }, { -1, 0, 0 }, { 0, -1, 0 }, { 0, dz / l, h / l }, { 0, dz / l, -h / l } };
                s.d = new double[] { dx, dx, 0.0, pan, pan };
                borner(s, -dx, dx, 0.0, h, -dz, dz);
                s.m = toile;
                solides.add(s);
                double e = 0.6;
                Solide o = new Solide();
                o.n = new double[][] { { 1, 0, 0 }, { -1, 0, 0 }, { 0, -1, 0 }, { 0, dz / l, h / l }, { 0, dz / l, -h / l } };
                o.d = new double[] { dx + 0.02, -(dx - 0.05), 0.0, pan * e, pan * e };
                borner(o, dx - 0.05, dx + 0.02, 0.0, h * e, -dz * e, dz * e);
                o.m = sombre;
                solides.add(o);
                for (int i = -1; i <= 1; i += 2) boite(i * dx - 0.04, i * dx + 0.04, 0.0, h + 0.18, -0.04, 0.04, poutre);
                break;
            }
            case ROUE_EAU: {
                // Une roue à aubes (axe selon x) : quatre rais qui la traversent et huit pales ; chaque phase la tourne de 45° ÷ PHASES_MOULIN.
                Matiere roueBois = new Matiere(9, 1.2, 2.6, 0.2, PLANCHES);
                double rc = 1.7, yc = 1.75, a0 = p.phase * Math.PI / 4.0 / PHASES_MOULIN;
                for (int k = 0; k < 4; k++) rais(-0.05, 0.05, yc, 0.0, a0 + k * Math.PI / 4.0, rc * 2.0 - 0.3, 0.1, roueBois);
                for (int k = 0; k < 8; k++) {
                    double a = a0 + k * Math.PI / 4.0;
                    rais(-0.18, 0.18, yc + Math.cos(a) * (rc - 0.3), Math.sin(a) * (rc - 0.3), a, 0.6, 0.08, roueBois);
                    double b = a + Math.PI / 8.0;
                    rais(-0.16, -0.1, yc + Math.cos(b) * (rc - 0.05), Math.sin(b) * (rc - 0.05), b + Math.PI / 2.0, 0.72, 0.08, roueBois);
                    rais(0.1, 0.16, yc + Math.cos(b) * (rc - 0.05), Math.sin(b) * (rc - 0.05), b + Math.PI / 2.0, 0.72, 0.08, roueBois);
                }
                octogone(0.0, 0.0, 0.2, yc - 0.2, yc + 0.2, sombre);
                break;
            }
            case LOUP: {
                // Un loup, la tête vers +x : corps, poitrail, tête et museau, oreilles, yeux qui luisent, queue basse ; les pattes avancent
                // tour à tour (phase % PAS_LOUP) ; le pelage est gris ou brun (phase / PAS_LOUP).
                int pas = p.phase % PAS_LOUP;
                Matiere poil = p.phase / PAS_LOUP == 0 ? new Matiere(5, 1.1, 2.6, 0.4, UNI) : new Matiere(9, 0.9, 2.4, 0.4, UNI);
                Matiere clair = new Matiere(5, 2.0, 2.4, 0.3, UNI);
                Matiere yeux = new Matiere(R_FEU, 5.6, 1.6, 0.6, UNI);
                boite(-0.45, 0.3, 0.38, 0.66, -0.14, 0.14, poil);
                boite(0.18, 0.44, 0.36, 0.74, -0.16, 0.16, poil);
                boite(0.4, 0.66, 0.6, 0.84, -0.12, 0.12, poil);
                boite(0.64, 0.84, 0.6, 0.71, -0.06, 0.06, clair);
                boite(0.46, 0.55, 0.84, 0.97, -0.11, -0.04, poil);
                boite(0.46, 0.55, 0.84, 0.97, 0.04, 0.11, poil);
                boite(0.65, 0.68, 0.74, 0.77, -0.09, -0.05, yeux);
                boite(0.65, 0.68, 0.74, 0.77, 0.05, 0.09, yeux);
                bras(-0.62, 0.52, 0.52, 0.44, 0.1, -0.05, 0.05, poil);
                double[] balance = { 0.09, 0.0, -0.09, 0.0 };
                double a = balance[pas], b = balance[(pas + 2) % PAS_LOUP];
                double[][] pattes = { { 0.3, -0.1, a }, { 0.3, 0.1, b }, { -0.35, -0.1, b }, { -0.35, 0.1, a } };
                for (double[] q : pattes) boite(q[0] + q[2] - 0.045, q[0] + q[2] + 0.045, 0.0, 0.44, q[1] - 0.045, q[1] + 0.045, poil);
                break;
            }
            case AILES: {
                // Les ailes d'un moulin à vent (dans le plan de la façade) : deux bras croisés, quatre toiles ; chaque phase les tourne de 90° ÷ PHASES_MOULIN.
                Matiere toile = new Matiere(3, 2.4, 3.0, 0.2, PLANCHES);
                double yc = h - 0.4, a0 = p.phase * Math.PI / 2.0 / PHASES_MOULIN;
                bras(0.0, yc, Math.PI / 4.0 + a0, 7.0, 0.55, 0.0, 0.1, toile);
                bras(0.0, yc, -Math.PI / 4.0 + a0, 7.0, 0.55, 0.0, 0.1, toile);
                octogone(0.0, -0.1, 0.22, yc - 0.22, yc + 0.22, poutre);
                break;
            }
            case FEU: {
                // Un feu de camp : un cercle de pierres, deux bûches croisées, un lit de braises et des flammes (la variante change leur forme).
                Matiere caillou = new Matiere(5, 1.0, 3.0, 0.3, UNI);
                Matiere buche = new Matiere(9, 0.8, 2.4, 0.2, TRONCS);
                Matiere braise = new Matiere(R_FEU, 3.6, 2.2, 0.4, UNI);
                Matiere flamme = new Matiere(R_FEU, 5.6, 1.6, 0.6, UNI);
                for (int k = 0; k < 9; k++) {
                    double a = k * Math.PI * 2.0 / 9.0;
                    octogone(Math.cos(a) * 0.55, Math.sin(a) * 0.55, 0.11 + r.nextDouble() * 0.04, 0.0, 0.12 + r.nextDouble() * 0.06, caillou);
                }
                boite(-0.42, 0.42, 0.0, 0.12, -0.07, 0.07, buche);
                boite(-0.07, 0.07, 0.04, 0.16, -0.42, 0.42, buche);
                octogone(0.0, 0.0, 0.3, 0.0, 0.1, braise);
                // Des langues de feu fines et hautes, autour d'un petit cœur clair.
                pyramide(0.0, 0.0, 0.1, 0.1, 0.1, Math.toRadians(72), flamme);
                for (int k = 0; k < 5; k++) {
                    double a = k * Math.PI * 2.0 / 5.0 + r.nextDouble() * 0.8, d = 0.08 + r.nextDouble() * 0.12;
                    double cote = 0.06 + r.nextDouble() * 0.03, haut = 0.3 + r.nextDouble() * 0.3;
                    pyramide(Math.cos(a) * d, Math.sin(a) * d, cote, cote, 0.1, Math.atan(haut / cote), braise);
                }
                break;
            }
            case PUITS: {
                Matiere toit = new Matiere(r.nextBoolean() ? 13 : 5, 0.8, 3.4, 0.5, TUILES);
                octogone(0.0, 0.0, 0.75, 0.0, 0.8, pierre).dessus = sombre;
                boite(-0.68, -0.56, 0.8, 2.1, -0.06, 0.06, poutre);
                boite(0.56, 0.68, 0.8, 2.1, -0.06, 0.06, poutre);
                boite(-0.62, 0.62, 1.7, 1.78, -0.04, 0.04, bois);
                octogone(0.0, 0.0, 0.16, 1.15, 1.45, bois);
                pyramide(0.0, 0.0, 0.95, 0.7, 2.05, Math.toRadians(40), toit);
                break;
            }
            default:
                break;
        }
    }

    private void arche(double dx, double dz, double h, Matiere pierre) {
        Matiere dalle = new Matiere(4, 1.2, 3.0, 0.2, BLOCS);
        Matiere claveau = new Matiere(pierre.rangee, 2.0, 3.0, 0.3, BLOCS);
        double pile = 0.65, demi = dx - pile, cle = h - 0.6;
        double fleche = Math.max(0.35, Math.min(demi, cle - 0.2));
        double rayon = (fleche * fleche + demi * demi) / (2.0 * fleche), cy = cle - rayon;
        for (int k = -1; k <= 1; k += 2) {
            double x0 = k < 0 ? -dx : dx - pile, x1 = k < 0 ? -dx + pile : dx;
            boite(x0, x1, 0.0, h, -dz, dz, pierre).dessus = dalle;
            for (int q = -1; q <= 1; q += 2) bec(k < 0 ? -dx : dx, -k, q, dz, pile, cle - fleche + 0.3, pierre);
        }
        int n = 12;
        for (int i = 0; i < n; i++) {
            double xa = -demi + 2.0 * demi * i / n, xb = -demi + 2.0 * demi * (i + 1) / n;
            double ya = cy + Math.sqrt(Math.max(0.0, rayon * rayon - xa * xa)), yb = cy + Math.sqrt(Math.max(0.0, rayon * rayon - xb * xb));
            double nx = yb - ya, ny = -(xb - xa), l = Math.hypot(nx, ny);
            nx /= l;
            ny /= l;
            double plan = nx * xa + ny * ya;
            Solide s = new Solide();
            s.n = new double[][] { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { nx, ny, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
            s.d = new double[] { xb, -xa, h, plan, dz, dz };
            borner(s, xa, xb, Math.min(ya, yb), h, -dz, dz);
            s.m = pierre;
            s.dessus = dalle;
            solides.add(s);
            for (int q = -1; q <= 1; q += 2) {
                Solide c = new Solide();
                double z0 = q < 0 ? -dz - 0.04 : dz, z1 = q < 0 ? -dz : dz + 0.04;
                c.n = new double[][] { { 1, 0, 0 }, { -1, 0, 0 }, { -nx, -ny, 0 }, { nx, ny, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
                c.d = new double[] { xb, -xa, -plan + 0.4, plan, z1, -z0 };
                borner(c, xa, xb, Math.min(ya, yb), Math.max(ya, yb) + 0.45, z0, z1);
                c.m = claveau;
                solides.add(c);
            }
        }
        boite(-dx, dx, h - 0.18, h, -dz - 0.07, dz + 0.07, dalle);
        for (int k = -1; k <= 1; k += 2) {
            double z0 = k < 0 ? -dz : dz - 0.3, z1 = k < 0 ? -dz + 0.3 : dz;
            boite(-dx, dx, h, h + 0.65, z0, z1, pierre);
            boite(-dx, dx, h + 0.65, h + 0.78, z0 - 0.04, z1 + 0.04, dalle);
        }
    }

    private void bec(double xc, int interieur, int q, double dz, double pile, double haut, Matiere m) {
        Solide s = new Solide();
        double r2 = Math.sqrt(0.5);
        s.n = new double[][] { { 0, 0, -q }, { -interieur, 0, 0 }, { interieur * r2, 0, q * r2 }, { 0, 1, 0 }, { 0, -1, 0 } };
        s.d = new double[] { -dz, -interieur * xc, (interieur * xc + dz + pile) * r2, haut, 0.0 };
        double xa = Math.min(xc, xc + interieur * pile), xb = Math.max(xc, xc + interieur * pile);
        double za = q > 0 ? dz : -dz - pile, zb = q > 0 ? dz + pile : -dz;
        borner(s, xa, xb, 0.0, haut, za, zb);
        s.m = m;
        solides.add(s);
    }

    private Solide octogone(double cx, double cz, double r, double y0, double y1, Matiere m) {
        Solide s = new Solide();
        s.n = new double[10][];
        s.d = new double[10];
        for (int k = 0; k < 8; k++) {
            double a = Math.PI / 8 + k * Math.PI / 4;
            s.n[k] = new double[] { Math.cos(a), 0, Math.sin(a) };
            s.d[k] = Math.cos(a) * cx + Math.sin(a) * cz + r;
        }
        s.n[8] = new double[] { 0, 1, 0 };
        s.d[8] = y1;
        s.n[9] = new double[] { 0, -1, 0 };
        s.d[9] = -y0;
        borner(s, cx - r * 1.1, cx + r * 1.1, y0, y1, cz - r * 1.1, cz + r * 1.1);
        s.m = m;
        solides.add(s);
        return s;
    }

    private Solide boite(double x0, double x1, double y0, double y1, double z0, double z1, Matiere m) {
        Solide s = new Solide();
        s.n = new double[][] { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        s.d = new double[] { x1, -x0, y1, -y0, z1, -z0 };
        borner(s, x0, x1, y0, y1, z0, z1);
        s.m = m;
        solides.add(s);
        return s;
    }

    private static void borner(Solide s, double x0, double x1, double y0, double y1, double z0, double z1) {
        s.min[0] = x0;
        s.max[0] = x1;
        s.min[1] = y0;
        s.max[1] = y1;
        s.min[2] = z0;
        s.max[2] = z1;
    }

    private static double toucher(Solide s, double[] o, double[] d, int[] face) {
        double t0 = -1e30, t1 = 1e30;
        for (int k = 0; k < 3; k++) {
            if (Math.abs(d[k]) < 1e-12) {
                if (o[k] < s.min[k] - 1e-6 || o[k] > s.max[k] + 1e-6) return -1.0;
                continue;
            }
            double a = (s.min[k] - 1e-6 - o[k]) / d[k], b = (s.max[k] + 1e-6 - o[k]) / d[k];
            if (a > b) {
                double c = a;
                a = b;
                b = c;
            }
            t0 = Math.max(t0, a);
            t1 = Math.min(t1, b);
            if (t0 > t1) return -1.0;
        }
        double entree = -1e30, fin = 1e30;
        int fe = -1;
        for (int k = 0; k < s.n.length; k++) {
            double[] n = s.n[k];
            double den = n[0] * d[0] + n[1] * d[1] + n[2] * d[2];
            double dist = s.d[k] - (n[0] * o[0] + n[1] * o[1] + n[2] * o[2]);
            if (Math.abs(den) < 1e-12) {
                if (dist < 0.0) return -1.0;
                continue;
            }
            double t = dist / den;
            if (den < 0.0) {
                if (t > entree) {
                    entree = t;
                    fe = k;
                }
            } else if (t < fin) fin = t;
            if (entree > fin) return -1.0;
        }
        if (entree <= 0.0 || fe < 0) return -1.0;
        face[0] = fe;
        if (s.trous != null) {
            double x = o[0] + d[0] * entree, z = o[2] + d[2] * entree;
            for (double[] t : s.trous) {
                double ex = x - t[0], ez = z - t[1];
                double r2 = ex * ex + ez * ez;
                double bord = t[2] * (0.8 + 0.35 * hasard((long) (t[0] * 1000), (int) Math.floor(Math.atan2(ez, ex) * 4.0), 3));
                if (r2 < bord * bord) face[0] = fe + TROU;
            }
        }
        return entree;
    }

    private static double[] versMonde(double x, double y, double z, double cy, double sy) {
        return new double[] { x * cy + z * sy, y, -x * sy + z * cy };
    }

    private static double[] versLocal(double x, double y, double z, double cy, double sy) {
        return new double[] { x * cy - z * sy, y, x * sy + z * cy };
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

    public void dispose() {
        if (ouvrier != null) ouvrier.shutdownNow();
        for (Image im : cache.values()) im.texture.dispose();
        cache.clear();
    }
}
