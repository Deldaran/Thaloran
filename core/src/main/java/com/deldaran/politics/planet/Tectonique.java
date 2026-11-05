package com.deldaran.politics.planet;

import java.util.Arrays;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.math.Vector3;

public class Tectonique {

    private final Monde monde;

    private final int[] nouvellePlaque;
    private final float[] nouvelleEpaisseur;
    private final boolean[] nouveauContinental;
    private final float[] nouvelAge;

    private final float[][] matrices;
    private final boolean[] enMouvement;
    private final float seuilRotation;
    private final int[] candidats = new int[Sphere.NOMBRE_VOISINS + 1];
    private final int[] marque;
    private int tampon = 0;
    private final IntArray rang = new IntArray();

    private final RandomXS128 rng;
    private final IntArray trousARemplir = new IntArray();
    private final IntArray plaquesARemplir = new IntArray();
    private final float[] ageMoyen;
    private final IntArray arcs = new IntArray();
    private final Erosion erosion;
    private float facteurCroissance = 1f;

    private final float[] couples;
    private final int[] taille;
    private final int[] tailleContinent;

    private final int[] contacts;
    private final int[] dureeContact;
    private final float[] ageOcean;
    private final int[] tailleOcean;
    private final IntArray cellules = new IntArray();
    private final IntArray frontiere = new IntArray();
    public int nombreFractures = 0;
    public int nombreRuptures = 0;
    public int nombreSoudures = 0;

    private final float[] nouveauVolcan;
    private final int[] pointsChauds;



    public Tectonique(Monde monde) {
        this.monde = monde;
        this.rng = new RandomXS128(monde.seed);

        nouvellePlaque = new int[monde.largeur * monde.hauteur];
        nouvelleEpaisseur = new float[monde.largeur * monde.hauteur];
        nouveauContinental = new boolean[monde.largeur * monde.hauteur];
        nouvelAge = new float[monde.largeur * monde.hauteur];
        nouveauVolcan = new float[monde.largeur * monde.hauteur];
        pointsChauds = new int[Croute.NOMBRE_POINTS_CHAUDS];
        for (int k = 0; k < pointsChauds.length; k++) {
            pointsChauds[k] = rng.nextInt(monde.plaque.length);
        }

        matrices = new float[monde.plaques.size][9];
        enMouvement = new boolean[monde.plaques.size];
        seuilRotation = (float) Math.sqrt(4.0 * Math.PI / monde.plaque.length);
        marque = new int[monde.plaque.length];

        ageMoyen = new float[monde.plaques.size];

        erosion = new Erosion(monde);
        couples = new float[monde.plaques.size * 3];
        taille = new int[monde.plaques.size];
        tailleContinent = new int[monde.plaques.size];

        contacts = new int[monde.plaques.size * monde.plaques.size];
        dureeContact = new int[monde.plaques.size * monde.plaques.size];
        ageOcean = new float[monde.plaques.size];
        tailleOcean = new int[monde.plaques.size];


    }

    public void pas() {
        calculerFacteurCroissance();
        calculerAgesMoyens();
        appliquerForces();
        calculerDeplacements();
        viderTampons();
        deplacerCases();
        remplirTrous();
        elargirArcs();
        eleverContinents();
        eleverContinents();
        activerPointsChauds();

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
        souderPlaques();
        fracturerPlaques();


    }

    private void calculerDeplacements() {
        for (Plaque plaque : monde.plaques) {
            plaque.rotationAccumulee.add(plaque.rotation);
            float angle = plaque.rotationAccumulee.len();
            enMouvement[plaque.id] = angle >= seuilRotation;
            if (enMouvement[plaque.id]) {
                remplirMatriceInverse(matrices[plaque.id], plaque.rotationAccumulee, angle);
                plaque.rotationAccumulee.setZero();
            }
        }
    }

    private void remplirMatriceInverse(float[] m, Vector3 rotation, float angle) {
        float kx = rotation.x / angle, ky = rotation.y / angle, kz = rotation.z / angle;
        float c = (float) Math.cos(-angle), s = (float) Math.sin(-angle), u = 1f - c;
        m[0] = c + kx * kx * u;      m[1] = kx * ky * u - kz * s; m[2] = kx * kz * u + ky * s;
        m[3] = ky * kx * u + kz * s; m[4] = c + ky * ky * u;      m[5] = ky * kz * u - kx * s;
        m[6] = kz * kx * u - ky * s; m[7] = kz * ky * u + kx * s; m[8] = c + kz * kz * u;
    }
    private void calculerFacteurCroissance() {
        int continentales = 0;
        for (boolean continentale : monde.isContinentale) {
            if (continentale) continentales++;
        }
        float part = (float) continentales / monde.isContinentale.length;
        facteurCroissance = Math.max(0f, 1f - part / Croute.PART_CONTINENTS_CIBLE);
    }

    private void appliquerForces() {
        Sphere s = monde.sphere;
        Arrays.fill(couples, 0f);
        Arrays.fill(taille, 0);
        Arrays.fill(tailleContinent, 0);

        for (int i = 0; i < s.nombre; i++) {
            int a = monde.plaque[i];
            taille[a]++;
            boolean continentale = monde.isContinentale[i];
            if (continentale) tailleContinent[a]++;

            for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                int j = s.voisin(i, k);
                int b = monde.plaque[j];
                if (a == b) continue;

                float dx = (s.x[j] - s.x[i]) / seuilRotation;
                float dy = (s.y[j] - s.y[i]) / seuilRotation;
                float dz = (s.z[j] - s.z[i]) / seuilRotation;
                float convergence = vitesseRelative(i, a, b, dx, dy, dz);

                float force = 0f;
                if (continentale) {
                    if (convergence > 0f && monde.isContinentale[j]) {
                        force = -Croute.FORCE_COLLISION;
                        contacts[Math.min(a, b) * monde.plaques.size + Math.max(a, b)]++;
                    }
                } else if (convergence > 0f) {
                    boolean plonge = monde.isContinentale[j] || ageMoyen[a] >= ageMoyen[b];
                    if (plonge) {
                        force = Croute.FORCE_SLAB * Math.min(1f, monde.age[i] / Croute.AGE_SLAB_MAX);
                        creuserFosse(i);
                    }
                } else if (!monde.isContinentale[j]) {
                    force = -Croute.FORCE_DORSALE;
                }
                if (force != 0f) ajouterCouple(a, i, dx * force, dy * force, dz * force);
            }
        }

        
        float vitesseMax = Croute.VITESSE_MAX * seuilRotation;
        for (Plaque plaque : monde.plaques) {
            int p = plaque.id;
            if (taille[p] == 0) continue;
            float gain = seuilRotation / taille[p];
            plaque.rotation.add(couples[p * 3] * gain, couples[p * 3 + 1] * gain, couples[p * 3 + 2] * gain);
            float partContinent = (float) tailleContinent[p] / taille[p];
            plaque.rotation.scl(1f - Croute.TRAINEE_OCEAN - Croute.TRAINEE_CONTINENT * partContinent);
            plaque.rotation.limit(vitesseMax);
        }
    }

    private void creuserFosse(int i) {
        Sphere s = monde.sphere;
        monde.fosse[i] = Croute.PROFONDEUR_FOSSE;
        for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
            int voisin = s.voisin(i, k);
            if (monde.plaque[voisin] == monde.plaque[i]) {
                monde.fosse[voisin] = Math.max(monde.fosse[voisin], Croute.PROFONDEUR_FOSSE * 0.5f);
            }
        }
    }

    private void activerPointsChauds() {
        Sphere s = monde.sphere;
        for (int k = 0; k < pointsChauds.length; k++) {
            if (rng.nextFloat() < Croute.PROBA_DEPLACEMENT_POINT_CHAUD) {
                pointsChauds[k] = rng.nextInt(s.nombre);
            }
            int centre = pointsChauds[k];
            construireVolcan(centre, Croute.GAIN_VOLCAN);
            for (int v = 0; v < Sphere.NOMBRE_VOISINS; v++) {
                construireVolcan(s.voisin(centre, v), Croute.GAIN_VOLCAN * 0.5f);
            }
        }
    }

    private void construireVolcan(int i, float gain) {
        nouveauVolcan[i] = Math.min(Croute.HAUTEUR_VOLCAN_MAX, nouveauVolcan[i] + gain);
    }

    private void souderPlaques() {
        int nombre = monde.plaques.size;
        for (int a = 0; a < nombre; a++) {
            for (int b = a + 1; b < nombre; b++) {
                int paire = a * nombre + b;
                int nombreContacts = contacts[paire];
                contacts[paire] = 0;

                if (taille[a] == 0 || taille[b] == 0) {
                    dureeContact[paire] = 0;
                } else if (nombreContacts >= Croute.CONTACT_SOUDURE) {
                    dureeContact[paire]++;
                } else if (dureeContact[paire] > 0) {
                    dureeContact[paire]--;
                }

                if (dureeContact[paire] >= Croute.DUREE_SOUDURE) {
                    dureeContact[paire] = 0;
                    souder(a, b);
                }
            }
        }
    }

    private void fracturerPlaques() {
        int tailleMin = (int) (Croute.TAILLE_FRACTURE * monde.plaque.length);
        for (int p = 0; p < monde.plaques.size; p++) {
            if (taille[p] < tailleMin || rng.nextFloat() >= Croute.PROBA_FRACTURE) continue;

            int libre = trouverPlaqueLibre();
            if (libre == -1) return;
            fracturer(p, libre);
        }
        ouvrirSubductions();
    }

    private void ouvrirSubductions() {
        Arrays.fill(ageOcean, 0f);
        Arrays.fill(tailleOcean, 0);
        Arrays.fill(tailleContinent, 0);
        for (int i = 0; i < monde.plaque.length; i++) {
            int p = monde.plaque[i];
            if (monde.isContinentale[i]) {
                tailleContinent[p]++;
            } else {
                ageOcean[p] += monde.age[i];
                tailleOcean[p]++;
            }
        }

        for (int p = 0; p < monde.plaques.size; p++) {
            if (tailleContinent[p] == 0 || tailleOcean[p] < Croute.TAILLE_RUPTURE_MIN) continue;
            if (ageOcean[p] / tailleOcean[p] < Croute.AGE_RUPTURE) continue;
            if (rng.nextFloat() >= Croute.PROBA_RUPTURE) continue;

            int libre = trouverPlaqueLibre();
            if (libre == -1) return;
            detacherOcean(p, libre);
        }
    }

    private void detacherOcean(int p, int libre) {
        Sphere s = monde.sphere;
        int graineOcean = -1;
        cellules.clear();
        for (int i = 0; i < s.nombre; i++) {
            if (monde.plaque[i] != p) continue;
            if (monde.isContinentale[i]) {
                cellules.add(i);
            } else if (graineOcean == -1 || monde.age[i] > monde.age[graineOcean]) {
                graineOcean = i;
            }
        }
        int graineContinent = cellules.get(rng.nextInt(cellules.size));

        monde.plaque[graineOcean] = libre;
        int compte = 1;
        frontiere.clear();
        frontiere.add(graineOcean);
        while (frontiere.size > 0) {
            int depuis = frontiere.pop();
            for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                int voisin = s.voisin(depuis, k);
                if (monde.plaque[voisin] != p || monde.isContinentale[voisin]) continue;
                monde.plaque[voisin] = libre;
                frontiere.add(voisin);
                compte++;
            }
        }

        separer(p, libre, graineContinent, graineOcean, -Croute.IMPULSION_RIFT);
        taille[libre] = compte;
        taille[p] -= compte;
        nombreRuptures++;
    }

    private int trouverPlaqueLibre() {
        for (int p = 0; p < taille.length; p++) {
            if (taille[p] == 0) return p;
        }
        return -1;
    }

    private void fracturer(int p, int libre) {
        Sphere s = monde.sphere;
        cellules.clear();
        for (int i = 0; i < s.nombre; i++) {
            if (monde.plaque[i] == p) cellules.add(i);
        }

        int graineA = cellules.get(rng.nextInt(cellules.size));
        int graineB = graineA;
        float meilleur = 2f;
        for (int k = 0; k < 300; k++) {
            int essai = cellules.get(rng.nextInt(cellules.size));
            float proximite = s.x[graineA] * s.x[essai] + s.y[graineA] * s.y[essai] + s.z[graineA] * s.z[essai];
            if (proximite < meilleur) {
                meilleur = proximite;
                graineB = essai;
            }
        }

        tampon++;
        marque[graineA] = tampon;
        marque[graineB] = tampon;
        monde.plaque[graineB] = libre;
        frontiere.clear();
        frontiere.add(graineA);
        frontiere.add(graineB);
        while (frontiere.size > 0) {
            int depuis = frontiere.removeIndex(rng.nextInt(frontiere.size));
            for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                int voisin = s.voisin(depuis, k);
                if (monde.plaque[voisin] != p || marque[voisin] == tampon) continue;
                marque[voisin] = tampon;
                monde.plaque[voisin] = monde.plaque[depuis];
                frontiere.add(voisin);
            }
        }

        separer(p, libre, graineA, graineB, Croute.IMPULSION_RIFT);

        int compte = 0;
        for (int k = 0; k < cellules.size; k++) {
            if (monde.plaque[cellules.get(k)] == libre) compte++;
        }
        taille[libre] = compte;
        taille[p] -= compte;
        nombreFractures++;
    }

    private void separer(int p, int libre, int graineP, int graineLibre, float impulsion) {
        Sphere s = monde.sphere;
        Vector3 axe = new Vector3(s.x[graineP], s.y[graineP], s.z[graineP])
            .crs(s.x[graineLibre], s.y[graineLibre], s.z[graineLibre]).nor().scl(impulsion * seuilRotation);
        Plaque parente = monde.plaques.get(p);
        Plaque nouvelle = monde.plaques.get(libre);
        nouvelle.rotation.set(parente.rotation).add(axe);
        nouvelle.rotationAccumulee.setZero();
        parente.rotation.sub(axe);
    }

    private void souder(int a, int b) {
        Plaque plaqueA = monde.plaques.get(a);
        Plaque plaqueB = monde.plaques.get(b);
        float total = taille[a] + taille[b];
        plaqueA.rotation.scl(taille[a] / total).mulAdd(plaqueB.rotation, taille[b] / total);
        plaqueB.rotation.setZero();
        plaqueB.rotationAccumulee.setZero();

        for (int i = 0; i < monde.plaque.length; i++) {
            if (monde.plaque[i] == b) monde.plaque[i] = a;
        }
        taille[a] += taille[b];
        taille[b] = 0;
        nombreSoudures++;
    }

    private float vitesseRelative(int i, int a, int b, float dx, float dy, float dz) {
        Sphere s = monde.sphere;
        Vector3 wa = monde.plaques.get(a).rotation;
        Vector3 wb = monde.plaques.get(b).rotation;
        float wx = wa.x - wb.x, wy = wa.y - wb.y, wz = wa.z - wb.z;
        float vx = wy * s.z[i] - wz * s.y[i];
        float vy = wz * s.x[i] - wx * s.z[i];
        float vz = wx * s.y[i] - wy * s.x[i];
        return vx * dx + vy * dy + vz * dz;
    }

    private void ajouterCouple(int p, int i, float fx, float fy, float fz) {
        Sphere s = monde.sphere;
        couples[p * 3] += s.y[i] * fz - s.z[i] * fy;
        couples[p * 3 + 1] += s.z[i] * fx - s.x[i] * fz;
        couples[p * 3 + 2] += s.x[i] * fy - s.y[i] * fx;
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
        Arrays.fill(nouveauVolcan, 0f);
    }

    private void deplacerCases() {
        Sphere s = monde.sphere;
        for (int cible = 0; cible < s.nombre; cible++) {
            int nombreCandidats = 0;
            candidats[nombreCandidats++] = monde.plaque[cible];
            for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
                int p = monde.plaque[s.voisin(cible, k)];
                boolean dejaVu = false;
                for (int c = 0; c < nombreCandidats; c++) {
                    if (candidats[c] == p) dejaVu = true;
                }
                if (!dejaVu) candidats[nombreCandidats++] = p;
            }

            for (int c = 0; c < nombreCandidats; c++) {
                int p = candidats[c];
                int source = trouverSource(cible, p);
                if (monde.plaque[source] != p) continue;

                if (nouvellePlaque[cible] == -1) {
                    remplacerCible(source, cible);
                } else {
                    resoudreCollision(source, cible);
                }
            }
        }
    }

    private int trouverSource(int cible, int p) {
        if (!enMouvement[p]) return cible;

        Sphere s = monde.sphere;
        float[] m = matrices[p];
        float px = s.x[cible], py = s.y[cible], pz = s.z[cible];
        return s.plusProche(
            m[0] * px + m[1] * py + m[2] * pz,
            m[3] * px + m[4] * py + m[5] * pz,
            m[6] * px + m[7] * py + m[8] * pz);
    }

    private void resoudreCollision(int arrivant, int cible) {
        boolean arrivantContinental = monde.isContinentale[arrivant];
        boolean cibleContinentale = nouveauContinental[cible];

        if (arrivantContinental && cibleContinentale) {
            nouvelleEpaisseur[cible] += monde.epaisseur[arrivant] * Croute.GAIN_COLLISION;

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
        Sphere s = monde.sphere;
        for (int k = 0; k < arcs.size; k++) {
            int centre = arcs.get(k);
            int plaque = nouvellePlaque[centre];
            tampon++;
            marque[centre] = tampon;
            rang.clear();
            rang.add(centre);
            int debut = 0;
            for (int distance = 1; distance <= Croute.RAYON_ARC; distance++) {
                int fin = rang.size;
                float gain = Croute.GAIN_ARC * facteurCroissance * 0.5f * (1 - (float) distance / (Croute.RAYON_ARC + 1));
                for (int r = debut; r < fin; r++) {
                    int point = rang.get(r);
                    for (int v = 0; v < Sphere.NOMBRE_VOISINS; v++) {
                        int voisin = s.voisin(point, v);
                        if (marque[voisin] == tampon || nouvellePlaque[voisin] != plaque) continue;
                        marque[voisin] = tampon;
                        rang.add(voisin);
                        nouvelleEpaisseur[voisin] = Math.min(Croute.EPAISSEUR_MAX, nouvelleEpaisseur[voisin] + gain);
                        verifierSeuilContinental(voisin);
                    }
                }
                debut = fin;
            }
        }
        arcs.clear();
    }

    private void remplacerCible(int arrivant, int cible) {
        nouvellePlaque[cible] = monde.plaque[arrivant];
        nouvelleEpaisseur[cible] = monde.epaisseur[arrivant];
        nouveauContinental[cible] = monde.isContinentale[arrivant];
        nouvelAge[cible] = monde.age[arrivant] + 1f;
        nouveauVolcan[cible] = monde.volcan[arrivant] * Croute.DECLIN_VOLCAN;

    }

    private void verifierSeuilContinental(int cible) {
        if (!nouveauContinental[cible] && nouvelleEpaisseur[cible] >= Croute.SEUIL_CONTINENTAL) {
            nouveauContinental[cible] = true;
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
        Sphere s = monde.sphere;
        for (int i = 0; i < s.nombre; i++) {
            if (nouveauContinental[i]) continue;
            int plaque = nouvellePlaque[i];
            int terresProches = 0;
            for (int a = 0; a < Sphere.NOMBRE_VOISINS; a++) {
                int voisin = s.voisin(i, a);
                if (nouveauContinental[voisin] && nouvellePlaque[voisin] == plaque) terresProches++;
                for (int b = 0; b < Sphere.NOMBRE_VOISINS; b++) {
                    int loin = s.voisin(voisin, b);
                    if (nouveauContinental[loin] && nouvellePlaque[loin] == plaque) terresProches++;
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
        int depart = rng.nextInt(Sphere.NOMBRE_VOISINS);
        for (int k = 0; k < Sphere.NOMBRE_VOISINS; k++) {
            int voisin = monde.sphere.voisin(i, (depart + k) % Sphere.NOMBRE_VOISINS);
            if (nouvellePlaque[voisin] != -1) {
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
        System.arraycopy(nouveauVolcan, 0, monde.volcan, 0, monde.volcan.length);

    }

    
}
