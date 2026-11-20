package com.deldaran.politics;

import java.util.Arrays;

public class Pose {
    public static final int CORPS_X = 0, CORPS_Y = 1, CORPS_Z = 2, CORPS_TANGAGE = 3, CORPS_ROULIS = 4, CORPS_LACET = 5;
    public static final int BUSTE_TANGAGE = 6, BUSTE_ROULIS = 7, BUSTE_LACET = 8;
    public static final int TETE_TANGAGE = 9, TETE_ROULIS = 10, TETE_LACET = 11;
    public static final int BD_LEVE = 12, BD_ECARTE = 13, BD_COUDE = 14, BD_CROISE = 15;
    public static final int BG_LEVE = 16, BG_ECARTE = 17, BG_COUDE = 18, BG_CROISE = 19;
    public static final int JD_HANCHE = 20, JD_GENOU = 21, JD_ECARTE = 22;
    public static final int JG_HANCHE = 23, JG_GENOU = 24, JG_ECARTE = 25;
    public static final int CAPE = 26, OBJET_D_ANGLE = 27, OBJET_G_ANGLE = 28, BD_TOURNE = 29, BG_TOURNE = 30;
    public static final int NOMBRE = 31;
    public static final String[] NOMS = { "corps.x", "corps.y", "corps.z", "corps.tangage", "corps.roulis", "corps.lacet",
            "buste.tangage", "buste.roulis", "buste.lacet", "tete.tangage", "tete.roulis", "tete.lacet",
            "bd.leve", "bd.ecarte", "bd.coude", "bd.croise", "bg.leve", "bg.ecarte", "bg.coude", "bg.croise",
            "jd.hanche", "jd.genou", "jd.ecarte", "jg.hanche", "jg.genou", "jg.ecarte", "cape", "od.angle", "og.angle", "bd.tourne", "bg.tourne" };
    private static final boolean[] ANGLE = new boolean[NOMBRE];
    private static final double[] REPOS = new double[NOMBRE];

    static {
        Arrays.fill(ANGLE, true);
        ANGLE[CORPS_X] = ANGLE[CORPS_Y] = ANGLE[CORPS_Z] = ANGLE[CAPE] = false;
        REPOS[BD_ECARTE] = REPOS[BG_ECARTE] = 0.12;
        REPOS[BD_COUDE] = REPOS[BG_COUDE] = 0.25;
        REPOS[JD_GENOU] = REPOS[JG_GENOU] = 0.04;
        REPOS[CAPE] = 0.06;
    }

    public static final int GARDER = -2, AUCUN = -1;
    public final double[] v = new double[NOMBRE];
    public int objetD = GARDER, objetG = GARDER, expression = -1;
    public boolean naturel, marche;
    public double phase = -1.0;

    public Pose() {
        System.arraycopy(REPOS, 0, v, 0, NOMBRE);
    }

    public Pose(Pose autre) {
        copier(autre);
    }

    public Pose copier(Pose autre) {
        System.arraycopy(autre.v, 0, v, 0, NOMBRE);
        objetD = autre.objetD;
        objetG = autre.objetG;
        expression = autre.expression;
        naturel = autre.naturel;
        marche = autre.marche;
        phase = autre.phase;
        return this;
    }

    public static int indice(String nom) {
        for (int i = 0; i < NOMBRE; i++) if (NOMS[i].equals(nom)) return i;
        return -1;
    }

    public static boolean angle(int i) {
        return ANGLE[i];
    }

    public static Pose repos() {
        Pose p = new Pose();
        p.naturel = true;
        return p;
    }

    public static Pose marche(double phase) {
        Pose p = new Pose();
        p.naturel = true;
        if (phase < 0.0) return p;
        p.marche = true;
        p.phase = phase;
        double marche = Math.sin(phase), retour = Math.cos(phase);
        for (int k = 0; k < 2; k++) {
            double m = k == 0 ? marche : -marche;
            double w = k == 0 ? retour : -retour;
            int hanche = k == 0 ? JG_HANCHE : JD_HANCHE;
            p.v[hanche] = 0.42 * m;
            p.v[hanche + 1] = 0.08 + 0.75 * Math.max(0.0, w) * Math.max(0.0, 1.0 - Math.abs(m) * 0.3);
            double bras = (k == 0 ? -marche : marche) * 0.5;
            int leve = k == 0 ? BG_LEVE : BD_LEVE;
            p.v[leve] = bras;
            p.v[leve + 2] = 0.25 + 0.3 * Math.max(0.0, bras);
        }
        p.v[CAPE] = 0.22 + 0.05 * Math.sin(phase * 2.0);
        return p;
    }

    public static Pose melanger(Pose a, Pose b, double t, Pose sortie) {
        for (int i = 0; i < NOMBRE; i++) sortie.v[i] = a.v[i] + (b.v[i] - a.v[i]) * t;
        Pose proche = t < 0.5 ? a : b;
        sortie.objetD = proche.objetD;
        sortie.objetG = proche.objetG;
        sortie.expression = proche.expression;
        sortie.naturel = a.naturel && b.naturel;
        sortie.marche = proche.marche;
        sortie.phase = proche.phase;
        return sortie;
    }

    public long cle() {
        long h = 1469598103934665603L;
        for (double x : v) h = (h ^ Math.round(x * 1000.0)) * 1099511628211L;
        h = (h ^ objetD) * 1099511628211L;
        h = (h ^ objetG) * 1099511628211L;
        h = (h ^ expression) * 1099511628211L;
        return h ^ (naturel ? 7 : 0);
    }
}
