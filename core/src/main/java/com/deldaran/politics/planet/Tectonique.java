package com.deldaran.politics.planet;

import java.util.Arrays;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.IntArray;

public class Tectonique {
    private static final int[] DX = {1,-1,0,0};
    private static final int[] DY = {0,0,1,-1};

    private final Monde monde;

    private final int[] nouvellePlaque;
    private final float[] nouvelleEpaisseur;
    private final boolean[] nouveauContinental;
    private final float[] nouvelAge;

    private final int[] pasX;
    private final int[] pasY; 

    private final RandomXS128 rng;
    private final IntArray trousARemplir = new IntArray();
    private final IntArray plaquesARemplir = new IntArray();
    private final float[] ageMoyen;
    private final IntArray arcs = new IntArray();
    private final int[] contacts; 
    private final Erosion erosion;
    private float facteurCroissance = 1f;



    public Tectonique(Monde monde) {
        this.monde = monde;
        this.rng = new RandomXS128(monde.seed);

        nouvellePlaque = new int[monde.largeur * monde.hauteur];
        nouvelleEpaisseur = new float[monde.largeur * monde.hauteur];
        nouveauContinental = new boolean[monde.largeur * monde.hauteur];
        nouvelAge = new float[monde.largeur * monde.hauteur];
        contacts = new int[monde.plaques.size * monde.plaques.size];

        pasX = new int[monde.plaques.size];
        pasY = new int[monde.plaques.size];
        ageMoyen = new float[monde.plaques.size];

        erosion = new Erosion(monde);
    }

    public void pas() {
        calculerFacteurCroissance();
        changerCaps();
        calculerAgesMoyens();
        calculerDeplacements();
        viderTampons();
        freinerCollisions();
        deplacerCases();
        remplirTrous();
        elargirArcs();
        eleverContinents();
        if((int) monde.temps % Croute.PERIODE_FUSION == 0) {
            fusionnerTerres();
        }
        copierVersMonde();
        monde.temps += 1f;
        monde.calculerToutesAltitudes();
        monde.calculerToutesAltitudes();
        erosion.appliquer();
        if ((int) monde.temps % 10 == 0) {
            monde.calculerNiveauMer();
        }

    }

    private void calculerDeplacements() {
        for (Plaque plaque : monde.plaques) {
            plaque.decalageX += plaque.vitesse.x;
            plaque.decalageY += plaque.vitesse.y;

            pasX[plaque.id] = (int) plaque.decalageX;
            pasY[plaque.id] = (int) plaque.decalageY;

            plaque.decalageX -= pasX[plaque.id];
            plaque.decalageY -= pasY[plaque.id];
        }
    }

    private void calculerFacteurCroissance() {
        int continentales = 0;
        for (boolean continentale : monde.isContinentale) {
            if (continentale) continentales++;
        }
        float part = (float) continentales / monde.isContinentale.length;
        facteurCroissance = Math.max(0f, 1f - part / Croute.PART_CONTINENTS_CIBLE);
    }

    private void calculerAgesMoyens() {
        Arrays.fill(ageMoyen, 0f);
        int[] compte = new int[ageMoyen.length];

        for (int i = 0; i < monde.plaque.length; i++) {
            int p = monde.plaque[i];
            ageMoyen[p] += monde.age[i];
            compte[p]++;
        }
        for (int p = 0; p < monde.plaques.size; p++) {
            if (compte[p] > 0) {
                ageMoyen[p] /= compte[p];
            }
        }
    }

    private void viderTampons() {
        Arrays.fill(nouvellePlaque, -1);
        Arrays.fill(nouvelleEpaisseur, 0f);
        Arrays.fill(nouveauContinental, false);
        Arrays.fill(nouvelAge, 0f);
    }

    private void deplacerCases() {
        for (int i = 0; i < monde.plaque.length; i++) {
            int p = monde.plaque[i];
            int pasXInt = pasX[p];
            int pasYInt = pasY[p];
            
            int yVoulu = (i / monde.largeur) + pasYInt;
            boolean sortDeLaCarte = yVoulu < 0 || yVoulu >= monde.hauteur;

            Plaque plaque = monde.plaques.get(p);
            if (sortDeLaCarte) {
                if (!monde.isContinentale[i]) continue;
                plaque.vitesse.y = yVoulu < 0 ? Math.abs(plaque.vitesse.y) : -Math.abs(plaque.vitesse.y);
            }
            int nouvelleY = MathUtils.clamp(yVoulu, 0, monde.hauteur - 1);
            int nouvelleX = (i % monde.largeur) + pasXInt;

            int nouvelIndex = monde.index(nouvelleX, nouvelleY);
            if (nouvellePlaque[nouvelIndex] == -1) {
                remplacerCible(i, nouvelIndex);
            }else{
                //collision de plaques, on garde la plus épaisse
                resoudreCollision(i, nouvelIndex);
            }
        }
    }
    
    private void changerCaps() {
        for (Plaque plaque : monde.plaques) {
            if (rng.nextFloat() < 1f / 150f) {
                float vitesse = plaque.vitesse.len();
                plaque.vitesse.set(vitesse, 0).rotateDeg(rng.nextFloat() * 360f);
            }
        }
    }

    private void resoudreCollision(int arrivant, int cible) {
        boolean arrivantContinental = monde.isContinentale[arrivant];
        boolean cibleContinentale = nouveauContinental[cible];

        if (arrivantContinental && cibleContinentale) {
            nouvelleEpaisseur[cible] += monde.epaisseur[arrivant] * Croute.GAIN_COLLISION;

            int plaqueA = monde.plaque[arrivant];
            int plaqueB = nouvellePlaque[cible];
            if (plaqueA != plaqueB) {
                contacts[Math.min(plaqueA, plaqueB) * monde.plaques.size + Math.max(plaqueA, plaqueB)]++;
            }
        } else if (arrivantContinental) {
           remplacerCible(arrivant, cible);
           arcs.add(cible);
           nouvelleEpaisseur[cible] += Croute.GAIN_ARC * facteurCroissance;
        } else if (cibleContinentale) {
            nouvelleEpaisseur[cible] += Croute.GAIN_ARC * facteurCroissance;
            arcs.add(cible);
        } else {
            int plaqueArrivante = monde.plaque[arrivant];
            int plaqueCible = nouvellePlaque[cible];

            if (plaqueArrivante != plaqueCible) {
                if (ageMoyen[plaqueArrivante] < ageMoyen[plaqueCible]) {
                    float epaisseurCible = nouvelleEpaisseur[cible];
                    remplacerCible(arrivant, cible);
                    nouvelleEpaisseur[cible] = Math.max(epaisseurCible, nouvelleEpaisseur[cible]);
                }
                nouvelleEpaisseur[cible] += Croute.GAIN_ARC * facteurCroissance;
                arcs.add(cible);
            }
        }

        if (nouvelleEpaisseur[cible] > Croute.EPAISSEUR_MAX) {
            nouvelleEpaisseur[cible] = Croute.EPAISSEUR_MAX;
        }
        verifierSeuilContinental(cible);
    }

    private void elargirArcs() {
        int rayon = Croute.RAYON_ARC;

        for (int k = 0; k < arcs.size; k++) {
            int centre = arcs.get(k);
            int plaque = nouvellePlaque[centre];
            int x = centre % monde.largeur;
            int y = centre / monde.largeur;

            for (int dy = -rayon; dy <= rayon; dy++) {
                for (int dx = -rayon; dx <= rayon; dx++) {
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    if (distance == 0 || distance > rayon) continue;

                    int voisin = monde.index(x + dx, y + dy);
                    if (voisin == -1 || nouvellePlaque[voisin] != plaque) continue;

                    float gain = Croute.GAIN_ARC * facteurCroissance *  0.5f * (1 - distance / (rayon + 1));
                    nouvelleEpaisseur[voisin] = Math.min(Croute.EPAISSEUR_MAX, nouvelleEpaisseur[voisin] + gain);
                    verifierSeuilContinental(voisin);
                }
            }
        }
        arcs.clear();
    }

    private void remplacerCible(int arrivant, int cible) {
        nouvellePlaque[cible] = monde.plaque[arrivant];
        nouvelleEpaisseur[cible] = monde.epaisseur[arrivant];
        nouveauContinental[cible] = monde.isContinentale[arrivant];
        nouvelAge[cible] = monde.age[arrivant] + 1f;
    }

    private void verifierSeuilContinental(int cible) {
        if (!nouveauContinental[cible] && nouvelleEpaisseur[cible] >= Croute.SEUIL_CONTINENTAL) {
            nouveauContinental[cible] = true;
        }
    }

    private void freinerCollisions() {
        int nombre = monde.plaques.size;
        int[] taille = new int[nombre];
        for (int p : monde.plaque) {
            taille[p]++;
        }

        for (int a = 0; a < nombre; a++) {
            for (int b = a + 1; b < nombre; b++) {
                int nombreContacts = contacts[a * nombre + b];
                if (nombreContacts == 0) continue;
                contacts[a * nombre + b] = 0;

                float force = Math.min(1f, nombreContacts * Croute.FROTTEMENT_CONTINENTS);
                float tailleA = Math.max(1, taille[a]);
                float tailleB = Math.max(1, taille[b]);
                Plaque plaqueA = monde.plaques.get(a);
                Plaque plaqueB = monde.plaques.get(b);

                float communeX = (plaqueA.vitesse.x * tailleA + plaqueB.vitesse.x * tailleB) / (tailleA + tailleB);
                float communeY = (plaqueA.vitesse.y * tailleA + plaqueB.vitesse.y * tailleB) / (tailleA + tailleB);

                plaqueA.vitesse.x += (communeX - plaqueA.vitesse.x) * force;
                plaqueA.vitesse.y += (communeY - plaqueA.vitesse.y) * force;
                plaqueB.vitesse.x += (communeX - plaqueB.vitesse.x) * force;
                plaqueB.vitesse.y += (communeY - plaqueB.vitesse.y) * force;
            }
        }
    }


    private void eleverContinents() {
        for (int i = 0; i < nouvelleEpaisseur.length; i++) {
            if (nouveauContinental[i] && nouvelleEpaisseur[i] < Croute.EPAISSEUR_CONTINENTALE) {
                nouvelleEpaisseur[i] = Math.min(Croute.EPAISSEUR_CONTINENTALE,
                    nouvelleEpaisseur[i] + Croute.GAIN_ELEVATION);
            }
        }
    }

    private void fusionnerTerres() {
        int rayon = Croute.RAYON_FUSION;

        for (int i = 0; i < nouvelleEpaisseur.length; i++) {
            if (nouveauContinental[i]) continue;

            int x = i % monde.largeur;
            int y = i / monde.largeur;
            int plaque = nouvellePlaque[i];
            int terresProches = 0;

            for (int dy = -rayon; dy <= rayon; dy++) {
                for (int dx = -rayon; dx <= rayon; dx++) {
                    int voisin = monde.index(x + dx, y + dy);
                    if (voisin != -1 && nouveauContinental[voisin] && nouvellePlaque[voisin] == plaque) {
                        terresProches++;
                    }
                }
            }

            if (terresProches >= Croute.VOISINS_FUSION) {
                nouvelleEpaisseur[i] += Croute.GAIN_FUSION * facteurCroissance;
                verifierSeuilContinental(i);
            }
        }
    }


    private void remplirTrous() {
        boolean resteDesTrous = true;
        while (resteDesTrous) {
            resteDesTrous = false;
            trousARemplir.clear();
            plaquesARemplir.clear();

            for (int i = 0; i < nouvellePlaque.length; i++) {
                if (nouvellePlaque[i] != -1) continue;

                int plaqueVoisine = trouverPlaqueVoisine(i);
                if (plaqueVoisine == -1) {
                    resteDesTrous = true;
                } else {
                    trousARemplir.add(i);
                    plaquesARemplir.add(plaqueVoisine);
                }
            }
            for (int k = 0; k < trousARemplir.size; k++) {
                int i = trousARemplir.get(k);
                nouvellePlaque[i] = plaquesARemplir.get(k);
                nouvelleEpaisseur[i] = Croute.EPAISSEUR_OCEANIQUE;
                nouveauContinental[i] = false;
                nouvelAge[i] = 0f;
            }
        }
    }

    private int trouverPlaqueVoisine(int i) {
        int x = i % monde.largeur;
        int y = i / monde.largeur;
        int depart = rng.nextInt(4);
        for (int k = 0; k < 4; k++) {
            int d = (depart + k) % 4;
            int voisin = monde.index(x + DX[d], y + DY[d]);
            if (voisin != -1 && nouvellePlaque[voisin] != -1) {
                return nouvellePlaque[voisin];
            }
        }
        return -1;
    }

    private void copierVersMonde() {
        System.arraycopy(nouvellePlaque, 0, monde.plaque, 0, monde.plaque.length);
        System.arraycopy(nouvelleEpaisseur, 0, monde.epaisseur, 0, monde.epaisseur.length);
        System.arraycopy(nouveauContinental, 0, monde.isContinentale, 0, monde.isContinentale.length);
        System.arraycopy(nouvelAge, 0, monde.age, 0, monde.age.length);
    }

    
}
