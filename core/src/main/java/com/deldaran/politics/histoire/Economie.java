package com.deldaran.politics.histoire;

public class Economie {
    public static final int NOURRITURE = 0, BOIS = 1, MINERAI = 2, NOMBRE = 3;
    public static final String[] NOMS = { "nourriture", "bois", "minerai" };

    public static class Reglages {
        public float rendement = 1.5f;
        public float boisParCase = 40f;
        public float mineraiParCase = 40f;
        public float partBucherons = 0.1f;
        public float partMineurs = 0.1f;
        public float boisHabitant = 0.02f;
        public float boisUrbain = 0.1f;
        public float mineraiHabitant = 0.005f;
        public float mineraiUrbain = 0.05f;
        public float prixDemande = 1f;
        public float prixMin = 0.1f;
        public float prixMax = 10f;
        public float coutTerre = 0.1f;
        public float coutMer = 0.02f;
        public float force = 0.5f;
        public int etapes = 3;
        public float lissage = 0.1f;
        public float emplois = 1f;
        public float partCommerce = 0.6f;
        public final float[] perte = { 0.5f, 0.05f, 0f };
        public final float[] reserve = { 0.1f, 1f, 1f };
    }

    public final Reglages reglages;
    public final float[][] stock;
    public final float[][] production;
    public final float[][] besoin;
    public final float[][] prix;
    public final float[][] valeur;
    public final float[] importe;
    public final float[] exporte;
    public final float[] trafic;
    public final double[] bourse;
    public final double[] produit = new double[NOMBRE];
    public final double[] consomme = new double[NOMBRE];
    public final double[] perdu = new double[NOMBRE];
    public final double[] echange = new double[NOMBRE];

    private final Regions regions;
    private final float[][] suivant;
    private final float[][] arrivee;
    private final float[] traficAnnee;
    private final float[] sorties;

    public Economie(Regions regions, Reglages reglages) {
        this.regions = regions;
        this.reglages = reglages;
        int n = regions.nombre;
        stock = new float[NOMBRE][n];
        production = new float[NOMBRE][n];
        besoin = new float[NOMBRE][n];
        prix = new float[NOMBRE][n];
        valeur = new float[NOMBRE][n];
        suivant = new float[NOMBRE][n];
        arrivee = new float[NOMBRE][n];
        importe = new float[n];
        exporte = new float[n];
        sorties = new float[n];
        trafic = new float[n];
        traficAnnee = new float[n];
        bourse = new double[n];
    }

    public void avancer(Population p) {
        java.util.Arrays.fill(traficAnnee, 0f);
        java.util.Arrays.fill(sorties, 0f);
        for (int r = 0; r < regions.nombre; r++) {
            if (regions.estTerre[r]) produire(p, r);
        }
        for (int g = 0; g < NOMBRE; g++) {
            for (int r = 0; r < regions.nombre; r++) {
                consommer(g, r);
            }
            for (int e = 0; e < reglages.etapes; e++) {
                propager(g);
                echanger(g);
            }
        }
        for (int r = 0; r < regions.nombre; r++) {
            trafic[r] += reglages.lissage * (traficAnnee[r] - trafic[r]);
            exporte[r] += reglages.lissage * (Math.max(0f, sorties[r]) - exporte[r]);
        }
    }

    private void produire(Population p, int r) {
        int habitants = 0, urbains = 0;
        float nourriture = 0f, terres = 0f;
        for (int q = 0; q < Peuple.NOMBRE; q++) {
            Foule f = p.foules[q][r];
            if (f == null) continue;
            int villes = f.urbains();
            habitants += f.vivants;
            urbains += villes;
            nourriture += (f.vivants - villes) * reglages.rendement;
            if (f.vivants > 0) terres = Math.max(terres, p.capacite[q][r]);
        }
        int ruraux = habitants - urbains;
        production[NOURRITURE][r] = Math.min(nourriture, terres);
        production[BOIS][r] = Math.min(ruraux * reglages.partBucherons, regions.bois[r] * reglages.boisParCase);
        production[MINERAI][r] = Math.min(ruraux * reglages.partMineurs, regions.minerai[r] * reglages.mineraiParCase);
        besoin[NOURRITURE][r] = habitants;
        besoin[BOIS][r] = habitants * reglages.boisHabitant + urbains * reglages.boisUrbain;
        besoin[MINERAI][r] = habitants * reglages.mineraiHabitant + urbains * reglages.mineraiUrbain;
    }

    private void consommer(int g, int r) {
        if (!regions.estTerre[r]) return;
        float fabrique = production[g][r], voulu = besoin[g][r];
        float disponible = stock[g][r] + fabrique;
        float mange = Math.min(disponible, voulu);
        float reste = disponible - mange;
        float pourri = reste * reglages.perte[g];
        stock[g][r] = reste - pourri;
        produit[g] += fabrique;
        consomme[g] += mange;
        perdu[g] += pourri;
        if (g == NOURRITURE) {
            importe[r] += reglages.lissage * (Math.max(0f, mange - fabrique) - importe[r]);
        }
        prix[g][r] = voulu > 0f ? coursDe(voulu * reglages.reserve[g], stock[g][r]) : 0f;
    }

    private float coursDe(float attendu, float disponible) {
        if (disponible <= 0f) return reglages.prixMax;
        return Math.max(reglages.prixMin, Math.min(reglages.prixMax, attendu / disponible));
    }

    private float cout(int depuis, int vers) {
        return regions.estTerre[depuis] && regions.estTerre[vers] ? reglages.coutTerre : reglages.coutMer;
    }

    private void propager(int g) {
        float[] v = valeur[g], s = suivant[g], local = prix[g];
        for (int r = 0; r < regions.nombre; r++) {
            float meilleure = local[r];
            for (int k = regions.debutVoisins[r]; k < regions.debutVoisins[r + 1]; k++) {
                int n = regions.voisins[k];
                meilleure = Math.max(meilleure, v[n] * (1f - cout(r, n)));
            }
            s[r] = meilleure;
        }
        System.arraycopy(s, 0, v, 0, regions.nombre);
    }

    private void echanger(int g) {
        float[] v = valeur[g], reserve = stock[g], recu = arrivee[g];
        java.util.Arrays.fill(recu, 0f);
        for (int r = 0; r < regions.nombre; r++) {
            float surplus = reserve[r] - besoin[g][r] * reglages.reserve[g];
            if (surplus <= 0f) continue;
            int cible = -1;
            float offre = prix[g][r];
            for (int k = regions.debutVoisins[r]; k < regions.debutVoisins[r + 1]; k++) {
                int n = regions.voisins[k];
                float w = v[n] * (1f - cout(r, n));
                if (w > offre) {
                    offre = w;
                    cible = n;
                }
            }
            if (cible < 0 || offre < reglages.prixDemande) continue;
            float envoi = besoin[g][r] > 0f ? surplus * reglages.force : surplus;
            float route = envoi * cout(r, cible);
            float livre = envoi - route;
            reserve[r] -= envoi;
            recu[cible] += livre;
            if (g == NOURRITURE) {
                sorties[r] += envoi;
                sorties[cible] -= livre;
            }
            perdu[g] += route;
            bourse[r] += (double) livre * offre;
            bourse[cible] -= (double) livre * offre;
            traficAnnee[cible] += livre * offre;
            echange[g] += livre;
        }
        for (int r = 0; r < regions.nombre; r++) {
            reserve[r] += recu[r];
        }
    }

    public float partCommerce(int r, int habitants) {
        float t = trafic[r] * reglages.emplois;
        return reglages.partCommerce * t / (t + Math.max(1, habitants));
    }

    public double total(float[][] valeurs, int g) {
        double somme = 0.0;
        for (float v : valeurs[g]) {
            somme += v;
        }
        return somme;
    }

    public float prixMoyen(int g) {
        double somme = 0.0;
        int marches = 0;
        for (int r = 0; r < regions.nombre; r++) {
            if (besoin[g][r] <= 0f) continue;
            somme += prix[g][r];
            marches++;
        }
        return marches == 0 ? 0f : (float) (somme / marches);
    }

    public double stockTotal(int g) {
        double total = 0.0;
        for (float s : stock[g]) {
            total += s;
        }
        return total;
    }
}
