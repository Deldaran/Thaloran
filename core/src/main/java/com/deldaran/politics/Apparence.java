package com.deldaran.politics;

public class Apparence {
    public static final int CORPS = 0, VISAGE = 1, PANTALON = 2, BOTTES = 3, MANTEAU = 4, TUNIQUE = 5, ROBE = 6, ARMURE = 7, CEINTURE = 8;
    public static final int COURT = 9, CHIGNON = 10, LONG = 11, MECHE = 12, CRETE = 13, BARBE_COURTE = 14, BARBE_LONGUE = 15;
    public static final int CAPUCHE = 16, CASQUE = 17, POINTU = 18, BATON = 19;

    public static final int CHEVEUX = 4, TISSU = 5, CUIR = 6, METAL = 7, BOIS = 8, YEUX = 9;

    public final long graine;
    public final int espece;
    public final int[] pieces = new int[14];
    public final int[] lignes = new int[14];
    public final int[] rampes = new int[14];
    public int nombre;
    public int yeux;
    public int cheveux;

    public Apparence(long graine, int espece) {
        this.graine = graine;
        this.espece = espece;
    }

    public void ajouter(int piece, int ligne, int rampe) {
        pieces[nombre] = piece;
        lignes[nombre] = ligne;
        rampes[nombre] = rampe;
        nombre++;
    }

    public long cle() {
        return graine * 4L + espece;
    }
}
