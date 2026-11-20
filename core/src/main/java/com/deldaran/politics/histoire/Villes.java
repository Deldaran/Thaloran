package com.deldaran.politics.histoire;

import java.util.Arrays;

public class Villes {
    public static final short VIVANTE = Short.MIN_VALUE;
    public static final int PAS_MESURE = 10;
    public static final byte FONDEE = 0, REFONDEE = 1, SACCAGEE = 2, CONQUISE = 3, ABANDONNEE = 4, CAPITALE = 5, DECHUE = 6;
    public static final String[] EVENEMENTS = { "fondee", "refondee", "saccagee", "conquise", "abandonnee", "devient capitale", "n'est plus capitale" };
    public static final int GUERRE = 0, DECLIN = 1;
    public static final byte MARQUE_CAPITALE = 1, MARQUE_GUERRE = 2;
    private static final int TAILLE_DEPART = 256;
    private static final int DELAI_REFONDATION = 80;

    public volatile int nombre = 0;
    public int ruines = 0, abandonsGuerre = 0, refondations = 0;
    public int[] caseDe = new int[TAILLE_DEPART];
    public int[] region = new int[TAILLE_DEPART];
    public byte[] peuple = new byte[TAILLE_DEPART];
    public short[] fondation = new short[TAILLE_DEPART];
    public short[] fin = new short[TAILLE_DEPART];
    public int[] habitants = new int[TAILLE_DEPART];
    public int[] pic = new int[TAILLE_DEPART];
    public short[] anneePic = new short[TAILLE_DEPART];
    public int[] origine = new int[TAILLE_DEPART];
    public int[] heritiere = new int[TAILLE_DEPART];
    public short[] mesures = new short[TAILLE_DEPART];
    public int[][] courbe = new int[TAILLE_DEPART][];
    public int[][] maitre = new int[TAILLE_DEPART][];
    public byte[][] marques = new byte[TAILLE_DEPART][];

    public volatile int nombreEvenements = 0;
    public int[] evVille = new int[TAILLE_DEPART];
    public short[] evAnnee = new short[TAILLE_DEPART];
    public byte[] evType = new byte[TAILLE_DEPART];
    public int[] evValeur = new int[TAILLE_DEPART];

    public int ajouter(int caseVille, int regionVille, int indicePeuple, int annee) {
        int n = nombre;
        if (n == caseDe.length) agrandir();
        caseDe[n] = caseVille;
        region[n] = regionVille;
        peuple[n] = (byte) indicePeuple;
        fondation[n] = (short) annee;
        fin[n] = VIVANTE;
        anneePic[n] = (short) annee;
        origine[n] = -1;
        heritiere[n] = -1;
        courbe[n] = new int[8];
        maitre[n] = new int[8];
        marques[n] = new byte[8];
        int ancienne = -1;
        for (int v = n - 1; v >= 0; v--) {
            if (region[v] == regionVille && peuple[v] == indicePeuple && fin[v] != VIVANTE && heritiere[v] < 0 && annee - fin[v] <= DELAI_REFONDATION) {
                ancienne = v;
                break;
            }
        }
        nombre = n + 1;
        if (ancienne >= 0) {
            origine[n] = ancienne;
            heritiere[ancienne] = n;
            refondations++;
            noter(n, annee, REFONDEE, ancienne);
        } else {
            noter(n, annee, FONDEE, -1);
        }
        return n;
    }

    private void agrandir() {
        int n = caseDe.length * 2;
        caseDe = Arrays.copyOf(caseDe, n);
        region = Arrays.copyOf(region, n);
        peuple = Arrays.copyOf(peuple, n);
        fondation = Arrays.copyOf(fondation, n);
        fin = Arrays.copyOf(fin, n);
        habitants = Arrays.copyOf(habitants, n);
        pic = Arrays.copyOf(pic, n);
        anneePic = Arrays.copyOf(anneePic, n);
        origine = Arrays.copyOf(origine, n);
        heritiere = Arrays.copyOf(heritiere, n);
        mesures = Arrays.copyOf(mesures, n);
        courbe = Arrays.copyOf(courbe, n);
        maitre = Arrays.copyOf(maitre, n);
        marques = Arrays.copyOf(marques, n);
    }

    public void noter(int ville, int annee, byte type, int valeur) {
        int k = nombreEvenements;
        if (k == evVille.length) {
            int n = k * 2;
            evVille = Arrays.copyOf(evVille, n);
            evAnnee = Arrays.copyOf(evAnnee, n);
            evType = Arrays.copyOf(evType, n);
            evValeur = Arrays.copyOf(evValeur, n);
        }
        evVille[k] = ville;
        evAnnee[k] = (short) annee;
        evType[k] = type;
        evValeur[k] = valeur;
        nombreEvenements = k + 1;
    }

    public void actualiser(int ville, int nombreHabitants, int annee) {
        habitants[ville] = nombreHabitants;
        if (nombreHabitants > pic[ville]) {
            pic[ville] = nombreHabitants;
            anneePic[ville] = (short) annee;
        }
        if (nombreHabitants == 0 && fin[ville] == VIVANTE) {
            fin[ville] = (short) annee;
            ruines++;
            noter(ville, annee, ABANDONNEE, DECLIN);
        }
    }

    public void abandonner(int ville, int annee) {
        if (fin[ville] != VIVANTE) return;
        habitants[ville] = 0;
        fin[ville] = (short) annee;
        ruines++;
        abandonsGuerre++;
        noter(ville, annee, ABANDONNEE, GUERRE);
    }

    public void mesurer(int annee, Etats etats) {
        int total = nombre;
        for (int v = 0; v < total; v++) {
            if (fin[v] != VIVANTE && fin[v] < annee - PAS_MESURE) continue;
            int m = mesures[v];
            if (m == courbe[v].length) {
                courbe[v] = Arrays.copyOf(courbe[v], m * 2);
                maitre[v] = Arrays.copyOf(maitre[v], m * 2);
                marques[v] = Arrays.copyOf(marques[v], m * 2);
            }
            int e = region[v] >= 0 && region[v] < etats.regionEtat.length ? etats.regionEtat[region[v]] : -1;
            byte marque = 0;
            if (fin[v] == VIVANTE && e >= 0 && etats.estVivant(e)) {
                if (etats.capitale[e] == v) marque |= MARQUE_CAPITALE;
                if (etats.ennemi[e] >= 0) marque |= MARQUE_GUERRE;
            }
            byte avant = m > 0 ? marques[v][m - 1] : 0;
            if ((marque & MARQUE_CAPITALE) != 0 && (avant & MARQUE_CAPITALE) == 0) noter(v, annee, CAPITALE, e);
            if ((marque & MARQUE_CAPITALE) == 0 && (avant & MARQUE_CAPITALE) != 0) noter(v, annee, DECHUE, e);
            courbe[v][m] = habitants[v];
            maitre[v][m] = e;
            marques[v][m] = marque;
            mesures[v] = (short) (m + 1);
        }
    }

    public int habitantsEn(int ville, int annee) {
        int m = mesures[ville];
        if (m == 0 || annee < fondation[ville]) return 0;
        double t = (annee - fondation[ville]) / (double) PAS_MESURE;
        int i = (int) Math.floor(t);
        if (i >= m - 1) return fin[ville] != VIVANTE && annee >= fin[ville] ? 0 : courbe[ville][m - 1];
        double f = t - i;
        return (int) Math.round(courbe[ville][i] + (courbe[ville][i + 1] - courbe[ville][i]) * f);
    }

    public String raconter(int ville) {
        StringBuilder b = new StringBuilder();
        for (int k = 0; k < nombreEvenements; k++) {
            if (evVille[k] != ville) continue;
            if (b.length() > 0) b.append(", ");
            b.append(EVENEMENTS[evType[k]]).append(" an ").append(evAnnee[k]);
            if (evType[k] == SACCAGEE) b.append(" (").append(evValeur[k]).append(" morts)");
            if (evType[k] == REFONDEE) b.append(" (apres ").append(evValeur[k]).append(")");
            if (evType[k] == ABANDONNEE) b.append(evValeur[k] == GUERRE ? " (guerre)" : " (declin)");
        }
        return b.toString();
    }

    public boolean estVivante(int ville) {
        return fin[ville] == VIVANTE;
    }

    public int[] plusGrandes(int combien) {
        int[] meilleures = new int[combien];
        Arrays.fill(meilleures, -1);
        int total = nombre;
        for (int v = 0; v < total; v++) {
            int courante = v;
            for (int k = 0; k < combien; k++) {
                if (meilleures[k] < 0 || habitants[courante] > habitants[meilleures[k]]) {
                    int ancienne = meilleures[k];
                    meilleures[k] = courante;
                    courante = ancienne;
                    if (courante < 0) break;
                }
            }
        }
        return meilleures;
    }

    public void archiver(Archive a) throws java.io.IOException {
        nombre = a.entier(nombre);
        ruines = a.entier(ruines);
        abandonsGuerre = a.entier(abandonsGuerre);
        refondations = a.entier(refondations);
        caseDe = a.entiers(caseDe, nombre);
        region = a.entiers(region, nombre);
        peuple = a.octets(peuple, nombre);
        fondation = a.courts(fondation, nombre);
        fin = a.courts(fin, nombre);
        habitants = a.entiers(habitants, nombre);
        pic = a.entiers(pic, nombre);
        anneePic = a.courts(anneePic, nombre);
        origine = a.entiers(origine, nombre);
        heritiere = a.entiers(heritiere, nombre);
        mesures = a.courts(mesures, nombre);
        if (courbe.length < mesures.length) {
            courbe = Arrays.copyOf(courbe, mesures.length);
            maitre = Arrays.copyOf(maitre, mesures.length);
            marques = Arrays.copyOf(marques, mesures.length);
        }
        for (int v = 0; v < nombre; v++) {
            int taille = Math.max(8, mesures[v]);
            if (a.enLecture()) {
                courbe[v] = new int[taille];
                maitre[v] = new int[taille];
                marques[v] = new byte[taille];
            }
            courbe[v] = a.entiers(courbe[v], mesures[v]);
            maitre[v] = a.entiers(maitre[v], mesures[v]);
            marques[v] = a.octets(marques[v], mesures[v]);
        }
        nombreEvenements = a.entier(nombreEvenements);
        evVille = a.entiers(evVille, nombreEvenements);
        evAnnee = a.courts(evAnnee, nombreEvenements);
        evType = a.octets(evType, nombreEvenements);
        evValeur = a.entiers(evValeur, nombreEvenements);
    }
}
