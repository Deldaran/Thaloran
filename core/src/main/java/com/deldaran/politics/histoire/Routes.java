package com.deldaran.politics.histoire;

import com.deldaran.politics.planet.Biome;
import com.deldaran.politics.planet.Monde;
import com.deldaran.politics.planet.Sphere;
import com.deldaran.politics.planet.Terrain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Routes {
    public static final byte SENTIER = 1, CHEMIN = 2, ROUTE = 3, VOIE = 4;
    public static final String[] NIVEAUX = { "", "sentier", "chemin", "route", "voie pavee" };
    public static final short OUVERTE = Short.MIN_VALUE;
    private static final int TAILLE_DEPART = 256;
    private static final double PORTEE_KM = 160.0, PORTEE_ROYALE_KM = 480.0;
    private static final int VOISINES = 3;
    private static final int AGE_MIN = 20;
    private static final int RECHERCHE_MAX = 8000;
    private static final float REUTILISATION = 0.6f, MONTAGNE = 1.5f, GUE = 1.5f, PENTE = 3f;

    public volatile int nombre = 0;
    public int[] villeA = new int[TAILLE_DEPART], villeB = new int[TAILLE_DEPART];
    public short[] creation = new short[TAILLE_DEPART], fin = new short[TAILLE_DEPART];
    public byte[] niveau = new byte[TAILLE_DEPART];
    public float[] longueurKm = new float[TAILLE_DEPART];
    public int[][] cases = new int[TAILLE_DEPART][];
    public byte[] niveauCase, ruineCase;
    public double kmOuverts;
    public int ouvertes;

    private final Monde monde;
    private final Sphere sphere;
    private final double espacementKm;
    private final float echelle, seuilRiviere;
    private final HashSet<Long> paires = new HashSet<>();
    private final float[] cout;
    private final int[] venu, marque;
    private int tour = 0;
    private double[] tas = new double[1024];
    private int[] tasCase = new int[1024];
    private int tailleTas;

    public Routes(Monde monde, float debitVille) {
        this.monde = monde;
        sphere = monde.sphere;
        int n = sphere.nombre;
        espacementKm = Math.sqrt(4.0 * Math.PI * Terrain.RAYON_KM * Terrain.RAYON_KM / n);
        niveauCase = new byte[n];
        ruineCase = new byte[n];
        cout = new float[n];
        venu = new int[n];
        marque = new int[n];
        float[] ecarts = new float[n];
        int compte = 0;
        for (int c = 0; c < n; c++) {
            if (monde.estSousLEau(c)) continue;
            int d = sphere.voisin(c, 0);
            ecarts[compte++] = Math.abs(monde.altitude[c] - monde.altitude[d]);
        }
        Arrays.sort(ecarts, 0, compte);
        echelle = compte > 0 ? Math.max(1e-6f, ecarts[(int) (compte * 0.95)]) : 1f;
        seuilRiviere = debitVille * 2f;
    }

    public void avancer(Population p, int annee) {
        Villes v = p.villes;
        Etats e = p.etats;
        for (int r = 0; r < nombre; r++) {
            if (fin[r] != OUVERTE) continue;
            int a = villeA[r], b = villeB[r];
            if (!v.estVivante(a) || !v.estVivante(b)) {
                fin[r] = (short) annee;
                continue;
            }
            int m = Math.min(v.habitants[a], v.habitants[b]);
            int ea = e.regionEtat[v.region[a]], eb = e.regionEtat[v.region[b]];
            boolean meme = ea >= 0 && ea == eb && e.estVivant(ea);
            boolean royale = meme && (e.capitale[ea] == a || e.capitale[ea] == b);
            byte voulu = SENTIER;
            if (m >= 400) voulu = CHEMIN;
            if (m >= 1500 || (royale && m >= 400)) voulu = ROUTE;
            if (royale && m >= 2500) voulu = VOIE;
            if (voulu > niveau[r]) niveau[r] = voulu;
        }
        int total = v.nombre;
        double portee = 2.0 * Math.sin(PORTEE_KM / Terrain.RAYON_KM / 2.0);
        int[] proches = new int[VOISINES];
        double[] distances = new double[VOISINES];
        for (int a = 0; a < total; a++) {
            if (!v.estVivante(a) || annee - v.fondation[a] < AGE_MIN) continue;
            int ca = v.caseDe[a];
            Arrays.fill(proches, -1);
            Arrays.fill(distances, Double.MAX_VALUE);
            for (int b = 0; b < total; b++) {
                if (b == a || !v.estVivante(b) || annee - v.fondation[b] < AGE_MIN) continue;
                double d = distance(ca, v.caseDe[b]);
                if (d > portee || d >= distances[VOISINES - 1]) continue;
                int k = VOISINES - 1;
                while (k > 0 && distances[k - 1] > d) {
                    distances[k] = distances[k - 1];
                    proches[k] = proches[k - 1];
                    k--;
                }
                distances[k] = d;
                proches[k] = b;
            }
            for (int b : proches) {
                if (b >= 0) relier(p, a, b, annee, PORTEE_KM);
            }
            int etat = e.regionEtat[v.region[a]];
            if (etat >= 0 && e.estVivant(etat)) {
                int capitale = e.capitale[etat];
                if (capitale >= 0 && capitale != a && v.estVivante(capitale)) relier(p, a, capitale, annee, PORTEE_ROYALE_KM);
            }
        }
        recalculer();
    }

    private double distance(int c, int d) {
        double dx = sphere.x[c] - sphere.x[d], dy = sphere.y[c] - sphere.y[d], dz = sphere.z[c] - sphere.z[d];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void relier(Population p, int a, int b, int annee, double porteeKm) {
        long cle = a < b ? ((long) a << 32) | b : ((long) b << 32) | a;
        if (paires.contains(cle)) return;
        Villes v = p.villes;
        if (distance(v.caseDe[a], v.caseDe[b]) * Terrain.RAYON_KM > porteeKm) return;
        paires.add(cle);
        int[] chemin = chercher(v.caseDe[a], v.caseDe[b]);
        if (chemin == null) return;
        int r = nombre;
        if (r == villeA.length) agrandir();
        villeA[r] = a;
        villeB[r] = b;
        creation[r] = (short) annee;
        fin[r] = OUVERTE;
        niveau[r] = SENTIER;
        cases[r] = chemin;
        float km = 0f;
        for (int k = 1; k < chemin.length; k++) km += (float) (distance(chemin[k - 1], chemin[k]) * Terrain.RAYON_KM);
        longueurKm[r] = km;
        for (int c : chemin) {
            if (niveauCase[c] < SENTIER) niveauCase[c] = SENTIER;
        }
        nombre = r + 1;
    }

    private void agrandir() {
        int n = villeA.length * 2;
        villeA = Arrays.copyOf(villeA, n);
        villeB = Arrays.copyOf(villeB, n);
        creation = Arrays.copyOf(creation, n);
        fin = Arrays.copyOf(fin, n);
        niveau = Arrays.copyOf(niveau, n);
        longueurKm = Arrays.copyOf(longueurKm, n);
        cases = Arrays.copyOf(cases, n);
    }

    private int[] chercher(int depart, int arrivee) {
        tour++;
        tailleTas = 0;
        cout[depart] = 0f;
        venu[depart] = -1;
        marque[depart] = tour;
        pousser(heuristique(depart, arrivee), depart);
        int explores = 0;
        while (tailleTas > 0 && explores < RECHERCHE_MAX) {
            int c = tasCase[0];
            double f = tas[0];
            retirer();
            if (c == arrivee) break;
            if (f - heuristique(c, arrivee) > cout[c] + 1e-4) continue;
            explores++;
            for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                int d = sphere.voisin(c, k);
                if (d < 0) continue;
                if (d != arrivee && monde.estSousLEau(d)) continue;
                float g = cout[c] + pas(c, d);
                if (marque[d] == tour && g >= cout[d]) continue;
                marque[d] = tour;
                cout[d] = g;
                venu[d] = c;
                pousser(g + heuristique(d, arrivee), d);
            }
        }
        if (marque[arrivee] != tour) return null;
        List<Integer> l = new ArrayList<>();
        for (int c = arrivee; c >= 0; c = venu[c]) l.add(c);
        int[] chemin = new int[l.size()];
        for (int k = 0; k < chemin.length; k++) chemin[k] = l.get(chemin.length - 1 - k);
        return chemin;
    }

    private double heuristique(int c, int d) {
        return distance(c, d) * Terrain.RAYON_KM / espacementKm * REUTILISATION;
    }

    private float pas(int c, int d) {
        float longueur = (float) (distance(c, d) * Terrain.RAYON_KM / espacementKm);
        float facteur = 1f + PENTE * Math.abs(monde.altitude[d] - monde.altitude[c]) / echelle;
        if (monde.biome[d] == Biome.MONTAGNE) facteur += MONTAGNE;
        if (monde.debit[d] > seuilRiviere) facteur += GUE;
        if (niveauCase[d] > 0) facteur *= REUTILISATION;
        return longueur * facteur;
    }

    private void pousser(double f, int c) {
        if (tailleTas == tas.length) {
            tas = Arrays.copyOf(tas, tailleTas * 2);
            tasCase = Arrays.copyOf(tasCase, tailleTas * 2);
        }
        int i = tailleTas++;
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (tas[parent] <= f) break;
            tas[i] = tas[parent];
            tasCase[i] = tasCase[parent];
            i = parent;
        }
        tas[i] = f;
        tasCase[i] = c;
    }

    private void retirer() {
        tailleTas--;
        if (tailleTas == 0) return;
        double f = tas[tailleTas];
        int c = tasCase[tailleTas];
        int i = 0;
        while (true) {
            int g = i * 2 + 1;
            if (g >= tailleTas) break;
            if (g + 1 < tailleTas && tas[g + 1] < tas[g]) g++;
            if (tas[g] >= f) break;
            tas[i] = tas[g];
            tasCase[i] = tasCase[g];
            i = g;
        }
        tas[i] = f;
        tasCase[i] = c;
    }

    public void recalculer() {
        Arrays.fill(niveauCase, (byte) 0);
        Arrays.fill(ruineCase, (byte) 0);
        kmOuverts = 0.0;
        ouvertes = 0;
        for (int r = 0; r < nombre; r++) {
            boolean ouverte = fin[r] == OUVERTE;
            byte[] cible = ouverte ? niveauCase : ruineCase;
            for (int c : cases[r]) {
                if (cible[c] < niveau[r]) cible[c] = niveau[r];
            }
            if (ouverte) {
                ouvertes++;
                kmOuverts += longueurKm[r];
            }
        }
    }

    public int compter(byte voulu) {
        int n = 0;
        for (int r = 0; r < nombre; r++) {
            if (fin[r] == OUVERTE && niveau[r] == voulu) n++;
        }
        return n;
    }

    public String decrire(Population p, int ville) {
        StringBuilder b = new StringBuilder();
        for (int r = 0; r < nombre; r++) {
            if (villeA[r] != ville && villeB[r] != ville) continue;
            int autre = villeA[r] == ville ? villeB[r] : villeA[r];
            if (b.length() > 0) b.append(", ");
            b.append(Noms.ville(p.villes.peuple[autre], p.graine, autre)).append(" (").append(NIVEAUX[niveau[r]]).append(", ").append(Math.round(longueurKm[r])).append(" km");
            if (fin[r] != OUVERTE) b.append(", abandonnee an ").append(fin[r]);
            b.append(')');
        }
        return b.length() == 0 ? "aucune" : b.toString();
    }

    public void archiver(Archive a) throws java.io.IOException {
        nombre = a.entier(nombre);
        villeA = a.entiers(villeA, nombre);
        villeB = a.entiers(villeB, nombre);
        creation = a.courts(creation, nombre);
        fin = a.courts(fin, nombre);
        niveau = a.octets(niveau, nombre);
        longueurKm = a.reels(longueurKm, nombre);
        if (cases.length < villeA.length) cases = Arrays.copyOf(cases, villeA.length);
        for (int r = 0; r < nombre; r++) {
            int longueur = a.entier(cases[r] == null ? 0 : cases[r].length);
            if (a.enLecture()) cases[r] = new int[longueur];
            cases[r] = a.entiers(cases[r], longueur);
        }
        if (a.enLecture()) {
            paires.clear();
            for (int r = 0; r < nombre; r++) {
                int x = Math.min(villeA[r], villeB[r]), y = Math.max(villeA[r], villeB[r]);
                paires.add(((long) x << 32) | y);
            }
            recalculer();
        }
    }
}
