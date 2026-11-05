package com.deldaran.politics.planet;

import java.util.Arrays;

public class Erosion {
    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DY = {0, 0, 1, -1};

    private final Monde monde;
    private final float[] variation;

    public Erosion(Monde monde) {
        this.monde = monde;
        variation = new float[monde.epaisseur.length];
    }

    public void appliquer() {
        Arrays.fill(variation, 0f);

        for (int i = 0; i < variation.length; i++) {
            float surplus = monde.epaisseur[i] - Croute.EPAISSEUR_CONTINENTALE;
            if (surplus <= 0 || monde.estSousLEau(i)) continue;

            int voisinBas = trouverVoisinLePlusBas(i);
            if (voisinBas == -1) continue;

            float pente = monde.altitude[i] - monde.altitude[voisinBas];
            if (pente < Croute.PENTE_MIN) continue;

            float transfert = Croute.EROSION * (pente - Croute.PENTE_MIN) * Croute.EPAISSEUR_PAR_KM;
            transfert = Math.min(transfert, surplus);
            variation[i] -= transfert;
            variation[voisinBas] += transfert;
        }

        for (int i = 0; i < variation.length; i++) {
            if (variation[i] == 0f) continue;

            monde.epaisseur[i] += variation[i];
            if (monde.epaisseur[i] >= Croute.SEUIL_CONTINENTAL) {
                monde.isContinentale[i] = true;
            }
            monde.calculerAltitude(i);
        }
    }

    private int trouverVoisinLePlusBas(int i) {
        int x = i % monde.largeur;
        int y = i / monde.largeur;
        int voisinBas = -1;
        float altitudeMin = monde.altitude[i];

        for (int d = 0; d < 4; d++) {
            int voisin = monde.index(x + DX[d], y + DY[d]);
            if (voisin != -1 && monde.altitude[voisin] < altitudeMin) {
                altitudeMin = monde.altitude[voisin];
                voisinBas = voisin;
            }
        }
        return voisinBas;
    }
}
