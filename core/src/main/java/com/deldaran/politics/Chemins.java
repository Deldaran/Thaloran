package com.deldaran.politics;

import java.util.Arrays;

public class Chemins {
    private static final int EXPANSIONS = 80000;
    private static final float DIAGONALE = 1.4142135f;
    private static final int[] DI = { 1, -1, 0, 0, 1, 1, -1, -1 };
    private static final int[] DJ = { 0, 0, 1, -1, 1, -1, 1, -1 };

    private int capacite;
    private float[] cout = new float[0];
    private int[] parent = new int[0];
    private int[] vu = new int[0];
    private int generation;
    private long[] tas = new long[1024];
    private int taille;
    private int[] trace = new int[256];

    public static boolean praticable(byte o) {
        return o == Grille.VIDE || o == Grille.RUE || o == Grille.PLACE || o == Grille.COUR;
    }

    private static float prix(byte o) {
        return o == Grille.RUE || o == Grille.PLACE ? 1f : 1.6f;
    }

    public float[] trouver(Grille g, double a0, double b0, double a1, double b1) {
        int cote = g.cote;
        int n = cote * cote;
        if (n > capacite) {
            capacite = n;
            cout = new float[n];
            parent = new int[n];
            vu = new int[n];
            generation = 0;
        }
        generation++;
        int depart = proche(g, a0, b0), arrivee = proche(g, a1, b1);
        if (depart < 0 || arrivee < 0) return null;
        int ai = arrivee % cote, aj = arrivee / cote;
        taille = 0;
        vu[depart] = generation;
        cout[depart] = 0f;
        parent[depart] = -1;
        pousser(heuristique(depart % cote, depart / cote, ai, aj), depart);
        int expansions = 0;
        boolean trouve = false;
        while (taille > 0 && expansions < EXPANSIONS) {
            long haut = tirer();
            int k = (int) haut;
            float f = Float.intBitsToFloat((int) (haut >>> 32));
            int i = k % cote, j = k / cote;
            if (f > cout[k] + heuristique(i, j, ai, aj) + 1e-3f) continue;
            if (k == arrivee) {
                trouve = true;
                break;
            }
            expansions++;
            for (int d = 0; d < 8; d++) {
                int ni = i + DI[d], nj = j + DJ[d];
                if (ni < 0 || nj < 0 || ni >= cote || nj >= cote) continue;
                int m = nj * cote + ni;
                byte o = g.occupation[m];
                if (!praticable(o)) continue;
                if (d >= 4 && (!praticable(g.occupation[j * cote + ni]) || !praticable(g.occupation[nj * cote + i]))) continue;
                float c = cout[k] + prix(o) * (d >= 4 ? DIAGONALE : 1f);
                if (vu[m] == generation && c >= cout[m]) continue;
                vu[m] = generation;
                cout[m] = c;
                parent[m] = k;
                pousser(c + heuristique(ni, nj, ai, aj), m);
            }
        }
        if (!trouve) return null;
        int longueur = 0;
        for (int k = arrivee; k >= 0; k = parent[k]) {
            if (longueur == trace.length) trace = Arrays.copyOf(trace, longueur * 2);
            trace[longueur++] = k;
        }
        float[] points = new float[(longueur + 2) * 2];
        int p = 0;
        points[p++] = (float) a0;
        points[p++] = (float) b0;
        int courant = longueur - 1;
        double ca = a0, cb = b0;
        while (courant > 0) {
            int suivant = courant - 1;
            while (suivant > 0 && direct(g, ca, cb, g.centre(trace[suivant - 1] % cote), g.centre(trace[suivant - 1] / cote))) suivant--;
            ca = g.centre(trace[suivant] % cote);
            cb = g.centre(trace[suivant] / cote);
            points[p++] = (float) ca;
            points[p++] = (float) cb;
            courant = suivant;
        }
        int fin = g.indice(a1, b1);
        if (fin >= 0 && praticable(g.occupation[fin])) {
            if (p >= 4 && direct(g, points[p - 4], points[p - 3], a1, b1)) p -= 2;
            points[p++] = (float) a1;
            points[p++] = (float) b1;
        }
        return Arrays.copyOf(points, p);
    }

    private static float heuristique(int i, int j, int ai, int aj) {
        int dx = Math.abs(i - ai), dy = Math.abs(j - aj);
        return Math.max(dx, dy) + (DIAGONALE - 1f) * Math.min(dx, dy);
    }

    public static boolean direct(Grille g, double a0, double b0, double a1, double b1) {
        double l = Math.hypot(a1 - a0, b1 - b0);
        int pas = Math.max(1, (int) Math.ceil(l / 0.35));
        for (int s = 0; s <= pas; s++) {
            double t = (double) s / pas;
            double a = a0 + (a1 - a0) * t, b = b0 + (b1 - b0) * t;
            for (int c = 0; c < 4; c++) {
                int k = g.indice(a + ((c & 1) == 0 ? -0.3 : 0.3), b + ((c & 2) == 0 ? -0.3 : 0.3));
                if (k < 0 || !praticable(g.occupation[k])) return false;
            }
        }
        return true;
    }

    public static int proche(Grille g, double a, double b) {
        int k = g.indice(a, b);
        if (k >= 0 && praticable(g.occupation[k])) return k;
        int cote = g.cote;
        int i0 = (int) Math.floor(a) + cote / 2, j0 = (int) Math.floor(b) + cote / 2;
        for (int r = 1; r <= 8; r++) {
            int meilleur = -1;
            double d = Double.MAX_VALUE;
            for (int j = j0 - r; j <= j0 + r; j++) {
                for (int i = i0 - r; i <= i0 + r; i++) {
                    if (Math.max(Math.abs(i - i0), Math.abs(j - j0)) != r) continue;
                    if (i < 0 || j < 0 || i >= cote || j >= cote) continue;
                    int m = j * cote + i;
                    if (!praticable(g.occupation[m])) continue;
                    double e = Math.hypot(g.centre(i) - a, g.centre(j) - b);
                    if (e < d) {
                        d = e;
                        meilleur = m;
                    }
                }
            }
            if (meilleur >= 0) return meilleur;
        }
        return -1;
    }

    private void pousser(float f, int k) {
        if (taille == tas.length) tas = Arrays.copyOf(tas, taille * 2);
        long v = ((long) Float.floatToIntBits(f) << 32) | (k & 0xFFFFFFFFL);
        int i = taille++;
        while (i > 0) {
            int p = (i - 1) >> 1;
            if (tas[p] <= v) break;
            tas[i] = tas[p];
            i = p;
        }
        tas[i] = v;
    }

    private long tirer() {
        long haut = tas[0];
        long v = tas[--taille];
        int i = 0;
        while (true) {
            int e = i * 2 + 1;
            if (e >= taille) break;
            if (e + 1 < taille && tas[e + 1] < tas[e]) e++;
            if (tas[e] >= v) break;
            tas[i] = tas[e];
            i = e;
        }
        tas[i] = v;
        return haut;
    }
}
