package com.deldaran.politics.histoire;

import com.deldaran.politics.Metiers;

/**
 * Lot G4 : le métier des habitants de la Foule.
 *
 * La Foule ne garde pas de métier pour chacun : on le tire de l'id de l'habitant et de sa ville, à partir des ateliers de
 * l'économie de la ville (plus un atelier a d'ouvriers, plus il a de chances d'être le sien). Ceux qui ne travaillent pas dans un
 * atelier vont aux champs, aux bêtes ou au bois. Le tirage ne dépend que de l'id : le même habitant garde le même métier, tant que
 * l'atelier existe. Lot H1 : le lieu de travail et la compétence qu'il demande servent à la fiche de chaque habitant (Gens).
 */
final class Emplois {
    /** La part des adultes d'une ville qui travaillent la terre, au minimum, même si les ateliers emploient beaucoup. */
    static final float CHAMPS_MIN = 0.35f;
    /** La part des habitants qui sont en âge de travailler. */
    static final float ADULTES = 0.6f;

    private Emplois() {
    }

    private static long graine(int ville, long id) {
        return Travail.melanger(id * 2654435761L + ville * 40503L);
    }

    /** L'atelier du joueur où cet habitant travaille, ou null. */
    static Entreprise.Atelier employeur(Joueur j, long id) {
        for (Entreprise.Atelier x : j.entreprise().ateliers) {
            for (Affaires.Employe o : x.gens) if (o.graine == id) return x;
        }
        return null;
    }

    /** Lot H1 : le lieu où travaille cet habitant (un atelier, la place, les champs, la forêt, la mine, les routes). */
    static int lieu(Joueur j, Population p, int ville, long id) {
        Entreprise.Atelier x = employeur(j, id);
        if (x != null) return x.lieu;
        Integer fixe = j.gens() == null ? null : j.gens().lieuFixe(id);
        if (fixe != null) return fixe;
        if (ville < 0 || !p.villes.estVivante(ville)) return Biens.CHAMPS;
        EconomieVille e = j.marche().ville(ville, j.jour());
        float ateliers = 0f;
        for (EconomieVille.Agent a : e.agents) if (!a.joueur) ateliers += Math.max(0f, a.ouvriers);
        float adultes = Math.max(1f, p.villes.habitants[ville] * ADULTES);
        float champs = Math.max(adultes * CHAMPS_MIN, adultes - ateliers);
        double u = (graine(ville, id) >>> 11) / (double) (1L << 53) * (champs + ateliers);
        if (u >= champs) {
            u -= champs;
            for (EconomieVille.Agent a : e.agents) {
                if (a.joueur || a.ouvriers <= 0f) continue;
                if (u < a.ouvriers) return a.lieu;
                u -= a.ouvriers;
            }
        }
        return Biens.CHAMPS;
    }

    /** « travaille chez le forgeron », « travaille aux champs »… */
    static String metier(Joueur j, Population p, int ville, long id) {
        Entreprise.Atelier x = employeur(j, id);
        if (x != null) return "travaille dans ton atelier (" + Biens.nomBoutique(x.lieu).toLowerCase() + ")";
        return phrase(lieu(j, p, ville, id), graine(ville, id));
    }

    /** Lot H1 : la compétence que demande le travail en ce lieu. */
    static int competence(int lieu) {
        switch (lieu) {
            case Metiers.FORGE: return Fiche.FORGE;
            case Metiers.MENUISERIE: return Fiche.BOIS;
            case Metiers.BOULANGERIE: case Metiers.BOUCHERIE: case Metiers.TANNERIE: case Metiers.TISSERAND: case Metiers.POTIER: return Fiche.METIERS_FINS;
            case Metiers.MOULIN: case Metiers.GRENIER: case Metiers.ENTREPOT: case Metiers.MAIRIE: return Fiche.INTENDANCE;
            case Metiers.AUBERGE: case Metiers.TAVERNE: case Biens.MARCHE: case Biens.DEHORS: return Fiche.COMMERCE;
            case Metiers.ECOLE: return Fiche.LETTRES;
            case Metiers.PECHERIE: return Fiche.SURVIE;
            case Metiers.CASERNE: case Metiers.CHATEAU: return Fiche.HAST;
            case Metiers.BAINS: return Fiche.MEDECINE;
            default: return Fiche.LABEUR;
        }
    }

    /** Ce que fait celui qui travaille en ce lieu de l'économie (champs, forêt, mine, colporteurs, place, ateliers). */
    static String phrase(int lieu, long g) {
        switch (lieu) {
            case Biens.CHAMPS: return ((g >>> 5) & 3L) == 3L ? "garde les bêtes" : "travaille aux champs";
            case Biens.FORET: return "coupe du bois";
            case Biens.MINE: return "travaille à la mine";
            case Biens.DEHORS: return "court les routes avec les colporteurs";
            case Biens.MARCHE: return "tient un étal sur la place";
            default:
                String role = Missions.role(lieu);
                if (!role.startsWith("un artisan")) return "travaille chez " + role;
                String n = Biens.nomBoutique(lieu).toLowerCase();
                return "travaille " + (n.startsWith("le ") ? "au " + n.substring(3) : n.startsWith("les ") ? "aux " + n.substring(4) : "à " + n);
        }
    }

    /** Le métier d'un père ou d'une mère, tiré de l'origine choisie à la création (« forgeron », « paysanne ») ; null s'il n'y en a pas. */
    static String parent(int origine, int sexe) {
        String[][] m = Creation.METIERS_PARENTS;
        if (origine < 0 || origine >= m.length || m[origine] == null) return null;
        return m[origine][sexe == 1 ? 1 : 0];
    }
}
