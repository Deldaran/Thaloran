package com.deldaran.politics.histoire;

import java.util.Arrays;
import java.util.HashMap;

public class Maisons {
    public static final byte ETRANGER = 0, ENFANT = 1, PARENT = 2;
    private static final int TAILLE_DEPART = 256;

    public volatile int nombre = 0;
    public byte[] peuple = new byte[TAILLE_DEPART];
    public long[] fondateur = new long[TAILLE_DEPART];
    public short[] fondation = new short[TAILLE_DEPART];
    public int[] regnes = new int[TAILLE_DEPART];
    public int[] anneesRegnees = new int[TAILLE_DEPART];
    public int[] dernierEtat = new int[TAILLE_DEPART];

    public volatile int nombreRegnes = 0;
    public long[] personne = new long[TAILLE_DEPART];
    public byte[] sexe = new byte[TAILLE_DEPART];
    public int[] maison = new int[TAILLE_DEPART];
    public int[] etat = new int[TAILLE_DEPART];
    public short[] debut = new short[TAILLE_DEPART];
    public short[] fin = new short[TAILLE_DEPART];
    public int[] precedent = new int[TAILLE_DEPART];
    public byte[] lien = new byte[TAILLE_DEPART];

    private final HashMap<Long, Integer> parPersonne = new HashMap<>();

    public int fonder(int indicePeuple, long premier, int annee, int etatFonde) {
        int m = nombre;
        if (m == peuple.length) {
            int n = m * 2;
            peuple = Arrays.copyOf(peuple, n);
            fondateur = Arrays.copyOf(fondateur, n);
            fondation = Arrays.copyOf(fondation, n);
            regnes = Arrays.copyOf(regnes, n);
            anneesRegnees = Arrays.copyOf(anneesRegnees, n);
            dernierEtat = Arrays.copyOf(dernierEtat, n);
        }
        peuple[m] = (byte) indicePeuple;
        fondateur[m] = premier;
        fondation[m] = (short) annee;
        dernierEtat[m] = etatFonde;
        nombre = m + 1;
        return m;
    }

    public int commencer(long id, int sexeRegnant, int m, int e, int annee, int avant, byte parente) {
        int r = nombreRegnes;
        if (r == personne.length) {
            int n = r * 2;
            personne = Arrays.copyOf(personne, n);
            sexe = Arrays.copyOf(sexe, n);
            maison = Arrays.copyOf(maison, n);
            etat = Arrays.copyOf(etat, n);
            debut = Arrays.copyOf(debut, n);
            fin = Arrays.copyOf(fin, n);
            precedent = Arrays.copyOf(precedent, n);
            lien = Arrays.copyOf(lien, n);
        }
        personne[r] = id;
        sexe[r] = (byte) sexeRegnant;
        maison[r] = m;
        etat[r] = e;
        debut[r] = (short) annee;
        fin[r] = Etats.VIVANT;
        precedent[r] = avant;
        lien[r] = parente;
        regnes[m]++;
        dernierEtat[m] = e;
        parPersonne.put(id, r);
        nombreRegnes = r + 1;
        return r;
    }

    public void finir(int r, int annee) {
        if (r < 0 || fin[r] != Etats.VIVANT) return;
        fin[r] = (short) annee;
        anneesRegnees[maison[r]] += annee - debut[r];
    }

    public int regneDe(long id) {
        Integer r = parPersonne.get(id);
        return r == null ? -1 : r;
    }

    public int sexeDe(long id) {
        int r = regneDe(id);
        return r < 0 ? 0 : sexe[r];
    }

    public static boolean porteRegnant(byte type) {
        return type == Chronique.FONDATION || type == Chronique.SUCCESSION || type == Chronique.REVOLUTION || type == Chronique.CRISE || type == Chronique.REGENCE || type == Chronique.USURPATION;
    }
    public void archiver(Archive a) throws java.io.IOException {
        nombre = a.entier(nombre);
        peuple = a.octets(peuple, nombre);
        fondateur = a.longs(fondateur, nombre);
        fondation = a.courts(fondation, nombre);
        regnes = a.entiers(regnes, nombre);
        anneesRegnees = a.entiers(anneesRegnees, nombre);
        dernierEtat = a.entiers(dernierEtat, nombre);
        nombreRegnes = a.entier(nombreRegnes);
        personne = a.longs(personne, nombreRegnes);
        sexe = a.octets(sexe, nombreRegnes);
        maison = a.entiers(maison, nombreRegnes);
        etat = a.entiers(etat, nombreRegnes);
        debut = a.courts(debut, nombreRegnes);
        fin = a.courts(fin, nombreRegnes);
        precedent = a.entiers(precedent, nombreRegnes);
        lien = a.octets(lien, nombreRegnes);
        parPersonne.clear();
        for (int r = 0; r < nombreRegnes; r++) {
            parPersonne.put(personne[r], r);
        }
    }

}
