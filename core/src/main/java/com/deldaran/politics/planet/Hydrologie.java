package com.deldaran.politics.planet;

import com.badlogic.gdx.math.RandomXS128;
import java.util.Arrays;
import java.util.PriorityQueue;

public class Hydrologie {
    private static final float PENTE_MINIMALE = 0.0005f;
    private static final float EROSION_RIVIERE = 0.002f;
    private static final float EROSION_MAX = 0.002f;
    private static final float CAPACITE_TRANSPORT = 0.01f;
    private static final float DEPOT = 0.3f;
    private static final float DIFFUSION_PENTES = 0.001f;
    private static final float ALTITUDE_MIN_TERRE = 0.02f;
    private static final float PROFONDEUR_LAC_MIN = 0.02f;
    private static final float RUGOSITE = 0.15f;
    private static final int PASSES_RUGOSITE = 4;
    private static final int PERIODE_ROUTAGE = 5;
    private static final float PART_DELTA = 0.1f;
    private static final int PORTEE_DELTA = 6;
    private static final float CREUSEMENT_GLACIERS = 6f;
    private static final float SEUIL_CUVETTE = 0.55f;
    private static final float CREUSEMENT_MIN = 0.2f;
    private static final float TEMPERATURE_GLACIERS = 8f;
    private static final float ECART_GLACIERS = 10f;
    private static final int PASSES_GLACIERS = 2;

    private final Monde monde;
    private final float[] surface;
    private final int[] receveur;
    private final int[] ordre;
    private final boolean[] visite;
    private final float[] charge;
    private final float[] tampon;
    private final float[] bruit;
    private final PriorityQueue<Integer> file;
    private int nombreOrdre = 0;
    private int pasEffectues = 0;
    private float tamponBas;
    private float tamponHaut;

    public Hydrologie(Monde monde) {
        this.monde = monde;
        int n = monde.plaque.length;
        surface = new float[n];
        receveur = new int[n];
        ordre = new int[n];
        visite = new boolean[n];
        charge = new float[n];
        tampon = new float[n];
        bruit = new float[n];
        file = new PriorityQueue<>(n / 4, (a, b) -> Float.compare(surface[a], surface[b]));
        ajouterRugosite();
    }

    private void ajouterRugosite() {
        Sphere s = monde.sphere;
        RandomXS128 rng = new RandomXS128(monde.seed + 1);
        for (int i = 0; i < s.nombre; i++) {
            bruit[i] = rng.nextFloat();
        }
        System.arraycopy(bruit, 0, tampon, 0, s.nombre);
        lisserTampon(PASSES_RUGOSITE);
        for (int i = 0; i < s.nombre; i++) {
            if (!monde.estSousLEau(i)) {
                monde.altitude[i] += RUGOSITE * valeurNormalisee(i);
            }
        }
    }

    public void creuserGlaciers() {
        Sphere s = monde.sphere;
        RandomXS128 rng = new RandomXS128(monde.seed + 2);
        for (int i = 0; i < s.nombre; i++) {
            tampon[i] = rng.nextFloat();
        }
        lisserTampon(PASSES_GLACIERS);
        for (int i = 0; i < s.nombre; i++) {
            if (monde.estSousLEau(i)) continue;
            float froid = (TEMPERATURE_GLACIERS - monde.temperature[i]) / ECART_GLACIERS;
            float force = Math.max(CREUSEMENT_MIN, Math.min(1f, froid));
            float creux = CREUSEMENT_GLACIERS * force * Math.max(0f, valeurNormalisee(i) - SEUIL_CUVETTE);
            monde.altitude[i] = Math.max(monde.altitude[i] - creux, monde.niveauMer + ALTITUDE_MIN_TERRE);
        }
    }

    private void lisserTampon(int passes) {
        Sphere s = monde.sphere;
        for (int passe = 0; passe < passes; passe++) {
            for (int i = 0; i < s.nombre; i++) {
                float somme = tampon[i];
                for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                    somme += tampon[s.voisin(i, k)];
                }
                charge[i] = somme / (Sphere.NOMBRE_VOISINS + 1);
            }
            System.arraycopy(charge, 0, tampon, 0, s.nombre);
        }

        tamponBas = Float.MAX_VALUE;
        tamponHaut = -Float.MAX_VALUE;
        for (int i = 0; i < s.nombre; i++) {
            tamponBas = Math.min(tamponBas, tampon[i]);
            tamponHaut = Math.max(tamponHaut, tampon[i]);
        }
    }

    private float valeurNormalisee(int i) {
        return (tampon[i] - tamponBas) / (tamponHaut - tamponBas);
    }

    public void pas() {
        if (pasEffectues % PERIODE_ROUTAGE == 0) {
            remplirDepressions();
        }
        accumulerDebits();
        creuser();
        diffuserPentes();
        pasEffectues++;
    }

    private void remplirDepressions() {
        Sphere s = monde.sphere;
        Arrays.fill(visite, false);
        file.clear();
        nombreOrdre = 0;

        for (int i = 0; i < s.nombre; i++) {
            if (!monde.estSousLEau(i)) continue;
            visite[i] = true;
            surface[i] = monde.niveauMer;
            receveur[i] = -1;
            for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                if (!monde.estSousLEau(s.voisin(i, k))) {
                    file.add(i);
                    break;
                }
            }
        }

        while (!file.isEmpty()) {
            int courant = file.poll();
            if (!monde.estSousLEau(courant)) {
                ordre[nombreOrdre++] = courant;
            }
            for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                int voisin = s.voisin(courant, k);
                if (visite[voisin]) continue;
                visite[voisin] = true;
                receveur[voisin] = courant;
                float plancher = surface[courant] + PENTE_MINIMALE * (0.5f + bruit[voisin]);
                surface[voisin] = Math.max(monde.altitude[voisin], plancher);
                file.add(voisin);
            }
        }

        for (int i = 0; i < s.nombre; i++) {
            monde.lac[i] = !monde.estSousLEau(i) && !monde.estGele(i) && surface[i] - monde.altitude[i] > PROFONDEUR_LAC_MIN;
        }
    }

    private void accumulerDebits() {
        for (int i = 0; i < monde.debit.length; i++) {
            monde.debit[i] = monde.estSousLEau(i) ? 0f : monde.pluie[i] * 0.001f;
        }
        for (int k = nombreOrdre - 1; k >= 0; k--) {
            int i = ordre[k];
            int r = receveur[i];
            if (r != -1 && !monde.estSousLEau(r)) {
                monde.debit[r] += monde.debit[i];
            }
        }
    }

    private void creuser() {
        Arrays.fill(charge, 0f);
        for (int k = nombreOrdre - 1; k >= 0; k--) {
            int i = ordre[k];
            int r = receveur[i];
            if (r == -1) continue;

            float pente = Math.max(0f, monde.altitude[i] - Math.max(monde.altitude[r], monde.niveauMer));
            float racine = (float) Math.sqrt(monde.debit[i]);
            float erosion = Math.min(EROSION_MAX, Math.min(EROSION_RIVIERE * racine * pente, pente * 0.5f));
            if (monde.lac[i]) erosion = 0f;

            float capacite = CAPACITE_TRANSPORT * monde.debit[i] * pente;
            float chargeTotale = charge[i] + erosion;
            float depot = Math.max(0f, chargeTotale - capacite) * DEPOT;

            monde.altitude[i] += depot - erosion;
            monde.altitude[i] = Math.max(monde.altitude[i], monde.niveauMer + ALTITUDE_MIN_TERRE);
            if (monde.estSousLEau(r)) {
                deposerEnMer(r, (chargeTotale - depot) * PART_DELTA);
            } else {
                charge[r] += chargeTotale - depot;
            }
        }
    }

    private void deposerEnMer(int depart, float sediment) {
        Sphere s = monde.sphere;
        int courant = depart;
        float sommet = monde.niveauMer + ALTITUDE_MIN_TERRE;
        for (int saut = 0; saut < PORTEE_DELTA && sediment > 0f; saut++) {
            float place = Math.max(0f, sommet - monde.altitude[courant]);
            float pose = Math.min(place, sediment);
            monde.altitude[courant] += pose;
            sediment -= pose;

            int suivant = -1;
            float plusBas = Float.MAX_VALUE;
            for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                int voisin = s.voisin(courant, k);
                if (monde.estSousLEau(voisin) && monde.altitude[voisin] < plusBas) {
                    plusBas = monde.altitude[voisin];
                    suivant = voisin;
                }
            }
            if (suivant == -1) return;
            courant = suivant;
        }
    }

    private void diffuserPentes() {
        Sphere s = monde.sphere;
        for (int i = 0; i < s.nombre; i++) {
            tampon[i] = monde.altitude[i];
            if (monde.estSousLEau(i)) continue;
            float somme = 0f;
            for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                somme += Math.max(monde.altitude[s.voisin(i, k)], monde.niveauMer);
            }
            float moyenne = somme / Sphere.NOMBRE_VOISINS;
            tampon[i] += DIFFUSION_PENTES * (moyenne - monde.altitude[i]);
            tampon[i] = Math.max(tampon[i], monde.niveauMer + ALTITUDE_MIN_TERRE);
        }
        System.arraycopy(tampon, 0, monde.altitude, 0, s.nombre);
    }
}
