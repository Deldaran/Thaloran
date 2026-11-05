package com.deldaran.politics.planet;

import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.IntArray;

public class GenerateurMonde {
    public int nombrePlaques = 25;
    public float distanceMinGraines = 0.45f;
    public int nombreCratons = 5;
    public int tailleCratonMin = 200;
    public int tailleCratonMax = 600;
    public float vitesseMin = 0.2f;
    public float vitesseMax = 1.0f;
    public float profondeurOcean = 3f;

    private static final int[] DX = { 1, -1, 0, 0 };
    private static final int[] DY = { 0, 0, 1, -1 };

    private Monde monde;
    private RandomXS128 rng;

    public Monde genererMonde(int largeur, int hauteur, long seed) {
        monde = new Monde(largeur, hauteur, seed);
        rng = new RandomXS128(seed);

        remplirOcean();
        IntArray graines = placerGraines();
        faireGrandirPlaques(graines);
        donnerVitessesEtCouleurs();
        creerCratons();
        initialiserEau();
        return monde;
    }

    private void remplirOcean() {
        for (int i = 0; i < monde.plaque.length; i++) {
            monde.plaque[i] = -1;
            monde.epaisseur[i] = Croute.EPAISSEUR_OCEANIQUE;
            monde.isContinentale[i] = false;
            monde.age[i] = 0;
        }
    }

    private IntArray placerGraines() {
        IntArray graines = new IntArray();
        float distMinCarree = distanceMinGraines * distanceMinGraines;
        int essais = 0;
        while (graines.size < nombrePlaques && essais < 10000) {
            essais++;
            int index = rng.nextInt(monde.plaque.length);

            boolean tropProche = false;
            for (int k = 0; k < graines.size; k++) {
                if (distanceCarree(index, graines.get(k)) < distMinCarree) {
                    tropProche = true;
                    break;
                }
            }
            if (tropProche) continue;

            int id = monde.plaques.size;
            monde.plaques.add(new Plaque(id));
            monde.plaque[index] = id;
            graines.add(index);
        }
        return graines;
    }

    private float distanceCarree(int i1, int i2) {
        Sphere s = monde.sphere;
        float dx = s.x[i1] - s.x[i2];
        float dy = s.y[i1] - s.y[i2];
        float dz = s.z[i1] - s.z[i2];
        return dx * dx + dy * dy + dz * dz;
    }

    private void faireGrandirPlaques(IntArray graines) {
        IntArray frontiere = new IntArray(graines);
        croissanceAleatoire(frontiere, Integer.MAX_VALUE, new Regle() {
            @Override
            public boolean peutPrendre(int depuis, int voisin) {
                return monde.plaque[voisin] == -1;
            }

            @Override
            public void prendre(int depuis, int voisin) {
                monde.plaque[voisin] = monde.plaque[depuis];
            }
        });
    }

    private void donnerVitessesEtCouleurs() {
        float espacement = (float) Math.sqrt(4.0 * Math.PI / monde.plaque.length);
        for (Plaque plaque : monde.plaques) {
            float v = vitesseMin + rng.nextFloat() * (vitesseMax - vitesseMin);
            float hauteur = rng.nextFloat() * 2f - 1f;
            float angle = rng.nextFloat() * 6.2831855f;
            float rayon = (float) Math.sqrt(1f - hauteur * hauteur);
            plaque.rotation.set(rayon * (float) Math.cos(angle), rayon * (float) Math.sin(angle), hauteur).scl(v * espacement);
            plaque.couleur.fromHsv(rng.nextFloat() * 360f, 0.6f, 0.9f);
            plaque.couleur.a = 1f;
        }
    }

    private void creerCratons() {
        Regle regleCraton = new Regle() {
            @Override
            public boolean peutPrendre(int depuis, int voisin) {
                return !monde.isContinentale[voisin];
            }

            @Override
            public void prendre(int depuis, int voisin) {
                rendreContinentale(voisin);
            }
        };

        for (int c = 0; c < nombreCratons; c++) {
            int depart = rng.nextInt(monde.plaque.length);
            rendreContinentale(depart);

            int taille = tailleCratonMin + rng.nextInt(tailleCratonMax - tailleCratonMin + 1);
            IntArray frontiere = new IntArray();
            frontiere.add(depart);
            croissanceAleatoire(frontiere, taille - 1, regleCraton);
        }
    }

    private void rendreContinentale(int i) {
        monde.isContinentale[i] = true;
        monde.epaisseur[i] = Croute.EPAISSEUR_CONTINENTALE;
    }

    private void initialiserEau() {
        monde.calculerToutesAltitudes();

        float fond = Float.MAX_VALUE;
        for (int i = 0; i < monde.altitude.length; i++) {
            fond = Math.min(fond, monde.altitude[i]);
        }
        monde.niveauMer = fond + profondeurOcean;

        float volume = 0;
        for (int i = 0; i < monde.altitude.length; i++) {
            volume += Math.max(0f, monde.niveauMer - monde.altitude[i]);
        }
        monde.volumeEau = volume;
    }

    private interface Regle {
        boolean peutPrendre(int depuis, int voisin);
        void prendre(int depuis, int voisin);
    }

    private int croissanceAleatoire(IntArray frontiere, int maxCases, Regle regle) {
        int prises = 0;
        while (frontiere.size > 0 && prises < maxCases) {
            int depuis = frontiere.removeIndex(rng.nextInt(frontiere.size));

            for (int k = 0; k < Sphere.NOMBRE_VOISINS && prises < maxCases; k++) {
                int voisin = monde.sphere.voisin(depuis, k);
                if (regle.peutPrendre(depuis, voisin)) {
                    regle.prendre(depuis, voisin);
                    frontiere.add(voisin);
                    prises++;
                }
            }
        }
        return prises;
    }

}
