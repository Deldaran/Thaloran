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
    public static final float GAIN_ARC = 0.5f;
    public static final float GAIN_COLLISION = 0.25f;
    public static final int RAYON_ARC = 4;
    public static final float GAIN_ELEVATION = 0.2f;

    public static final int RAYON_FUSION = 2;
    public static final int VOISINS_FUSION = 9;
    public static final float GAIN_FUSION = 3f;
    public static final int PERIODE_FUSION = 5;
    
    public static final float EROSION = 0.1f;
    public static final float PENTE_MIN = 0.3f;
    public static final float EPAISSEUR_PAR_KM = 5.5f;
    public static final float FROTTEMENT_CONTINENTS = 0.01f;
    public static final float PART_CONTINENTS_CIBLE = 0.35f;



}
