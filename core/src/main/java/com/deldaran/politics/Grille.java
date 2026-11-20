package com.deldaran.politics;

import java.util.Arrays;

public class Grille {
    public static final byte VIDE = 0, EAU = 1, INTERDIT = 2, RUE = 3, PLACE = 4, BATIMENT = 5, COUR = 6, OBJET = 7, ARBRE = 8;
    public final int cote;
    public final float[] sol;
    public final byte[] occupation;
    public final int[] proprietaire;
    public final float[] distanceRue, capRue;
    public boolean riviere;

    public Grille(int cote) {
        this.cote = cote;
        sol = new float[cote * cote];
        occupation = new byte[cote * cote];
        proprietaire = new int[cote * cote];
        distanceRue = new float[cote * cote];
        capRue = new float[cote * cote];
        Arrays.fill(proprietaire, -1);
        Arrays.fill(distanceRue, Float.MAX_VALUE);
    }

    public double centre(int i) {
        return i - cote / 2 + 0.5;
    }

    public int indice(double a, double b) {
        int i = (int) Math.floor(a) + cote / 2, j = (int) Math.floor(b) + cote / 2;
        if (i < 0 || j < 0 || i >= cote || j >= cote) return -1;
        return j * cote + i;
    }

    public double hauteur(double a, double b) {
        double u = a + cote / 2 - 0.5, v = b + cote / 2 - 0.5;
        int i = (int) Math.floor(u), j = (int) Math.floor(v);
        i = Math.max(0, Math.min(cote - 2, i));
        j = Math.max(0, Math.min(cote - 2, j));
        double fu = Math.max(0.0, Math.min(1.0, u - i)), fv = Math.max(0.0, Math.min(1.0, v - j));
        int k = j * cote + i;
        double bas = sol[k] + (sol[k + 1] - sol[k]) * fu;
        double haut = sol[k + cote] + (sol[k + cote + 1] - sol[k + cote]) * fu;
        return bas + (haut - bas) * fv;
    }

    public void segment(double a0, double b0, double a1, double b1, double demi, byte type, double portee) {
        double bord = Math.max(demi, portee);
        int i0 = Math.max(0, (int) Math.floor(Math.min(a0, a1) - bord) + cote / 2);
        int i1 = Math.min(cote - 1, (int) Math.floor(Math.max(a0, a1) + bord) + cote / 2);
        int j0 = Math.max(0, (int) Math.floor(Math.min(b0, b1) - bord) + cote / 2);
        int j1 = Math.min(cote - 1, (int) Math.floor(Math.max(b0, b1) + bord) + cote / 2);
        double da = a1 - a0, db = b1 - b0, l2 = da * da + db * db;
        float cap = (float) Math.atan2(db, da);
        for (int j = j0; j <= j1; j++) {
            double b = centre(j);
            for (int i = i0; i <= i1; i++) {
                double a = centre(i);
                double t = l2 < 1e-9 ? 0.0 : Math.max(0.0, Math.min(1.0, ((a - a0) * da + (b - b0) * db) / l2));
                double d = Math.hypot(a - a0 - da * t, b - b0 - db * t);
                int k = j * cote + i;
                if (d < demi && occupation[k] != BATIMENT) occupation[k] = type;
                if (portee > 0.0 && d < distanceRue[k]) {
                    distanceRue[k] = (float) d;
                    capRue[k] = cap;
                }
            }
        }
    }

    public boolean libre(double ac, double bc, double c, double s, double hx, double hz, int moi, boolean ruesPermises) {
        double r = Math.abs(c) * hx + Math.abs(s) * hz, q = Math.abs(s) * hx + Math.abs(c) * hz;
        int i0 = (int) Math.floor(ac - r) + cote / 2, i1 = (int) Math.floor(ac + r) + cote / 2;
        int j0 = (int) Math.floor(bc - q) + cote / 2, j1 = (int) Math.floor(bc + q) + cote / 2;
        if (i0 < 0 || j0 < 0 || i1 >= cote || j1 >= cote) return false;
        for (int j = j0; j <= j1; j++) {
            double db = centre(j) - bc;
            for (int i = i0; i <= i1; i++) {
                double da = centre(i) - ac;
                double x = da * c + db * s, z = da * s - db * c;
                if (Math.abs(x) > hx || Math.abs(z) > hz) continue;
                int k = j * cote + i;
                byte o = occupation[k];
                if (o == VIDE) continue;
                if (proprietaire[k] == moi && moi >= 0) continue;
                if (ruesPermises && (o == RUE || o == PLACE)) continue;
                return false;
            }
        }
        return true;
    }

    public void marquer(double ac, double bc, double c, double s, double hx, double hz, byte type, int moi) {
        double r = Math.abs(c) * hx + Math.abs(s) * hz, q = Math.abs(s) * hx + Math.abs(c) * hz;
        int i0 = Math.max(0, (int) Math.floor(ac - r) + cote / 2), i1 = Math.min(cote - 1, (int) Math.floor(ac + r) + cote / 2);
        int j0 = Math.max(0, (int) Math.floor(bc - q) + cote / 2), j1 = Math.min(cote - 1, (int) Math.floor(bc + q) + cote / 2);
        for (int j = j0; j <= j1; j++) {
            double db = centre(j) - bc;
            for (int i = i0; i <= i1; i++) {
                double da = centre(i) - ac;
                double x = da * c + db * s, z = da * s - db * c;
                if (Math.abs(x) > hx || Math.abs(z) > hz) continue;
                int k = j * cote + i;
                if (occupation[k] != VIDE && proprietaire[k] != moi) continue;
                occupation[k] = type;
                proprietaire[k] = moi;
            }
        }
    }

    public boolean disqueLibre(double a, double b, double rayon) {
        int i0 = (int) Math.floor(a - rayon) + cote / 2, i1 = (int) Math.floor(a + rayon) + cote / 2;
        int j0 = (int) Math.floor(b - rayon) + cote / 2, j1 = (int) Math.floor(b + rayon) + cote / 2;
        if (i0 < 0 || j0 < 0 || i1 >= cote || j1 >= cote) return false;
        for (int j = j0; j <= j1; j++) {
            for (int i = i0; i <= i1; i++) {
                if (Math.hypot(centre(i) - a, centre(j) - b) > rayon + 0.5) continue;
                if (occupation[j * cote + i] != VIDE) return false;
            }
        }
        return true;
    }

    public void disque(double a, double b, double rayon, byte type) {
        int i0 = Math.max(0, (int) Math.floor(a - rayon) + cote / 2), i1 = Math.min(cote - 1, (int) Math.floor(a + rayon) + cote / 2);
        int j0 = Math.max(0, (int) Math.floor(b - rayon) + cote / 2), j1 = Math.min(cote - 1, (int) Math.floor(b + rayon) + cote / 2);
        for (int j = j0; j <= j1; j++) {
            for (int i = i0; i <= i1; i++) {
                if (Math.hypot(centre(i) - a, centre(j) - b) > rayon) continue;
                int k = j * cote + i;
                if (occupation[k] == VIDE) occupation[k] = type;
            }
        }
    }

    public double[] mesurer(double ac, double bc, double c, double s, double hx, double hz) {
        double basse = Double.MAX_VALUE, haute = -Double.MAX_VALUE, somme = 0.0;
        int n = 0;
        int ni = Math.max(2, (int) Math.ceil(hx * 2.0)), nj = Math.max(2, (int) Math.ceil(hz * 2.0));
        for (int j = 0; j <= nj; j++) {
            double z = -hz + 2.0 * hz * j / nj;
            for (int i = 0; i <= ni; i++) {
                double x = -hx + 2.0 * hx * i / ni;
                double h = hauteur(ac + x * c + z * s, bc + x * s - z * c);
                basse = Math.min(basse, h);
                haute = Math.max(haute, h);
                somme += h;
                n++;
            }
        }
        return new double[] { basse, haute, somme / n };
    }
}
