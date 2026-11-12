package com.deldaran.politics;

import com.deldaran.politics.histoire.Population;
import java.util.ArrayList;
import java.util.List;

public final class Metiers {
    public static final int MAISON = 0, MAIRIE = 1, BOULANGERIE = 2, BOUCHERIE = 3, MOULIN = 4, GRENIER = 5, FORGE = 6, MENUISERIE = 7, TANNERIE = 8, TISSERAND = 9, POTIER = 10,
            AUBERGE = 11, TAVERNE = 12, ENTREPOT = 13, PECHERIE = 14, CASERNE = 15, ECOLE = 16, BAINS = 17, CHATEAU = 18, NOMBRE = 19;
    public static final int PLACE = 0, GRANDE_RUE = 1, RUE = 2, BORD = 3, RIVE = 4, HAUT = 5;
    public static final String[] NOMS = { "maison", "mairie", "boulangerie", "boucherie", "moulin", "grenier", "forge", "menuiserie", "tannerie", "tisserand", "potier",
            "auberge", "taverne", "entrepot", "pecherie", "caserne", "ecole", "bains", "chateau" };
    public static final int[] SEUIL = { 0, 150, 40, 120, 60, 150, 40, 100, 200, 150, 200, 80, 60, 200, 30, 900, 400, 1000, 0 };
    public static final int[] PAR = { 0, 0, 220, 350, 700, 600, 350, 500, 900, 600, 700, 1200, 350, 700, 500, 0, 0, 0, 0 };
    public static final int[] MAX = { 0, 1, 6, 4, 3, 3, 5, 3, 2, 3, 2, 2, 5, 3, 3, 1, 1, 1, 1 };
    public static final int[] LIEU = { RUE, PLACE, RUE, RUE, BORD, BORD, GRANDE_RUE, RUE, RIVE, RUE, RUE, GRANDE_RUE, PLACE, RIVE, RIVE, BORD, RUE, RUE, HAUT };
    public static final int[] ENSEIGNE = { -1, -1, 11, 2, -1, -1, 5, 9, 4, 13, 2, 11, 13, -1, 10, -1, -1, 10, -1 };
    public static final int[] DRAPEAU = { -1, 13, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 2, -1, -1, 2 };
    private static final int[] ORDRE = { CHATEAU, MAIRIE, CASERNE, AUBERGE, TAVERNE, BOULANGERIE, FORGE, MOULIN, BOUCHERIE, GRENIER, ENTREPOT, PECHERIE, MENUISERIE, TISSERAND, POTIER, TANNERIE, ECOLE, BAINS };

    public static class Profil {
        public int habitants, pic, age, peuple;
        public boolean capitale, eau, riviere;
        public float nourriture, bois, minerai, peche;
    }

    private Metiers() {
    }

    public static Profil profil(Population population, int v) {
        Profil p = new Profil();
        if (population == null) return p;
        p.habitants = population.villes.habitants[v];
        p.pic = population.villes.pic[v];
        p.age = population.annee - population.villes.fondation[v];
        p.peuple = population.villes.peuple[v];
        int region = population.villes.region[v];
        if (region >= 0 && region < population.regions.nombre) {
            p.nourriture = population.regions.nourriture[region];
            p.bois = population.regions.bois[region];
            p.minerai = population.regions.minerai[region];
            p.peche = population.regions.peche[region];
            int etat = population.etats.regionEtat[region];
            p.capitale = etat >= 0 && population.etats.estVivant(etat) && population.etats.capitale[etat] == v;
        }
        return p;
    }

    public static boolean permis(int m, Profil p) {
        switch (m) {
            case CHATEAU:
                return p.capitale;
            case CASERNE:
                return p.capitale || p.habitants >= SEUIL[CASERNE];
            case ENTREPOT:
                return p.eau;
            case PECHERIE:
                return p.eau && p.peche > 0f;
            case MENUISERIE:
                return p.bois > 0f;
            default:
                return true;
        }
    }

    public static int[] besoins(Profil p) {
        List<Integer> liste = new ArrayList<>();
        for (int m : ORDRE) {
            if (!permis(m, p)) continue;
            if (m == CASERNE && p.capitale) {
                liste.add(m);
                continue;
            }
            if (p.habitants < SEUIL[m]) continue;
            int par = PAR[m];
            if (m == FORGE && p.minerai > 0f) par /= 2;
            int nombre = par == 0 ? 1 : 1 + (p.habitants - SEUIL[m]) / par;
            nombre = Math.min(nombre, MAX[m]);
            for (int k = 0; k < nombre; k++) liste.add(m);
        }
        int[] t = new int[liste.size()];
        for (int k = 0; k < t.length; k++) t[k] = liste.get(k);
        return t;
    }

    public static int etals(Profil p) {
        if (p.habitants < 120) return 0;
        return Math.min(10, 3 + p.habitants / 200);
    }
}
