package com.deldaran.politics;

import com.deldaran.politics.histoire.Peuple;
import java.util.Random;

public class Traits {
    public static final int RASE = 0, COURT = 1, MI_LONG = 2, LONG = 3, QUEUE = 4, TRESSE = 5, CHIGNON = 6, CRETE = 7;
    public static final int COUPES = 8;
    public static final int SANS_BARBE = 0, PLEINE = 1, MOUSTACHE = 2, BOUC = 3, TRESSEE = 4;
    public static final int BARBES = 5;
    public static final int NEUTRE = 0, COLERE = 1, TRISTE = 2, JOIE = 3, PEUR = 4, DOULEUR = 5;
    public static final int EXPRESSIONS = 6;
    public static final String[] NOMS_COUPES = { "rase", "court", "mi-long", "long", "queue", "tresse", "chignon", "crete" };
    public static final String[] NOMS_BARBES = { "aucune", "pleine", "moustache", "bouc", "tressee" };
    public static final String[] NOMS_EXPRESSIONS = { "neutre", "colere", "triste", "joie", "peur", "douleur" };
    public static final double POUSSE_CHEVEUX = 0.04, POUSSE_BARBE = 0.045;
    public static final double CHEVEUX_MAX = 90.0, BARBE_MAX = 60.0;
    private static final double PALIER = 2.0;

    public int coupe;
    public double cheveux;
    public int barbe;
    public double longueurBarbe;
    public int expression;

    public Traits() {
    }

    public Traits(Traits autre) {
        coupe = autre.coupe;
        cheveux = autre.cheveux;
        barbe = autre.barbe;
        longueurBarbe = autre.longueurBarbe;
        expression = autre.expression;
    }

    public static Traits tirer(long graine, int espece, boolean femme, boolean enfant) {
        Random r = new Random(graine * 131L + 17L);
        Traits t = new Traits();
        if (femme) {
            int[] coupes = { LONG, LONG, MI_LONG, QUEUE, TRESSE, CHIGNON, COURT };
            t.coupe = coupes[r.nextInt(coupes.length)];
            t.cheveux = t.coupe == COURT ? 6 + r.nextInt(6) : 25 + r.nextInt(45);
        } else if (espece == Peuple.ORK) {
            int[] coupes = { CRETE, RASE, QUEUE, COURT };
            t.coupe = coupes[r.nextInt(coupes.length)];
            t.cheveux = t.coupe == RASE ? 0.3 : t.coupe == QUEUE ? 20 + r.nextInt(30) : 4 + r.nextInt(8);
        } else if (espece == Peuple.ELFE) {
            int[] coupes = { LONG, MI_LONG, QUEUE, TRESSE };
            t.coupe = coupes[r.nextInt(coupes.length)];
            t.cheveux = 25 + r.nextInt(55);
        } else {
            int[] coupes = { COURT, COURT, MI_LONG, RASE, QUEUE, LONG };
            t.coupe = coupes[r.nextInt(coupes.length)];
            t.cheveux = t.coupe == RASE ? r.nextInt(2) : t.coupe == COURT ? 2 + r.nextInt(6) : 15 + r.nextInt(30);
        }
        if (enfant) t.cheveux = Math.min(t.cheveux, 30);
        if (!femme && !enfant) {
            if (espece == Peuple.NAIN) {
                t.barbe = r.nextFloat() < 0.6f ? TRESSEE : PLEINE;
                t.longueurBarbe = 15 + r.nextInt(40);
            } else if (espece != Peuple.ELFE && r.nextFloat() < 0.45f) {
                int[] barbes = { PLEINE, PLEINE, MOUSTACHE, BOUC };
                t.barbe = barbes[r.nextInt(barbes.length)];
                t.longueurBarbe = 1 + r.nextInt(14);
            }
        }
        return t;
    }

    public void pousser(double jours, boolean imberbe) {
        if (coupe != RASE || cheveux < 1.0) cheveux = Math.min(CHEVEUX_MAX, cheveux + jours * POUSSE_CHEVEUX);
        if (!imberbe) longueurBarbe = Math.min(BARBE_MAX, longueurBarbe + jours * POUSSE_BARBE);
    }

    public void couper(double cm) {
        cheveux = Math.min(cheveux, cm);
    }

    public void raser() {
        longueurBarbe = 0.0;
    }

    public int palierCheveux() {
        return (int) Math.floor(cheveux / PALIER);
    }

    public int palierBarbe() {
        return longueurBarbe < 0.4 ? 0 : longueurBarbe < 1.0 ? 1 : 1 + (int) Math.floor(longueurBarbe / PALIER);
    }

    public long cle() {
        long h = coupe;
        h = h * 131L + palierCheveux();
        h = h * 131L + barbe;
        h = h * 131L + palierBarbe();
        h = h * 131L + expression;
        return h * 0x9E3779B97F4A7C15L;
    }
}
