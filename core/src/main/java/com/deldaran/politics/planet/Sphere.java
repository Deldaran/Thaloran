package com.deldaran.politics.planet;

import java.util.Arrays;

public class Sphere {
    public static final int NOMBRE_VOISINS = 6;

    public final int nombre;
    public final float[] x, y, z;
    public final int[] voisins;

    private final int casesParAxe;
    private final float tailleCase;
    private final int[] tete;
    private final int[] suivant;

    public Sphere(int nombre) {
        this.nombre = nombre;
        x = new float[nombre];
        y = new float[nombre];
        z = new float[nombre];
        placerPoints();

        float espacement = (float) Math.sqrt(4.0 * Math.PI / nombre);
        tailleCase = espacement * 2f;
        casesParAxe = (int) Math.ceil(2.2f / tailleCase);
        tete = new int[casesParAxe * casesParAxe * casesParAxe];
        suivant = new int[nombre];
        rangerPoints();

        voisins = new int[nombre * NOMBRE_VOISINS];
        calculerVoisins();
    }

    private void placerPoints() {
        double angleOr = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < nombre; i++) {
            double hauteur = 1.0 - (2.0 * i + 1.0) / nombre;
            double rayon = Math.sqrt(1.0 - hauteur * hauteur);
            double angle = i * angleOr;
            x[i] = (float) (rayon * Math.cos(angle));
            y[i] = (float) (rayon * Math.sin(angle));
            z[i] = (float) hauteur;
        }
    }

    private int caseAxe(float valeur) {
        int c = (int) ((valeur + 1.1f) / tailleCase);
        return Math.max(0, Math.min(casesParAxe - 1, c));
    }

    private void rangerPoints() {
        Arrays.fill(tete, -1);
        for (int i = 0; i < nombre; i++) {
            int c = (caseAxe(z[i]) * casesParAxe + caseAxe(y[i])) * casesParAxe + caseAxe(x[i]);
            suivant[i] = tete[c];
            tete[c] = i;
        }
    }

    public int plusProche(float px, float py, float pz) {
        int cx = caseAxe(px), cy = caseAxe(py), cz = caseAxe(pz);
        int meilleur = -1;
        float distanceMin = Float.MAX_VALUE;

        for (int dz = -1; dz <= 1; dz++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int ax = cx + dx, ay = cy + dy, az = cz + dz;
                    if (ax < 0 || ay < 0 || az < 0 || ax >= casesParAxe || ay >= casesParAxe || az >= casesParAxe) continue;

                    for (int i = tete[(az * casesParAxe + ay) * casesParAxe + ax]; i != -1; i = suivant[i]) {
                        float ex = x[i] - px, ey = y[i] - py, ez = z[i] - pz;
                        float distance = ex * ex + ey * ey + ez * ez;
                        if (distance < distanceMin) {
                            distanceMin = distance;
                            meilleur = i;
                        }
                    }
                }
            }
        }
        return meilleur;
    }

    private void calculerVoisins() {
        int[] meilleurs = new int[NOMBRE_VOISINS];
        float[] distances = new float[NOMBRE_VOISINS];

        for (int i = 0; i < nombre; i++) {
            Arrays.fill(meilleurs, -1);
            Arrays.fill(distances, Float.MAX_VALUE);
            int cx = caseAxe(x[i]), cy = caseAxe(y[i]), cz = caseAxe(z[i]);

            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int ax = cx + dx, ay = cy + dy, az = cz + dz;
                        if (ax < 0 || ay < 0 || az < 0 || ax >= casesParAxe || ay >= casesParAxe || az >= casesParAxe) continue;

                        for (int j = tete[(az * casesParAxe + ay) * casesParAxe + ax]; j != -1; j = suivant[j]) {
                            if (j == i) continue;
                            float ex = x[j] - x[i], ey = y[j] - y[i], ez = z[j] - z[i];
                            inserer(meilleurs, distances, j, ex * ex + ey * ey + ez * ez);
                        }
                    }
                }
            }
            System.arraycopy(meilleurs, 0, voisins, i * NOMBRE_VOISINS, NOMBRE_VOISINS);
        }
    }

    private void inserer(int[] meilleurs, float[] distances, int point, float distance) {
        int position = NOMBRE_VOISINS - 1;
        if (distance >= distances[position]) return;

        while (position > 0 && distances[position - 1] > distance) {
            distances[position] = distances[position - 1];
            meilleurs[position] = meilleurs[position - 1];
            position--;
        }
        distances[position] = distance;
        meilleurs[position] = point;
    }

    public int voisin(int i, int k) {
        return voisins[i * NOMBRE_VOISINS + k];
    }
}
