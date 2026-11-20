package com.deldaran.politics.histoire;

import com.deldaran.politics.Tenue;
import com.deldaran.politics.Traits;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Lot K3 : l'apparence que le joueur choisit à la création : le visage (une graine), la coiffure, la barbe, l'habit et ses
 * couleurs, la cape. Chaque réglage vaut -1 tant qu'on n'y a pas touché : on garde alors ce que la graine donne.
 */
public final class Aspect {
    public static final int TUNIQUE = 0, CHEMISE = 1, ROBE = 2;
    public static final String[] VETEMENTS = { "Tunique", "Chemise", "Robe" };
    public static final int SANS_CAPE = 0, CAPE = 1, CAPUCHE = 2;
    public static final String[] CAPES = { "Aucune", "Cape", "Capuche" };
    /** Les rangées de palettes/sol.png qu'on peut prendre pour les tissus : des bruns, des gris, des verts, du sang, du pourpre… */
    public static final int[] TISSUS = { 2, 9, 4, 5, 23, 11, 10, 13, 15, 17, 19, 20, 21, 18, 16, 22 };
    /** La longueur des cheveux (cm) qu'on donne à chaque coupe choisie. */
    private static final double[] LONGUEURS = { 0.5, 5, 18, 40, 30, 38, 30, 6 };

    public int visage;
    public int coupe = -1, barbe = -1, vetement = -1, haut = -1, bas = -1, cape = -1, couleurCape = -1;

    public Aspect copie() {
        Aspect a = new Aspect();
        a.visage = visage;
        a.coupe = coupe;
        a.barbe = barbe;
        a.vetement = vetement;
        a.haut = haut;
        a.bas = bas;
        a.cape = cape;
        a.couleurCape = couleurCape;
        return a;
    }

    /** La graine de l'apparence d'un personnage né sous cet identifiant avec ce visage (la même dans l'aperçu et dans le monde). */
    public static long graine(long id, int visage) {
        return id * 31L + visage * 1000003L;
    }

    /** Les traits (coiffure, barbe) : ceux de la graine, puis les choix du joueur. */
    public Traits traits(long graine, int espece, boolean femme) {
        Traits t = Traits.tirer(graine, espece, femme, false);
        appliquer(t);
        return t;
    }

    public void appliquer(Traits t) {
        if (coupe >= 0 && coupe != t.coupe) {
            t.coupe = coupe;
            t.cheveux = LONGUEURS[coupe];
        }
        if (barbe >= 0 && barbe != t.barbe) {
            t.barbe = barbe;
            t.longueurBarbe = barbe == Traits.SANS_BARBE ? 0 : barbe == Traits.TRESSEE ? 25 : 6;
        }
    }

    /** La tenue de tous les jours : celle de la graine, avec l'habit, les couleurs et la cape choisis. */
    public Tenue tenue(long graine, boolean femme) {
        Tenue t = Tenue.tirer(graine, femme, false);
        appliquer(t);
        return t;
    }

    public void appliquer(Tenue t) {
        int v = vetementDe(t);
        int couleurHaut = haut >= 0 ? haut : t.couleurs[pieceHaut(v)];
        int couleurBas = bas >= 0 ? bas : t.porte(Tenue.PANTALON) ? t.couleurs[Tenue.PANTALON] : couleurHaut;
        if (vetement >= 0 || haut >= 0 || bas >= 0) {
            t.enlever(Tenue.TUNIQUE).enlever(Tenue.CHEMISE).enlever(Tenue.ROBE).enlever(Tenue.PANTALON);
            int vv = vetement >= 0 ? vetement : v;
            t.mettre(pieceHaut(vv), couleurHaut);
            if (vv != ROBE) t.mettre(Tenue.PANTALON, couleurBas);
        }
        int c = capeDe(t);
        int teinte = couleurCape >= 0 ? couleurCape : t.porte(Tenue.CAPE) ? t.couleurs[Tenue.CAPE] : t.porte(Tenue.CAPUCHE) ? t.couleurs[Tenue.CAPUCHE] : TISSUS[0];
        if (cape >= 0 || couleurCape >= 0) {
            int cc = cape >= 0 ? cape : c;
            t.enlever(Tenue.CAPE).enlever(Tenue.CAPUCHE);
            if (cc >= CAPE) t.mettre(Tenue.CAPE, teinte);
            if (cc == CAPUCHE) t.mettre(Tenue.CAPUCHE, teinte);
        }
    }

    public static int vetementDe(Tenue t) {
        return t.porte(Tenue.ROBE) ? ROBE : t.porte(Tenue.CHEMISE) ? CHEMISE : TUNIQUE;
    }

    public static int pieceHaut(int vetement) {
        return vetement == ROBE ? Tenue.ROBE : vetement == CHEMISE ? Tenue.CHEMISE : Tenue.TUNIQUE;
    }

    public static int capeDe(Tenue t) {
        return t.porte(Tenue.CAPUCHE) ? CAPUCHE : t.porte(Tenue.CAPE) ? CAPE : SANS_CAPE;
    }

    /** Le tissu suivant (ou précédent) dans TISSUS, à partir d'une rangée de la palette. */
    public static int tissuSuivant(int rangee, int sens) {
        int i = 0;
        for (int k = 0; k < TISSUS.length; k++) if (TISSUS[k] == rangee) i = k;
        return TISSUS[Math.floorMod(i + sens, TISSUS.length)];
    }

    public void ecrire(DataOutputStream s) throws IOException {
        s.writeInt(visage);
        s.writeByte(coupe);
        s.writeByte(barbe);
        s.writeByte(vetement);
        s.writeByte(haut);
        s.writeByte(bas);
        s.writeByte(cape);
        s.writeByte(couleurCape);
    }

    public static Aspect lire(DataInputStream s) throws IOException {
        Aspect a = new Aspect();
        a.visage = s.readInt();
        a.coupe = s.readByte();
        a.barbe = s.readByte();
        a.vetement = s.readByte();
        a.haut = s.readByte();
        a.bas = s.readByte();
        a.cape = s.readByte();
        a.couleurCape = s.readByte();
        return a;
    }
}
