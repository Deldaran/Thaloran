package com.deldaran.politics.histoire;

import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.IntArray;
import com.deldaran.politics.planet.Biome;
import com.deldaran.politics.planet.Monde;
import com.deldaran.politics.planet.Sphere;

public class Regions {
    public static final class Reglages {
        public int ecartTerre = 3;
        public int ecartMer = 8;
        public float debitFleuve = 30f;
        public float bonusFleuve = 0.6f;
        public float pecheCotiere = 0.3f;
        public float debutMinerai = 0.5f;
        public float etendueMinerai = 0.7f;
        public final float[] fertilite = new float[12];
        public final float[] bois = new float[12];
        
        public Reglages() {
            fertilite[Biome.PRAIRIE] = 1f;
            fertilite[Biome.FORET] = 0.7f;
            fertilite[Biome.SAVANE] = 0.6f;
            fertilite[Biome.JUNGLE] = 0.4f;
            fertilite[Biome.TAIGA] = 0.2f;
            fertilite[Biome.MONTAGNE] = 0.1f;
            fertilite[Biome.TOUNDRA] = 0.05f;
            fertilite[Biome.DESERT] = 0.03f;
            bois[Biome.FORET] = 1f;
            bois[Biome.JUNGLE] = 1f;
            bois[Biome.TAIGA] = 0.8f;
            bois[Biome.SAVANE] = 0.2f;
        }
    }

    public final int nombre;
    public final int[] regionDe;
    public final boolean[] estTerre;
    public final int[] cases;
    public final int[] centre;
    public final float[] nourriture;
    public final float[] bois;
    public final float[] minerai;
    public final float[] peche;
    public final int[] debutVoisins;
    public final int[] voisins;
    public final float[] fleuves;
    public final float[][] biomes;

    public int regionsTerre = 0;
    public int regionsMer = 0;
    public int ports = 0;
    public int casesTerre = 0;
    public float casesFertiles = 0f;

    private final Reglages reglages;

    public Regions(Monde monde, float hauteurPlaines, Reglages reglages) {
        this.reglages = reglages;
        int n = monde.plaque.length;
        regionDe = new int[n];
        IntArray graines = choisirGraines(monde);
        nombre = graines.size;
        grandir(monde, graines);

        estTerre = new boolean[nombre];
        cases = new int[nombre];
        centre = graines.toArray();
        nourriture = new float[nombre];
        bois = new float[nombre];
        minerai = new float[nombre];
        peche = new float[nombre];
        fleuves = new float[nombre];
        biomes = new float[nombre][12];

        for (int i = 0; i < n; i++) {
            ajouterCase(monde, i, hauteurPlaines);
        }

        debutVoisins = new int[nombre + 1];
        voisins = relier(monde);
        compter();
    }

    private IntArray choisirGraines(Monde monde) {
        int n = regionDe.length;
        RandomXS128 rng = new RandomXS128(monde.seed + 1000);
        int[] ordre = new int[n];
        for (int i = 0; i < n; i++) ordre[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int t = ordre[i];
            ordre[i] = ordre[j];
            ordre[j] = t;
        }

        boolean[] pris = new boolean[n];
        int[] marque = new int[n];
        IntArray graines = new IntArray();
        IntArray file = new IntArray();
        int tour = 0;
        for (int i : ordre) {
            if (pris[i]) continue;
            graines.add(i);
            tour++;
            boolean mer = monde.estSousLEau(i);
            int ecart = mer ? reglages.ecartMer : reglages.ecartTerre;
            file.clear();
            file.add(i);
            marque[i] = tour;
            pris[i] = true;
            int debut = 0;
            for (int pas = 0; pas < ecart; pas++) {
                int fin = file.size;
                for (; debut < fin; debut++) {
                    int c = file.get(debut);
                    for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                        int v = monde.sphere.voisin(c, k);
                        if (marque[v] == tour || monde.estSousLEau(v) != mer) continue;
                        marque[v] = tour;
                        pris[v] = true;
                        file.add(v);
                    }
                }
            }
        }
        return graines;
    }

    private void grandir(Monde monde, IntArray graines) {
        java.util.Arrays.fill(regionDe, -1);
        IntArray file = new IntArray(regionDe.length);
        for (int r = 0; r < graines.size; r++) {
            regionDe[graines.get(r)] = r;
            file.add(graines.get(r));
        }
        for (int debut = 0; debut < file.size; debut++) {
            int c = file.get(debut);
            boolean mer = monde.estSousLEau(c);
            for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                int v = monde.sphere.voisin(c, k);
                if (regionDe[v] >= 0 || monde.estSousLEau(v) != mer) continue;
                regionDe[v] = regionDe[c];
                file.add(v);
            }
        }
    }

    private void ajouterCase(Monde monde, int i, float hauteurPlaines) {
        int r = regionDe[i];
        cases[r]++;
        if (monde.estSousLEau(i)) return;
        estTerre[r] = true;

        byte biome = monde.biome[i];
        float fleuve = Math.min(1f, monde.debit[i] / reglages.debitFleuve);
        float fertile = monde.estGele(i) ? 0f : reglages.fertilite[biome] + reglages.bonusFleuve * fleuve;
        nourriture[r] += Math.min(1f, fertile);
        fleuves[r] += reglages.bonusFleuve * fleuve;
        biomes[r][biome]++;

        bois[r] += reglages.bois[biome];
        float hauteur = monde.altitude[i] - monde.niveauMer - hauteurPlaines;
        minerai[r] += Math.max(0f, Math.min(1f, (hauteur - reglages.debutMinerai) / reglages.etendueMinerai));
        for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
            if (monde.estSousLEau(monde.sphere.voisin(i, k))) {
                peche[r] += reglages.pecheCotiere;
                break;
            }
        }
    }

    private int[] relier(Monde monde) {
        IntArray[] listes = new IntArray[nombre];
        for (int r = 0; r < nombre; r++) listes[r] = new IntArray(8);
        for (int i = 0; i < regionDe.length; i++) {
            int r = regionDe[i];
            for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                int v = regionDe[monde.sphere.voisin(i, k)];
                if (v == r) continue;
                if (!listes[r].contains(v)) listes[r].add(v);
                if (!listes[v].contains(r)) listes[v].add(r);
            }
        }
        IntArray tout = new IntArray();
        for (int r = 0; r < nombre; r++) {
            debutVoisins[r] = tout.size;
            tout.addAll(listes[r]);
        }
        debutVoisins[nombre] = tout.size;
        return tout.toArray();
    }

    private void compter() {
        for (int r = 0; r < nombre; r++) {
            if (estTerre[r]) {
                regionsTerre++;
                casesTerre += cases[r];
                casesFertiles += nourriture[r];
                if (estPort(r)) ports++;
            } else {
                regionsMer++;
            }
        }
    }

    public boolean estPort(int r) {
        if (!estTerre[r]) return false;
        for (int k = debutVoisins[r]; k < debutVoisins[r + 1]; k++) {
            if (!estTerre[voisins[k]]) return true;
        }
        return false;
    }
}
