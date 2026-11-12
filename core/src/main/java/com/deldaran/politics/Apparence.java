package com.deldaran.politics;

public class Apparence {
    public static final int CHAPEAU = 0, HAUT = 1, BAS = 2, BOTTES = 3, COIFFURE = 4, BARBE = 5, CAPE = 6, EPAULES = 7, BRAS = 8, COU = 9;
    public static final int CATEGORIES = 10;

    public final long graine;
    public final int espece;
    public boolean femme;
    public boolean enfant;
    public int peau;
    public final int[] categories = new int[CATEGORIES];
    public final int[] objets = new int[CATEGORIES];
    public final int[][] couleurs = new int[CATEGORIES][];
    public int nombre;

    public Apparence(long graine, int espece) {
        this.graine = graine;
        this.espece = espece;
    }

    public void ajouter(int categorie, int objet, int[] couleursObjet) {
        categories[nombre] = categorie;
        objets[nombre] = objet;
        couleurs[nombre] = couleursObjet;
        nombre++;
    }

    public long cle() {
        return graine * 4L + espece;
    }
}
