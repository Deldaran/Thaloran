package com.deldaran.politics;

import java.util.Arrays;
import java.util.Random;

public class Tenue {
    public static final int BRAIES = 0, BANDE = 1, CHEMISE = 2, TUNIQUE = 3, ROBE = 4, PANTALON = 5, BOTTES = 6, CEINTURE = 7;
    public static final int CAPE = 8, CAPUCHE = 9, PLASTRON = 10, SAC = 11, EPEE = 12, OUTIL = 13;
    public static final int ARC = 14, CARQUOIS = 15, BOUCLIER = 16, LANTERNE = 17, HACHE = 18, LANCE = 19, DAGUE = 20, CASQUE = 21, MAILLE = 22, BATON = 23;
    public static final int PIECES = 24;
    public static final int[] MAIN_DROITE = { EPEE, HACHE, LANCE, BATON, DAGUE, OUTIL };
    public static final int[] MAIN_GAUCHE = { BOUCLIER, ARC, LANTERNE };
    private static final int[] TISSUS = { 4, 11, 13, 2, 15, 9, 5 };
    private static final int[] CUIRS = { 9, 2 };
    private static final int[] LINGES = { 3, 11 };

    public final int[] couleurs = new int[PIECES];
    public final boolean[] enMain = new boolean[PIECES];

    public Tenue() {
        Arrays.fill(couleurs, -1);
    }

    public Tenue(Tenue autre) {
        System.arraycopy(autre.couleurs, 0, couleurs, 0, PIECES);
        System.arraycopy(autre.enMain, 0, enMain, 0, PIECES);
    }

    public boolean porte(int piece) {
        return couleurs[piece] >= 0;
    }

    public Tenue mettre(int piece, int rangee) {
        couleurs[piece] = rangee;
        return this;
    }

    public Tenue enlever(int piece) {
        couleurs[piece] = -1;
        enMain[piece] = false;
        return this;
    }

    public Tenue tenir(int piece) {
        if (!porte(piece)) return this;
        int[] main = contient(MAIN_DROITE, piece) ? MAIN_DROITE : contient(MAIN_GAUCHE, piece) ? MAIN_GAUCHE : null;
        if (main == null) return this;
        for (int autre : main) enMain[autre] = false;
        enMain[piece] = true;
        return this;
    }

    public Tenue ranger(int piece) {
        enMain[piece] = false;
        return this;
    }

    public int tenue(int[] main) {
        for (int piece : main) if (porte(piece) && enMain[piece]) return piece;
        return -1;
    }

    private static boolean contient(int[] liste, int piece) {
        for (int p : liste) if (p == piece) return true;
        return false;
    }

    public long cle() {
        long h = 1469598103934665603L;
        for (int k = 0; k < PIECES; k++) h = (h ^ (couleurs[k] + 1 + (enMain[k] ? 64 : 0))) * 1099511628211L;
        return h;
    }

    public static Tenue nue(long graine, boolean femme) {
        Random r = new Random(graine * 31L + 7L);
        Tenue t = new Tenue();
        int linge = LINGES[r.nextInt(LINGES.length)];
        t.mettre(BRAIES, linge);
        if (femme) t.mettre(BANDE, linge);
        return t;
    }

    public static Tenue tirer(long graine, boolean femme, boolean enfant) {
        Random r = new Random(graine * 31L + 11L);
        Tenue t = nue(graine, femme);
        int haut = TISSUS[r.nextInt(TISSUS.length)];
        int bas = TISSUS[r.nextInt(TISSUS.length)];
        int cuir = CUIRS[r.nextInt(CUIRS.length)];
        int cape = TISSUS[r.nextInt(TISSUS.length)];
        if (femme && r.nextFloat() < 0.45f) {
            t.mettre(ROBE, haut);
        } else {
            t.mettre(r.nextFloat() < 0.55f ? TUNIQUE : CHEMISE, haut);
            t.mettre(PANTALON, bas);
        }
        if (r.nextFloat() < 0.85f) t.mettre(BOTTES, cuir);
        if (r.nextFloat() < 0.75f) t.mettre(CEINTURE, cuir);
        if (r.nextFloat() < 0.35f) t.mettre(CAPUCHE, cape);
        if (t.porte(CAPUCHE) || r.nextFloat() < 0.3f) t.mettre(CAPE, cape);
        if (!enfant) {
            if (r.nextFloat() < 0.25f) t.mettre(PLASTRON, 5);
            if (r.nextFloat() < 0.25f) t.mettre(SAC, cuir);
            if (r.nextFloat() < 0.2f) t.mettre(EPEE, 5);
            if (r.nextFloat() < 0.2f) t.mettre(OUTIL, 9);
            if (r.nextFloat() < 0.12f) t.mettre(MAILLE, 5);
            if (r.nextFloat() < 0.12f) t.mettre(CASQUE, 5);
            if (r.nextFloat() < 0.15f) t.mettre(BOUCLIER, r.nextBoolean() ? 9 : cape);
            if (r.nextFloat() < 0.12f) {
                t.mettre(ARC, 9);
                t.mettre(CARQUOIS, cuir);
            }
            if (r.nextFloat() < 0.1f) t.mettre(LANCE, 9);
            else if (r.nextFloat() < 0.1f) t.mettre(BATON, 9);
            if (r.nextFloat() < 0.12f) t.mettre(HACHE, 9);
            if (r.nextFloat() < 0.15f) t.mettre(DAGUE, 5);
            if (r.nextFloat() < 0.1f) t.mettre(LANTERNE, 5);
        }
        return t;
    }
}
