package com.deldaran.politics.histoire;

import com.badlogic.gdx.utils.IntArray;
import com.deldaran.politics.planet.Monde;
import com.deldaran.politics.planet.Terrain;
import java.util.stream.IntStream;

public class Population {
    public static final class Reglages {
        public int populationMax = 5000000;
        public float habitantsParKm2 = 2f;
        public float distanceBerceaux = 0.8f;
        public int ageAdulte = 16;
        public int ageFertileMax = 45;
        public float natalite = 0.30f;
        public float freinNatalite = 0.62f;
        public float famine = 1.5f;
        public float seuilMigration = 0.7f;
        public float migration = 0.25f;
        public float partMer = 0.03f;
        public final float[] mortalite = new float[110];
        public final Peuple[] peuples = Peuple.tous();

        public Reglages() {
            for (int age = 0; age < mortalite.length; age++) {
                float m;
                if (age == 0) m = 0.2f;
                else if (age < 5) m = 0.03f;
                else if (age < 15) m = 0.006f;
                else if (age < 40) m = 0.012f;
                else if (age < 60) m = 0.025f;
                else m = 0.05f * (float) Math.pow(1.09, age - 60);
                mortalite[age] = Math.min(1f, m);
            }
        }
    }

    public final Reglages reglages;
    public final Regions regions;
    public final Foule[][] foules;
    public final float[][] capacite;
    public final float[][] pression;
    public final int[] habitants;
    public final int[] vivantsPeuple = new int[Peuple.NOMBRE];
    public final int[] regionsPeuple = new int[Peuple.NOMBRE];
    public final double[] ageAuDeces = new double[Peuple.NOMBRE];
    public final int[] debutTerre;
    public final int[] terre;
    public final int[] debutMer;
    public final int[] mer;
    public final int[][] berceaux = new int[Peuple.NOMBRE][];

    public volatile int annee = 0;
    public volatile int vivants = 0;
    public int regionsHabitees = 0;
    public long naissancesTotales = 0;
    public long mortsTotales = 0;
    public int naissances, morts, migrants;
    public double ageMoyenAuDeces = 0;

    private final long graine;

    public Population(Monde monde, Regions regions, Reglages reglages) {
        this.regions = regions;
        this.reglages = reglages;
        graine = monde.seed;
        int n = regions.nombre;
        foules = new Foule[Peuple.NOMBRE][n];
        capacite = new float[Peuple.NOMBRE][n];
        pression = new float[Peuple.NOMBRE][n];
        habitants = new int[n];
        debutTerre = new int[n + 1];
        debutMer = new int[n + 1];

        double surfaceCase = 4.0 * Math.PI * Terrain.RAYON_KM * Terrain.RAYON_KM / monde.plaque.length;
        for (int r = 0; r < n; r++) {
            if (!regions.estTerre[r]) continue;
            for (int q = 0; q < Peuple.NOMBRE; q++) {
                Peuple peuple = reglages.peuples[q];
                foules[q][r] = new Foule(r, q, peuple, n);
                float terres = regions.fleuves[r] + regions.peche[r] * peuple.peche;
                for (int b = 0; b < peuple.attrait.length; b++) {
                    terres += regions.biomes[r][b] * peuple.attrait[b];
                }
                capacite[q][r] = (float) (terres * surfaceCase * reglages.habitantsParKm2);
            }
        }
        IntArray listeTerre = new IntArray();
        IntArray listeMer = new IntArray();
        relier(listeTerre, listeMer);
        terre = listeTerre.toArray();
        mer = listeMer.toArray();
        choisirBerceaux(monde);
        eveiller(0);
        compter();
    }

    private void relier(IntArray listeTerre, IntArray listeMer) {
        IntArray vus = new IntArray();
        for (int r = 0; r < regions.nombre; r++) {
            debutTerre[r] = listeTerre.size;
            debutMer[r] = listeMer.size;
            if (!regions.estTerre[r]) continue;
            vus.clear();
            for (int k = regions.debutVoisins[r]; k < regions.debutVoisins[r + 1]; k++) {
                int v = regions.voisins[k];
                if (regions.estTerre[v]) {
                    listeTerre.add(v);
                    continue;
                }
                for (int q = regions.debutVoisins[v]; q < regions.debutVoisins[v + 1]; q++) {
                    int port = regions.voisins[q];
                    if (port != r && regions.estTerre[port] && !vus.contains(port)) {
                        vus.add(port);
                        listeMer.add(port);
                    }
                }
            }
        }
        debutTerre[regions.nombre] = listeTerre.size;
        debutMer[regions.nombre] = listeMer.size;
    }

    private void choisirBerceaux(Monde monde) {
        IntArray choisis = new IntArray();
        float seuil = reglages.distanceBerceaux * reglages.distanceBerceaux;
        for (int q = 0; q < Peuple.NOMBRE; q++) {
            int debut = choisis.size;
            for (int essai = 0; essai < reglages.peuples[q].berceaux; essai++) {
                int meilleur = -1;
                for (int r = 0; r < regions.nombre; r++) {
                    if (!regions.estTerre[r] || (meilleur >= 0 && capacite[q][r] <= capacite[q][meilleur])) continue;
                    if (estLoin(monde, r, choisis, seuil)) meilleur = r;
                }
                if (meilleur >= 0) choisis.add(meilleur);
            }
            berceaux[q] = java.util.Arrays.copyOfRange(choisis.items, debut, choisis.size);
        }
    }

    private boolean estLoin(Monde monde, int r, IntArray choisis, float seuil) {
        for (int k = 0; k < choisis.size; k++) {
            int a = regions.centre[r], b = regions.centre[choisis.get(k)];
            float dx = monde.sphere.x[a] - monde.sphere.x[b];
            float dy = monde.sphere.y[a] - monde.sphere.y[b];
            float dz = monde.sphere.z[a] - monde.sphere.z[b];
            if (dx * dx + dy * dy + dz * dz < seuil) return false;
        }
        return true;
    }

    private void eveiller(int annee) {
        for (int q = 0; q < Peuple.NOMBRE; q++) {
            Peuple peuple = reglages.peuples[q];
            if (peuple.eveil != annee) continue;
            for (int b : berceaux[q]) {
                foules[q][b].melanger(graine, annee);
                foules[q][b].fonder(peuple.fondateurs, (short) annee, 40);
            }
        }
    }

    public int choisirDestination(int q, int depuis, float tirage, float choix) {
        int debut, fin;
        int[] liens;
        if (tirage < reglages.partMer) {
            liens = mer;
            debut = debutMer[depuis];
            fin = debutMer[depuis + 1];
        } else {
            liens = terre;
            debut = debutTerre[depuis];
            fin = debutTerre[depuis + 1];
        }
        if (fin <= debut) return -1;
        int v = liens[debut + (int) (choix * (fin - debut))];
        return pression[q][v] < pression[q][depuis] ? v : -1;
    }

    public void avancer() {
        int a = annee + 1;
        int n = regions.nombre;
        eveiller(a);
        float saturation = (float) vivants / reglages.populationMax;
        for (int r = 0; r < n; r++) {
            int total = 0;
            for (int q = 0; q < Peuple.NOMBRE; q++) {
                if (foules[q][r] != null) total += foules[q][r].vivants;
            }
            habitants[r] = total;
            for (int q = 0; q < Peuple.NOMBRE; q++) {
                pression[q][r] = Math.max(saturation, total / Math.max(1f, capacite[q][r]));
            }
        }
        IntStream.range(0, Peuple.NOMBRE * n).parallel().forEach(k -> {
            Foule f = foules[k / n][k % n];
            if (f == null || f.vivants == 0) return;
            f.melanger(graine, a);
            f.vivreAnnee(a, pression[k / n][k % n], this);
        });

        migrants = 0;
        for (int q = 0; q < Peuple.NOMBRE; q++) {
            for (Foule f : foules[q]) {
                if (f == null || f.nombrePartants == 0) continue;
                for (int k = 0; k < f.nombrePartants; k++) {
                    foules[q][f.destination[k]].accueillir(f, k);
                }
                migrants += f.nombrePartants;
                f.nombrePartants = 0;
            }
        }
        compter();
        annee = a;
    }

    private void compter() {
        int total = 0;
        boolean[] habitee = new boolean[regions.nombre];
        naissances = 0;
        morts = 0;
        long ages = 0;
        for (int q = 0; q < Peuple.NOMBRE; q++) {
            int vivantsDuPeuple = 0, regionsDuPeuple = 0, mortsDuPeuple = 0;
            long agesDuPeuple = 0;
            for (Foule f : foules[q]) {
                if (f == null) continue;
                vivantsDuPeuple += f.vivants;
                if (f.vivants > 0) {
                    regionsDuPeuple++;
                    habitee[f.region] = true;
                }
                naissances += f.naissances;
                mortsDuPeuple += f.morts;
                agesDuPeuple += f.sommeAgesAuDeces;
                f.naissances = 0;
                f.morts = 0;
                f.sommeAgesAuDeces = 0;
            }
            vivantsPeuple[q] = vivantsDuPeuple;
            regionsPeuple[q] = regionsDuPeuple;
            if (mortsDuPeuple > 0) ageAuDeces[q] = (double) agesDuPeuple / mortsDuPeuple;
            total += vivantsDuPeuple;
            morts += mortsDuPeuple;
            ages += agesDuPeuple;
        }
        int nombreHabitees = 0;
        for (boolean h : habitee) {
            if (h) nombreHabitees++;
        }
        vivants = total;
        regionsHabitees = nombreHabitees;
        naissancesTotales += naissances;
        mortsTotales += morts;
        if (morts > 0) ageMoyenAuDeces = (double) ages / morts;
    }
}
