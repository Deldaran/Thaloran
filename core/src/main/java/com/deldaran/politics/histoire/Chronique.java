package com.deldaran.politics.histoire;

public class Chronique {
    public static final byte FONDATION = 0, SUCCESSION = 1, GUERRE = 2, PAIX = 3, CONQUETE = 4, SAC = 5, CHUTE = 6, REVOLUTION = 7, SECESSION = 8, EXTINCTION = 9, CRISE = 10, REGENCE = 11, USURPATION = 12;
    public static final int TYPES = 13;

    private static final int TAILLE_DEPART = 1024;

    public volatile int nombre = 0;
    public final int[] parType = new int[TYPES];
    public short[] annee = new short[TAILLE_DEPART];
    public byte[] type = new byte[TAILLE_DEPART];
    public int[] sujet = new int[TAILLE_DEPART];
    public int[] objet = new int[TAILLE_DEPART];
    public int[] lieu = new int[TAILLE_DEPART];
    public long[] valeur = new long[TAILLE_DEPART];

    public void noter(int quand, byte quoi, int qui, int contre, int ou, long detail) {
        int n = nombre;
        if (n == annee.length) agrandir();
        annee[n] = (short) quand;
        type[n] = quoi;
        sujet[n] = qui;
        objet[n] = contre;
        lieu[n] = ou;
        valeur[n] = detail;
        parType[quoi]++;
        nombre = n + 1;
    }

    private void agrandir() {
        int n = annee.length * 2;
        annee = java.util.Arrays.copyOf(annee, n);
        type = java.util.Arrays.copyOf(type, n);
        sujet = java.util.Arrays.copyOf(sujet, n);
        objet = java.util.Arrays.copyOf(objet, n);
        lieu = java.util.Arrays.copyOf(lieu, n);
        valeur = java.util.Arrays.copyOf(valeur, n);
    }

    public String texte(Population p, int i) {
        Etats e = p.etats;
        String a = nomEtat(p, sujet[i]);
        String b = objet[i] >= 0 ? nomEtat(p, objet[i]) : "";
        int q = e.peuple[sujet[i]];
        String debut = "An " + annee[i] + " : ";
        switch (type[i]) {
            case FONDATION: return debut + personne(p, q, i) + " fonde " + a + ".";
            case SUCCESSION: return debut + personne(p, q, i) + " prend le pouvoir a " + a + ".";
            case CRISE: return debut + "sans heritier a " + a + ", " + personne(p, q, i) + " prend le trone.";
            case REGENCE: return debut + "regence a " + a + " pour l'enfant " + personne(p, q, i) + ".";
            case USURPATION: return debut + personne(p, q, i) + " usurpe le trone de " + a + ".";

            case GUERRE: return debut + a + " attaque " + b + (lieu[i] >= 0 ? " pour son " + Economie.NOMS[lieu[i]] : "") + ".";
            case PAIX: return debut + "paix entre " + a + " et " + b + ".";
            case CONQUETE: return debut + a + " prend une region a " + b + ".";
            case SAC: return debut + a + " met a sac une ville de " + b + " : " + valeur[i] + " morts.";
            case CHUTE: return debut + a + " tombe sous les coups de " + b + ".";
            case REVOLUTION: return debut + "a " + a + ", le peuple renverse et tue " + personne(p, q, i) + ".";

            case SECESSION: return debut + a + " fait secession de " + b + ".";
            default: return debut + a + " s'eteint, faute de successeur.";
        }
    }

    private String personne(Population p, int q, int i) {
        return Noms.personne(q, p.graine, valeur[i], p.maisons.sexeDe(valeur[i]));
    }


    public int recent(int rang) {
        int vus = 0;
        for (int i = nombre - 1; i >= 0; i--) {
            if (type[i] == SUCCESSION || type[i] == PAIX || type[i] == CONQUETE) continue;
            if (vus++ == rang) return i;
        }
        return -1;
    }

    private String nomEtat(Population p, int e) {
        return Noms.ville(p.etats.peuple[e], p.graine, p.etats.capitale[e]);
    }
    public void archiver(Archive a) throws java.io.IOException {
        nombre = a.entier(nombre);
        a.entiers(parType, TYPES);
        annee = a.courts(annee, nombre);
        type = a.octets(type, nombre);
        sujet = a.entiers(sujet, nombre);
        objet = a.entiers(objet, nombre);
        lieu = a.entiers(lieu, nombre);
        valeur = a.longs(valeur, nombre);
    }

}
