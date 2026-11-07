package com.deldaran.politics.histoire;

public class Villes {
    public static final short VIVANTE = Short.MIN_VALUE;
    private static final int TAILLE_DEPART = 256;

    public volatile int nombre = 0;
    public int ruines = 0;
    public int[] caseDe = new int[TAILLE_DEPART];
    public int[] region = new int[TAILLE_DEPART];
    public byte[] peuple = new byte[TAILLE_DEPART];
    public short[] fondation = new short[TAILLE_DEPART];
    public short[] fin = new short[TAILLE_DEPART];
    public int[] habitants = new int[TAILLE_DEPART];
    public int[] pic = new int[TAILLE_DEPART];
    public short[] anneePic = new short[TAILLE_DEPART];

    public int ajouter(int caseVille, int regionVille, int indicePeuple, int annee) {
        int n = nombre;
        if (n == caseDe.length) agrandir();
        caseDe[n] = caseVille;
        region[n] = regionVille;
        peuple[n] = (byte) indicePeuple;
        fondation[n] = (short) annee;
        fin[n] = VIVANTE;
        anneePic[n] = (short) annee;
        nombre = n + 1;
        return n;
    }

    private void agrandir() {
        int n = caseDe.length * 2;
        caseDe = java.util.Arrays.copyOf(caseDe, n);
        region = java.util.Arrays.copyOf(region, n);
        peuple = java.util.Arrays.copyOf(peuple, n);
        fondation = java.util.Arrays.copyOf(fondation, n);
        fin = java.util.Arrays.copyOf(fin, n);
        habitants = java.util.Arrays.copyOf(habitants, n);
        pic = java.util.Arrays.copyOf(pic, n);
        anneePic = java.util.Arrays.copyOf(anneePic, n);
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
        }
    }

    public boolean estVivante(int ville) {
        return fin[ville] == VIVANTE;
    }

    public int[] plusGrandes(int combien) {
        int[] meilleures = new int[combien];
        java.util.Arrays.fill(meilleures, -1);
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
}
