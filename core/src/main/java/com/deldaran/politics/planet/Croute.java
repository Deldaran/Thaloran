package com.deldaran.politics.planet;

public final class Croute {
    private Croute() {}

    public static final float EPAISSEUR_OCEANIQUE = 7f;
    public static final float EPAISSEUR_CONTINENTALE = 35f;

    public static final float DENSITE_OCEANIQUE    = 3.0f;
    public static final float DENSITE_CONTINENTALE = 2.7f;
    public static final float DENSITE_MANTEAU      = 3.3f;

    public static final float EPAISSEUR_MAX = 70f;
    public static final float SEUIL_CONTINENTAL = 20f;
    public static final int RAYON_ARC = 4;
    public static final float GAIN_ELEVATION = 0.2f;

    public static final int RAYON_FUSION = 2;
    public static final int VOISINS_FUSION = 15;
    public static final float GAIN_FUSION = 3f;
    public static final int PERIODE_FUSION = 5;
    
    public static final float EROSION = 0.1f;
    public static final float PENTE_MIN = 0.3f;
    public static final float EPAISSEUR_PAR_KM = 5.5f;
    public static final float FROTTEMENT_CONTINENTS = 0.01f;
    public static final float PART_CONTINENTS_CIBLE = 0.35f;

    public static final float GAIN_ARC = 0.8f;
    public static final float GAIN_COLLISION = 0.6f;

    public static final float FORCE_SLAB = 1f;
    public static final float FORCE_DORSALE = 0.15f;
    public static final float FORCE_COLLISION = 6f;
    public static final float AGE_SLAB_MAX = 100f;
    public static final float TRAINEE_OCEAN = 0.02f;
    public static final float TRAINEE_CONTINENT = 0.06f;
    public static final float VITESSE_MAX = 1.5f;

    public static final int CONTACT_SOUDURE = 60;
    public static final int DUREE_SOUDURE = 30;
    public static final float TAILLE_FRACTURE = 0.15f;
    public static final float PROBA_FRACTURE = 0.005f;
    public static final float IMPULSION_RIFT = 0.3f;
    public static final float AGE_RUPTURE = 70f;
    public static final float PROBA_RUPTURE = 0.05f;
    public static final int TAILLE_RUPTURE_MIN = 1500;

    public static final float SOULEVEMENT_DORSALE = 3f;
    public static final float SUBSIDENCE = 0.35f;
    public static final float SUBSIDENCE_MAX = 3.9f;
    public static final float PROFONDEUR_FOSSE = 3f;
    public static final int NOMBRE_POINTS_CHAUDS = 10;
    public static final float GAIN_VOLCAN = 3f;
    public static final float HAUTEUR_VOLCAN_MAX = 8f;
    public static final float DECLIN_VOLCAN = 0.98f;
    public static final float PROBA_DEPLACEMENT_POINT_CHAUD = 0.003f;
    public static final float RECUL_COTES = 0.004f;
    public static final float AGE_RECUL = 20f;
    public static final float ACCELERATION_MAX = 0.1f;
    public static final int TAILLE_INERTIE = 5000;

}
