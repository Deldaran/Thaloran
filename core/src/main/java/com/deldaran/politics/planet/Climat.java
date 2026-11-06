package com.deldaran.politics.planet;

import java.util.Arrays;

public class Climat {
    private static final float TEMPERATURE_EQUATEUR = 28f;
    private static final float ECART_POLES = 42f;
    private static final float REFROIDISSEMENT_ALTITUDE = 6.5f;
    private static final float MARGE_PLAINES = 0.3f;
    private static final int PASSES_VENT = 60;
    private static final int PASSES_LISSAGE = 3;
    private static final float EVAPORATION = 0.3f;
    private static final float PLUIE_BASE = 0.05f;
    private static final float PLUIE_RELIEF = 0.05f;
    private static final float RECYCLAGE_VEGETATION = 0.85f;
    private static final float MM_PAR_UNITE = 20000f;

    public float hauteurPlaines = 0f;

    private final Monde monde;
    private final float[] poids;
    private final float[] convection;
    private float[] humidite;
    private float[] humiditeSuivante;
    private final float[] lissage;

    public Climat(Monde monde) {
        this.monde = monde;
        int n = monde.plaque.length;
        poids = new float[n * Sphere.NOMBRE_VOISINS];
        convection = new float[n];
        humidite = new float[n];
        humiditeSuivante = new float[n];
        lissage = new float[n];
        preparerVents();
    }

    private void preparerVents() {
        Sphere s = monde.sphere;
        for (int i = 0; i < s.nombre; i++) {
            float z = s.z[i];
            float latitude = (float) Math.asin(z);
            float absolue = Math.abs(latitude);
            float rayon = (float) Math.max(0.01, Math.sqrt(1 - z * z));

            float ventEst = -(float) Math.cos(4 * absolue);
            float ventNord = -Math.signum(latitude) * (float) Math.sin(6 * absolue);

            float estX = -s.y[i] / rayon, estY = s.x[i] / rayon;
            float nordX = -z * s.x[i] / rayon, nordY = -z * s.y[i] / rayon, nordZ = rayon;

            float ventX = ventEst * estX + ventNord * nordX;
            float ventY = ventEst * estY + ventNord * nordY;
            float ventZ = ventNord * nordZ;

            float total = 0f;
            for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                int v = s.voisin(i, k);
                float dx = s.x[i] - s.x[v], dy = s.y[i] - s.y[v], dz = s.z[i] - s.z[v];
                float longueur = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                float alignement = (ventX * dx + ventY * dy + ventZ * dz) / longueur;
                poids[i * Sphere.NOMBRE_VOISINS + k] = Math.max(0f, alignement);
                total += poids[i * Sphere.NOMBRE_VOISINS + k];
            }
            for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                if (total > 0f) poids[i * Sphere.NOMBRE_VOISINS + k] /= total;
            }

            convection[i] = 0.55f + 0.45f * (float) Math.cos(6 * absolue);
        }
    }

    public void calculer() {
        calculerTemperatures();
        calculerPluies();
        lisserPluies();
        calculerBiomes();
    }

    
    private float hauteurRelief(int i) {
        return Math.max(0f, monde.altitude[i] - monde.niveauMer - hauteurPlaines);
    }

    public void calculerTemperatures() {
        mesurerPlaines();
        Sphere s = monde.sphere;
        for (int i = 0; i < s.nombre; i++) {
            monde.temperature[i] = TEMPERATURE_EQUATEUR - ECART_POLES * s.z[i] * s.z[i]
                - REFROIDISSEMENT_ALTITUDE * hauteurRelief(i);
        }
    }

    private void mesurerPlaines() {
        float somme = 0f;
        int terres = 0;
        for (int i = 0; i < monde.altitude.length; i++) {
            if (monde.estSousLEau(i)) continue;
            somme += monde.altitude[i] - monde.niveauMer;
            terres++;
        }
        hauteurPlaines = terres == 0 ? 0f : Math.max(0f, somme / terres - MARGE_PLAINES);
    }

    
    private float saturation(float temperature) {
        return Math.max(0.03f, Math.min(1f, (temperature + 15f) / 45f));
    }

    private void calculerPluies() {
        Sphere s = monde.sphere;
        Arrays.fill(humidite, 0f);

        for (int passe = 0; passe < PASSES_VENT; passe++) {
            for (int i = 0; i < s.nombre; i++) {
                float arrivee = 0f;
                float reliefAmont = 0f;
                for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                    float p = poids[i * Sphere.NOMBRE_VOISINS + k];
                    if (p == 0f) continue;
                    int v = s.voisin(i, k);
                    arrivee += p * humidite[v];
                    reliefAmont += p * hauteurRelief(v);
                }

                float maximum = saturation(monde.temperature[i]);
                boolean ocean = monde.estSousLEau(i);
                float pluie;
                if (ocean) {
                    arrivee += EVAPORATION * Math.max(0f, maximum - arrivee);
                    pluie = arrivee * PLUIE_BASE * convection[i];
                } else {
                    float montee = Math.max(0f, hauteurRelief(i) - reliefAmont);
                    pluie = arrivee * Math.min(0.8f, PLUIE_BASE * convection[i] + PLUIE_RELIEF * montee);
                }
                if (arrivee - pluie > maximum) {
                    pluie = arrivee - maximum;
                }

                float perte = ocean ? pluie : pluie * (1f - RECYCLAGE_VEGETATION);
                humiditeSuivante[i] = arrivee - perte;
                monde.pluie[i] = pluie * MM_PAR_UNITE;
            }
            float[] echange = humidite;
            humidite = humiditeSuivante;
            humiditeSuivante = echange;
        }
    }

    private void lisserPluies() {
        Sphere s = monde.sphere;
        for (int passe = 0; passe < PASSES_LISSAGE; passe++) {
            for (int i = 0; i < s.nombre; i++) {
                lissage[i] = monde.pluie[i];
                if (monde.estSousLEau(i)) continue;
                float somme = monde.pluie[i];
                int nombre = 1;
                for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                    int voisin = s.voisin(i, k);
                    if (monde.estSousLEau(voisin)) continue;
                    somme += monde.pluie[voisin];
                    nombre++;
                }
                lissage[i] = somme / nombre;
            }
            System.arraycopy(lissage, 0, monde.pluie, 0, s.nombre);
        }
    }


    private void calculerBiomes() {
        for (int i = 0; i < monde.plaque.length; i++) {
            monde.biome[i] = choisirBiome(i);
        }
    }

    private byte choisirBiome(int i) {
        return Biome.choisir(monde.temperature[i], monde.pluie[i], monde.estSousLEau(i), hauteurRelief(i));
    }

}
