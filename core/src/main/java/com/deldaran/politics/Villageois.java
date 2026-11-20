package com.deldaran.politics;

import java.util.Random;

public class Villageois {
    public static final int DORMIR = 0, REVEIL = 1, TRAVAIL = 2, MANGER = 3, LOISIR = 4, JOUER = 5, PAUSE = 6, DISCUTER = 7, SALUER = 8, FUIR = 9, TRAQUER = 10, COMBATTRE = 11, A_TERRE = 12, MORT = 13;
    public static final String[] NOMS = { "dort", "se reveille", "travaille", "mange", "se detend", "joue", "fait une pause", "discute", "salue", "fuit", "traque le joueur", "se bat", "est a terre", "est mort" };

    public final Personnage corps;
    public final Villages.Village village;
    /** Lot I : la maison peut changer (un mariage). */
    public int maison;
    public final int metier;
    public int travail;
    public final boolean enfant;
    public Villageois conjoint;
    public final Random hasard;
    public final double decalage;
    public float faim, fatigue, social;
    /** Lot H4 : le sommeil et l'envie de se distraire (0 comblé … 1 pressant) ; le salaire gagné qui n'est pas encore compté. */
    public float sommeil, envie, gain;
    /** Lot H4 : sa bourse (cuivre), son salaire (cuivre par heure de travail), ses heures (travail, lever, coucher). */
    public int bourse = 40;
    public float salaire = 1f, debut = 7f, fin = 18f, lever = 6f, coucher = 22f;
    /** Lot H4 : trop vieux pour travailler ; important (sa bourse est celle de sa fiche). */
    public boolean vieux, important;
    /** Lot H5 : un ouvrier du joueur ; les heures d'ouvrage faites sous les yeux du joueur, pas encore comptées. */
    public boolean ouvrier;
    public float ouvrage;
    public int activite = -1, lieu = -1;
    public boolean dedans, enRoute, affiche;
    public float[] chemin;
    public int etape;
    public float reste, gesteReste;
    public double cibleA, cibleB, regardA, regardB;
    public boolean attentChemin;
    public Villageois interlocuteur;
    public float salutAttente;
    public final long graine;
    public Relations.Lien lien;
    public float sante = 1f, coupAttente, cheminAttente;
    public boolean presente, garde;
    /** Geste à faire, tourné vers le joueur, en arrivant près de lui (le marchand qui vient servir) ; null sinon. */
    public String servir;
    /** Lot G3 : l'étal de la place que ce villageois tient le jour (indice d'objet du village), -1 sinon. */
    public int etal = -1;

    /** Lot H2 : le nom de l'habitant de la Foule qu'il est (sa graine est son id) ; null pour un villageois tiré de sa maison. */
    public String nomReel;

    public Villageois(Personnage corps, Villages.Village village, int maison, int travail, int metier, boolean enfant, long graine) {
        this.corps = corps;
        this.village = village;
        this.maison = maison;
        this.travail = travail;
        this.metier = metier;
        this.enfant = enfant;
        this.graine = graine;
        hasard = new Random(graine);
        decalage = (hasard.nextDouble() - 0.5) * 1.5;
        faim = hasard.nextFloat() * 0.5f;
        social = hasard.nextFloat() * 0.5f;
        sommeil = hasard.nextFloat() * 0.4f;
        envie = hasard.nextFloat() * 0.4f;
        if (enfant) coucher = 20f;
        corps.vie = this;
    }

    public String description() {
        String quoi = activite < 0 ? "-" : NOMS[activite];
        String metierNom = travail >= 0 ? Metiers.NOMS[village.metiers[travail]] : enfant ? "enfant" : "foyer";
        String qui = lien != null ? lien.nomComplet() + ", " : "";
        return qui + metierNom + ", " + quoi + (enRoute ? " (en route)" : "") + String.format(" faim %.0f%% fatigue %.0f%% social %.0f%% sommeil %.0f%% envie %.0f%% bourse %d", faim * 100f, fatigue * 100f, social * 100f, sommeil * 100f, envie * 100f, bourse);
    }
}
